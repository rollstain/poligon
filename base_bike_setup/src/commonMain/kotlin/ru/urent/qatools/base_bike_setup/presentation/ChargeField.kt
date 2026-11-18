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
import ru.urent.qatools.base_bike_setup.domain.TaskPriorityCalculator
import ru.urent.qatools.base_bike_setup.entities.ChargeTarget
import ru.urent.qatools.uikit.components.Suggestion
import ru.urent.qatools.uikit.components.SuggestionField
import ru.urent.qatools.uikit.components.SuggestionGroup
import ru.urent.qatools.uikit.components.matchesQuery
import ru.urent.qatools.uikit.theme.QaDimens

private val ChargeIconSize = 20.dp

@Composable
fun ChargeField(
    target: ChargeTarget,
    onTargetChange: (ChargeTarget) -> Unit,
    label: String?,
    modifier: Modifier = Modifier,
    menuWidth: Dp? = null,
) {
    SuggestionField(
        selectedText = chargeTargetLabel(target),
        label = label,
        placeholder = "Приоритет задачи или число 0–100",
        groupsFor = { query -> chargeSuggestionGroups(query) },
        onSelect = onTargetChange,
        suggestionIcon = { suggestion -> ChargeIcon(suggestion) },
        emptyText = "Введите приоритет задачи на замену АКБ или число от 0 до 100",
        leadingIcon = { ChargeIcon(target) },
        modifier = modifier,
        menuWidth = menuWidth,
    )
}

fun chargeSuggestionGroups(query: String): List<SuggestionGroup<ChargeTarget>> {
    val exactPercent = parsePercent(query)
    if (exactPercent != null) {
        val exactTarget = ChargeTarget.Percent(exactPercent)
        val exact = Suggestion<ChargeTarget>(exactTarget, chargeTargetLabel(exactTarget), "точный процент заряда")
        return listOf(SuggestionGroup(title = null, suggestions = listOf(exact), hiddenCount = 0))
    }
    val targets = mutableListOf<Suggestion<ChargeTarget>>()
    for (target in TaskPriorityCalculator.TaskTargets) {
        val title = chargeTargetLabel(target)
        if (!matchesQuery(title, query)) continue
        targets.add(Suggestion(target, title, null))
    }
    return listOf(SuggestionGroup(title = null, suggestions = targets, hiddenCount = 0))
}

@Composable
fun ChargeIcon(target: ChargeTarget) {
    Box(modifier = Modifier.size(ChargeIconSize), contentAlignment = Alignment.Center) {
        Box(Modifier.size(QaDimens.chipDotSize).background(chargeTargetColor(target), CircleShape))
    }
}
