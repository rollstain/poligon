package ru.urent.qatools.feature_templates.presentation.list.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.urent.qatools.base_bike_setup.entities.ApplyStatus
import ru.urent.qatools.base_bike_setup.entities.AvailabilityTarget
import ru.urent.qatools.base_bike_setup.presentation.StatusLineView
import ru.urent.qatools.base_bike_setup.presentation.chargeTargetColor
import ru.urent.qatools.base_bike_setup.presentation.chargeTargetLabel
import ru.urent.qatools.base_bike_setup.presentation.templateStatusLine
import ru.urent.qatools.feature_templates.presentation.list.mvi.TemplateCard
import ru.urent.qatools.feature_templates.presentation.list.mvi.TemplatesEvent
import ru.urent.qatools.feature_templates.presentation.list.mvi.TemplatesState
import ru.urent.qatools.uikit.components.QaButton
import ru.urent.qatools.uikit.components.QaButtonStyle
import ru.urent.qatools.uikit.theme.QaColors
import ru.urent.qatools.uikit.theme.QaDimens

private const val BIKES_SHOWN_IN_ROW = 5
private const val PLACE_TEXT_MAX_LINES = 2
private const val NAME_COLUMN_WEIGHT = 1.3f
private const val BIKES_COLUMN_WEIGHT = 1.6f
private const val TARGETS_COLUMN_WEIGHT = 1.4f
private const val PLACE_COLUMN_WEIGHT = 1.1f
private const val STATUS_COLUMN_WEIGHT = 1.5f
private val ActionColumnWidth = 160.dp
private val MixBarWidth = 140.dp
private val MixBarHeight = 8.dp
private val MixBarShape = RoundedCornerShape(4.dp)
private val MixSegmentGap = 2.dp
private val BikePillShape = RoundedCornerShape(8.dp)
private val BikeSwatchSize = 8.dp
private val BikeSwatchShape = RoundedCornerShape(2.dp)
private val ProgressHeight = 4.dp

@Composable
fun TemplatesView(state: TemplatesState, onEvent: (TemplatesEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = QaDimens.screenPaddingHorizontal, vertical = QaDimens.screenPaddingVertical),
        verticalArrangement = Arrangement.spacedBy(QaDimens.spaceL),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceL)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(QaDimens.spaceXs)) {
                Text("Шаблоны", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Одна кнопка ставит все байки шаблона, заряд считается по трекеру и модели каждого байка на этом стенде",
                    style = MaterialTheme.typography.bodyMedium,
                    color = QaColors.textSecondary,
                )
            }
            QaButton(text = "Новый шаблон", onClick = { onEvent(TemplatesEvent.OnCreateTemplateClicked) })
        }
        Surface(shape = RoundedCornerShape(QaDimens.radiusCard), color = QaColors.surface, modifier = Modifier.fillMaxWidth()) {
            if (state.isLoaded && state.templates.isEmpty()) {
                Text(
                    "Шаблонов пока нет. Создайте первый: добавьте байки, их место и заряд.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = QaColors.textSecondary,
                    modifier = Modifier.padding(QaDimens.spaceXxl),
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = QaDimens.spaceS)) {
                    item {
                        Row(
                            modifier = Modifier.padding(horizontal = QaDimens.spaceXl, vertical = QaDimens.spaceM),
                            horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceL),
                        ) {
                            for ((title, weight) in listOf(
                                "Шаблон" to NAME_COLUMN_WEIGHT,
                                "Байки" to BIKES_COLUMN_WEIGHT,
                                "Что ставит" to TARGETS_COLUMN_WEIGHT,
                                "Где" to PLACE_COLUMN_WEIGHT,
                                "Последнее применение" to STATUS_COLUMN_WEIGHT,
                            )) {
                                Text(title, style = MaterialTheme.typography.bodySmall, color = QaColors.textSecondary, modifier = Modifier.weight(weight))
                            }
                            Box(Modifier.width(ActionColumnWidth))
                        }
                    }
                    items(state.templates, key = { card -> card.template.id }) { card ->
                        HorizontalDivider(thickness = QaDimens.dividerThickness, color = QaColors.stroke)
                        TemplateRow(card = card, onEvent = onEvent)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TemplateRow(card: TemplateCard, onEvent: (TemplatesEvent) -> Unit) {
    val template = card.template
    val bikes = template.bikes
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Открыть шаблон") { onEvent(TemplatesEvent.OnTemplateClicked(template.id)) }
            .padding(horizontal = QaDimens.spaceXl, vertical = QaDimens.spaceL),
        horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceL),
    ) {
        Column(modifier = Modifier.weight(NAME_COLUMN_WEIGHT), verticalArrangement = Arrangement.spacedBy(QaDimens.spaceXs)) {
            Text(template.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                if (bikes.isEmpty()) "Байков пока нет" else "Байков: ${bikes.size}",
                style = MaterialTheme.typography.bodySmall,
                color = QaColors.textSecondary,
            )
        }
        FlowRow(
            modifier = Modifier.weight(BIKES_COLUMN_WEIGHT),
            horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceXs),
            verticalArrangement = Arrangement.spacedBy(QaDimens.spaceXs),
        ) {
            for (bike in bikes.take(BIKES_SHOWN_IN_ROW)) {
                Row(
                    modifier = Modifier
                        .background(QaColors.background, BikePillShape)
                        .padding(horizontal = QaDimens.spaceS, vertical = QaDimens.spaceXs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceXs),
                ) {
                    Box(Modifier.size(BikeSwatchSize).background(chargeTargetColor(bike.setup.target), BikeSwatchShape))
                    Text("S.${bike.setup.bikeNumber}", style = MaterialTheme.typography.labelMedium)
                }
            }
            if (bikes.size > BIKES_SHOWN_IN_ROW) {
                Text(
                    "ещё ${bikes.size - BIKES_SHOWN_IN_ROW}",
                    style = MaterialTheme.typography.labelMedium,
                    color = QaColors.textSecondary,
                    modifier = Modifier.padding(horizontal = QaDimens.spaceXs, vertical = QaDimens.spaceXs),
                )
            }
        }
        Column(modifier = Modifier.weight(TARGETS_COLUMN_WEIGHT), verticalArrangement = Arrangement.spacedBy(QaDimens.spaceXs)) {
            val targetCounts = bikes.groupingBy { it.setup.target }.eachCount()
            if (targetCounts.isNotEmpty()) {
                Row(
                    modifier = Modifier.width(MixBarWidth).height(MixBarHeight).clip(MixBarShape),
                    horizontalArrangement = Arrangement.spacedBy(MixSegmentGap),
                ) {
                    for ((target, count) in targetCounts) {
                        Box(Modifier.weight(count.toFloat()).fillMaxHeight().background(chargeTargetColor(target)))
                    }
                }
                Text(
                    targetCounts.entries.joinToString(", ") { (target, count) -> "${chargeTargetLabel(target)} ×$count" },
                    style = MaterialTheme.typography.bodyMedium,
                )
                val availableCount = bikes.count { it.setup.availability == AvailabilityTarget.Available }
                val disabledCount = bikes.count { it.setup.availability is AvailabilityTarget.Disabled }
                val accessChanges = mutableListOf<String>()
                if (availableCount > 0) accessChanges.add("вернуть в доступ: $availableCount")
                if (disabledCount > 0) accessChanges.add("снять с доступа: $disabledCount")
                Text(
                    if (accessChanges.isEmpty()) "доступ не меняется" else accessChanges.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = QaColors.textSecondary,
                )
            }
        }
        Column(modifier = Modifier.weight(PLACE_COLUMN_WEIGHT), verticalArrangement = Arrangement.spacedBy(QaDimens.spaceXs)) {
            if (card.cityNames.isNotEmpty()) {
                Text(card.cityNames.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (card.techZoneNames.isEmpty()) "вне техзон" else card.techZoneNames.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = QaColors.textSecondary,
                    maxLines = PLACE_TEXT_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(modifier = Modifier.weight(STATUS_COLUMN_WEIGHT), verticalArrangement = Arrangement.spacedBy(QaDimens.spaceS)) {
            val bikeStatuses = bikes.map { card.statuses[it.id] }
            StatusLineView(
                templateStatusLine(
                    bikeStatuses = bikeStatuses,
                    isApplying = card.isApplying,
                    changedCount = card.changedSinceApplyCount,
                    appliedAt = card.lastAppliedAt,
                ),
            )
            if (card.isApplying && bikes.isNotEmpty()) {
                val confirmedCount = bikeStatuses.count { it is ApplyStatus.Done && it.isTargetReached }
                LinearProgressIndicator(
                    progress = { confirmedCount.toFloat() / bikes.size },
                    color = QaColors.brand,
                    trackColor = QaColors.brandSelection,
                    modifier = Modifier.fillMaxWidth().height(ProgressHeight),
                )
            }
        }
        Box(modifier = Modifier.width(ActionColumnWidth), contentAlignment = Alignment.CenterEnd) {
            QaButton(
                text = if (card.isApplying) "Применяется…" else "Применить все",
                onClick = { onEvent(TemplatesEvent.OnApplyAllClicked(template)) },
                style = QaButtonStyle.Secondary,
                enabled = !card.isApplying && bikes.isNotEmpty(),
            )
        }
    }
}
