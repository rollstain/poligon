package ru.urent.qatools.core_entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TaskPriority {
    @SerialName("Critical")
    CRITICAL,

    @SerialName("Burning")
    BURNING,

    @SerialName("High")
    HIGH,

    @SerialName("Normal")
    NORMAL,
}
