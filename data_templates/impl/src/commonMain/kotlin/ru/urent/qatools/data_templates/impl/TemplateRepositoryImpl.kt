package ru.urent.qatools.data_templates.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.urent.qatools.core_entities.ChargeState
import ru.urent.qatools.core_entities.GeoPoint
import ru.urent.qatools.data_templates.ApplyResultEntity
import ru.urent.qatools.data_templates.BikeSettingsEntity
import ru.urent.qatools.data_templates.BikeSetupEntity
import ru.urent.qatools.data_templates.StoredAvailability
import ru.urent.qatools.data_templates.TemplateBikeEntity
import ru.urent.qatools.data_templates.TemplateEntity
import ru.urent.qatools.data_templates.TemplateRepository
import ru.urent.qatools.data_templates.db.BikeSetupDatabase
import ru.urent.qatools.data_templates.db.Template_bike

internal class TemplateRepositoryImpl(private val database: BikeSetupDatabase) : TemplateRepository {
    private val queries = database.templateQueries
    private val defaultsQueries = database.setupDefaultsQueries
    private val resultQueries = database.applyResultQueries

    override val setupDefaults: Flow<BikeSettingsEntity?> = defaultsQueries.selectDefaults().asFlow().mapToOneOrNull(Dispatchers.IO).map { row ->
        if (row == null) {
            null
        } else {
            settingsOf(
                targetState = row.target_state,
                targetPercent = row.target_percent,
                availability = row.availability,
                reasonName = row.disabled_reason_name,
                reasonTitle = row.disabled_reason_title,
            )
        }
    }

    override fun getTemplates(standBaseUrl: String): Flow<List<TemplateEntity>> = combine(
        queries.selectTemplates(standBaseUrl).asFlow().mapToList(Dispatchers.IO),
        queries.selectBikesForStand(standBaseUrl).asFlow().mapToList(Dispatchers.IO),
    ) { templateRows, bikeRows ->
        val bikesByTemplate = bikeRows.groupBy { it.template_id }
        val templates = mutableListOf<TemplateEntity>()
        for (row in templateRows) {
            val bikes = bikesByTemplate[row.id].orEmpty().map { bikeRow -> templateBikeOf(bikeRow) }
            templates.add(TemplateEntity(id = row.id, name = row.name, bikes = bikes))
        }
        templates
    }

    override fun getTemplate(templateId: Long): Flow<TemplateEntity?> = combine(
        queries.selectTemplate(templateId).asFlow().mapToOneOrNull(Dispatchers.IO),
        queries.selectBikes(templateId).asFlow().mapToList(Dispatchers.IO),
    ) { templateRow, bikeRows ->
        if (templateRow == null) {
            null
        } else {
            TemplateEntity(id = templateRow.id, name = templateRow.name, bikes = bikeRows.map { bikeRow -> templateBikeOf(bikeRow) })
        }
    }

    override fun getApplyResults(standBaseUrl: String): Flow<List<ApplyResultEntity>> =
        resultQueries.selectResultsForStand(standBaseUrl).asFlow().mapToList(Dispatchers.IO).map { rows ->
            val results = mutableListOf<ApplyResultEntity>()
            for (row in rows) {
                val setup = BikeSetupEntity(
                    simulatorId = row.simulator_id.toInt(),
                    location = GeoPoint(latitude = row.latitude, longitude = row.longitude),
                    settings = settingsOf(
                        targetState = row.target_state,
                        targetPercent = row.target_percent,
                        availability = row.availability,
                        reasonName = row.disabled_reason_name,
                        reasonTitle = row.disabled_reason_title,
                    ),
                )
                results.add(
                    ApplyResultEntity(
                        templateBikeId = row.template_bike_id,
                        appliedSetup = setup,
                        statusJson = row.status_json,
                        appliedAtEpochMillis = row.applied_at_epoch_millis,
                    ),
                )
            }
            results
        }

    override suspend fun create(standBaseUrl: String, name: String): Long = withContext(Dispatchers.IO) {
        database.transactionWithResult {
            queries.insertTemplate(stand_base_url = standBaseUrl, name = name)
            queries.lastInsertedId().executeAsOne()
        }
    }

    override suspend fun rename(templateId: Long, name: String): Unit = withContext(Dispatchers.IO) {
        queries.renameTemplate(name = name, id = templateId)
    }

    override suspend fun delete(templateId: Long): Unit = withContext(Dispatchers.IO) {
        queries.deleteTemplate(templateId)
    }

    override suspend fun addBike(templateId: Long, setup: BikeSetupEntity): Unit = withContext(Dispatchers.IO) {
        val settings = setup.settings
        queries.insertBike(
            template_id = templateId,
            simulator_id = setup.simulatorId.toLong(),
            latitude = setup.location.latitude,
            longitude = setup.location.longitude,
            target_state = settings.targetState?.name,
            target_percent = settings.targetPercent,
            availability = settings.availability.name,
            disabled_reason_name = settings.disabledReasonName,
            disabled_reason_title = settings.disabledReasonTitle,
        )
    }

    override suspend fun moveBike(bikeId: Long, location: GeoPoint): Unit = withContext(Dispatchers.IO) {
        queries.moveBike(latitude = location.latitude, longitude = location.longitude, id = bikeId)
    }

    override suspend fun changeBikeSettings(bikeId: Long, settings: BikeSettingsEntity): Unit = withContext(Dispatchers.IO) {
        queries.changeBikeSettings(
            target_state = settings.targetState?.name,
            target_percent = settings.targetPercent,
            availability = settings.availability.name,
            disabled_reason_name = settings.disabledReasonName,
            disabled_reason_title = settings.disabledReasonTitle,
            id = bikeId,
        )
    }

    override suspend fun deleteBike(bikeId: Long): Unit = withContext(Dispatchers.IO) {
        queries.deleteBike(bikeId)
    }

    override suspend fun saveApplyResult(result: ApplyResultEntity): Unit = withContext(Dispatchers.IO) {
        val setup = result.appliedSetup
        val settings = setup.settings
        resultQueries.saveResult(
            template_bike_id = result.templateBikeId,
            applied_at_epoch_millis = result.appliedAtEpochMillis,
            simulator_id = setup.simulatorId.toLong(),
            latitude = setup.location.latitude,
            longitude = setup.location.longitude,
            target_state = settings.targetState?.name,
            target_percent = settings.targetPercent,
            availability = settings.availability.name,
            disabled_reason_name = settings.disabledReasonName,
            disabled_reason_title = settings.disabledReasonTitle,
            status_json = result.statusJson,
        )
    }

    override suspend fun saveSetupDefaults(settings: BikeSettingsEntity): Unit = withContext(Dispatchers.IO) {
        defaultsQueries.saveDefaults(
            target_state = settings.targetState?.name,
            target_percent = settings.targetPercent,
            availability = settings.availability.name,
            disabled_reason_name = settings.disabledReasonName,
            disabled_reason_title = settings.disabledReasonTitle,
        )
    }

    private fun templateBikeOf(row: Template_bike): TemplateBikeEntity = TemplateBikeEntity(
        id = row.id,
        setup = BikeSetupEntity(
            simulatorId = row.simulator_id.toInt(),
            location = GeoPoint(latitude = row.latitude, longitude = row.longitude),
            settings = settingsOf(
                targetState = row.target_state,
                targetPercent = row.target_percent,
                availability = row.availability,
                reasonName = row.disabled_reason_name,
                reasonTitle = row.disabled_reason_title,
            ),
        ),
    )

    private fun settingsOf(
        targetState: String?,
        targetPercent: Double?,
        availability: String,
        reasonName: String?,
        reasonTitle: String?,
    ): BikeSettingsEntity = BikeSettingsEntity(
        targetState = if (targetState == null) null else runCatching { ChargeState.valueOf(targetState) }.getOrNull(),
        targetPercent = targetPercent,
        availability = runCatching { StoredAvailability.valueOf(availability) }.getOrNull() ?: StoredAvailability.KEEP,
        disabledReasonName = reasonName,
        disabledReasonTitle = reasonTitle,
    )
}
