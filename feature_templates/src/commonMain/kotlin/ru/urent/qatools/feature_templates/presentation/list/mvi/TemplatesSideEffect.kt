package ru.urent.qatools.feature_templates.presentation.list.mvi

sealed interface TemplatesSideEffect {
    data class OpenTemplate(val templateId: Long) : TemplatesSideEffect
}
