package ru.urent.qatools.data_simulator.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SimulatorStatusResponse(
    @SerialName("Ping")
    val pingSeconds: Int?,
)
