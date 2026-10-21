package ru.urent.qatools.data_location.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.urent.qatools.data_location.ZoneType

@Serializable
data class ParkingListRequest(
    @SerialName("areaIds")
    val areaIds: List<String>,
    @SerialName("types")
    val types: List<ZoneType>,
    @SerialName("withDisabled")
    val withDisabled: Boolean,
)
