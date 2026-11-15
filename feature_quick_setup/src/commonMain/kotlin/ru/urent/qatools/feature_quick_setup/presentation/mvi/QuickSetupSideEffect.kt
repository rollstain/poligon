package ru.urent.qatools.feature_quick_setup.presentation.mvi

import ru.urent.qatools.core_entities.GeoPoint

sealed interface QuickSetupSideEffect {
    data class OpenQrCode(val text: String) : QuickSetupSideEffect

    data class FocusMap(val point: GeoPoint) : QuickSetupSideEffect
}
