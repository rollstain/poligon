package ru.urent.qatools.base_bike_setup.entities

import ru.urent.qatools.core_entities.ChargeState

sealed interface ChargeTarget {
    data class State(val state: ChargeState) : ChargeTarget
    data class Percent(val percent: Double) : ChargeTarget
}

data class ChargeThresholds(
    val emptyPercent: Double,
    val powerOffPercent: Double,
    val disablePercent: Double,
    val needToChangePercent: Double,
)

data class VoltageRange(val minVolts: Double, val maxVolts: Double)
