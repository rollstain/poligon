package ru.urent.qatools.data_location.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.urent.qatools.data_location.ZoneType

@Serializable
data class ParkingListResponse(
    @SerialName("entries")
    val entries: List<Entry?>?,
    @SerialName("totalPages")
    val totalPages: Int?,
) {
    @Serializable
    data class Entry(
        @SerialName("id")
        val id: String?,
        @SerialName("name")
        val name: String?,
        @SerialName("isDisabled")
        val isDisabled: Boolean?,
        @SerialName("center")
        val center: CoordinateResponse?,
        @SerialName("coordinates")
        val coordinates: List<CoordinateResponse?>?,
        @SerialName("type")
        val type: ZoneType?,
        @SerialName("radius")
        val radius: Double?,
    )
}
