package ru.urent.qatools.data_bikes.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DisableReasonRequest(
    @SerialName("ids")
    val ids: List<String>,
    @SerialName("reasonId")
    val reasonId: String,
    @SerialName("description")
    val description: String,
)
