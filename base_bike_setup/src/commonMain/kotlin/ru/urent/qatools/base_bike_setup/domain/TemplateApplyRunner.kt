package ru.urent.qatools.base_bike_setup.domain

import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.urent.qatools.base_bike_setup.entities.ApplyStatus
import ru.urent.qatools.base_bike_setup.entities.BikeApplyResult
import ru.urent.qatools.base_bike_setup.entities.BikeContext
import ru.urent.qatools.base_bike_setup.entities.Template
import ru.urent.qatools.base_bike_setup.entities.TemplateBike

class TemplateApplyRunner(
    private val bikeSetupInteractor: BikeSetupInteractor,
    private val templateInteractor: TemplateInteractor,
    standBaseUrls: Flow<String?>,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val appliesJob = SupervisorJob()
    private val sessionResults = MutableStateFlow<Map<Long, BikeApplyResult>>(emptyMap())
    private val mutableRunningTemplateIds = MutableStateFlow<Set<Long>>(emptySet())
    val runningTemplateIds: StateFlow<Set<Long>> = mutableRunningTemplateIds.asStateFlow()

    init {
        scope.launch {
            standBaseUrls.distinctUntilChanged().drop(1).collect {
                appliesJob.cancelChildren()
                sessionResults.value = emptyMap()
            }
        }
    }

    fun getResults(standBaseUrl: String): Flow<Map<Long, BikeApplyResult>> =
        combine(templateInteractor.getApplyResults(standBaseUrl), sessionResults) { stored, session -> stored + session }

    suspend fun confirmRecalculatedCharge(standBaseUrl: String, bike: TemplateBike, standBike: BikeContext): Boolean {
        val current = getResults(standBaseUrl).first()[bike.id] ?: return false
        val applied = current.status as? ApplyStatus.Done ?: return false
        if (applied.isTargetReached || current.appliedSetup != bike.setup) return false
        val status = bikeSetupInteractor.confirmRecalculatedCharge(setup = bike.setup, applied = applied, bike = standBike)
        if (status == applied) return false
        val result = current.copy(status = status)
        sessionResults.update { it + (bike.id to result) }
        templateInteractor.saveApplyResult(templateBikeId = bike.id, result = result)
        return true
    }

    fun applyTemplate(standBaseUrl: String, template: Template) {
        mutableRunningTemplateIds.update { it + template.id }
        val appliedAt = Clock.System.now()
        val setups = template.bikes.associate { bike -> bike.id to bike.setup }
        val apply = scope.launch(appliesJob) {
            bikeSetupInteractor.applyAll(standBaseUrl = standBaseUrl, bikes = template.bikes).collect { (bikeId, status) ->
                val result = BikeApplyResult(appliedSetup = setups.getValue(bikeId), status = status, appliedAt = appliedAt)
                sessionResults.update { it + (bikeId to result) }
                val isFinished = status is ApplyStatus.Done || status is ApplyStatus.Failed
                if (isFinished) templateInteractor.saveApplyResult(templateBikeId = bikeId, result = result)
            }
        }
        apply.invokeOnCompletion { mutableRunningTemplateIds.update { it - template.id } }
    }
}
