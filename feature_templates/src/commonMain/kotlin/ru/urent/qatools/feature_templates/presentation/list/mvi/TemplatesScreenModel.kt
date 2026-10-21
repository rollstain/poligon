package ru.urent.qatools.feature_templates.presentation.list.mvi

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import ru.urent.qatools.base_bike_setup.domain.LocationInteractor
import ru.urent.qatools.base_bike_setup.domain.PlacementCalculator
import ru.urent.qatools.base_bike_setup.domain.TemplateApplyRunner
import ru.urent.qatools.base_bike_setup.domain.TemplateInteractor
import ru.urent.qatools.base_bike_setup.entities.BikeApplyResult
import ru.urent.qatools.base_bike_setup.entities.CityTechZones
import ru.urent.qatools.base_bike_setup.entities.Template
import ru.urent.qatools.core_base_feature.BaseScreenModel
import ru.urent.qatools.core_logger.AppLog

class TemplatesScreenModel(
    componentContext: ComponentContext,
    private val standBaseUrl: String,
    private val templateInteractor: TemplateInteractor,
    private val locationInteractor: LocationInteractor,
    private val runner: TemplateApplyRunner,
    log: AppLog,
) : BaseScreenModel<TemplatesState, TemplatesSideEffect, TemplatesEvent>(
    componentContext = componentContext,
    initialState = TemplatesState(templates = emptyList(), isLoaded = false),
    log = log,
) {
    private val cityTechZones = MutableStateFlow<List<CityTechZones>>(emptyList())

    init {
        onFetchData()
    }

    override fun dispatch(event: TemplatesEvent) {
        when (event) {
            TemplatesEvent.OnCreateTemplateClicked -> onCreateTemplateClicked()
            is TemplatesEvent.OnTemplateClicked -> screenModelScope { postSideEffect(TemplatesSideEffect.OpenTemplate(event.templateId)) }
            is TemplatesEvent.OnApplyAllClicked -> runner.applyTemplate(standBaseUrl = standBaseUrl, template = event.template)
        }
    }

    override fun onFetchData() {
        onFetchDataScope {
            launch {
                val cities = locationInteractor.getCities()
                cityTechZones.value = locationInteractor.getTechZonesOfCities(standBaseUrl = standBaseUrl, cities = cities)
            }
            combine(
                templateInteractor.getTemplates(standBaseUrl),
                runner.getResults(standBaseUrl),
                runner.runningTemplateIds,
                cityTechZones,
            ) { templates, results, runningIds, cityTechZones ->
                cardsOf(templates = templates, results = results, runningIds = runningIds, cityTechZones = cityTechZones)
            }
                .collect { cards -> updateState { it.copy(templates = cards, isLoaded = true) } }
        }
    }

    private fun onCreateTemplateClicked() {
        screenModelScope {
            val templateId = templateInteractor.create(standBaseUrl = standBaseUrl, name = NEW_TEMPLATE_NAME)
            postSideEffect(TemplatesSideEffect.OpenTemplate(templateId))
        }
    }

    private fun cardsOf(
        templates: List<Template>,
        results: Map<Long, BikeApplyResult>,
        runningIds: Set<Long>,
        cityTechZones: List<CityTechZones>,
    ): List<TemplateCard> {
        val statuses = results.mapValues { (_, result) -> result.status }
        val cities = cityTechZones.map { it.city }
        val techZones = cityTechZones.flatMap { it.techZones }
        val cards = mutableListOf<TemplateCard>()
        for (template in templates) {
            val templateResults = template.bikes.mapNotNull { bike -> results[bike.id] }
            val changedCount = template.bikes.count { bike ->
                val result = results[bike.id]
                result != null && result.appliedSetup != bike.setup
            }
            val cityNames = linkedSetOf<String>()
            val techZoneNames = linkedSetOf<String>()
            for (bike in template.bikes) {
                val location = bike.setup.location
                val city = locationInteractor.getNearestCity(cities, location)
                if (city != null) cityNames.add(city.name)
                val techZone = techZones.firstOrNull { PlacementCalculator.isInsidePolygon(location, it.outline) }
                if (techZone != null) techZoneNames.add(techZone.name)
            }
            cards.add(
                TemplateCard(
                    template = template,
                    statuses = statuses,
                    isApplying = template.id in runningIds,
                    changedSinceApplyCount = changedCount,
                    lastAppliedAt = templateResults.maxOfOrNull { it.appliedAt },
                    cityNames = cityNames.toList(),
                    techZoneNames = techZoneNames.toList(),
                ),
            )
        }
        return cards
    }

    private companion object {
        const val NEW_TEMPLATE_NAME = "Новый шаблон"
    }
}
