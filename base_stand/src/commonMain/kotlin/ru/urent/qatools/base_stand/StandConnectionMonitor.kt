package ru.urent.qatools.base_stand

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.io.IOException
import ru.urent.qatools.core_logger.AppLog
import ru.urent.qatools.core_network.SkipRequestLog
import ru.urent.qatools.core_network.StandRejectionListener

enum class StandConnectionState { CONNECTED, VPN_REQUIRED, UNREACHABLE }

class StandConnectionMonitor(
    private val createProbeClient: () -> HttpClient,
    private val scope: CoroutineScope,
    private val log: AppLog,
) : StandRejectionListener {
    private val mutableState = MutableStateFlow(StandConnectionState.CONNECTED)
    val state: StateFlow<StandConnectionState> = mutableState.asStateFlow()
    private val mutableReconnections = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val reconnections: SharedFlow<Unit> = mutableReconnections.asSharedFlow()
    private var probeJob: Job? = null

    override fun onStandRejected(standOrigin: String) {
        synchronized(this) {
            if (probeJob?.isActive == true) return
            probeJob = scope.launch { probeUntilConnected(standOrigin) }
        }
    }

    private suspend fun probeUntilConnected(standOrigin: String) {
        while (currentCoroutineContext().isActive) {
            val probed = probe(standOrigin)
            val previous = mutableState.value
            if (probed != previous) log.info(transitionMessage(probed))
            mutableState.value = probed
            if (probed == StandConnectionState.CONNECTED) {
                if (previous != StandConnectionState.CONNECTED) mutableReconnections.tryEmit(Unit)
                return
            }
            delay(PROBE_INTERVAL_MS.milliseconds)
        }
    }

    private suspend fun probe(standOrigin: String): StandConnectionState {
        val client = createProbeClient()
        return try {
            val response = client.get("$standOrigin$STAND_CONFIG_PATH") { attributes.put(SkipRequestLog, Unit) }
            if (response.status == HttpStatusCode.Forbidden) StandConnectionState.VPN_REQUIRED else StandConnectionState.CONNECTED
        } catch (exception: IOException) {
            StandConnectionState.UNREACHABLE
        } finally {
            client.close()
        }
    }

    private fun transitionMessage(state: StandConnectionState): String = when (state) {
        StandConnectionState.CONNECTED -> "Связь со стендом восстановлена"
        StandConnectionState.VPN_REQUIRED -> "Стенд закрыт без VPN — ждём переподключения"
        StandConnectionState.UNREACHABLE -> "Стенд не отвечает — ждём связи"
    }

    private companion object {
        const val PROBE_INTERVAL_MS = 3_000L
        const val STAND_CONFIG_PATH = "/admin/assets/config.json"
    }
}
