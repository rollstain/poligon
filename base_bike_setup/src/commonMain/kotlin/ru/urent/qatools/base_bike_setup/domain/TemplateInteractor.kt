package ru.urent.qatools.base_bike_setup.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.urent.qatools.base_bike_setup.domain.mappers.TemplateMapper
import ru.urent.qatools.base_bike_setup.entities.AvailabilityTarget
import ru.urent.qatools.base_bike_setup.entities.BikeApplyResult
import ru.urent.qatools.base_bike_setup.entities.BikeSetup
import ru.urent.qatools.base_bike_setup.entities.ChargeTarget
import ru.urent.qatools.base_bike_setup.entities.SetupDefaults
import ru.urent.qatools.base_bike_setup.entities.Template
import ru.urent.qatools.core_entities.GeoPoint
import ru.urent.qatools.data_templates.TemplateRepository

class TemplateInteractor(
    private val templateRepository: TemplateRepository,
    private val templateMapper: TemplateMapper,
) {
    val setupDefaults: Flow<SetupDefaults> = templateRepository.setupDefaults.map { entity -> templateMapper.toSetupDefaults(entity) }

    fun getTemplates(standBaseUrl: String): Flow<List<Template>> =
        templateRepository.getTemplates(standBaseUrl).map { entities -> entities.map { entity -> templateMapper.toDomain(entity) } }

    fun getTemplate(templateId: Long): Flow<Template?> = templateRepository.getTemplate(templateId).map { entity ->
        if (entity == null) null else templateMapper.toDomain(entity)
    }

    fun getApplyResults(standBaseUrl: String): Flow<Map<Long, BikeApplyResult>> =
        templateRepository.getApplyResults(standBaseUrl).map { entities ->
            val results = mutableMapOf<Long, BikeApplyResult>()
            for (entity in entities) {
                val result = templateMapper.toDomain(entity) ?: continue
                results[entity.templateBikeId] = result
            }
            results
        }

    suspend fun create(standBaseUrl: String, name: String): Long = templateRepository.create(standBaseUrl = standBaseUrl, name = name)

    suspend fun rename(templateId: Long, name: String) = templateRepository.rename(templateId = templateId, name = name)

    suspend fun delete(templateId: Long) = templateRepository.delete(templateId)

    suspend fun addBike(templateId: Long, setup: BikeSetup) =
        templateRepository.addBike(templateId = templateId, setup = templateMapper.toDatabase(setup))

    suspend fun moveBike(bikeId: Long, location: GeoPoint) = templateRepository.moveBike(bikeId = bikeId, location = location)

    suspend fun changeBikeSettings(bikeId: Long, target: ChargeTarget, availability: AvailabilityTarget) =
        templateRepository.changeBikeSettings(bikeId = bikeId, settings = templateMapper.toDatabase(target = target, availability = availability))

    suspend fun deleteBike(bikeId: Long) = templateRepository.deleteBike(bikeId)

    suspend fun saveApplyResult(templateBikeId: Long, result: BikeApplyResult) =
        templateRepository.saveApplyResult(templateMapper.toDatabase(templateBikeId = templateBikeId, result = result))

    suspend fun saveSetupDefaults(defaults: SetupDefaults) =
        templateRepository.saveSetupDefaults(templateMapper.toDatabase(target = defaults.target, availability = defaults.availability))
}
