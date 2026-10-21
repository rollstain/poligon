package ru.urent.qatools.data_bikes.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrackerResponse(
    @SerialName("identifier")
    val identifier: String?,
    @SerialName("driver")
    val driver: String?,
    @SerialName("location")
    val location: CoordinateResponse?,
    @SerialName("telemetry")
    val telemetry: Telemetry?,
) {
    @Serializable
    data class Telemetry(
        @SerialName("telemetry")
        val values: Values?,
    )

    @Serializable
    data class Values(
        @SerialName("mainBatteryVoltage")
        val mainBatteryVoltage: Double?,
    )
}
