package ru.urent.qatools.base_bike_setup.entities

import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.urent.qatools.core_entities.BatteryCapacityCategory
import ru.urent.qatools.core_entities.DriverTaskReason

data class DriverTask(val reason: DriverTaskReason?)

data class BikeStatus(
    val isOnline: Boolean?,
    val offlineSince: Instant?,
    val isGpsLost: Boolean,
    val gpsLostSince: Instant?,
    val isDisabled: Boolean?,
    val disabledReason: String?,
    val hasLowBatteryReason: Boolean,
    val batteryCategory: BatteryCapacityCategory,
    val driverTask: DriverTask?,
)

data class DisabledReason(val id: String, val name: String, val title: String)

@Serializable
sealed interface AvailabilityTarget {
    @Serializable
    @SerialName("Keep")
    data object Keep : AvailabilityTarget

    @Serializable
    @SerialName("Available")
    data object Available : AvailabilityTarget

    @Serializable
    @SerialName("Disabled")
    data class Disabled(val reasonName: String, val reasonTitle: String) : AvailabilityTarget
}
