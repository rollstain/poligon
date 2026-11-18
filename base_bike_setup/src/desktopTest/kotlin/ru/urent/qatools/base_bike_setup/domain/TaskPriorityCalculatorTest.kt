package ru.urent.qatools.base_bike_setup.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import ru.urent.qatools.base_bike_setup.entities.BatteryTask
import ru.urent.qatools.base_bike_setup.entities.ChargeBand
import ru.urent.qatools.base_bike_setup.entities.ChargeTarget
import ru.urent.qatools.base_bike_setup.entities.PriorityConfig
import ru.urent.qatools.base_bike_setup.entities.PriorityThresholds
import ru.urent.qatools.base_bike_setup.entities.TaskRules
import ru.urent.qatools.base_bike_setup.entities.TaskZone
import ru.urent.qatools.base_bike_setup.entities.TechZoneLimit
import ru.urent.qatools.core_entities.GeoPoint
import ru.urent.qatools.core_entities.TaskPriority

class TaskPriorityCalculatorTest {

    @Test
    fun thresholdsComeFromModelAndCriticalPercentOfPlatform() {
        val thresholds = TaskPriorityCalculator.thresholdsOf(disablePercent = 15.0, needToChangePercent = 25.0, config = Tm0Config)

        assertEquals(PriorityThresholds(criticalPercent = 4, burningPercent = 15, highPercent = 25), thresholds)
    }

    @Test
    fun modelWithoutThresholdsUsesPlatformPercents() {
        val thresholds = TaskPriorityCalculator.thresholdsOf(disablePercent = 0.0, needToChangePercent = 0.0, config = Tm0Config)

        assertEquals(PriorityThresholds(criticalPercent = 4, burningPercent = 10, highPercent = 30), thresholds)
    }

    @Test
    fun inconsistentThresholdsFallBackToBackendDefaults() {
        val thresholds = TaskPriorityCalculator.thresholdsOf(disablePercent = 25.0, needToChangePercent = 15.0, config = Tm0Config)

        assertEquals(PriorityThresholds(criticalPercent = 0, burningPercent = 5, highPercent = 27), thresholds)
    }

    @Test
    fun configuredTechZoneGivesAllPrioritiesFromBusinessDocument() {
        val bands = TaskPriorityCalculator.bandsAt(rules(Adler41), InsideAdler41)

        assertEquals(TaskZone.Limited(zoneName = "Адлер 4.1", taskPercent = 38), bands.zone)
        assertEquals(
            listOf(
                ChargeBand(ChargeTarget.Task(TaskPriority.CRITICAL), 0.0, 4.0),
                ChargeBand(ChargeTarget.Task(TaskPriority.BURNING), 4.0, 15.0),
                ChargeBand(ChargeTarget.Task(TaskPriority.HIGH), 15.0, 25.0),
                ChargeBand(ChargeTarget.Task(TaskPriority.NORMAL), 25.0, 38.0),
                ChargeBand(ChargeTarget.NoTask, 38.0, 100.0),
            ),
            bands.bands,
        )
        assertEquals(listOf(2.0, 9.5, 20.0, 31.5, 69.0), TaskPriorityCalculator.TaskTargets.map { TaskPriorityCalculator.targetPercent(it, bands) })
    }

    @Test
    fun placeOutsideConfiguredTechZonesHasNoNormalTask() {
        val bands = TaskPriorityCalculator.bandsAt(rules(Adler41), OutsideZones)

        assertEquals(TaskZone.GeoIndependent, bands.zone)
        assertNull(TaskPriorityCalculator.targetPercent(ChargeTarget.Task(TaskPriority.NORMAL), bands))
        assertEquals(20.0, TaskPriorityCalculator.targetPercent(ChargeTarget.Task(TaskPriority.HIGH), bands))
        assertEquals(ChargeBand(ChargeTarget.NoTask, 25.0, 100.0), bands.bands.last())
    }

    @Test
    fun switchedOffTechZoneSettingCreatesNoTasksAtAll() {
        val bands = TaskPriorityCalculator.bandsAt(rules(Adler41.copy(isEnabled = false)), InsideAdler41)

        assertEquals(TaskZone.SwitchedOff(zoneName = "Адлер 4.1"), bands.zone)
        assertEquals(listOf(ChargeBand(ChargeTarget.NoTask, 0.0, 100.0)), bands.bands)
        assertNull(TaskPriorityCalculator.targetPercent(ChargeTarget.Task(TaskPriority.CRITICAL), bands))
    }

    @Test
    fun increasedBatteryDividesZonePercentWhenStandEnablesIt() {
        val increasedThresholds = PriorityThresholds(criticalPercent = 4, burningPercent = 10, highPercent = 15)
        val rules = TaskRules(thresholds = increasedThresholds, zoneLimits = listOf(Adler41), zonePercentDivider = DIVIDER)

        val bands = TaskPriorityCalculator.bandsAt(rules, InsideAdler41)

        assertEquals(TaskZone.Limited(zoneName = "Адлер 4.1", taskPercent = 35), bands.zone)
        assertEquals(25.0, TaskPriorityCalculator.targetPercent(ChargeTarget.Task(TaskPriority.NORMAL), bands))
    }

    @Test
    fun widestEnabledTechZoneUnderBikeLimitsTasks() {
        val narrowZone = Adler41.copy(name = "Адлер узкая", zonePercent = 30)

        val bands = TaskPriorityCalculator.bandsAt(rules(narrowZone, Adler41), InsideAdler41)

        assertEquals(TaskZone.Limited(zoneName = "Адлер 4.1", taskPercent = 38), bands.zone)
    }

    @Test
    fun techZonePercentBelowRecommendedLevelCutsHighPriority() {
        val bands = TaskPriorityCalculator.bandsAt(rules(Adler41.copy(zonePercent = 20)), InsideAdler41)

        assertEquals(ChargeBand(ChargeTarget.Task(TaskPriority.HIGH), 15.0, 20.0), bands.bands.first { it.target == ChargeTarget.Task(TaskPriority.HIGH) })
        assertNull(TaskPriorityCalculator.targetPercent(ChargeTarget.Task(TaskPriority.NORMAL), bands))
        assertEquals(60.0, TaskPriorityCalculator.targetPercent(ChargeTarget.NoTask, bands))
    }

    @Test
    fun targetIsReachedOnlyWhenStandTaskMatchesIt() {
        val high = ChargeTarget.Task(TaskPriority.HIGH)

        assertTrue(TaskPriorityCalculator.isReached(high, BatteryTask.Active(TaskPriority.HIGH)))
        assertFalse(TaskPriorityCalculator.isReached(high, BatteryTask.Active(TaskPriority.BURNING)))
        assertFalse(TaskPriorityCalculator.isReached(high, BatteryTask.Unknown))
        assertTrue(TaskPriorityCalculator.isReached(ChargeTarget.NoTask, BatteryTask.Absent))
        assertFalse(TaskPriorityCalculator.isReached(ChargeTarget.NoTask, BatteryTask.Unknown))
        assertTrue(TaskPriorityCalculator.isReached(ChargeTarget.Percent(37.5), BatteryTask.Unknown))
    }

    private fun rules(vararg zones: TechZoneLimit) = TaskRules(
        thresholds = PriorityThresholds(criticalPercent = 4, burningPercent = 15, highPercent = 25),
        zoneLimits = zones.toList(),
        zonePercentDivider = null,
    )

    private companion object {
        const val DIVIDER = 1.1
        val Tm0Config = PriorityConfig(
            criticalPercent = 4,
            burningFallbackPercent = 10,
            highFallbackPercent = 30,
            defaultZonePercent = 45,
            isIncreasedBatteryEnabled = false,
            increasedBatteryDivider = DIVIDER,
        )
        val InsideAdler41 = GeoPoint(latitude = 43.43, longitude = 39.93)
        val OutsideZones = GeoPoint(latitude = 43.5, longitude = 39.93)
        val Adler41 = TechZoneLimit(
            name = "Адлер 4.1",
            outline = listOf(
                GeoPoint(latitude = 43.42, longitude = 39.92),
                GeoPoint(latitude = 43.42, longitude = 39.94),
                GeoPoint(latitude = 43.44, longitude = 39.94),
                GeoPoint(latitude = 43.44, longitude = 39.92),
            ),
            isEnabled = true,
            zonePercent = 38,
        )
    }
}
