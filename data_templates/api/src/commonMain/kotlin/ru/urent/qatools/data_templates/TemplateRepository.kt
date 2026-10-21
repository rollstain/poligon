package ru.urent.qatools.data_templates

import kotlinx.coroutines.flow.Flow
import ru.urent.qatools.core_entities.GeoPoint

interface TemplateRepository {
    val setupDefaults: Flow<BikeSettingsEntity?>
    fun getTemplates(standBaseUrl: String): Flow<List<TemplateEntity>>
    fun getTemplate(templateId: Long): Flow<TemplateEntity?>
    fun getApplyResults(standBaseUrl: String): Flow<List<ApplyResultEntity>>
    suspend fun create(standBaseUrl: String, name: String): Long
    suspend fun rename(templateId: Long, name: String)
    suspend fun delete(templateId: Long)
    suspend fun addBike(templateId: Long, setup: BikeSetupEntity)
    suspend fun moveBike(bikeId: Long, location: GeoPoint)
    suspend fun changeBikeSettings(bikeId: Long, settings: BikeSettingsEntity)
    suspend fun deleteBike(bikeId: Long)
    suspend fun saveApplyResult(result: ApplyResultEntity)
    suspend fun saveSetupDefaults(settings: BikeSettingsEntity)
}
