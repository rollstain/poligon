package ru.urent.qatools.data_bikes.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DisabledReasonsResponse(
    @SerialName("entries")
    val entries: List<Entry?>?,
) {
    @Serializable
    data class Entry(
        @SerialName("id")
        val id: String?,
        @SerialName("name")
        val name: String?,
        @SerialName("description")
        val description: String?,
        @SerialName("isHidden")
        val isHidden: Boolean?,
    )
}
