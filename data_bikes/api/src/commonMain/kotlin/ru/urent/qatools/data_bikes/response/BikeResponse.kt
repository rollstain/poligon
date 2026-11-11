package ru.urent.qatools.data_bikes.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.urent.qatools.core_entities.BatteryCapacityCategory
import ru.urent.qatools.core_entities.ChargeState

@Serializable
data class BikeResponse(
    @SerialName("id")
    val id: String?,
    @SerialName("identifier")
    val identifier: String?,
    @SerialName("vendorId")
    val vendorId: String?,
    @SerialName("trackerId")
    val trackerImei: String?,
    @SerialName("model")
    val model: Model?,
    @SerialName("bikeCity")
    val bikeCity: City?,
    @SerialName("charge")
    val charge: Charge?,
    @SerialName("state")
    val state: State?,
    @SerialName("bikeBatteryInternal")
    val internalBattery: Battery?,
) {
    @Serializable
    data class State(
        @SerialName("state")
        val flags: Flags?,
        @SerialName("mainDisabledReason")
        val mainDisabledReason: String?,
        @SerialName("disabledReasonTags")
        val disabledReasonTags: List<String?>?,
    )

    @Serializable
    data class Flags(
        @SerialName("connected")
        val isConnected: Boolean?,
        @SerialName("connectionLostDate")
        val connectionLostAt: String?,
        @SerialName("nullableCoordinates")
        val hasNoCoordinates: Boolean?,
        @SerialName("nullableCoordinatesDateTime")
        val noCoordinatesSince: String?,
        @SerialName("disabled")
        val isDisabled: Boolean?,
    )

    @Serializable
    data class Battery(
        @SerialName("bikeBatteryModel")
        val model: BatteryModel?,
    )

    @Serializable
    data class BatteryModel(
        @SerialName("batteryCapacityCategory")
        val capacityCategory: BatteryCapacityCategory?,
    )

    @Serializable
    data class Model(
        @SerialName("id")
        val id: String?,
        @SerialName("name")
        val name: String?,
    )

    @Serializable
    data class City(
        @SerialName("cityId")
        val cityId: String?,
        @SerialName("cityName")
        val cityName: String?,
    )

    @Serializable
    data class Charge(
        @SerialName("batteryPercent")
        val batteryFraction: Double?,
        @SerialName("status")
        val status: ChargeState?,
    )
}
