package ru.urent.qatools.data_opstask.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TechZoneSettingsResponse(
    @SerialName("entries")
    val entries: List<Entry>?,
    @SerialName("totalPages")
    val totalPages: Int?,
) {
    @Serializable
    data class Entry(
        @SerialName("zoneId")
        val zoneId: String?,
        @SerialName("areaId")
        val areaId: String?,
        @SerialName("vehicleBatteryPercent")
        val vehicleBatteryPercent: Int?,
        @SerialName("isEnabled")
        val isEnabled: Boolean?,
    )
}
