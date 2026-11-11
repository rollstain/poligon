package ru.urent.qatools.data_templates

import ru.urent.qatools.core_entities.ChargeState
import ru.urent.qatools.core_entities.GeoPoint

enum class StoredAvailability { KEEP, AVAILABLE, DISABLED }

data class BikeSettingsEntity(
    val targetState: ChargeState?,
    val targetPercent: Double?,
    val availability: StoredAvailability,
    val disabledReasonName: String?,
    val disabledReasonTitle: String?,
)

data class BikeSetupEntity(val bikeNumber: Int, val location: GeoPoint, val settings: BikeSettingsEntity)

data class TemplateBikeEntity(val id: Long, val setup: BikeSetupEntity)

data class TemplateEntity(val id: Long, val name: String, val bikes: List<TemplateBikeEntity>)

data class ApplyResultEntity(
    val templateBikeId: Long,
    val appliedSetup: BikeSetupEntity,
    val statusJson: String,
    val appliedAtEpochMillis: Long,
)

data class LastCityEntity(val id: String, val name: String)
