package ru.urent.qatools.base_bike_setup.domain

import kotlin.math.abs
import kotlin.math.round
import ru.urent.qatools.base_bike_setup.entities.BatteryTask
import ru.urent.qatools.base_bike_setup.entities.ChargeBand
import ru.urent.qatools.base_bike_setup.entities.ChargeBands
import ru.urent.qatools.base_bike_setup.entities.ChargeTarget
import ru.urent.qatools.base_bike_setup.entities.PriorityConfig
import ru.urent.qatools.base_bike_setup.entities.PriorityThresholds
import ru.urent.qatools.base_bike_setup.entities.TaskRules
import ru.urent.qatools.base_bike_setup.entities.TaskZone
import ru.urent.qatools.base_bike_setup.entities.TechZoneLimit
import ru.urent.qatools.core_entities.GeoPoint
import ru.urent.qatools.core_entities.TaskPriority

object TaskPriorityCalculator {
    private const val EMPTY_PERCENT = 0.0
    private const val FULL_PERCENT = 100.0
    private const val BAND_ENDS_COUNT = 2
    private const val SWITCHED_OFF_ZONE_TASK_PERCENT = -1.0
    private const val MISSING_MODEL_THRESHOLD_TOLERANCE = 0.0001
    private const val DEFAULT_CRITICAL_PERCENT = 0
    private const val DEFAULT_BURNING_PERCENT = 5
    private const val DEFAULT_HIGH_PERCENT = 27

    val TaskTargets: List<ChargeTarget> = TaskPriority.entries.map { priority -> ChargeTarget.Task(priority) } + ChargeTarget.NoTask

    fun thresholdsOf(disablePercent: Double, needToChangePercent: Double, config: PriorityConfig): PriorityThresholds {
        val burningPercent = if (abs(disablePercent) < MISSING_MODEL_THRESHOLD_TOLERANCE) {
            config.burningFallbackPercent
        } else {
            round(disablePercent).toInt()
        }
        val highPercent = if (abs(needToChangePercent) < MISSING_MODEL_THRESHOLD_TOLERANCE) {
            config.highFallbackPercent
        } else {
            round(needToChangePercent).toInt()
        }
        val isConsistent = burningPercent < highPercent && config.criticalPercent < burningPercent
        if (!isConsistent) {
            return PriorityThresholds(
                criticalPercent = DEFAULT_CRITICAL_PERCENT,
                burningPercent = DEFAULT_BURNING_PERCENT,
                highPercent = DEFAULT_HIGH_PERCENT,
            )
        }
        return PriorityThresholds(criticalPercent = config.criticalPercent, burningPercent = burningPercent, highPercent = highPercent)
    }

    fun bandsAt(rules: TaskRules, location: GeoPoint): ChargeBands {
        val zone = taskZoneAt(rules, location)
        val thresholds = rules.thresholds
        val taskLimit = when (zone) {
            is TaskZone.Limited -> zone.taskPercent.toDouble()
            TaskZone.GeoIndependent -> thresholds.highPercent.toDouble()
            is TaskZone.SwitchedOff -> SWITCHED_OFF_ZONE_TASK_PERCENT
        }
        val criticalPercent = thresholds.criticalPercent.toDouble()
        val burningPercent = thresholds.burningPercent.toDouble()
        val highPercent = thresholds.highPercent.toDouble()
        val criticalTo = minOf(criticalPercent, taskLimit)
        val burningTo = minOf(burningPercent, taskLimit)
        val highTo = minOf(highPercent, taskLimit)
        val normalTo = minOf(FULL_PERCENT, taskLimit)
        val noTaskFrom = maxOf(taskLimit, EMPTY_PERCENT)
        val bands = listOfNotNull(
            if (criticalTo >= EMPTY_PERCENT) ChargeBand(ChargeTarget.Task(TaskPriority.CRITICAL), EMPTY_PERCENT, criticalTo) else null,
            if (burningTo > criticalPercent) ChargeBand(ChargeTarget.Task(TaskPriority.BURNING), criticalPercent, burningTo) else null,
            if (highTo > burningPercent) ChargeBand(ChargeTarget.Task(TaskPriority.HIGH), burningPercent, highTo) else null,
            if (normalTo > highPercent) ChargeBand(ChargeTarget.Task(TaskPriority.NORMAL), highPercent, normalTo) else null,
            if (noTaskFrom < FULL_PERCENT) ChargeBand(ChargeTarget.NoTask, noTaskFrom, FULL_PERCENT) else null,
        )
        return ChargeBands(bands = bands, zone = zone)
    }

    fun targetPercent(target: ChargeTarget, bands: ChargeBands): Double? {
        if (target is ChargeTarget.Percent) return target.percent
        val band = bands.bands.firstOrNull { it.target == target } ?: return null
        return (band.fromPercent + band.toPercent) / BAND_ENDS_COUNT
    }

    fun isReached(target: ChargeTarget, task: BatteryTask): Boolean = when (target) {
        is ChargeTarget.Percent -> true
        is ChargeTarget.Task -> task == BatteryTask.Active(target.priority)
        ChargeTarget.NoTask -> task == BatteryTask.Absent
    }

    private fun taskZoneAt(rules: TaskRules, location: GeoPoint): TaskZone {
        val containing = rules.zoneLimits.filter { PlacementCalculator.isInsidePolygon(location, it.outline) }
        if (containing.isEmpty()) return TaskZone.GeoIndependent
        val divider = rules.zonePercentDivider
        val widest = containing.filter { it.isEnabled }.maxByOrNull { zone -> taskPercentOf(zone, divider) }
            ?: return TaskZone.SwitchedOff(containing.first().name)
        return TaskZone.Limited(zoneName = widest.name, taskPercent = taskPercentOf(widest, divider))
    }

    private fun taskPercentOf(zone: TechZoneLimit, divider: Double?): Int =
        if (divider == null) zone.zonePercent else round(zone.zonePercent / divider).toInt()
}
