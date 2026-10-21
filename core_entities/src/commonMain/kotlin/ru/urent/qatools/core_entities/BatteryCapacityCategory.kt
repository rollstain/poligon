package ru.urent.qatools.core_entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class BatteryCapacityCategory {
    @SerialName("NotSet")
    NOT_SET,

    @SerialName("Normal")
    NORMAL,

    @SerialName("Increased")
    INCREASED,
}
