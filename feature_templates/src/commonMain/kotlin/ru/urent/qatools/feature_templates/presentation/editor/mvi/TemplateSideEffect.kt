package ru.urent.qatools.feature_templates.presentation.editor.mvi

sealed interface TemplateSideEffect {
    data object Back : TemplateSideEffect

    data class OpenQrCode(val text: String) : TemplateSideEffect
}
