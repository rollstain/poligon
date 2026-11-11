package ru.urent.qatools.composeapp.update.mvi

sealed interface UpdateBannerSideEffect {
    data object CloseApp : UpdateBannerSideEffect
}
