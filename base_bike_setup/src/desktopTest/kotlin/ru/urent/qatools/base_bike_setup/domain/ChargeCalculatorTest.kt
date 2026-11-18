package ru.urent.qatools.base_bike_setup.domain

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import ru.urent.qatools.base_bike_setup.entities.VoltageRange

class ChargeCalculatorTest {
    private val defaultTrackerRange = VoltageRange(minVolts = 33.4, maxVolts = 41.5)
    private val wideTrackerRange = VoltageRange(minVolts = 31.2, maxVolts = 41.55)

    @Test
    fun wholePercentIsRounded() {
        assertEquals(38, ChargeCalculator.wholePercent(37.5))
        assertEquals(37, ChargeCalculator.wholePercent(37.4))
        assertEquals(10, ChargeCalculator.wholePercent(9.5))
    }

    @Test
    fun percentOnTracker4071GivesVoltageThatBackendReadsAsSamePercent() {
        val millivolts = ChargeCalculator.millivolts(percent = 9.5, range = defaultTrackerRange)

        val backendPercent = backendPercent(millivolts, defaultTrackerRange)
        assertTrue(abs(backendPercent - 9.5) < 0.01, "backend would show $backendPercent%")
    }

    @Test
    fun exactPercentVerifiedOnStandMatchesSentVoltage() {
        assertEquals(34413, ChargeCalculator.millivolts(percent = 12.506150610652986, range = defaultTrackerRange))
    }

    @Test
    fun wideRangeTrackerUsesThatTrackerRange() {
        assertEquals(33270, ChargeCalculator.millivolts(percent = 20.0, range = wideTrackerRange))
    }

    @Test
    fun zeroPercentIsTrackerMinimum() {
        assertEquals(33400, ChargeCalculator.millivolts(percent = 0.0, range = defaultTrackerRange))
    }

    private fun backendPercent(millivolts: Int, range: VoltageRange): Double =
        (millivolts / 1000.0 - range.minVolts) / (range.maxVolts - range.minVolts) * 100
}
