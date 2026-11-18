package ru.urent.qatools.data_opstask.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpsTaskConfigResponse(
    @SerialName("entries")
    val entries: List<Entry>?,
) {
    @Serializable
    data class Entry(
        @SerialName("name")
        val name: String?,
        @SerialName("data")
        val data: String?,
        @SerialName("defaultData")
        val defaultData: String?,
    )
}
