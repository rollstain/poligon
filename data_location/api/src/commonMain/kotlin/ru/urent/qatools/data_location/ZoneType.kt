package ru.urent.qatools.data_location

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ZoneType {
    @SerialName("PARKING")
    PARKING,

    @SerialName("CIRCLEPARKING")
    CIRCLE_PARKING,

    @SerialName("RENT")
    RENT,

    @SerialName("NOTALLOWED")
    NOT_ALLOWED,

    @SerialName("RESTRICTED")
    RESTRICTED,
}
