package ru.urent.qatools.base_bike_setup.domain.mappers

import ru.urent.qatools.base_bike_setup.entities.BatteryTask
import ru.urent.qatools.base_bike_setup.entities.PriorityConfig
import ru.urent.qatools.base_bike_setup.entities.TechZone
import ru.urent.qatools.base_bike_setup.entities.TechZoneLimit
import ru.urent.qatools.data_opstask.response.OpsTaskConfigResponse
import ru.urent.qatools.data_opstask.response.OpsTasksResponse
import ru.urent.qatools.data_opstask.response.TechZoneSettingsResponse

class OpsTaskMapper {

    fun toPriorityConfig(response: OpsTaskConfigResponse): PriorityConfig {
        val entries = response.entries.orEmpty()
        val divider = valueOf(entries, INCREASED_BATTERY_DIVIDER_KEY)?.toDoubleOrNull() ?: DEFAULT_INCREASED_BATTERY_DIVIDER
        return PriorityConfig(
            criticalPercent = valueOf(entries, CRITICAL_PERCENT_KEY)?.toIntOrNull() ?: DEFAULT_CRITICAL_PERCENT,
            burningFallbackPercent = valueOf(entries, BURNING_PERCENT_KEY)?.toIntOrNull() ?: DEFAULT_BURNING_PERCENT,
            highFallbackPercent = valueOf(entries, HIGH_PERCENT_KEY)?.toIntOrNull() ?: DEFAULT_HIGH_PERCENT,
            defaultZonePercent = valueOf(entries, DEFAULT_ZONE_PERCENT_KEY)?.toIntOrNull() ?: DEFAULT_ZONE_PERCENT,
            isIncreasedBatteryEnabled = valueOf(entries, INCREASED_BATTERY_ENABLED_KEY).equals(TRUE_TEXT, ignoreCase = true),
            increasedBatteryDivider = if (divider == 0.0) DEFAULT_INCREASED_BATTERY_DIVIDER else divider,
        )
    }

    fun toZoneLimits(settings: List<TechZoneSettingsResponse.Entry>, techZones: List<TechZone>, defaultZonePercent: Int): List<TechZoneLimit> {
        val techZonesById = techZones.associateBy { it.id }
        val limits = mutableListOf<TechZoneLimit>()
        for (setting in settings) {
            val zoneId = setting.zoneId
            val techZone = if (zoneId == null) null else techZonesById[zoneId]
            if (techZone == null) continue
            limits.add(
                TechZoneLimit(
                    name = techZone.name,
                    outline = techZone.outline,
                    isEnabled = setting.isEnabled == true,
                    zonePercent = setting.vehicleBatteryPercent ?: defaultZonePercent,
                ),
            )
        }
        return limits
    }

    fun toBatteryTask(response: OpsTasksResponse): BatteryTask {
        val tasks = response.entries.orEmpty()
        if (tasks.isEmpty()) return BatteryTask.Absent
        val priority = tasks.firstNotNullOfOrNull { it.priority } ?: return BatteryTask.Unknown
        return BatteryTask.Active(priority)
    }

    private fun valueOf(entries: List<OpsTaskConfigResponse.Entry>, key: String): String? {
        val entry = entries.firstOrNull { it.name == key } ?: return null
        val data = entry.data
        return if (data.isNullOrBlank()) entry.defaultData else data
    }

    private companion object {
        const val CRITICAL_PERCENT_KEY = "OpsTaskPriority:ChangeBatteryCriticalPriorityPercent"
        const val BURNING_PERCENT_KEY = "OpsTaskPriority:ChangeBatteryBurningPriorityPercent"
        const val HIGH_PERCENT_KEY = "OpsTaskPriority:ChangeBatteryHighPriorityPercent"
        const val DEFAULT_ZONE_PERCENT_KEY = "OpsTasks:TechZoneSettings:DefaultCreationBatteryPercent"
        const val INCREASED_BATTERY_ENABLED_KEY = "IncreasedBattery:IsEnabled"
        const val INCREASED_BATTERY_DIVIDER_KEY = "IncreasedBattery:DividerForNormalPriority"
        const val TRUE_TEXT = "true"
        const val DEFAULT_CRITICAL_PERCENT = 0
        const val DEFAULT_BURNING_PERCENT = 5
        const val DEFAULT_HIGH_PERCENT = 27
        const val DEFAULT_ZONE_PERCENT = 45
        const val DEFAULT_INCREASED_BATTERY_DIVIDER = 1.0
    }
}
