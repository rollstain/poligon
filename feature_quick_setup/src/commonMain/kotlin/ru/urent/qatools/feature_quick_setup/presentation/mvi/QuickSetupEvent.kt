package ru.urent.qatools.feature_quick_setup.presentation.mvi

import ru.urent.qatools.base_bike_setup.entities.AvailabilityTarget
import ru.urent.qatools.base_bike_setup.entities.BikeContext
import ru.urent.qatools.base_bike_setup.entities.ChargeTarget
import ru.urent.qatools.base_bike_setup.entities.City
import ru.urent.qatools.base_bike_setup.entities.TechZone
import ru.urent.qatools.base_bike_setup.entities.Template
import ru.urent.qatools.core_entities.GeoPoint

sealed interface QuickSetupEvent {
    data class OnSimulatorIdChanged(val text: String) : QuickSetupEvent

    data object OnRefreshBikeClicked : QuickSetupEvent

    data class OnQrCodeClicked(val bike: BikeContext) : QuickSetupEvent

    data class OnTargetChanged(val target: ChargeTarget) : QuickSetupEvent

    data class OnAvailabilityChanged(val availability: AvailabilityTarget) : QuickSetupEvent

    data class OnMapClicked(val location: GeoPoint) : QuickSetupEvent

    data class OnParkingAreaIdle(val center: GeoPoint) : QuickSetupEvent

    data class OnPinMoved(val pinId: String, val location: GeoPoint) : QuickSetupEvent

    data class OnCoordinatesTextChanged(val text: String) : QuickSetupEvent

    data class OnCitySelected(val city: City) : QuickSetupEvent

    data class OnTechZoneSelected(val techZone: TechZone) : QuickSetupEvent

    data class OnPointSelected(val point: GeoPoint) : QuickSetupEvent

    data object OnApplyClicked : QuickSetupEvent

    data class OnAddToTemplateClicked(val template: Template) : QuickSetupEvent
}
