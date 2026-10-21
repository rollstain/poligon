package ru.urent.qatools.base_bike_setup.domain

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import ru.urent.qatools.base_bike_setup.entities.ChargeTarget
import ru.urent.qatools.base_bike_setup.entities.ChargeThresholds
import ru.urent.qatools.base_bike_setup.entities.VoltageRange
import ru.urent.qatools.core_entities.ChargeState

class ChargeCalculatorTest {
    private val ninebotL60LThresholds = ChargeThresholds(
        emptyPercent = 0.0,
        powerOffPercent = 10.0,
        disablePercent = 15.0,
        needToChangePercent = 25.0,
    )
    private val ninebotEs4Thresholds = ChargeThresholds(
        emptyPercent = 1.0,
        powerOffPercent = 15.0,
        disablePercent = 25.0,
        needToChangePercent = 26.0,
    )
    private val defaultTrackerRange = VoltageRange(minVolts = 33.4, maxVolts = 41.5)
    private val wideTrackerRange = VoltageRange(minVolts = 31.2, maxVolts = 41.55)

    @Test
    fun lowStateTargetsMiddleOfLowInterval() {
        val percent = ChargeCalculator.targetPercent(ChargeTarget.State(ChargeState.LOW), ninebotL60LThresholds)
        assertEquals(12.5, percent)
    }

    @Test
    fun lowStateOnTracker4071GivesVoltageThatBackendReadsAsTwelveAndHalfPercent() {
        val millivolts = ChargeCalculator.millivolts(
            target = ChargeTarget.State(ChargeState.LOW),
            thresholds = ninebotL60LThresholds,
            range = defaultTrackerRange,
        )
        val backendPercent = backendPercent(millivolts, defaultTrackerRange)
        assertTrue(abs(backendPercent - 12.5) < 0.01, "backend would show $backendPercent%")
    }

    @Test
    fun exactPercentVerifiedOnStandMatchesSentVoltage() {
        val millivolts = ChargeCalculator.millivolts(
            target = ChargeTarget.Percent(12.506150610652986),
            thresholds = ninebotL60LThresholds,
            range = defaultTrackerRange,
        )
        assertEquals(34413, millivolts)
    }

    @Test
    fun needToChangeOnWideRangeTrackerUsesThatTrackerRange() {
        val millivolts = ChargeCalculator.millivolts(
            target = ChargeTarget.State(ChargeState.NEED_TO_CHANGE),
            thresholds = ninebotL60LThresholds,
            range = wideTrackerRange,
        )
        assertEquals(33270, millivolts)
    }

    @Test
    fun emptyStateGoesOneVoltBelowTrackerMinimum() {
        val millivolts = ChargeCalculator.millivolts(
            target = ChargeTarget.State(ChargeState.EMPTY),
            thresholds = ninebotEs4Thresholds,
            range = defaultTrackerRange,
        )
        assertEquals(32400, millivolts)
        assertEquals(0.0, ChargeCalculator.targetPercent(ChargeTarget.State(ChargeState.EMPTY), ninebotEs4Thresholds))
    }

    @Test
    fun midpointsFollowModelSpecificThresholds() {
        assertEquals(8.0, ChargeCalculator.targetPercent(ChargeTarget.State(ChargeState.EXTREMELY_LOW), ninebotEs4Thresholds))
        assertEquals(20.0, ChargeCalculator.targetPercent(ChargeTarget.State(ChargeState.LOW), ninebotEs4Thresholds))
        assertEquals(25.5, ChargeCalculator.targetPercent(ChargeTarget.State(ChargeState.NEED_TO_CHANGE), ninebotEs4Thresholds))
        assertEquals(63.0, ChargeCalculator.targetPercent(ChargeTarget.State(ChargeState.NORMAL), ninebotEs4Thresholds))
    }

    private fun backendPercent(millivolts: Int, range: VoltageRange): Double =
        (millivolts / 1000.0 - range.minVolts) / (range.maxVolts - range.minVolts) * 100
}
