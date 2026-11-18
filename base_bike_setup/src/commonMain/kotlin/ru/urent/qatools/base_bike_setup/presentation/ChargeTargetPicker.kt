package ru.urent.qatools.base_bike_setup.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.urent.qatools.base_bike_setup.domain.TaskPriorityCalculator
import ru.urent.qatools.base_bike_setup.entities.ChargeBand
import ru.urent.qatools.base_bike_setup.entities.ChargeBands
import ru.urent.qatools.base_bike_setup.entities.ChargeTarget
import ru.urent.qatools.base_bike_setup.entities.TaskRules
import ru.urent.qatools.core_entities.GeoPoint
import ru.urent.qatools.uikit.components.QaTextField
import ru.urent.qatools.uikit.theme.QaColors
import ru.urent.qatools.uikit.theme.QaDimens

private const val FULL_PERCENT = 100.0
private const val TARGET_BUTTON_LABEL_SIZE_SP = 11
private val ExactFieldWidth = 140.dp
private val SelectedBorderWidth = 2.dp
private val SwatchSize = 10.dp
private val SwatchShape = RoundedCornerShape(3.dp)
private val TargetButtonLabelSize = TARGET_BUTTON_LABEL_SIZE_SP.sp
private val ScaleBarHeight = 12.dp
private val CompactScaleBarHeight = 6.dp
private val ScaleBarShape = RoundedCornerShape(6.dp)
private val ScaleMarkerWidth = 3.dp
private val ScaleMarkerHeight = 14.dp
private val ScaleTrackHeight = 18.dp
private val CurrentMarkerSize = 14.dp
private val CurrentMarkerStroke = 3.dp
private val CurrentLegendMarkerSize = 10.dp
private val ScaleSegmentGap = 2.dp

@Composable
fun ChargeTargetPicker(
    target: ChargeTarget,
    onTargetChange: (ChargeTarget) -> Unit,
    taskRules: TaskRules?,
    location: GeoPoint?,
    currentPercent: Double?,
    modifier: Modifier = Modifier,
) {
    var exactText by remember { mutableStateOf("") }
    LaunchedEffect(target) {
        if (target is ChargeTarget.Percent && parsePercent(exactText) != target.percent) exactText = formatNumber(target.percent)
    }
    val bands = if (taskRules == null || location == null) null else TaskPriorityCalculator.bandsAt(taskRules, location)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(QaDimens.spaceM)) {
        if (bands == null) {
            Text(
                "Шкала появится, когда найдём байк и место — интервалы зависят от модели, АКБ и техзоны",
                style = MaterialTheme.typography.bodyMedium,
                color = QaColors.textSecondary,
            )
        } else {
            ChargeScale(target = target, bands = bands, currentPercent = currentPercent)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceXs)) {
            for (taskTarget in TaskPriorityCalculator.TaskTargets) {
                val band = bands?.bands?.firstOrNull { it.target == taskTarget }
                val isEnabled = bands == null || band != null
                val isSelected = target == taskTarget
                val labelColor = when {
                    !isEnabled -> QaColors.disabledContent
                    isSelected -> QaColors.brandText
                    else -> QaColors.textPrimary
                }
                Surface(
                    onClick = { onTargetChange(taskTarget) },
                    enabled = isEnabled,
                    shape = RoundedCornerShape(QaDimens.radiusField),
                    color = if (isSelected) QaColors.brandSelection else QaColors.surface,
                    border = if (isSelected) BorderStroke(SelectedBorderWidth, QaColors.brand) else BorderStroke(QaDimens.dividerThickness, QaColors.stroke),
                    modifier = Modifier.weight(1f).semantics { contentDescription = chargeTargetLabel(taskTarget) },
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = QaDimens.spaceS),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(QaDimens.spaceXs),
                    ) {
                        Box(Modifier.size(SwatchSize).background(if (isEnabled) chargeTargetColor(taskTarget) else QaColors.disabledContent, SwatchShape))
                        Text(
                            chargeTargetShortLabel(taskTarget),
                            fontSize = TargetButtonLabelSize,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = labelColor,
                            maxLines = 1,
                        )
                        if (bands != null) {
                            Text(
                                if (band == null) "нет" else bandRangeText(band),
                                fontSize = TargetButtonLabelSize,
                                color = if (isEnabled) QaColors.textSecondary else QaColors.disabledContent,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
        if (bands != null && taskRules != null) {
            Text(
                taskZoneText(bands.zone, isZonePercentDivided = taskRules.zonePercentDivider != null),
                style = MaterialTheme.typography.bodySmall,
                color = QaColors.textSecondary,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceM)) {
            Text("Или точный процент", style = MaterialTheme.typography.bodyMedium, color = QaColors.textSecondary, modifier = Modifier.weight(1f))
            val isExactSelected = target is ChargeTarget.Percent
            QaTextField(
                value = exactText,
                onValueChange = { text ->
                    exactText = text
                    val percent = parsePercent(text)
                    if (percent != null) onTargetChange(ChargeTarget.Percent(percent))
                },
                label = "Точный %",
                placeholder = "например 37,5",
                errorText = if (exactText.isNotEmpty() && parsePercent(exactText) == null) "Число от 0 до 100" else null,
                modifier = Modifier
                    .width(ExactFieldWidth)
                    .then(
                        if (isExactSelected) {
                            Modifier.border(SelectedBorderWidth, QaColors.brand, RoundedCornerShape(QaDimens.radiusField))
                        } else {
                            Modifier
                        },
                    )
                    .onFocusChanged { focusState ->
                        val percent = parsePercent(exactText)
                        if (focusState.isFocused && percent != null) onTargetChange(ChargeTarget.Percent(percent))
                    },
            )
        }
    }
}

@Composable
private fun ChargeScale(target: ChargeTarget, bands: ChargeBands, currentPercent: Double?) {
    val segments = scaleSegments(bands)
    val percent = TaskPriorityCalculator.targetPercent(target, bands)
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(QaDimens.spaceXs)) {
        if (percent == null) {
            Text(
                "«${chargeTargetLabel(target)}» в этом месте не получить",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = QaColors.negative,
            )
        } else {
            val beforeMarker = percent.coerceIn(0.0, FULL_PERCENT).toFloat()
            val afterMarker = (FULL_PERCENT - percent).coerceIn(0.0, FULL_PERCENT).toFloat()
            Row(modifier = Modifier.fillMaxWidth()) {
                if (beforeMarker > 0f) Spacer(Modifier.weight(beforeMarker))
                Text("станет ${formatPercent(percent)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                if (afterMarker > 0f) Spacer(Modifier.weight(afterMarker))
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(ScaleTrackHeight), contentAlignment = Alignment.CenterStart) {
            Row(
                modifier = Modifier.fillMaxWidth().height(ScaleBarHeight).clip(ScaleBarShape),
                horizontalArrangement = Arrangement.spacedBy(ScaleSegmentGap),
            ) {
                for (segment in segments) {
                    Box(Modifier.weight((segment.toPercent - segment.fromPercent).toFloat()).fillMaxHeight().background(chargeTargetColor(segment.target)))
                }
            }
            if (currentPercent != null) {
                val beforeCurrent = currentPercent.coerceIn(0.0, FULL_PERCENT).toFloat()
                val afterCurrent = (FULL_PERCENT - currentPercent).coerceIn(0.0, FULL_PERCENT).toFloat()
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (beforeCurrent > 0f) Spacer(Modifier.weight(beforeCurrent))
                    Box(
                        Modifier
                            .offset(x = -CurrentMarkerSize / 2)
                            .size(CurrentMarkerSize)
                            .background(QaColors.surface, CircleShape)
                            .border(CurrentMarkerStroke, QaColors.textPrimary, CircleShape),
                    )
                    if (afterCurrent > 0f) Spacer(Modifier.weight(afterCurrent))
                }
            }
            if (percent != null) {
                val beforeMarker = percent.coerceIn(0.0, FULL_PERCENT).toFloat()
                val afterMarker = (FULL_PERCENT - percent).coerceIn(0.0, FULL_PERCENT).toFloat()
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (beforeMarker > 0f) Spacer(Modifier.weight(beforeMarker))
                    Box(Modifier.width(ScaleMarkerWidth).height(ScaleTrackHeight).background(QaColors.textPrimary, ScaleBarShape))
                    if (afterMarker > 0f) Spacer(Modifier.weight(afterMarker))
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ScaleSegmentGap)) {
            for ((index, segment) in segments.withIndex()) {
                Row(modifier = Modifier.weight((segment.toPercent - segment.fromPercent).toFloat())) {
                    Text(
                        "${formatNumber(segment.fromPercent)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = QaColors.textSecondary,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.weight(1f),
                    )
                    if (index == segments.lastIndex) {
                        Text("${formatNumber(FULL_PERCENT)}%", style = MaterialTheme.typography.bodySmall, color = QaColors.textSecondary, maxLines = 1)
                    }
                }
            }
        }
        if (currentPercent != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceS)) {
                Box(
                    Modifier
                        .size(CurrentLegendMarkerSize)
                        .background(QaColors.surface, CircleShape)
                        .border(CurrentMarkerStroke, QaColors.textPrimary, CircleShape),
                )
                Text("сейчас на стенде ${formatPercent(currentPercent)}", style = MaterialTheme.typography.bodySmall, color = QaColors.textSecondary)
            }
        }
    }
}

@Composable
fun ChargeScaleBar(percent: Double, bands: ChargeBands, modifier: Modifier = Modifier) {
    val beforeMarker = percent.coerceIn(0.0, FULL_PERCENT).toFloat()
    val afterMarker = (FULL_PERCENT - percent).coerceIn(0.0, FULL_PERCENT).toFloat()
    Box(modifier = modifier.height(ScaleMarkerHeight), contentAlignment = Alignment.CenterStart) {
        Row(modifier = Modifier.fillMaxWidth().height(CompactScaleBarHeight).clip(ScaleBarShape)) {
            for (segment in scaleSegments(bands)) {
                Box(Modifier.weight((segment.toPercent - segment.fromPercent).toFloat()).fillMaxHeight().background(chargeTargetColor(segment.target)))
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            if (beforeMarker > 0f) Spacer(Modifier.weight(beforeMarker))
            Box(Modifier.width(ScaleMarkerWidth).height(ScaleMarkerHeight).background(QaColors.textPrimary, ScaleBarShape))
            if (afterMarker > 0f) Spacer(Modifier.weight(afterMarker))
        }
    }
}

private fun scaleSegments(bands: ChargeBands): List<ChargeBand> = bands.bands.filter { it.toPercent > it.fromPercent }
