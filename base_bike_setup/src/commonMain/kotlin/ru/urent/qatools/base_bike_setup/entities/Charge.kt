package ru.urent.qatools.base_bike_setup.entities

import ru.urent.qatools.core_entities.GeoPoint
import ru.urent.qatools.core_entities.TaskPriority

sealed interface ChargeTarget {
    data class Task(val priority: TaskPriority) : ChargeTarget
    data object NoTask : ChargeTarget
    data class Percent(val percent: Double) : ChargeTarget
}

data class PriorityConfig(
    val criticalPercent: Int,
    val burningFallbackPercent: Int,
    val highFallbackPercent: Int,
    val defaultZonePercent: Int,
    val isIncreasedBatteryEnabled: Boolean,
    val increasedBatteryDivider: Double,
)

data class PriorityThresholds(val criticalPercent: Int, val burningPercent: Int, val highPercent: Int)

data class TechZoneLimit(val name: String, val outline: List<GeoPoint>, val isEnabled: Boolean, val zonePercent: Int)

data class TaskRules(val thresholds: PriorityThresholds, val zoneLimits: List<TechZoneLimit>, val zonePercentDivider: Double?)

data class ChargeBand(val target: ChargeTarget, val fromPercent: Double, val toPercent: Double)

sealed interface TaskZone {
    data class Limited(val zoneName: String, val taskPercent: Int) : TaskZone
    data object GeoIndependent : TaskZone
    data class SwitchedOff(val zoneName: String) : TaskZone
}

data class ChargeBands(val bands: List<ChargeBand>, val zone: TaskZone)

data class VoltageRange(val minVolts: Double, val maxVolts: Double)
