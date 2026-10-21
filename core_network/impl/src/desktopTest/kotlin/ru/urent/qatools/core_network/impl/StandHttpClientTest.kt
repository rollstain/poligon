package ru.urent.qatools.core_network.impl

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import ru.urent.qatools.core_network.SessionExpiredException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import ru.urent.qatools.testing_stand.RecordingLog
import ru.urent.qatools.testing_stand.TestEngineFactory

class StandHttpClientTest {

    @Test
    fun expiredTokenIsReplacedBySavedPasswordLoginAndRequestIsRepeated() = runTest {
        val stand = FakeStand(StandardTestDispatcher(testScheduler))
        var reauthenticationCount = 0
        var isSessionExpired = false
        val client = stand.client(
            reauthenticate = {
                reauthenticationCount++
                FRESH_TOKEN
            },
            onSessionExpired = { isSessionExpired = true },
        )

        val body = client.get("bikes/api/ping").bodyAsText()

        assertEquals(OK_BODY, body)
        assertEquals(1, reauthenticationCount)
        assertFalse(isSessionExpired)
        assertEquals(listOf<String?>("Bearer $EXPIRED_TOKEN", "Bearer $FRESH_TOKEN"), stand.authorizationHeaders)
    }

    @Test
    fun withoutSavedPasswordSessionExpires() = runTest {
        val stand = FakeStand(StandardTestDispatcher(testScheduler))
        var isSessionExpired = false
        val client = stand.client(reauthenticate = { null }, onSessionExpired = { isSessionExpired = true })

        assertFailsWith<SessionExpiredException> { client.get("bikes/api/ping") }
        assertTrue(isSessionExpired)
    }

    private class FakeStand(dispatcher: CoroutineDispatcher) {
        val authorizationHeaders = mutableListOf<String?>()

        private val engine = MockEngine(
            MockEngineConfig().apply {
                this.dispatcher = dispatcher
                addHandler { request ->
                    val authorization = request.headers[HttpHeaders.Authorization]
                    authorizationHeaders.add(authorization)
                    if (authorization == "Bearer $FRESH_TOKEN") {
                        respond(OK_BODY, HttpStatusCode.OK)
                    } else {
                        respond("", HttpStatusCode.Unauthorized)
                    }
                }
            },
        )

        fun client(reauthenticate: suspend () -> String?, onSessionExpired: () -> Unit): HttpClient =
            HttpClientFactoryImpl(engineFactory = TestEngineFactory { engine }, cachingEngineFactory = TestEngineFactory { engine }, log = RecordingLog()).createForStand(
                baseUrl = BASE_URL,
                accessToken = EXPIRED_TOKEN,
                reauthenticate = reauthenticate,
                onSessionExpired = onSessionExpired,
                onRejected = {},
            ).client
    }

    private companion object {
        const val BASE_URL = "https://stand.test"
        const val EXPIRED_TOKEN = "expired-token"
        const val FRESH_TOKEN = "fresh-token"
        const val OK_BODY = "ok"
    }
}
