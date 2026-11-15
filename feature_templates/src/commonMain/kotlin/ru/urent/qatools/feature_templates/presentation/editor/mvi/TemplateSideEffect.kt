package ru.urent.qatools.feature_templates.presentation.editor.mvi

import ru.urent.qatools.core_entities.GeoPoint

sealed interface TemplateSideEffect {
    data object Back : TemplateSideEffect

    data class OpenQrCode(val text: String) : TemplateSideEffect

    data class FocusMap(val point: GeoPoint) : TemplateSideEffect
}
