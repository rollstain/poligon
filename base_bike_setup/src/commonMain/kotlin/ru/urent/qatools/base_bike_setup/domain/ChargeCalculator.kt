package ru.urent.qatools.base_bike_setup.domain

import kotlin.math.roundToInt
import ru.urent.qatools.base_bike_setup.entities.VoltageRange

object ChargeCalculator {
    private const val FULL_PERCENT = 100.0
    private const val MILLIVOLTS_IN_VOLT = 1000.0

    fun millivolts(percent: Double, range: VoltageRange): Int {
        val volts = range.minVolts + percent / FULL_PERCENT * (range.maxVolts - range.minVolts)
        return toMillivolts(volts)
    }

    fun toMillivolts(volts: Double): Int = (volts * MILLIVOLTS_IN_VOLT).roundToInt()

    fun wholePercent(percent: Double): Int = percent.roundToInt()
}
