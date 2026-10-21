package ru.urent.qatools.feature_templates.presentation.list.mvi

import kotlin.time.Instant
import ru.urent.qatools.base_bike_setup.entities.ApplyStatus
import ru.urent.qatools.base_bike_setup.entities.Template

data class TemplatesState(val templates: List<TemplateCard>, val isLoaded: Boolean)

data class TemplateCard(
    val template: Template,
    val statuses: Map<Long, ApplyStatus>,
    val isApplying: Boolean,
    val changedSinceApplyCount: Int,
    val lastAppliedAt: Instant?,
    val cityNames: List<String>,
    val techZoneNames: List<String>,
)
