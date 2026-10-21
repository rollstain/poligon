package ru.urent.qatools.core_network.impl

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import ru.urent.qatools.core_network.SkipRequestLog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import ru.urent.qatools.testing_stand.RecordingLog
import ru.urent.qatools.testing_stand.TestEngineFactory

class RequestLoggingTest {
    private val log = RecordingLog()
    private val clientFactory = HttpClientFactoryImpl(
        engineFactory = TestEngineFactory { MockEngine { respond("", HttpStatusCode.OK) } },
        cachingEngineFactory = TestEngineFactory { MockEngine { respond("", HttpStatusCode.OK) } },
        log = log,
    )
    private val client = clientFactory.createPlain()

    @Test
    fun requestIsLoggedWithStatusAndSecretParametersMasked() = runTest {
        client.get("$BASE_URL$STATUS_PATH") {
            parameter(ID_PARAMETER, SIMULATOR_ID)
            parameter(API_KEY_PARAMETER, API_KEY)
        }

        val line = log.lines.single()
        assertTrue(line.startsWith("GET $HOST$STATUS_PATH?$API_KEY_PARAMETER=***&$ID_PARAMETER=$SIMULATOR_ID → 200"), line)
        assertTrue(API_KEY !in line, line)
    }

    @Test
    fun relativePathOfStandRequestIsLoggedAfterHost() = runTest {
        val standConnection = clientFactory.createForStand(
            baseUrl = BASE_URL,
            accessToken = ACCESS_TOKEN,
            reauthenticate = { null },
            onSessionExpired = {},
            onRejected = {},
        )

        standConnection.client.get(RELATIVE_PATH)

        assertTrue(log.lines.single().startsWith("GET $HOST/$RELATIVE_PATH → 200"), log.lines.single())
    }

    @Test
    fun requestMarkedToSkipIsNotLogged() = runTest {
        client.get("$BASE_URL$STATUS_PATH") { attributes.put(SkipRequestLog, Unit) }

        assertEquals(emptyList(), log.lines)
    }

    private companion object {
        const val HOST = "stand.test"
        const val BASE_URL = "https://$HOST"
        const val STATUS_PATH = "/simulator-signal/api/status"
        const val ID_PARAMETER = "id"
        const val SIMULATOR_ID = "4071"
        const val API_KEY_PARAMETER = "apiKey"
        const val API_KEY = "2gis-secret"
        const val ACCESS_TOKEN = "token"
        const val RELATIVE_PATH = "bikes/api/disabledreasons/allowed"
    }
}
