package ru.urent.qatools.data_location.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CitiesResponse(
    @SerialName("entries")
    val entries: List<Entry?>?,
) {
    @Serializable
    data class Entry(
        @SerialName("id")
        val id: String?,
        @SerialName("name")
        val name: String?,
        @SerialName("isCity")
        val isCity: Boolean?,
        @SerialName("center")
        val center: CoordinateResponse?,
    )
}
