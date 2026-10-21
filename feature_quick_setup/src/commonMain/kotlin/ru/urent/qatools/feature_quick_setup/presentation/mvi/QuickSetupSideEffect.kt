package ru.urent.qatools.feature_quick_setup.presentation.mvi

sealed interface QuickSetupSideEffect {
    data class OpenQrCode(val text: String) : QuickSetupSideEffect
}
