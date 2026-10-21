package ru.urent.qatools.core_network.impl

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import ru.urent.qatools.core_network.UnexpectedResponseException
import ru.urent.qatools.core_network.StandUnavailableException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import ru.urent.qatools.testing_stand.RecordingLog
import ru.urent.qatools.testing_stand.TestEngineFactory

class ReconnectingHttpClientTest {
    private val log = RecordingLog()

    @Test
    fun requestAfterRejectionGoesThroughNewConnection() = runTest {
        val network = FakeNetwork(firstConnectionAnswer = HttpStatusCode.Forbidden)
        val connection = standConnection(network)

        assertFailsWith<UnexpectedResponseException> { connection.client.get(STAND_URL) }
        val retried = connection.client.get(STAND_URL)

        assertEquals(HttpStatusCode.OK, retried.status)
        assertEquals(2, network.openedConnections)
    }

    @Test
    fun requestAfterBrokenConnectionGoesThroughNewConnection() = runTest {
        val network = FakeNetwork(firstConnectionAnswer = null)
        val connection = standConnection(network)

        assertFailsWith<StandUnavailableException> { connection.client.get(STAND_URL) }
        val retried = connection.client.get(STAND_URL)

        assertEquals(HttpStatusCode.OK, retried.status)
        assertEquals(2, network.openedConnections)
    }

    @Test
    fun acceptedRequestKeepsConnection() = runTest {
        val network = FakeNetwork(firstConnectionAnswer = HttpStatusCode.OK)
        val connection = standConnection(network)

        connection.client.get(STAND_URL)
        connection.client.get(STAND_URL)

        assertEquals(1, network.openedConnections)
    }

    private fun standConnection(network: FakeNetwork) =
        HttpClientFactoryImpl(
            engineFactory = TestEngineFactory { network.openConnection() },
            cachingEngineFactory = TestEngineFactory { network.openConnection() },
            log = log,
        ).createForStand(
            baseUrl = STAND_URL,
            accessToken = ACCESS_TOKEN,
            reauthenticate = { null },
            onSessionExpired = {},
            onRejected = {},
        )

    private class FakeNetwork(private val firstConnectionAnswer: HttpStatusCode?) {
        var openedConnections = 0
            private set

        fun openConnection(): MockEngine {
            val isFirstConnection = openedConnections == 0
            openedConnections += 1
            return MockEngine {
                val answer = if (isFirstConnection) firstConnectionAnswer else HttpStatusCode.OK
                if (answer == null) throw IOException("Соединение разорвано")
                respond("", answer)
            }
        }
    }

    private companion object {
        const val STAND_URL = "https://stand.test"
        const val ACCESS_TOKEN = "token"
    }
}
