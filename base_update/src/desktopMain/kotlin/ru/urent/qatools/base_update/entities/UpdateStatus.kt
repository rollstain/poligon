package ru.urent.qatools.base_update.entities

import java.io.File

sealed interface UpdateStatus {
    data object NotChecked : UpdateStatus

    data object Checking : UpdateStatus

    data object UpToDate : UpdateStatus

    data object CheckFailed : UpdateStatus

    data object DevelopmentBuild : UpdateStatus

    sealed interface Found : UpdateStatus {
        val installer: ReleaseInstaller
    }

    data class Available(override val installer: ReleaseInstaller) : Found

    data class Downloading(override val installer: ReleaseInstaller, val percent: Int) : Found

    data class DownloadFailed(override val installer: ReleaseInstaller) : Found

    data class Downloaded(override val installer: ReleaseInstaller, val file: File) : Found

    data class Installing(override val installer: ReleaseInstaller) : Found

    data class InstallFailed(override val installer: ReleaseInstaller, val file: File) : Found
}
