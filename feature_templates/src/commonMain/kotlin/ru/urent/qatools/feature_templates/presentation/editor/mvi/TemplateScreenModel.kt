package ru.urent.qatools.feature_templates.presentation.editor.mvi

import com.arkivanov.decompose.ComponentContext
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.urent.qatools.base_bike_setup.domain.BikeSetupInteractor
import ru.urent.qatools.base_bike_setup.domain.LocationInteractor
import ru.urent.qatools.base_bike_setup.domain.TemplateApplyRunner
import ru.urent.qatools.base_bike_setup.domain.TemplateInteractor
import ru.urent.qatools.base_bike_setup.domain.bikeQrCodeText
import ru.urent.qatools.base_bike_setup.entities.BikeApplyResult
import ru.urent.qatools.base_bike_setup.entities.BikeLookup
import ru.urent.qatools.base_bike_setup.entities.BikeSetup
import ru.urent.qatools.base_bike_setup.entities.City
import ru.urent.qatools.base_bike_setup.entities.EmptyMapZones
import ru.urent.qatools.base_bike_setup.entities.SavedCity
import ru.urent.qatools.base_bike_setup.entities.TechZone
import ru.urent.qatools.base_bike_setup.entities.Template
import ru.urent.qatools.base_bike_setup.entities.TemplateBike
import ru.urent.qatools.base_map.MapFocus
import ru.urent.qatools.core_base_feature.BaseScreenModel
import ru.urent.qatools.core_entities.GeoPoint
import ru.urent.qatools.core_entities.RequestResult
import ru.urent.qatools.core_entities.TileSource
import ru.urent.qatools.core_logger.AppLog

class TemplateScreenModel(
    componentContext: ComponentContext,
    private val standBaseUrl: String,
    private val templateId: Long,
    private val templateInteractor: TemplateInteractor,
    private val locationInteractor: LocationInteractor,
    private val bikeSetupInteractor: BikeSetupInteractor,
    private val runner: TemplateApplyRunner,
    private val tileSource: TileSource?,
    private val reconnections: Flow<Unit>,
    log: AppLog,
) : BaseScreenModel<TemplateState, TemplateSideEffect, TemplateEvent>(
    componentContext = componentContext,
    initialState = TemplateState(
        name = "",
        rows = emptyList(),
        lastAppliedAt = null,
        selectedBikeId = null,
        zones = EmptyMapZones,
        isZonesFailed = false,
        isApplying = false,
        isRefreshing = false,
        reasons = emptyList(),
        cityName = null,
        cities = emptyList(),
        techZones = emptyList(),
        cityTechZones = emptyList(),
        focus = null,
        newBike = EmptyNewBike,
        isDeleteConfirmVisible = false,
        tileSource = tileSource,
    ),
    log = log,
) {
    private var template: Template? = null
    private var lookups: Map<Int, BikeLookup> = emptyMap()
    private var results: Map<Long, BikeApplyResult> = emptyMap()
    private var runningTemplateIds: Set<Long> = emptySet()
    private var nameDraft: String? = null
    private var city: SavedCity? = null
    private var newBikeLookupJob: Job? = null
    private val requestedLookups = mutableSetOf<Int>()
    private val requestedZoneCityIds = mutableSetOf<String>()
    private var parkingAreaCenter: GeoPoint? = null

    init {
        onFetchData()
    }

    override fun dispatch(event: TemplateEvent) {
        when (event) {
            TemplateEvent.OnBackClicked -> screenModelScope { postSideEffect(TemplateSideEffect.Back) }
            is TemplateEvent.OnNameChanged -> onNameChanged(event.name)
            is TemplateEvent.OnBikeClicked -> updateState {
                it.copy(selectedBikeId = event.bike.id, focus = MapFocus(event.bike.setup.location))
            }
            is TemplateEvent.OnPinClicked -> onPinClicked(event.pinId)
            is TemplateEvent.OnBikeTechZoneSelected -> {
                updateState { it.copy(selectedBikeId = event.bikeId, focus = MapFocus(event.techZone.center)) }
                screenModelScope { templateInteractor.moveBike(bikeId = event.bikeId, location = event.techZone.center) }
            }
            is TemplateEvent.OnPinMoved -> onPinMoved(pinId = event.pinId, location = event.location)
            is TemplateEvent.OnParkingAreaIdle -> {
                parkingAreaCenter = event.center
                val city = locationInteractor.getNearestCity(cities = state.cities, point = event.center) ?: return
                loadZones(city.id)
            }
            is TemplateEvent.OnChargeChanged -> screenModelScope {
                templateInteractor.changeBikeSettings(bikeId = event.bike.id, target = event.target, availability = event.bike.setup.availability)
            }
            is TemplateEvent.OnAvailabilityChanged -> screenModelScope {
                templateInteractor.changeBikeSettings(bikeId = event.bike.id, target = event.bike.setup.target, availability = event.availability)
            }
            TemplateEvent.OnApplyAllClicked -> onApplyAllClicked()
            TemplateEvent.OnRefreshStatusesClicked -> onRefreshStatusesClicked()
            is TemplateEvent.OnCitySelected -> onCitySelected(event.city)
            is TemplateEvent.OnTechZoneSelected -> updateState { it.copy(focus = MapFocus(event.techZone.center)) }
            is TemplateEvent.OnPointSelected -> updateState { it.copy(focus = MapFocus(event.point)) }
            is TemplateEvent.OnRemoveBikeClicked -> screenModelScope { templateInteractor.deleteBike(event.bikeId) }
            is TemplateEvent.OnQrCodeClicked -> screenModelScope { postSideEffect(TemplateSideEffect.OpenQrCode(bikeQrCodeText(event.bike))) }
            is TemplateEvent.OnNewBikeTextChanged -> onNewBikeTextChanged(event.text)
            TemplateEvent.OnAddNewBikeClicked -> onAddNewBikeClicked()
            TemplateEvent.OnDeleteClicked -> updateState { it.copy(isDeleteConfirmVisible = true) }
            TemplateEvent.OnDeleteDismissed -> updateState { it.copy(isDeleteConfirmVisible = false) }
            TemplateEvent.OnBikeCardClosed -> updateState { it.copy(selectedBikeId = null) }
            TemplateEvent.OnDeleteConfirmed -> screenModelScope {
                templateInteractor.delete(templateId)
                postSideEffect(TemplateSideEffect.Back)
            }
        }
    }

    override fun onFetchData() {
        onFetchDataScope {
            launch {
                templateInteractor.getTemplate(templateId).collect { loaded ->
                    template = loaded
                    publishTemplate()
                    if (loaded != null) lookUpMissing(loaded.bikes)
                }
            }
            launch {
                runner.getResults(standBaseUrl).collect { loaded ->
                    results = loaded
                    publishTemplate()
                }
            }
            launch {
                runner.runningTemplateIds.collect { ids ->
                    val wasApplying = templateId in runningTemplateIds
                    runningTemplateIds = ids
                    publishTemplate()
                    if (wasApplying && templateId !in ids) onRefreshStatusesClicked()
                }
            }
            launch { loadReasons() }
            launch { loadCities() }
            launch { reconnections.collect { reloadStandData() } }
        }
    }

    private fun publishTemplate() {
        val bikes = template?.bikes.orEmpty()
        val rows = mutableListOf<TemplateBikeRow>()
        for (bike in bikes) {
            val result = results[bike.id]
            rows.add(
                TemplateBikeRow(
                    bike = bike,
                    lookup = lookups[bike.setup.bikeNumber],
                    status = result?.status,
                    isChangedSinceApply = result != null && result.appliedSetup != bike.setup,
                ),
            )
        }
        val lastAppliedAt = bikes.mapNotNull { bike -> results[bike.id]?.appliedAt }.maxOrNull()
        val name = nameDraft ?: template?.name.orEmpty()
        val isApplying = templateId in runningTemplateIds
        val newBikeFound = (state.newBike.lookup as? BikeLookup.Found)?.bike
        val canAddNewBike = newBikeFound != null && (newBikeFound.location != null || bikes.isNotEmpty())
        updateState {
            it.copy(
                name = name,
                rows = rows,
                lastAppliedAt = lastAppliedAt,
                isApplying = isApplying,
                newBike = it.newBike.copy(canAdd = canAddNewBike),
            )
        }
    }

    private fun onNameChanged(name: String) {
        nameDraft = name
        publishTemplate()
        screenModelScope { templateInteractor.rename(templateId = templateId, name = name) }
    }

    private fun onPinClicked(pinId: String) {
        val bikeId = pinId.toLongOrNull() ?: return
        updateState { it.copy(selectedBikeId = bikeId) }
    }

    private fun onPinMoved(pinId: String, location: GeoPoint) {
        val bikeId = pinId.toLongOrNull() ?: return
        updateState { it.copy(selectedBikeId = bikeId) }
        screenModelScope { templateInteractor.moveBike(bikeId = bikeId, location = location) }
    }

    private fun onApplyAllClicked() {
        val current = template ?: return
        runner.applyTemplate(standBaseUrl = standBaseUrl, template = current)
    }

    private fun onRefreshStatusesClicked() {
        val bikes = template?.bikes.orEmpty()
        if (bikes.isEmpty()) return
        updateState { it.copy(isRefreshing = true) }
        screenModelScope {
            val freshLookups = lookUp(bikes.map { it.setup.bikeNumber })
            lookups = lookups + freshLookups
            publishTemplate()
            val confirmedBikeNumbers = mutableListOf<Int>()
            for (bike in bikes) {
                val standBike = (freshLookups[bike.setup.bikeNumber] as? BikeLookup.Found)?.bike ?: continue
                val isConfirmed = runner.confirmRecalculatedCharge(standBaseUrl = standBaseUrl, bike = bike, standBike = standBike)
                if (isConfirmed) confirmedBikeNumbers.add(bike.setup.bikeNumber)
            }
            if (confirmedBikeNumbers.isNotEmpty()) {
                lookups = lookups + lookUp(confirmedBikeNumbers)
                publishTemplate()
            }
            updateState { it.copy(isRefreshing = false) }
        }
    }

    private fun onCitySelected(selected: City) {
        screenModelScope {
            val techZones = switchCity(SavedCity(id = selected.id, name = selected.name))
            val focusPoint = techZones.firstOrNull()?.center ?: selected.center
            updateState { it.copy(focus = MapFocus(focusPoint)) }
        }
    }

    private fun onNewBikeTextChanged(text: String) {
        updateState { it.copy(newBike = EmptyNewBike.copy(bikeNumberText = text)) }
        newBikeLookupJob?.cancel()
        val bikeNumber = text.trim().toIntOrNull() ?: return
        newBikeLookupJob = screenModelScope {
            delay(LOOKUP_DEBOUNCE_MS.milliseconds)
            updateState { it.copy(newBike = it.newBike.copy(isLookingUp = true)) }
            val result = bikeSetupInteractor.lookup(standBaseUrl = standBaseUrl, bikeNumber = bikeNumber)
            lookups = lookups + (bikeNumber to result)
            updateState { it.copy(newBike = it.newBike.copy(lookup = result, isLookingUp = false)) }
            publishTemplate()
        }
    }

    private fun onAddNewBikeClicked() {
        val found = state.newBike.lookup as? BikeLookup.Found ?: return
        val location = found.bike.location ?: template?.bikes?.firstOrNull()?.setup?.location ?: return
        screenModelScope {
            val defaults = templateInteractor.setupDefaults.first()
            val setup = BikeSetup(
                bikeNumber = found.bike.bikeNumber,
                location = location,
                target = defaults.target,
                availability = defaults.availability,
            )
            templateInteractor.addBike(templateId = templateId, setup = setup)
            updateState { it.copy(newBike = EmptyNewBike) }
        }
    }

    private suspend fun loadReasons() {
        val reasons = bikeSetupInteractor.getDisabledReasons()
        updateState { it.copy(reasons = reasons) }
    }

    private suspend fun loadCities() {
        val cities = locationInteractor.getCities()
        updateState { it.copy(cities = cities) }
        val cityTechZones = locationInteractor.getTechZonesOfCities(standBaseUrl = standBaseUrl, cities = cities)
        updateState { it.copy(cityTechZones = cityTechZones) }
        val center = parkingAreaCenter ?: return
        val city = locationInteractor.getNearestCity(cities = cities, point = center) ?: return
        loadZones(city.id)
    }

    private fun reloadStandData() {
        screenModelScope { loadReasons() }
        screenModelScope { loadCities() }
        val currentCity = city
        if (currentCity != null) screenModelScope { switchCity(currentCity) }
        val bikeNumbers = template?.bikes.orEmpty().map { it.setup.bikeNumber }
        screenModelScope {
            val refreshed = lookUp(bikeNumbers)
            lookups = lookups + refreshed
            publishTemplate()
            for (lookup in refreshed.values) {
                val cityId = (lookup as? BikeLookup.Found)?.bike?.cityId ?: continue
                loadZones(cityId)
            }
        }
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

    private suspend fun lookUp(bikeNumbers: List<Int>): Map<Int, BikeLookup> = coroutineScope {
        val requests = bikeNumbers.distinct().map { bikeNumber -> async { bikeNumber to bikeSetupInteractor.lookup(standBaseUrl = standBaseUrl, bikeNumber = bikeNumber) } }
        requests.awaitAll().toMap()
    }

    private fun lookUpMissing(bikes: List<TemplateBike>) {
        for (bike in bikes) {
            val bikeNumber = bike.setup.bikeNumber
            if (!requestedLookups.add(bikeNumber)) continue
            screenModelScope {
                val result = bikeSetupInteractor.lookup(standBaseUrl = standBaseUrl, bikeNumber = bikeNumber)
                lookups = lookups + (bikeNumber to result)
                publishTemplate()
                val foundBike = (result as? BikeLookup.Found)?.bike
                val cityId = foundBike?.cityId
                val cityName = foundBike?.cityName
                if (cityId != null) loadZones(cityId)
                if (city == null && cityId != null && cityName != null) switchCity(SavedCity(id = cityId, name = cityName))
            }
        }
    }

    private suspend fun switchCity(newCity: SavedCity): List<TechZone> {
        city = newCity
        updateState { it.copy(cityName = newCity.name, techZones = emptyList()) }
        locationInteractor.saveLastCity(standBaseUrl = standBaseUrl, city = newCity)
        loadZones(newCity.id)
        val techZones = locationInteractor.getTechZones(newCity.id)
        updateState { it.copy(techZones = techZones) }
        return techZones
    }

    private companion object {
        const val LOOKUP_DEBOUNCE_MS = 500L
        val EmptyNewBike = NewBikeState(bikeNumberText = "", lookup = null, isLookingUp = false, canAdd = false)
    }
}
