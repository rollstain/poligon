package ru.urent.qatools.data_bikes.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CoordinateResponse(
    @SerialName("lat")
    val lat: Double?,
    @SerialName("lng")
    val lng: Double?,
)
