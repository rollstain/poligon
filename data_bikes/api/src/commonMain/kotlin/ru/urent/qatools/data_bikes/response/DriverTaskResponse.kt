package ru.urent.qatools.data_bikes.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.urent.qatools.core_entities.DriverTaskReason

@Serializable
data class DriverTaskResponse(
    @SerialName("id")
    val id: String?,
    @SerialName("reason")
    val reason: DriverTaskReason?,
)
