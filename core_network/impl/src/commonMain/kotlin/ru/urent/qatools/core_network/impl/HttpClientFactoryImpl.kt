package ru.urent.qatools.core_network.impl

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.cache.storage.CacheStorage
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.hostWithPort
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.io.IOException
import ru.urent.qatools.core_logger.AppLog
import ru.urent.qatools.core_network.HttpClientFactory
import ru.urent.qatools.core_network.QaJson
import ru.urent.qatools.core_network.ReconnectingHttpClient
import ru.urent.qatools.core_network.SessionExpiredException
import ru.urent.qatools.core_network.StandUnavailableException
import ru.urent.qatools.core_network.UnexpectedResponseException

class HttpClientFactoryImpl(
    private val engineFactory: HttpClientEngineFactory<HttpClientEngineConfig>,
    private val cachingEngineFactory: HttpClientEngineFactory<HttpClientEngineConfig>,
    private val log: AppLog,
) : HttpClientFactory {

    override fun createPlain(): HttpClient = HttpClient(engineFactory) { configurePlain(this) }

    override fun createCaching(storage: CacheStorage): HttpClient = HttpClient(cachingEngineFactory) {
        configurePlain(this)
        install(HttpCache) { publicStorage(storage) }
    }

    override fun createStandFacing(onRejected: (standOrigin: String) -> Unit): ReconnectingHttpClient =
        ReconnectingHttpClient { reconnect ->
            HttpClient(engineFactory) {
                reportRejections(this) { standOrigin ->
                    reconnect()
                    onRejected(standOrigin)
                }
                configurePlain(this)
            }
        }

    override fun createForStand(
        baseUrl: String,
        accessToken: String,
        reauthenticate: suspend () -> String?,
        onSessionExpired: () -> Unit,
        onRejected: (standOrigin: String) -> Unit,
    ): ReconnectingHttpClient = ReconnectingHttpClient { reconnect ->
        HttpClient(engineFactory) {
            configureForStand(
                config = this,
                baseUrl = baseUrl,
                accessToken = accessToken,
                reauthenticate = reauthenticate,
                onSessionExpired = onSessionExpired,
                onRejected = { standOrigin ->
                    reconnect()
                    onRejected(standOrigin)
                },
            )
        }
    }

    private fun configurePlain(config: HttpClientConfig<*>) {
        config.expectSuccess = false
        config.install(ContentNegotiation) { json(QaJson) }
        config.install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
        }
        config.install(RequestLogging) { this.log = this@HttpClientFactoryImpl.log }
    }

    private fun configureForStand(
        config: HttpClientConfig<*>,
        baseUrl: String,
        accessToken: String,
        reauthenticate: suspend () -> String?,
        onSessionExpired: () -> Unit,
        onRejected: (standOrigin: String) -> Unit,
    ) {
        configurePlain(config)
        config.defaultRequest { url("$baseUrl/") }
        config.install(Auth) {
            bearer {
                loadTokens { BearerTokens(accessToken = accessToken, refreshToken = null) }
                refreshTokens {
                    val refreshedToken = reauthenticate() ?: return@refreshTokens null
                    BearerTokens(accessToken = refreshedToken, refreshToken = null)
                }
                sendWithoutRequest { true }
            }
        }
        config.HttpResponseValidator {
            validateResponse { response ->
                if (response.status == HttpStatusCode.Unauthorized) {
                    onSessionExpired()
                    throw SessionExpiredException()
                }
                if (response.status == HttpStatusCode.Forbidden) onRejected(originOf(response.call.request.url))
                if (!response.status.isSuccess()) {
                    throw UnexpectedResponseException(response.status.value, response.call.request.url.encodedPath)
                }
            }
            handleResponseExceptionWithRequest { cause, request ->
                if (cause !is IOException) return@handleResponseExceptionWithRequest
                onRejected(originOf(request.url))
                throw StandUnavailableException(cause)
            }
        }
    }

    private fun reportRejections(config: HttpClientConfig<*>, onRejected: (standOrigin: String) -> Unit) {
        config.HttpResponseValidator {
            validateResponse { response ->
                if (response.status == HttpStatusCode.Forbidden) onRejected(originOf(response.call.request.url))
            }
            handleResponseExceptionWithRequest { cause, request ->
                if (cause is IOException) onRejected(originOf(request.url))
            }
        }
    }

    private fun originOf(url: Url): String = "${url.protocol.name}://${url.hostWithPort}"

    private companion object {
        const val REQUEST_TIMEOUT_MS = 30_000L
        const val CONNECT_TIMEOUT_MS = 10_000L
    }
}
