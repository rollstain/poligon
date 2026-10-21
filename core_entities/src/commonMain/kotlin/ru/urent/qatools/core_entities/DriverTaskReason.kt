package ru.urent.qatools.core_entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class DriverTaskReason {
    @SerialName("NeedRepair")
    NEED_REPAIR,

    @SerialName("ReadyForUse")
    READY_FOR_USE,

    @SerialName("OutOfZone")
    OUT_OF_ZONE,

    @SerialName("Idle")
    IDLE,
}
