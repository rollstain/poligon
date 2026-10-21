package ru.urent.qatools.feature_quick_setup.presentation.mvi

import ru.urent.qatools.base_bike_setup.entities.ApplyStatus
import ru.urent.qatools.base_bike_setup.entities.AvailabilityTarget
import ru.urent.qatools.base_bike_setup.entities.BikeLookup
import ru.urent.qatools.base_bike_setup.entities.ChargeTarget
import ru.urent.qatools.base_bike_setup.entities.City
import ru.urent.qatools.base_bike_setup.entities.DisabledReason
import ru.urent.qatools.base_bike_setup.entities.MapZones
import ru.urent.qatools.base_bike_setup.entities.SavedCity
import ru.urent.qatools.base_bike_setup.entities.TechZone
import ru.urent.qatools.base_bike_setup.entities.Template
import ru.urent.qatools.base_map.MapFocus
import ru.urent.qatools.core_entities.GeoPoint
import ru.urent.qatools.core_entities.TileSource

data class QuickSetupState(
    val simulatorIdText: String,
    val lookup: BikeLookup?,
    val isLookingUp: Boolean,
    val isRefreshingBike: Boolean,
    val target: ChargeTarget,
    val availability: AvailabilityTarget,
    val reasons: List<DisabledReason>,
    val location: GeoPoint?,
    val coordinatesText: String,
    val isCoordinatesInvalid: Boolean,
    val mapCenter: GeoPoint?,
    val focus: MapFocus?,
    val city: SavedCity?,
    val cities: List<City>,
    val techZones: List<TechZone>,
    val zones: MapZones,
    val isZonesFailed: Boolean,
    val status: ApplyStatus?,
    val isApplying: Boolean,
    val templates: List<Template>,
    val message: String?,
    val tileSource: TileSource?,
)
