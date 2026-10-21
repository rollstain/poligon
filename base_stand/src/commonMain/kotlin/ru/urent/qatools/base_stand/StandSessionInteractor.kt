package ru.urent.qatools.base_stand

import io.ktor.http.URLParserException
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.urent.qatools.core_entities.RequestFailure
import ru.urent.qatools.core_entities.RequestResult
import ru.urent.qatools.core_entities.TileSource
import ru.urent.qatools.core_logger.AppLog
import ru.urent.qatools.core_network.HttpClientFactory
import ru.urent.qatools.core_platform.TokenCipher
import ru.urent.qatools.data_stand.StandRepository
import ru.urent.qatools.data_stand.response.StandConfigResponse

class StandSessionInteractor(
    private val repository: StandRepository,
    private val tokenCipher: TokenCipher,
    private val httpClientFactory: HttpClientFactory,
    private val connectionMonitor: StandConnectionMonitor,
    private val log: AppLog,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableSession = MutableStateFlow<StandSession?>(null)
    val session: StateFlow<StandSession?> = mutableSession.asStateFlow()
    private val mutableSavedStands = MutableStateFlow<List<Stand>>(emptyList())
    val savedStands: StateFlow<List<Stand>> = mutableSavedStands.asStateFlow()
    val connectionState: StateFlow<StandConnectionState> = connectionMonitor.state
    val reconnections: SharedFlow<Unit> = connectionMonitor.reconnections

    suspend fun refreshSavedStands(): List<Stand> {
        val stands = mutableListOf<Stand>()
        for (saved in repository.getSavedStands()) {
            val hasSavedLogin = saved.encryptedAccessToken != null || saved.encryptedPassword != null
            stands.add(Stand(baseUrl = saved.baseUrl, email = saved.email, hasSavedLogin = hasSavedLogin))
        }
        mutableSavedStands.value = stands
        return stands
    }

    suspend fun getSavedPassword(baseUrl: String): String? = decryptOrNull(repository.getSavedStand(baseUrl)?.encryptedPassword)

    suspend fun restore(baseUrl: String): LoginResult {
        val saved = repository.getSavedStand(baseUrl)
        val email = saved?.email ?: return LoginResult.SavedLoginExpired
        val config = when (val loaded = loadConfig(baseUrl)) {
            is ConfigLoad.Failed -> return loaded.result
            is ConfigLoad.Loaded -> loaded.config
        }
        val savedToken = decryptOrNull(saved.encryptedAccessToken)
        val tokenCheck = if (savedToken == null) null else repository.checkToken(baseUrl = baseUrl, accessToken = savedToken)
        if (tokenCheck is RequestResult.Error && tokenCheck.failure !is RequestFailure.Http) return loginFailureOf(tokenCheck.failure)
        val accessToken = if (savedToken != null && tokenCheck is RequestResult.Success) {
            savedToken
        } else {
            when (val token = loginWithSavedPassword(baseUrl = baseUrl, email = email, config = config)) {
                is RequestResult.Success -> token.data
                is RequestResult.Error -> {
                    if (token.failure != RequestFailure.InvalidCredentials) return loginFailureOf(token.failure)
                    repository.clearToken(baseUrl)
                    return LoginResult.SavedLoginExpired
                }
            }
        }
        openSession(baseUrl = baseUrl, email = email, config = config, accessToken = accessToken)
        refreshSavedStands()
        return LoginResult.Success
    }

    suspend fun login(rawBaseUrl: String, email: String, password: String, rememberPassword: Boolean): LoginResult {
        val baseUrlProblem = baseUrlProblemOf(rawBaseUrl)
        if (baseUrlProblem != null) return LoginResult.Misconfigured(baseUrlProblem)
        val baseUrl = normalizeBaseUrl(rawBaseUrl)
        val config = when (val loaded = loadConfig(baseUrl)) {
            is ConfigLoad.Failed -> return loaded.result
            is ConfigLoad.Loaded -> loaded.config
        }
        val accessToken = when (val token = requestToken(baseUrl = baseUrl, config = config, email = email, password = password)) {
            is RequestResult.Error -> return loginFailureOf(token.failure)
            is RequestResult.Success -> token.data
        }
        val encryptedPassword = if (rememberPassword) tokenCipher.encrypt(password.encodeToByteArray()) else null
        repository.saveLogin(
            baseUrl = baseUrl,
            email = email,
            encryptedAccessToken = tokenCipher.encrypt(accessToken.encodeToByteArray()),
            encryptedPassword = encryptedPassword,
        )
        openSession(baseUrl = baseUrl, email = email, config = config, accessToken = accessToken)
        refreshSavedStands()
        return LoginResult.Success
    }

    suspend fun switchTo(rawBaseUrl: String): LoginResult {
        val baseUrlProblem = baseUrlProblemOf(rawBaseUrl)
        if (baseUrlProblem != null) return LoginResult.Misconfigured(baseUrlProblem)
        val baseUrl = normalizeBaseUrl(rawBaseUrl)
        val current = mutableSession.value ?: return LoginResult.SessionExpired
        if (baseUrl == current.baseUrl) return LoginResult.Success
        val restored = restore(baseUrl)
        if (restored != LoginResult.SavedLoginExpired) return restored
        val password = getSavedPassword(current.baseUrl) ?: return LoginResult.SavedPasswordMissing
        return login(rawBaseUrl = baseUrl, email = current.email, password = password, rememberPassword = true)
    }

    suspend fun logout() {
        val current = mutableSession.value ?: return
        repository.clearAllCredentials()
        refreshSavedStands()
        log.info("Выход: сохранённые пароли и токены всех стендов стёрты")
        mutableSession.value = null
        current.connection.close()
    }

    suspend fun forget(baseUrl: String) {
        repository.deleteStand(baseUrl)
        refreshSavedStands()
        log.info("Стенд ${hostOf(baseUrl)} забыт вместе с паролем и токеном")
        val current = mutableSession.value
        if (current?.baseUrl != baseUrl) return
        mutableSession.value = null
        current.connection.close()
    }

    private fun openSession(baseUrl: String, email: String, config: StandConfig, accessToken: String) {
        val previous = mutableSession.value
        val connection = httpClientFactory.createForStand(
            baseUrl = baseUrl,
            accessToken = accessToken,
            reauthenticate = {
                val token = loginWithSavedPassword(baseUrl = baseUrl, email = email, config = config)
                (token as? RequestResult.Success)?.data
            },
            onSessionExpired = { expireSession(baseUrl) },
            onRejected = connectionMonitor::onStandRejected,
        )
        log.info("Сессия открыта: ${hostOf(baseUrl)}")
        mutableSession.value = StandSession(baseUrl = baseUrl, email = email, config = config, connection = connection)
        previous?.connection?.close()
    }

    private suspend fun loginWithSavedPassword(baseUrl: String, email: String, config: StandConfig): RequestResult<String> {
        val password = getSavedPassword(baseUrl) ?: return RequestResult.Error(RequestFailure.InvalidCredentials)
        val token = requestToken(baseUrl = baseUrl, config = config, email = email, password = password)
        if (token is RequestResult.Success) {
            repository.saveToken(baseUrl = baseUrl, encryptedAccessToken = tokenCipher.encrypt(token.data.encodeToByteArray()))
        }
        if (token is RequestResult.Error && token.failure == RequestFailure.InvalidCredentials) {
            repository.clearCredentials(baseUrl)
            log.info("Сохранённый пароль для ${hostOf(baseUrl)} не подошёл и стёрт")
        }
        return token
    }

    private fun expireSession(baseUrl: String) {
        scope.launch {
            repository.clearToken(baseUrl)
            log.info("Сессия ${hostOf(baseUrl)} истекла")
            if (mutableSession.value?.baseUrl == baseUrl) mutableSession.value = null
        }
    }

    private suspend fun loadConfig(baseUrl: String): ConfigLoad {
        val response = when (val loaded = repository.getConfig(baseUrl)) {
            is RequestResult.Error -> {
                val failure = loaded.failure
                val isStandClosedToThisNetwork = failure is RequestFailure.Http && failure.statusCode == FORBIDDEN_STATUS_CODE
                return ConfigLoad.Failed(if (isStandClosedToThisNetwork) LoginResult.VpnRequired else loginFailureOf(failure))
            }
            is RequestResult.Success -> loaded.data
        }
        val clientId = response.auth?.clientId
        val clientSecret = response.auth?.clientSecret
        val scope = response.auth?.scope
        if (clientId == null || clientSecret == null || scope == null) {
            return ConfigLoad.Failed(LoginResult.Misconfigured("В config.json стенда нет настроек входа"))
        }
        val config = StandConfig(
            clientId = clientId,
            clientSecret = clientSecret,
            scope = scope,
            tileSource = tileSourceOf(response.api?.tileServers?.get(PREFERRED_TILE_SERVER)),
        )
        return ConfigLoad.Loaded(config)
    }

    private fun tileSourceOf(server: StandConfigResponse.TileServer?): TileSource? {
        val urlTemplate = server?.tileServerUrl
        if (urlTemplate.isNullOrBlank()) return null
        val subdomains = server.subdomains.orEmpty().split(SUBDOMAIN_SEPARATOR).filter { it.isNotBlank() }
        return TileSource(urlTemplate = urlTemplate, subdomains = subdomains, urlSuffix = server.apiKey.orEmpty())
    }

    private suspend fun requestToken(baseUrl: String, config: StandConfig, email: String, password: String): RequestResult<String> {
        val token = repository.requestToken(
            baseUrl = baseUrl,
            clientId = config.clientId,
            clientSecret = config.clientSecret,
            scope = config.scope,
            email = email,
            password = password,
        )
        return when (token) {
            is RequestResult.Error -> token
            is RequestResult.Success -> {
                val accessToken = token.data.accessToken
                if (accessToken == null) RequestResult.Error(RequestFailure.InvalidResponse) else RequestResult.Success(accessToken)
            }
        }
    }

    private fun loginFailureOf(failure: RequestFailure): LoginResult = when (failure) {
        RequestFailure.NoConnection -> LoginResult.Unavailable
        RequestFailure.SessionExpired -> LoginResult.SessionExpired
        RequestFailure.InvalidCredentials -> LoginResult.InvalidCredentials
        RequestFailure.InvalidResponse -> LoginResult.Unexpected(statusCode = null)
        is RequestFailure.Http -> LoginResult.Unexpected(statusCode = failure.statusCode)
    }

    private fun baseUrlProblemOf(rawBaseUrl: String): String? {
        val url = try {
            Url(withScheme(rawBaseUrl))
        } catch (exception: URLParserException) {
            return UNPARSABLE_BASE_URL_MESSAGE
        }
        if (url.protocol != URLProtocol.HTTPS) return "Нужен адрес стенда с https://"
        if (url.host.isBlank()) return UNPARSABLE_BASE_URL_MESSAGE
        return null
    }

    private fun normalizeBaseUrl(rawBaseUrl: String): String = "$HTTPS_PREFIX${Url(withScheme(rawBaseUrl)).host}"

    private fun withScheme(rawBaseUrl: String): String {
        val trimmed = rawBaseUrl.trim().trimEnd('/')
        return if (trimmed.contains(SCHEME_SEPARATOR)) trimmed else "$HTTPS_PREFIX$trimmed"
    }

    private fun hostOf(baseUrl: String): String = baseUrl.substringAfter(SCHEME_SEPARATOR)

    private fun decryptOrNull(encrypted: ByteArray?): String? =
        if (encrypted == null) null else tokenCipher.decrypt(encrypted).decodeToString()

    private sealed interface ConfigLoad {
        data class Loaded(val config: StandConfig) : ConfigLoad

        data class Failed(val result: LoginResult) : ConfigLoad
    }

    private companion object {
        const val HTTPS_PREFIX = "https://"
        const val SCHEME_SEPARATOR = "://"
        const val PREFERRED_TILE_SERVER = "2gis"
        const val SUBDOMAIN_SEPARATOR = ","
        const val FORBIDDEN_STATUS_CODE = 403
        const val UNPARSABLE_BASE_URL_MESSAGE = "Не получилось разобрать адрес стенда"
    }
}
