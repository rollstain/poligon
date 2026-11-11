package ru.urent.qatools.feature_templates.presentation.editor.mvi

import kotlin.time.Instant
import ru.urent.qatools.base_bike_setup.entities.ApplyStatus
import ru.urent.qatools.base_bike_setup.entities.BikeLookup
import ru.urent.qatools.base_bike_setup.entities.City
import ru.urent.qatools.base_bike_setup.entities.CityTechZones
import ru.urent.qatools.base_bike_setup.entities.DisabledReason
import ru.urent.qatools.base_bike_setup.entities.MapZones
import ru.urent.qatools.base_bike_setup.entities.TechZone
import ru.urent.qatools.base_bike_setup.entities.TemplateBike
import ru.urent.qatools.base_map.MapFocus
import ru.urent.qatools.core_entities.TileSource

data class TemplateState(
    val name: String,
    val rows: List<TemplateBikeRow>,
    val lastAppliedAt: Instant?,
    val selectedBikeId: Long?,
    val zones: MapZones,
    val isZonesFailed: Boolean,
    val isApplying: Boolean,
    val isRefreshing: Boolean,
    val reasons: List<DisabledReason>,
    val cityName: String?,
    val cities: List<City>,
    val techZones: List<TechZone>,
    val cityTechZones: List<CityTechZones>,
    val focus: MapFocus?,
    val newBike: NewBikeState,
    val isDeleteConfirmVisible: Boolean,
    val tileSource: TileSource?,
)

data class TemplateBikeRow(
    val bike: TemplateBike,
    val lookup: BikeLookup?,
    val status: ApplyStatus?,
    val isChangedSinceApply: Boolean,
)

data class NewBikeState(val bikeNumberText: String, val lookup: BikeLookup?, val isLookingUp: Boolean, val canAdd: Boolean)
