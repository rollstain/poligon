package ru.urent.qatools.base_bike_setup.domain.mappers

import kotlin.time.Instant
import kotlinx.serialization.SerializationException
import ru.urent.qatools.base_bike_setup.entities.ApplyStatus
import ru.urent.qatools.base_bike_setup.entities.AvailabilityTarget
import ru.urent.qatools.base_bike_setup.entities.BikeApplyResult
import ru.urent.qatools.base_bike_setup.entities.BikeSetup
import ru.urent.qatools.base_bike_setup.entities.ChargeTarget
import ru.urent.qatools.base_bike_setup.entities.SetupDefaults
import ru.urent.qatools.base_bike_setup.entities.Template
import ru.urent.qatools.base_bike_setup.entities.TemplateBike
import ru.urent.qatools.core_entities.ChargeState
import ru.urent.qatools.core_network.QaJson
import ru.urent.qatools.data_templates.ApplyResultEntity
import ru.urent.qatools.data_templates.BikeSettingsEntity
import ru.urent.qatools.data_templates.BikeSetupEntity
import ru.urent.qatools.data_templates.StoredAvailability
import ru.urent.qatools.data_templates.TemplateEntity

class TemplateMapper {

    fun toDomain(entity: TemplateEntity): Template {
        val bikes = mutableListOf<TemplateBike>()
        for (bike in entity.bikes) {
            val setup = toDomain(bike.setup) ?: continue
            bikes.add(TemplateBike(id = bike.id, setup = setup))
        }
        return Template(id = entity.id, name = entity.name, bikes = bikes)
    }

    fun toDomain(entity: BikeSetupEntity): BikeSetup? {
        val target = targetOf(entity.settings) ?: return null
        return BikeSetup(
            simulatorId = entity.simulatorId,
            location = entity.location,
            target = target,
            availability = availabilityOf(entity.settings),
        )
    }

    fun toDomain(entity: ApplyResultEntity): BikeApplyResult? {
        val setup = toDomain(entity.appliedSetup) ?: return null
        val status = try {
            QaJson.decodeFromString<ApplyStatus>(entity.statusJson)
        } catch (exception: SerializationException) {
            return null
        }
        return BikeApplyResult(appliedSetup = setup, status = status, appliedAt = Instant.fromEpochMilliseconds(entity.appliedAtEpochMillis))
    }

    fun toSetupDefaults(entity: BikeSettingsEntity?): SetupDefaults {
        if (entity == null) return SetupDefaults(target = DefaultTarget, availability = AvailabilityTarget.Keep)
        return SetupDefaults(target = targetOf(entity) ?: DefaultTarget, availability = availabilityOf(entity))
    }

    fun toDatabase(setup: BikeSetup): BikeSetupEntity = BikeSetupEntity(
        simulatorId = setup.simulatorId,
        location = setup.location,
        settings = toDatabase(target = setup.target, availability = setup.availability),
    )

    fun toDatabase(target: ChargeTarget, availability: AvailabilityTarget): BikeSettingsEntity {
        val storedAvailability = when (availability) {
            AvailabilityTarget.Keep -> StoredAvailability.KEEP
            AvailabilityTarget.Available -> StoredAvailability.AVAILABLE
            is AvailabilityTarget.Disabled -> StoredAvailability.DISABLED
        }
        val disabled = availability as? AvailabilityTarget.Disabled
        return BikeSettingsEntity(
            targetState = (target as? ChargeTarget.State)?.state,
            targetPercent = (target as? ChargeTarget.Percent)?.percent,
            availability = storedAvailability,
            disabledReasonName = disabled?.reasonName,
            disabledReasonTitle = disabled?.reasonTitle,
        )
    }

    fun toDatabase(templateBikeId: Long, result: BikeApplyResult): ApplyResultEntity = ApplyResultEntity(
        templateBikeId = templateBikeId,
        appliedSetup = toDatabase(result.appliedSetup),
        statusJson = QaJson.encodeToString(result.status),
        appliedAtEpochMillis = result.appliedAt.toEpochMilliseconds(),
    )

    private fun targetOf(settings: BikeSettingsEntity): ChargeTarget? {
        val state = settings.targetState
        val percent = settings.targetPercent
        return when {
            state != null -> ChargeTarget.State(state)
            percent != null -> ChargeTarget.Percent(percent)
            else -> null
        }
    }

    private fun availabilityOf(settings: BikeSettingsEntity): AvailabilityTarget {
        val reasonName = settings.disabledReasonName
        val reasonTitle = settings.disabledReasonTitle
        return when {
            settings.availability == StoredAvailability.AVAILABLE -> AvailabilityTarget.Available
            settings.availability == StoredAvailability.DISABLED && reasonName != null && reasonTitle != null ->
                AvailabilityTarget.Disabled(reasonName = reasonName, reasonTitle = reasonTitle)
            else -> AvailabilityTarget.Keep
        }
    }

    private companion object {
        val DefaultTarget = ChargeTarget.State(ChargeState.LOW)
    }
}
