package ru.urent.qatools.base_map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.urent.qatools.base_bike_setup.entities.DriverTask
import ru.urent.qatools.core_entities.DriverTaskReason
import ru.urent.qatools.base_bike_setup.presentation.ChargeColors
import ru.urent.qatools.base_bike_setup.presentation.DriverTaskBadge
import ru.urent.qatools.base_bike_setup.presentation.driverTaskColor
import ru.urent.qatools.base_bike_setup.presentation.driverTaskLabel
import ru.urent.qatools.uikit.theme.QaColors
import ru.urent.qatools.uikit.theme.QaDimens

private const val RENT_LINE_STROKE_DP = 2.5f
private const val TECH_ZONE_STROKE_DP = 1.5f
private const val TECH_ZONE_DASH_DP = 3f
private val LegendWidth = 312.dp
private val LegendCellWidth = 136.dp
private val LegendIconSize = 18.dp
private val LegendShape = RoundedCornerShape(14.dp)
private val ChargeSwatchShape = RoundedCornerShape(3.dp)
private val ChargeSwatchSize = 10.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MapLegend(modifier: Modifier = Modifier) {
    var isExpanded by remember { mutableStateOf(false) }
    if (!isExpanded) {
        Surface(
            onClick = { isExpanded = true },
            shape = CircleShape,
            color = QaColors.surface,
            contentColor = QaColors.textPrimary,
            shadowElevation = QaDimens.spaceXs,
            modifier = modifier,
        ) {
            Text(
                text = "Обозначения",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = QaDimens.spaceL, vertical = QaDimens.spaceM),
            )
        }
        return
    }
    Surface(
        shape = LegendShape,
        color = QaColors.surface,
        contentColor = QaColors.textPrimary,
        shadowElevation = QaDimens.spaceXs,
        modifier = modifier.width(LegendWidth),
    ) {
        Column(
            modifier = Modifier.padding(start = QaDimens.spaceL, end = QaDimens.spaceXs, bottom = QaDimens.spaceM),
            verticalArrangement = Arrangement.spacedBy(QaDimens.spaceS),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Обозначения",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { isExpanded = false }) { Text("Свернуть") }
            }
            Text("Цвет пина — заряд после применения", style = MaterialTheme.typography.bodySmall, color = QaColors.textSecondary)
            FlowRow(verticalArrangement = Arrangement.spacedBy(QaDimens.spaceXs)) {
                val chargeColors = listOf(
                    ChargeColors.empty to "Пустая",
                    ChargeColors.extremelyLow to "Сверхнизкий",
                    ChargeColors.low to "Низкий",
                    ChargeColors.needToChange to "Замена",
                    ChargeColors.normal to "Норма",
                    ChargeColors.exact to "Точный %",
                )
                for ((color, label) in chargeColors) {
                    Row(
                        modifier = Modifier.width(LegendCellWidth),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceS),
                    ) {
                        Box(Modifier.size(ChargeSwatchSize).background(color, ChargeSwatchShape))
                        Text(label, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Text("Значок на пине — задача водителю", style = MaterialTheme.typography.bodySmall, color = QaColors.textSecondary)
            for (reason in listOf(DriverTaskReason.READY_FOR_USE, DriverTaskReason.NEED_REPAIR)) {
                val task = DriverTask(reason)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceS)) {
                    DriverTaskBadge(color = driverTaskColor(task), size = LegendIconSize, hasOutline = false)
                    Text(driverTaskLabel(task).replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("Зоны", style = MaterialTheme.typography.bodySmall, color = QaColors.textSecondary)
            FlowRow(verticalArrangement = Arrangement.spacedBy(QaDimens.spaceXs)) {
                Row(
                    modifier = Modifier.width(LegendCellWidth),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceS),
                ) {
                    ParkingMarker(isActive = true)
                    Text("Парковка", style = MaterialTheme.typography.bodySmall)
                }
                Row(
                    modifier = Modifier.width(LegendCellWidth),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceS),
                ) {
                    ParkingMarker(isActive = false)
                    Text("Неактивная", style = MaterialTheme.typography.bodySmall)
                }
                Row(
                    modifier = Modifier.width(LegendCellWidth),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceS),
                ) {
                    Canvas(modifier = Modifier.size(LegendIconSize)) {
                        drawLine(
                            color = MapColors.rentZone,
                            start = Offset(0f, size.height / 2),
                            end = Offset(size.width, size.height / 2),
                            strokeWidth = RENT_LINE_STROKE_DP * density,
                        )
                    }
                    Text("Зона аренды", style = MaterialTheme.typography.bodySmall)
                }
                Row(
                    modifier = Modifier.width(LegendCellWidth),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceS),
                ) {
                    Canvas(modifier = Modifier.size(LegendIconSize)) {
                        val dash = TECH_ZONE_DASH_DP * density
                        drawRect(
                            color = MapColors.techZone,
                            style = Stroke(
                                width = TECH_ZONE_STROKE_DP * density,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
                            ),
                        )
                    }
                    Text("Техзона", style = MaterialTheme.typography.bodySmall)
                }
                Row(
                    modifier = Modifier.width(LegendCellWidth),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceS),
                ) {
                    ZoneSignMarker(isNoRiding = true)
                    Text("Запрет езды", style = MaterialTheme.typography.bodySmall)
                }
                Row(
                    modifier = Modifier.width(LegendCellWidth),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceS),
                ) {
                    ZoneSignMarker(isNoRiding = false)
                    Text("Запрет парковки", style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceS)) {
                BikePinLegendIcon(color = ChargeColors.normal)
                Text(
                    "Точка под хвостиком пина — координата байка",
                    style = MaterialTheme.typography.bodySmall,
                    color = QaColors.textSecondary,
                )
            }
        }
    }
}
