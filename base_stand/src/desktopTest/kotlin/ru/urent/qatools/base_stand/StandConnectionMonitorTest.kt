package ru.urent.qatools.base_stand

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import ru.urent.qatools.testing_stand.RecordingLog

class StandConnectionMonitorTest {
    private val log = RecordingLog()

    @Test
    fun lostVpnIsShownUntilStandLetsInAgain() = runTest {
        val stand = FakeStand(listOf(HttpStatusCode.Forbidden, HttpStatusCode.Forbidden, HttpStatusCode.OK))
        val monitor = monitor(this, stand)
        val states = async { monitor.state.take(STATES_UNTIL_RECOVERY).toList() }
        val reconnection = async { monitor.reconnections.first() }
        runCurrent()

        monitor.onStandRejected(STAND_ORIGIN)

        assertEquals(
            listOf(StandConnectionState.CONNECTED, StandConnectionState.VPN_REQUIRED, StandConnectionState.CONNECTED),
            states.await(),
        )
        reconnection.await()
        assertEquals(3, stand.probes)
    }

    @Test
    fun silentStandIsShownAsUnreachable() = runTest {
        val stand = FakeStand(listOf(null, HttpStatusCode.OK))
        val monitor = monitor(this, stand)
        val states = async { monitor.state.take(STATES_UNTIL_RECOVERY).toList() }
        runCurrent()

        monitor.onStandRejected(STAND_ORIGIN)

        assertEquals(
            listOf(StandConnectionState.CONNECTED, StandConnectionState.UNREACHABLE, StandConnectionState.CONNECTED),
            states.await(),
        )
    }

    @Test
    fun rejectionFromOpenStandShowsNothing() = runTest {
        val stand = FakeStand(listOf(HttpStatusCode.OK))
        val monitor = monitor(this, stand)
        val reconnections = mutableListOf<Unit>()
        val collector = launch { monitor.reconnections.toList(reconnections) }
        runCurrent()

        monitor.onStandRejected(STAND_ORIGIN)
        advanceUntilIdle()
        collector.cancel()

        assertEquals(1, stand.probes)
        assertEquals(StandConnectionState.CONNECTED, monitor.state.value)
        assertEquals(emptyList(), reconnections)
        assertEquals(emptyList(), log.lines)
    }

    private fun monitor(test: TestScope, stand: FakeStand) = StandConnectionMonitor(
        createProbeClient = { stand.client(StandardTestDispatcher(test.testScheduler)) },
        scope = test,
        log = log,
    )

    private class FakeStand(private val configAnswers: List<HttpStatusCode?>) {
        var probes = 0
            private set

        fun client(dispatcher: CoroutineDispatcher) = HttpClient(
            MockEngine(
                MockEngineConfig().apply {
                    this.dispatcher = dispatcher
                    addHandler {
                        val answer = configAnswers[minOf(probes, configAnswers.lastIndex)]
                        probes += 1
                        if (answer == null) throw IOException("Нет ответа")
                        respond("", answer)
                    }
                },
            ),
        )
    }

    private companion object {
        const val STAND_ORIGIN = "https://stand.test:443"
        const val STATES_UNTIL_RECOVERY = 3
    }
}
