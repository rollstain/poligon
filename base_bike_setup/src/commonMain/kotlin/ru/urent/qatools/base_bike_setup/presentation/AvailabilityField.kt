package ru.urent.qatools.base_bike_setup.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.urent.qatools.base_bike_setup.entities.AvailabilityTarget
import ru.urent.qatools.base_bike_setup.entities.DisabledReason
import ru.urent.qatools.base_bike_setup.entities.DriverTask
import ru.urent.qatools.uikit.components.Suggestion
import ru.urent.qatools.uikit.components.SuggestionField
import ru.urent.qatools.uikit.components.SuggestionGroup
import ru.urent.qatools.uikit.components.matchesQuery
import ru.urent.qatools.uikit.theme.QaDimens

private val AvailabilityIconSize = 20.dp

@Composable
fun AvailabilityField(
    target: AvailabilityTarget,
    reasons: List<DisabledReason>,
    onTargetChange: (AvailabilityTarget) -> Unit,
    label: String?,
    modifier: Modifier = Modifier,
    menuWidth: Dp? = null,
) {
    SuggestionField(
        selectedText = availabilityTargetLabel(target),
        label = label,
        placeholder = "Причина снятия или «в доступ»",
        groupsFor = { query -> availabilitySuggestionGroups(query, reasons) },
        onSelect = onTargetChange,
        suggestionIcon = { suggestion -> AvailabilityIcon(suggestion) },
        emptyText = if (reasons.isEmpty()) "Список причин не загрузился со стенда" else "Нет такой причины на стенде",
        leadingIcon = { AvailabilityIcon(target) },
        modifier = modifier,
        menuWidth = menuWidth,
    )
}

fun availabilitySuggestionGroups(query: String, reasons: List<DisabledReason>): List<SuggestionGroup<AvailabilityTarget>> {
    val keepOrAvailable = listOf(
        Suggestion<AvailabilityTarget>(AvailabilityTarget.Keep, "Не менять доступ", "останется как сейчас на стенде"),
        Suggestion<AvailabilityTarget>(
            AvailabilityTarget.Available,
            "Поставить в доступ",
            "при низком заряде стенд снимет сам и создаст задачу на замену АКБ",
        ),
    )
    val withDriverTask = mutableListOf<Suggestion<AvailabilityTarget>>()
    val otherReasons = mutableListOf<Suggestion<AvailabilityTarget>>()
    for (reason in reasons) {
        if (!matchesQuery(reason.title, query)) continue
        val target = AvailabilityTarget.Disabled(reasonName = reason.name, reasonTitle = reason.title)
        val taskReason = driverTaskReasonFor(reason.name)
        if (taskReason == null) {
            otherReasons.add(Suggestion(target, reason.title, null))
        } else {
            withDriverTask.add(Suggestion(target, reason.title, "задача водителю: ${driverTaskLabel(DriverTask(taskReason))}"))
        }
    }
    return listOf(
        SuggestionGroup(title = null, suggestions = keepOrAvailable.filter { matchesQuery(it.title, query) }, hiddenCount = 0),
        SuggestionGroup(title = "Снять — стенд создаст задачу водителю", suggestions = withDriverTask, hiddenCount = 0),
        SuggestionGroup(title = "Снять — другие причины", suggestions = otherReasons, hiddenCount = 0),
    ).filter { it.suggestions.isNotEmpty() }
}

@Composable
fun AvailabilityIcon(target: AvailabilityTarget) {
    val taskReason = if (target is AvailabilityTarget.Disabled) driverTaskReasonFor(target.reasonName) else null
    if (taskReason != null) {
        DriverTaskBadge(color = driverTaskColor(DriverTask(taskReason)), size = AvailabilityIconSize, hasOutline = false)
        return
    }
    Box(modifier = Modifier.size(AvailabilityIconSize), contentAlignment = Alignment.Center) {
        Box(Modifier.size(QaDimens.chipDotSize).background(availabilityTargetColor(target), CircleShape))
    }
}
