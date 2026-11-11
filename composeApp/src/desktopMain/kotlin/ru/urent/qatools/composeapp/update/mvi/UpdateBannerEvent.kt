package ru.urent.qatools.composeapp.update.mvi

sealed interface UpdateBannerEvent {
    data object OnDownloadClicked : UpdateBannerEvent

    data object OnInstallClicked : UpdateBannerEvent

    data object OnLaterClicked : UpdateBannerEvent
}
