package ru.urent.qatools.base_bike_setup.domain

import kotlin.math.ceil
import kotlin.math.roundToInt
import ru.urent.qatools.base_bike_setup.entities.ChargeTarget
import ru.urent.qatools.base_bike_setup.entities.ChargeThresholds
import ru.urent.qatools.base_bike_setup.entities.TrackerEmulator
import ru.urent.qatools.base_bike_setup.entities.VoltageRange
import ru.urent.qatools.core_entities.ChargeState

object ChargeCalculator {
    private const val EMPTY_PERCENT = 0.0
    private const val FULL_PERCENT = 100.0
    private const val EMPTY_VOLTAGE_MARGIN_VOLTS = 1.0
    private const val MILLIVOLTS_IN_VOLT = 1000.0

    fun targetPercent(target: ChargeTarget, thresholds: ChargeThresholds): Double = when (target) {
        is ChargeTarget.Percent -> target.percent
        is ChargeTarget.State -> when (target.state) {
            ChargeState.EMPTY -> EMPTY_PERCENT
            ChargeState.EXTREMELY_LOW -> middleOf(thresholds.emptyPercent, thresholds.powerOffPercent)
            ChargeState.LOW -> middleOf(thresholds.powerOffPercent, thresholds.disablePercent)
            ChargeState.NEED_TO_CHANGE -> middleOf(thresholds.disablePercent, thresholds.needToChangePercent)
            ChargeState.NORMAL -> middleOf(thresholds.needToChangePercent, FULL_PERCENT)
        }
    }

    fun millivolts(target: ChargeTarget, thresholds: ChargeThresholds, range: VoltageRange): Int {
        val isEmptyState = target is ChargeTarget.State && target.state == ChargeState.EMPTY
        val volts = if (isEmptyState) {
            range.minVolts - EMPTY_VOLTAGE_MARGIN_VOLTS
        } else {
            val percent = targetPercent(target, thresholds)
            range.minVolts + percent / FULL_PERCENT * (range.maxVolts - range.minVolts)
        }
        return (volts * MILLIVOLTS_IN_VOLT).roundToInt()
    }

    fun toMillivolts(volts: Double): Int = (volts * MILLIVOLTS_IN_VOLT).roundToInt()

    fun isEmptyReachable(emulator: TrackerEmulator, thresholds: ChargeThresholds): Boolean =
        emulator is TrackerEmulator.NavtelecomSimulator || thresholds.emptyPercent > EMPTY_PERCENT

    fun wholePercent(target: ChargeTarget, thresholds: ChargeThresholds): Int = when (target) {
        is ChargeTarget.Percent -> target.percent.roundToInt()
        is ChargeTarget.State -> ceil(targetPercent(target, thresholds)).toInt()
    }

    private fun middleOf(from: Double, to: Double): Double = (from + to) / 2
}
