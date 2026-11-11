package ru.urent.qatools.feature_settings.presentation.mvi

import java.io.File
import ru.urent.qatools.base_bike_setup.entities.AvailabilityTarget
import ru.urent.qatools.base_bike_setup.entities.ChargeTarget
import ru.urent.qatools.core_settings.AppTheme

sealed interface SettingsEvent {
    data class OnThemeSelected(val theme: AppTheme) : SettingsEvent

    data class OnUiScaleSelected(val uiScalePercent: Int) : SettingsEvent

    data object OnWindowedModeSelected : SettingsEvent

    data object OnBorderlessModeSelected : SettingsEvent

    data class OnForgetStandClicked(val baseUrl: String) : SettingsEvent

    data object OnLogoutClicked : SettingsEvent

    data object OnOpenLogsFolderClicked : SettingsEvent

    data object OnOpenDataFolderClicked : SettingsEvent

    data class OnLogArchiveFileChosen(val target: File) : SettingsEvent

    data class OnDefaultTargetChanged(val target: ChargeTarget) : SettingsEvent

    data class OnDefaultAvailabilityChanged(val availability: AvailabilityTarget) : SettingsEvent

    data object OnCheckUpdatesClicked : SettingsEvent
}
