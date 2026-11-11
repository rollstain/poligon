package ru.urent.qatools.feature_quick_setup.presentation.mvi

import com.arkivanov.decompose.ComponentContext
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.urent.qatools.base_bike_setup.domain.BikeSetupInteractor
import ru.urent.qatools.base_bike_setup.domain.LocationInteractor
import ru.urent.qatools.base_bike_setup.domain.TemplateInteractor
import ru.urent.qatools.base_bike_setup.domain.bikeQrCodeText
import ru.urent.qatools.base_bike_setup.entities.ApplyStatus
import ru.urent.qatools.base_bike_setup.entities.AvailabilityTarget
import ru.urent.qatools.base_bike_setup.entities.BikeLookup
import ru.urent.qatools.base_bike_setup.entities.BikeSetup
import ru.urent.qatools.base_bike_setup.entities.ChargeTarget
import ru.urent.qatools.base_bike_setup.entities.City
import ru.urent.qatools.base_bike_setup.entities.EmptyMapZones
import ru.urent.qatools.base_bike_setup.entities.SavedCity
import ru.urent.qatools.base_bike_setup.entities.TechZone
import ru.urent.qatools.base_bike_setup.entities.Template
import ru.urent.qatools.base_bike_setup.presentation.formatCoordinates
import ru.urent.qatools.base_bike_setup.presentation.parseCoordinates
import ru.urent.qatools.base_map.MapFocus
import ru.urent.qatools.core_base_feature.BaseScreenModel
import ru.urent.qatools.core_entities.ChargeState
import ru.urent.qatools.core_entities.GeoPoint
import ru.urent.qatools.core_entities.RequestResult
import ru.urent.qatools.core_entities.TileSource
import ru.urent.qatools.core_logger.AppLog

class QuickSetupScreenModel(
    componentContext: ComponentContext,
    private val standBaseUrl: String,
    private val bikeSetupInteractor: BikeSetupInteractor,
    private val templateInteractor: TemplateInteractor,
    private val locationInteractor: LocationInteractor,
    private val tileSource: TileSource?,
    private val reconnections: Flow<Unit>,
    log: AppLog,
) : BaseScreenModel<QuickSetupState, QuickSetupSideEffect, QuickSetupEvent>(
    componentContext = componentContext,
    initialState = QuickSetupState(
        bikeNumberText = "",
        lookup = null,
        isLookingUp = false,
        isRefreshingBike = false,
        target = ChargeTarget.State(ChargeState.LOW),
        availability = AvailabilityTarget.Keep,
        reasons = emptyList(),
        location = null,
        coordinatesText = "",
        isCoordinatesInvalid = false,
        mapCenter = null,
        focus = null,
        city = null,
        cities = emptyList(),
        techZones = emptyList(),
        zones = EmptyMapZones,
        isZonesFailed = false,
        status = null,
        isApplying = false,
        templates = emptyList(),
        message = null,
        tileSource = tileSource,
    ),
    log = log,
) {
    private var lookupJob: Job? = null
    private val requestedZoneCityIds = mutableSetOf<String>()
    private var parkingAreaCenter: GeoPoint? = null

    init {
        onFetchData()
    }

    override fun dispatch(event: QuickSetupEvent) {
        when (event) {
            is QuickSetupEvent.OnBikeNumberChanged -> onBikeNumberChanged(event.text)
            QuickSetupEvent.OnRefreshBikeClicked -> onRefreshBikeClicked()
            is QuickSetupEvent.OnQrCodeClicked -> screenModelScope { postSideEffect(QuickSetupSideEffect.OpenQrCode(bikeQrCodeText(event.bike))) }
            is QuickSetupEvent.OnTargetChanged -> updateState { it.copy(target = event.target, status = null) }
            is QuickSetupEvent.OnAvailabilityChanged -> updateState { it.copy(availability = event.availability, status = null) }
            is QuickSetupEvent.OnMapClicked -> onMapClicked(event.location)
            is QuickSetupEvent.OnPinMoved -> pickLocation(event.location)
            is QuickSetupEvent.OnParkingAreaIdle -> {
                parkingAreaCenter = event.center
                val city = locationInteractor.getNearestCity(cities = state.cities, point = event.center) ?: return
                loadZones(city.id)
            }
            is QuickSetupEvent.OnCoordinatesTextChanged -> onCoordinatesTextChanged(event.text)
            is QuickSetupEvent.OnCitySelected -> onCitySelected(event.city)
            is QuickSetupEvent.OnTechZoneSelected -> updateState { it.copy(focus = MapFocus(event.techZone.center)) }
            is QuickSetupEvent.OnPointSelected -> updateState { it.copy(focus = MapFocus(event.point)) }
            QuickSetupEvent.OnApplyClicked -> onApplyClicked()
            is QuickSetupEvent.OnAddToTemplateClicked -> onAddToTemplateClicked(event.template)
        }
    }

    override fun onFetchData() {
        onFetchDataScope {
            launch {
                val defaults = templateInteractor.setupDefaults.first()
                updateState { it.copy(target = defaults.target, availability = defaults.availability) }
            }
            launch {
                templateInteractor.getTemplates(standBaseUrl).collect { templates -> updateState { it.copy(templates = templates) } }
            }
            launch { loadReasons() }
            launch { reconnections.collect { reloadStandData() } }
            launch { openLastCity() }
        }
    }

    private suspend fun openLastCity() {
        val cities = loadCities()
        val lastCity = locationInteractor.getLastCity(standBaseUrl) ?: return
        if (state.city != null) return
        val techZones = switchCity(lastCity)
        val cityCenter = cities.firstOrNull { it.id == lastCity.id }?.center
        val mapCenter = state.mapCenter ?: techZones.firstOrNull()?.center ?: cityCenter
        updateState { it.copy(mapCenter = mapCenter) }
    }

    private fun onBikeNumberChanged(text: String) {
        val bikeNumber = text.trim().toIntOrNull()
        updateState {
            it.copy(bikeNumberText = text, lookup = null, isLookingUp = bikeNumber != null, status = null, message = null)
        }
        lookupJob?.cancel()
        if (bikeNumber == null) return
        lookupJob = screenModelScope {
            delay(LOOKUP_DEBOUNCE_MS.milliseconds)
            val result = bikeSetupInteractor.lookup(standBaseUrl = standBaseUrl, bikeNumber = bikeNumber)
            val foundBike = (result as? BikeLookup.Found)?.bike
            val bikeLocation = foundBike?.location
            val location = bikeLocation ?: state.location
            val coordinatesText = if (location == null) state.coordinatesText else formatCoordinates(location)
            val mapCenter = state.mapCenter ?: location
            val focus = if (bikeLocation == null) state.focus else MapFocus(bikeLocation)
            updateState {
                it.copy(
                    lookup = result,
                    isLookingUp = false,
                    location = location,
                    coordinatesText = coordinatesText,
                    isCoordinatesInvalid = false,
                    mapCenter = mapCenter,
                    focus = focus,
                )
            }
            val cityId = foundBike?.cityId
            val cityName = foundBike?.cityName
            if (cityId != null && cityName != null && cityId != state.city?.id) switchCity(SavedCity(id = cityId, name = cityName))
        }
    }

    private fun onRefreshBikeClicked() {
        val current = state
        val found = current.lookup as? BikeLookup.Found ?: return
        updateState { it.copy(isRefreshingBike = true) }
        screenModelScope {
            val refreshed = bikeSetupInteractor.lookup(standBaseUrl = standBaseUrl, bikeNumber = found.bike.bikeNumber)
            val refreshedBike = (refreshed as? BikeLookup.Found)?.bike
            val applied = current.status as? ApplyStatus.Done
            val location = current.location
            if (refreshedBike == null || applied == null || applied.isTargetReached || location == null) {
                updateState { it.copy(lookup = refreshed, isRefreshingBike = false) }
                return@screenModelScope
            }
            val setup = BikeSetup(
                bikeNumber = refreshedBike.bikeNumber,
                location = location,
                target = current.target,
                availability = current.availability,
            )
            val confirmed = bikeSetupInteractor.confirmRecalculatedCharge(setup = setup, applied = applied, bike = refreshedBike)
            val lookupAfterConfirm = if (confirmed == applied) refreshed else bikeSetupInteractor.lookup(standBaseUrl = standBaseUrl, bikeNumber = refreshedBike.bikeNumber)
            updateState { it.copy(lookup = lookupAfterConfirm, status = confirmed, isRefreshingBike = false) }
        }
    }

    private fun onMapClicked(location: GeoPoint) {
        if (state.lookup !is BikeLookup.Found) return
        pickLocation(location)
    }

    private fun onCoordinatesTextChanged(text: String) {
        val parsed = parseCoordinates(text)
        val location = parsed ?: state.location
        val mapCenter = state.mapCenter ?: parsed
        val focus = if (parsed == null) state.focus else MapFocus(parsed)
        updateState {
            it.copy(
                coordinatesText = text,
                isCoordinatesInvalid = parsed == null,
                location = location,
                mapCenter = mapCenter,
                focus = focus,
            )
        }
    }

    private fun onCitySelected(selected: City) {
        screenModelScope {
            val techZones = switchCity(SavedCity(id = selected.id, name = selected.name))
            val center = techZones.firstOrNull()?.center ?: selected.center
            val mapCenter = state.mapCenter ?: center
            updateState { it.copy(mapCenter = mapCenter, focus = MapFocus(center)) }
        }
    }

    private fun onApplyClicked() {
        val current = state
        val found = current.lookup as? BikeLookup.Found ?: return
        val location = current.location ?: return
        val setup = BikeSetup(
            bikeNumber = found.bike.bikeNumber,
            location = location,
            target = current.target,
            availability = current.availability,
        )
        updateState { it.copy(isApplying = true, message = null) }
        screenModelScope {
            bikeSetupInteractor.apply(standBaseUrl = standBaseUrl, setup = setup).collect { status -> updateState { it.copy(status = status) } }
            val refreshed = bikeSetupInteractor.lookup(standBaseUrl = standBaseUrl, bikeNumber = found.bike.bikeNumber)
            updateState { it.copy(isApplying = false, lookup = refreshed) }
        }
    }

    private fun onAddToTemplateClicked(template: Template) {
        val current = state
        val found = current.lookup as? BikeLookup.Found ?: return
        val location = current.location ?: return
        val setup = BikeSetup(
            bikeNumber = found.bike.bikeNumber,
            location = location,
            target = current.target,
            availability = current.availability,
        )
        screenModelScope {
            templateInteractor.addBike(templateId = template.id, setup = setup)
            updateState { it.copy(message = "${found.bike.identifier} добавлен в шаблон «${template.name}»") }
        }
    }

    private suspend fun loadReasons() {
        val reasons = bikeSetupInteractor.getDisabledReasons()
        updateState { it.copy(reasons = reasons) }
    }

    private suspend fun loadCities(): List<City> {
        val cities = locationInteractor.getCities()
        updateState { it.copy(cities = cities) }
        val center = parkingAreaCenter
        val cityInView = if (center == null) null else locationInteractor.getNearestCity(cities = cities, point = center)
        if (cityInView != null) loadZones(cityInView.id)
        return cities
    }

    private fun reloadStandData() {
        screenModelScope { loadReasons() }
        screenModelScope { loadCities() }
        val city = state.city
        if (city != null) screenModelScope { switchCity(city) }
        val bikeNumber = state.bikeNumberText.trim().toIntOrNull() ?: return
        lookupJob?.cancel()
        lookupJob = screenModelScope { refreshBikeKeepingPin(bikeNumber) }
    }

    private suspend fun refreshBikeKeepingPin(bikeNumber: Int) {
        val result = bikeSetupInteractor.lookup(standBaseUrl = standBaseUrl, bikeNumber = bikeNumber)
        val foundBike = (result as? BikeLookup.Found)?.bike
        val pinnedLocation = state.location
        val location = pinnedLocation ?: foundBike?.location
        val coordinatesText = if (pinnedLocation == null && location != null) formatCoordinates(location) else state.coordinatesText
        val mapCenter = state.mapCenter ?: location
        updateState { it.copy(lookup = result, location = location, coordinatesText = coordinatesText, mapCenter = mapCenter) }
        val cityId = foundBike?.cityId
        val cityName = foundBike?.cityName
        if (state.city == null && cityId != null && cityName != null) switchCity(SavedCity(id = cityId, name = cityName))
    }

    private fun pickLocation(location: GeoPoint) {
        val coordinatesText = formatCoordinates(location)
        updateState { it.copy(location = location, coordinatesText = coordinatesText, isCoordinatesInvalid = false) }
    }

    private suspend fun switchCity(city: SavedCity): List<TechZone> {
        updateState { it.copy(city = city, techZones = emptyList()) }
        locationInteractor.saveLastCity(standBaseUrl = standBaseUrl, city = city)
        loadZones(city.id)
        val techZones = locationInteractor.getTechZones(city.id)
        updateState { it.copy(techZones = techZones) }
        return techZones
    }

    private fun loadZones(cityId: String) {
        if (!requestedZoneCityIds.add(cityId)) return
        screenModelScope {
            when (val loaded = locationInteractor.getCityZones(standBaseUrl = standBaseUrl, cityId = cityId)) {
                is RequestResult.Error -> {
                    requestedZoneCityIds.remove(cityId)
                    updateState { it.copy(isZonesFailed = true) }
                }
                is RequestResult.Success -> {
                    val zones = state.zones + loaded.data
                    updateState { it.copy(zones = zones, isZonesFailed = false) }
                }
            }
        }
    }

    private companion object {
        const val LOOKUP_DEBOUNCE_MS = 500L
    }
}
