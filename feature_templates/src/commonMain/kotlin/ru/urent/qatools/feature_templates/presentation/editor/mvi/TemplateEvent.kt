package ru.urent.qatools.feature_templates.presentation.editor.mvi

import ru.urent.qatools.base_bike_setup.entities.AvailabilityTarget
import ru.urent.qatools.base_bike_setup.entities.BikeContext
import ru.urent.qatools.base_bike_setup.entities.ChargeTarget
import ru.urent.qatools.base_bike_setup.entities.City
import ru.urent.qatools.base_bike_setup.entities.TechZone
import ru.urent.qatools.base_bike_setup.entities.TemplateBike
import ru.urent.qatools.core_entities.GeoPoint

sealed interface TemplateEvent {
    data object OnBackClicked : TemplateEvent

    data class OnNameChanged(val name: String) : TemplateEvent

    data class OnBikeClicked(val bike: TemplateBike) : TemplateEvent

    data object OnBikeCardClosed : TemplateEvent

    data class OnPinClicked(val pinId: String) : TemplateEvent

    data class OnBikeTechZoneSelected(val bikeId: Long, val techZone: TechZone) : TemplateEvent

    data class OnParkingAreaIdle(val center: GeoPoint) : TemplateEvent

    data class OnPinMoved(val pinId: String, val location: GeoPoint) : TemplateEvent

    data class OnChargeChanged(val bike: TemplateBike, val target: ChargeTarget) : TemplateEvent

    data class OnAvailabilityChanged(val bike: TemplateBike, val availability: AvailabilityTarget) : TemplateEvent

    data object OnApplyAllClicked : TemplateEvent

    data object OnRefreshStatusesClicked : TemplateEvent

    data class OnCitySelected(val city: City) : TemplateEvent

    data class OnTechZoneSelected(val techZone: TechZone) : TemplateEvent

    data class OnPointSelected(val point: GeoPoint) : TemplateEvent

    data class OnRemoveBikeClicked(val bikeId: Long) : TemplateEvent

    data class OnQrCodeClicked(val bike: BikeContext) : TemplateEvent

    data class OnNewBikeTextChanged(val text: String) : TemplateEvent

    data object OnAddNewBikeClicked : TemplateEvent

    data object OnDeleteClicked : TemplateEvent

    data object OnDeleteDismissed : TemplateEvent

    data object OnDeleteConfirmed : TemplateEvent
}
