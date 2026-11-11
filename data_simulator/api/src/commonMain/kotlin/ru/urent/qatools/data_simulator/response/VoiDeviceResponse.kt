package ru.urent.qatools.data_simulator.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VoiDeviceResponse(
    @SerialName("lockSendIntervalMSec")
    val lockedReportIntervalMs: Int?,
    @SerialName("unlockSendIntervalMSec")
    val unlockedReportIntervalMs: Int?,
)
