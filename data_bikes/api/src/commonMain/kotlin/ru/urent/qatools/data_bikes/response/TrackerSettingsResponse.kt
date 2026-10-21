package ru.urent.qatools.data_bikes.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrackerSettingsResponse(
    @SerialName("minVoltage_OneBattery")
    val minVoltageOneBattery: Double?,
    @SerialName("maxVoltage_OneBattery")
    val maxVoltageOneBattery: Double?,
)
