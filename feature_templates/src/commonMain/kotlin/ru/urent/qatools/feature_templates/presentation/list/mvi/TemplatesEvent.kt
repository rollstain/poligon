package ru.urent.qatools.feature_templates.presentation.list.mvi

import ru.urent.qatools.base_bike_setup.entities.Template

sealed interface TemplatesEvent {
    data object OnCreateTemplateClicked : TemplatesEvent

    data class OnTemplateClicked(val templateId: Long) : TemplatesEvent

    data class OnApplyAllClicked(val template: Template) : TemplatesEvent
}
