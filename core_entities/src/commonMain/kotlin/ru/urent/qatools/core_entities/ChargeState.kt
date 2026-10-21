package ru.urent.qatools.core_entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ChargeState {
    @SerialName("Empty")
    EMPTY,

    @SerialName("ChargeIsExtremelyLow")
    EXTREMELY_LOW,

    @SerialName("ChargeIsLow")
    LOW,

    @SerialName("ChargeRecommendedToChange")
    NEED_TO_CHANGE,

    @SerialName("Ok")
    NORMAL,
}
