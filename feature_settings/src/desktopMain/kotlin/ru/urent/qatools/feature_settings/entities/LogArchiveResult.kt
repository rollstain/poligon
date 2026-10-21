package ru.urent.qatools.feature_settings.entities

sealed interface LogArchiveResult {
    data class Saved(val path: String) : LogArchiveResult

    data class Failed(val reason: String) : LogArchiveResult
}
