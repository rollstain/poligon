package ru.urent.qatools.data_bikes.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.urent.qatools.core_entities.BatteryCapacityCategory
import ru.urent.qatools.core_entities.ChargeState

@Serializable
data class TransportModelResponse(
    @SerialName("name")
    val name: String?,
    @SerialName("chargeThresholds")
    val chargeThresholds: List<ChargeThreshold?>?,
    @SerialName("disableStates")
    val disableStates: List<ChargeState>?,
) {
    @Serializable
    data class ChargeThreshold(
        @SerialName("batteryCapacityCategory")
        val batteryCapacityCategory: BatteryCapacityCategory?,
        @SerialName("batteryLevelToEmpty")
        val batteryLevelToEmpty: Double?,
        @SerialName("batteryLevelToPoweroff")
        val batteryLevelToPoweroff: Double?,
        @SerialName("batteryLevelToDisable")
        val batteryLevelToDisable: Double?,
        @SerialName("batteryLevelNeedToChange")
        val batteryLevelNeedToChange: Double?,
    )
}
