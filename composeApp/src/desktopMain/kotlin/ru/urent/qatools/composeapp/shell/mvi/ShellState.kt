package ru.urent.qatools.composeapp.shell.mvi

import ru.urent.qatools.base_stand.StandConnectionState
import ru.urent.qatools.base_stand.StandOption

data class ShellState(
    val currentBaseUrl: String?,
    val standHost: String,
    val standLabel: String,
    val standOptions: List<StandOption>,
    val isSwitchingStand: Boolean,
    val standSwitchError: String?,
    val isSidebarCollapsed: Boolean,
    val connectionState: StandConnectionState,
)
