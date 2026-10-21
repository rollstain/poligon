package ru.urent.qatools.data_location.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TechZonesResponse(
    @SerialName("entries")
    val entries: List<Entry?>?,
) {
    @Serializable
    data class Entry(
        @SerialName("id")
        val id: String?,
        @SerialName("name")
        val name: String?,
        @SerialName("areaId")
        val areaId: String?,
        @SerialName("disabled")
        val isDisabled: Boolean?,
        @SerialName("center")
        val center: CoordinateResponse?,
        @SerialName("coordinates")
        val coordinates: List<CoordinateResponse?>?,
    )
}
