package ru.urent.qatools.base_bike_setup.domain

import ru.urent.qatools.base_bike_setup.domain.mappers.LocationMapper
import ru.urent.qatools.base_bike_setup.domain.mappers.OpsTaskMapper
import ru.urent.qatools.base_bike_setup.entities.BatteryTask
import ru.urent.qatools.base_bike_setup.entities.TaskRules
import ru.urent.qatools.base_bike_setup.entities.TechZone
import ru.urent.qatools.core_entities.BatteryCapacityCategory
import ru.urent.qatools.core_entities.RequestResult
import ru.urent.qatools.data_location.LocationRepository
import ru.urent.qatools.data_opstask.OpsTaskRepository

class BatteryTaskInteractor(
    private val opsTaskRepository: OpsTaskRepository,
    private val locationRepository: LocationRepository,
    private val locationMapper: LocationMapper,
    private val opsTaskMapper: OpsTaskMapper,
) {

    suspend fun getRules(batteryCategory: BatteryCapacityCategory, disablePercent: Double, needToChangePercent: Double): RequestResult<TaskRules> {
        val config = when (val loaded = opsTaskRepository.getConfig()) {
            is RequestResult.Error -> return loaded
            is RequestResult.Success -> opsTaskMapper.toPriorityConfig(loaded.data)
        }
        val settings = when (val loaded = opsTaskRepository.getTechZoneSettings()) {
            is RequestResult.Error -> return loaded
            is RequestResult.Success -> loaded.data
        }
        val cityIds = settings.mapNotNull { it.areaId }.distinct()
        val techZones = when (val loaded = loadTechZones(cityIds)) {
            is RequestResult.Error -> return loaded
            is RequestResult.Success -> loaded.data
        }
        val isZonePercentDivided = config.isIncreasedBatteryEnabled && batteryCategory == BatteryCapacityCategory.INCREASED
        val rules = TaskRules(
            thresholds = TaskPriorityCalculator.thresholdsOf(
                disablePercent = disablePercent,
                needToChangePercent = needToChangePercent,
                config = config,
            ),
            zoneLimits = opsTaskMapper.toZoneLimits(settings = settings, techZones = techZones, defaultZonePercent = config.defaultZonePercent),
            zonePercentDivider = if (isZonePercentDivided) config.increasedBatteryDivider else null,
        )
        return RequestResult.Success(rules)
    }

    suspend fun getTask(identifier: String): BatteryTask {
        val loaded = opsTaskRepository.getActiveChangeBatteryTasks(identifier) as? RequestResult.Success ?: return BatteryTask.Unknown
        return opsTaskMapper.toBatteryTask(loaded.data)
    }

    private suspend fun loadTechZones(cityIds: List<String>): RequestResult<List<TechZone>> {
        if (cityIds.isEmpty()) return RequestResult.Success(emptyList())
        return when (val loaded = locationRepository.getTechZones(cityIds)) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> RequestResult.Success(locationMapper.toTechZones(loaded.data))
        }
    }
}
