package ru.urent.qatools.base_bike_setup.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.urent.qatools.core_entities.ChargeState
import ru.urent.qatools.core_entities.GeoPoint

data class BikeSetup(
    val simulatorId: Int,
    val location: GeoPoint,
    val target: ChargeTarget,
    val availability: AvailabilityTarget,
)

data class BikeContext(
    val simulatorId: Int,
    val imei: String,
    val bikeId: String,
    val vendorId: String,
    val identifier: String,
    val modelName: String,
    val cityId: String?,
    val cityName: String?,
    val chargePercent: Double?,
    val chargeState: ChargeState?,
    val location: GeoPoint?,
    val voltageRange: VoltageRange,
    val thresholds: ChargeThresholds,
    val disableStates: Set<ChargeState>,
    val status: BikeStatus,
)

sealed interface BikeLookup {
    data class Found(val bike: BikeContext) : BikeLookup
    data class Failed(val failure: ApplyFailure) : BikeLookup
}

@Serializable
sealed interface ApplyStatus {
    @Serializable
    @SerialName("Resolving")
    data object Resolving : ApplyStatus

    @Serializable
    @SerialName("WaitingForTracker")
    data class WaitingForTracker(val percent: Double) : ApplyStatus

    @Serializable
    @SerialName("Done")
    data class Done(
        val percent: Double,
        val state: ChargeState?,
        val isTargetReached: Boolean,
        val availability: AvailabilityTarget,
    ) : ApplyStatus

    @Serializable
    @SerialName("Failed")
    data class Failed(val failure: ApplyFailure) : ApplyStatus
}

@Serializable
sealed interface ApplyFailure {
    @Serializable
    @SerialName("TrackerNotFound")
    data object TrackerNotFound : ApplyFailure

    @Serializable
    @SerialName("BikeNotLinked")
    data object BikeNotLinked : ApplyFailure

    @Serializable
    @SerialName("UnsupportedDriver")
    data class UnsupportedDriver(val driver: String) : ApplyFailure

    @Serializable
    @SerialName("NoVoltageRange")
    data object NoVoltageRange : ApplyFailure

    @Serializable
    @SerialName("NoChargeThresholds")
    data object NoChargeThresholds : ApplyFailure

    @Serializable
    @SerialName("StandUnavailable")
    data object StandUnavailable : ApplyFailure

    @Serializable
    @SerialName("SessionExpired")
    data object SessionExpired : ApplyFailure

    @Serializable
    @SerialName("NotConfirmed")
    data object NotConfirmed : ApplyFailure

    @Serializable
    @SerialName("ReasonNotFound")
    data class ReasonNotFound(val reasonTitle: String) : ApplyFailure

    @Serializable
    @SerialName("AvailabilityNotChanged")
    data class AvailabilityNotChanged(val cause: ApplyFailure) : ApplyFailure

    @Serializable
    @SerialName("AvailabilityIgnored")
    data class AvailabilityIgnored(val isDisabled: Boolean, val currentReason: String?) : ApplyFailure

    @Serializable
    @SerialName("Unexpected")
    data class Unexpected(val statusCode: Int?) : ApplyFailure
}
