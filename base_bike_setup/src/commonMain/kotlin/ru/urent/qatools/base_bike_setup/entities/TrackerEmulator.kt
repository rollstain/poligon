package ru.urent.qatools.base_bike_setup.entities

import kotlin.time.Duration

sealed interface TrackerEmulator {
    data class NavtelecomSimulator(val simulatorId: Int, val voltageRange: VoltageRange) : TrackerEmulator

    data object Omni : TrackerEmulator

    data class Voi(val reportInterval: Duration) : TrackerEmulator
}
