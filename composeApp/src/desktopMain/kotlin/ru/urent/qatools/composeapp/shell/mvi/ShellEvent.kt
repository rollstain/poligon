package ru.urent.qatools.composeapp.shell.mvi

sealed interface ShellEvent {
    data object OnTemplatesClicked : ShellEvent

    data object OnQuickSetupClicked : ShellEvent

    data object OnQrCodeClicked : ShellEvent

    data object OnSettingsClicked : ShellEvent

    data class OnStandSelected(val baseUrl: String) : ShellEvent

    data object OnSidebarCollapseClicked : ShellEvent

    data object OnSidebarExpandClicked : ShellEvent
}
