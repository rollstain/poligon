package ru.urent.qatools.feature_settings.presentation.mvi

import ru.urent.qatools.base_bike_setup.entities.DisabledReason
import ru.urent.qatools.base_bike_setup.entities.SetupDefaults
import ru.urent.qatools.base_stand.Stand
import ru.urent.qatools.core_settings.AppSettings

data class SettingsState(
    val appSettings: AppSettings?,
    val email: String?,
    val currentBaseUrl: String?,
    val currentHost: String?,
    val savedStands: List<Stand>,
    val archiveMessage: String?,
    val isArchiveSaved: Boolean,
    val setupDefaults: SetupDefaults?,
    val reasons: List<DisabledReason>,
    val dataDirectoryPath: String,
)
