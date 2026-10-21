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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.urent.qatools.base_bike_setup.domain.ChargeCalculator
import ru.urent.qatools.base_bike_setup.entities.ChargeTarget
import ru.urent.qatools.base_bike_setup.entities.ChargeThresholds
import ru.urent.qatools.core_entities.ChargeState
import ru.urent.qatools.uikit.components.QaTextField
import ru.urent.qatools.uikit.theme.QaColors
import ru.urent.qatools.uikit.theme.QaDimens

private const val FULL_PERCENT = 100.0
private val ExactFieldWidth = 140.dp
private val SelectedBorderWidth = 2.dp
private val SwatchSize = 10.dp
private val SwatchShape = RoundedCornerShape(3.dp)
private val StateButtonLabelSize = 11.sp
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
    thresholds: ChargeThresholds?,
    currentPercent: Double?,
    modifier: Modifier = Modifier,
) {
    var exactText by remember { mutableStateOf("") }
    LaunchedEffect(target) {
        if (target is ChargeTarget.Percent && parsePercent(exactText) != target.percent) exactText = formatNumber(target.percent)
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(QaDimens.spaceM)) {
        if (thresholds == null) {
            Text(
                "Шкала порогов появится, когда найдём байк — пороги у каждой модели свои",
                style = MaterialTheme.typography.bodyMedium,
                color = QaColors.textSecondary,
            )
        } else {
            ChargeScale(
                percent = ChargeCalculator.targetPercent(target, thresholds),
                currentPercent = currentPercent,
                thresholds = thresholds,
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceXs)) {
            for (state in ChargeState.entries) {
                val isSelected = target is ChargeTarget.State && target.state == state
                val buttonLabel = when (state) {
                    ChargeState.EMPTY -> "Пустая"
                    ChargeState.EXTREMELY_LOW -> "Сверхниз."
                    ChargeState.LOW -> "Низкий"
                    ChargeState.NEED_TO_CHANGE -> "Замена"
                    ChargeState.NORMAL -> "Норма"
                }
                Surface(
                    onClick = { onTargetChange(ChargeTarget.State(state)) },
                    shape = RoundedCornerShape(QaDimens.radiusField),
                    color = if (isSelected) QaColors.brandSelection else QaColors.surface,
                    border = if (isSelected) BorderStroke(SelectedBorderWidth, QaColors.brand) else BorderStroke(QaDimens.dividerThickness, QaColors.stroke),
                    modifier = Modifier.weight(1f).semantics { contentDescription = chargeStateLabel(state) },
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = QaDimens.spaceS),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(QaDimens.spaceXs),
                    ) {
                        Box(Modifier.size(SwatchSize).background(chargeStateColor(state), SwatchShape))
                        Text(
                            buttonLabel,
                            fontSize = StateButtonLabelSize,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) QaColors.brandText else QaColors.textPrimary,
                            maxLines = 1,
                        )
                        if (thresholds != null) {
                            Text(
                                thresholdRangeText(state, thresholds),
                                fontSize = StateButtonLabelSize,
                                color = QaColors.textSecondary,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
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
private fun ChargeScale(percent: Double, currentPercent: Double?, thresholds: ChargeThresholds) {
    val segments = chargeScaleSegments(thresholds)
    val beforeMarker = percent.coerceIn(0.0, FULL_PERCENT).toFloat()
    val afterMarker = (FULL_PERCENT - percent).coerceIn(0.0, FULL_PERCENT).toFloat()
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(QaDimens.spaceXs)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            if (beforeMarker > 0f) Spacer(Modifier.weight(beforeMarker))
            Text("станет ${formatPercent(percent)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            if (afterMarker > 0f) Spacer(Modifier.weight(afterMarker))
        }
        Box(modifier = Modifier.fillMaxWidth().height(ScaleTrackHeight), contentAlignment = Alignment.CenterStart) {
            Row(
                modifier = Modifier.fillMaxWidth().height(ScaleBarHeight).clip(ScaleBarShape),
                horizontalArrangement = Arrangement.spacedBy(ScaleSegmentGap),
            ) {
                for ((color, from, to) in segments) {
                    Box(Modifier.weight((to - from).toFloat()).fillMaxHeight().background(color))
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
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (beforeMarker > 0f) Spacer(Modifier.weight(beforeMarker))
                Box(Modifier.width(ScaleMarkerWidth).height(ScaleTrackHeight).background(QaColors.textPrimary, ScaleBarShape))
                if (afterMarker > 0f) Spacer(Modifier.weight(afterMarker))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ScaleSegmentGap)) {
            for ((index, segment) in segments.withIndex()) {
                val (_, from, to) = segment
                Row(modifier = Modifier.weight((to - from).toFloat())) {
                    Text(
                        "${formatNumber(from)}%",
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
fun ChargeScaleBar(percent: Double, thresholds: ChargeThresholds, modifier: Modifier = Modifier) {
    val beforeMarker = percent.coerceIn(0.0, FULL_PERCENT).toFloat()
    val afterMarker = (FULL_PERCENT - percent).coerceIn(0.0, FULL_PERCENT).toFloat()
    Box(modifier = modifier.height(ScaleMarkerHeight), contentAlignment = Alignment.CenterStart) {
        Row(modifier = Modifier.fillMaxWidth().height(CompactScaleBarHeight).clip(ScaleBarShape)) {
            for ((color, from, to) in chargeScaleSegments(thresholds)) {
                Box(Modifier.weight((to - from).toFloat()).fillMaxHeight().background(color))
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            if (beforeMarker > 0f) Spacer(Modifier.weight(beforeMarker))
            Box(Modifier.width(ScaleMarkerWidth).height(ScaleMarkerHeight).background(QaColors.textPrimary, ScaleBarShape))
            if (afterMarker > 0f) Spacer(Modifier.weight(afterMarker))
        }
    }
}

private fun chargeScaleSegments(thresholds: ChargeThresholds): List<Triple<Color, Double, Double>> = listOf(
    Triple(ChargeColors.extremelyLow, thresholds.emptyPercent, thresholds.powerOffPercent),
    Triple(ChargeColors.low, thresholds.powerOffPercent, thresholds.disablePercent),
    Triple(ChargeColors.needToChange, thresholds.disablePercent, thresholds.needToChangePercent),
    Triple(ChargeColors.normal, thresholds.needToChangePercent, FULL_PERCENT),
).filter { (_, from, to) -> to > from }
