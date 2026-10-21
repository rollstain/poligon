package ru.urent.qatools.base_bike_setup.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.time.Clock
import kotlin.time.Instant
import ru.urent.qatools.base_bike_setup.entities.BikeContext
import ru.urent.qatools.base_bike_setup.entities.DriverTask
import ru.urent.qatools.uikit.theme.QaColors
import ru.urent.qatools.uikit.theme.QaDimens

private val TaskBadgeSize = 16.dp

private data class StandFact(val label: String, val value: String, val dotColor: Color?, val task: DriverTask?)

private data class StandSummary(
    val connection: StandFact,
    val gps: StandFact,
    val charge: StandFact,
    val access: StandFact,
    val driverTask: StandFact,
    val battery: StandFact,
    val coordinates: StandFact,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StandFacts(bike: BikeContext, modifier: Modifier = Modifier) {
    val summary = standSummary(bike, Clock.System.now())
    val facts = listOf(summary.connection, summary.gps, summary.charge, summary.access, summary.driverTask, summary.battery, summary.coordinates)
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceXl),
        verticalArrangement = Arrangement.spacedBy(QaDimens.spaceS),
    ) {
        for (fact in facts) {
            Column(verticalArrangement = Arrangement.spacedBy(QaDimens.spaceXs)) {
                Text(fact.label, style = MaterialTheme.typography.bodySmall, color = QaColors.textSecondary)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceS)) {
                    val task = fact.task
                    val dotColor = fact.dotColor
                    if (task != null) {
                        DriverTaskBadge(color = driverTaskColor(task), size = TaskBadgeSize, hasOutline = false)
                    } else if (dotColor != null) {
                        Box(Modifier.size(QaDimens.chipDotSize).background(dotColor, CircleShape))
                    }
                    Text(fact.value, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun StandStateLines(bike: BikeContext, modifier: Modifier = Modifier) {
    val summary = standSummary(bike, Clock.System.now())
    Column(modifier = modifier) {
        if (bike.status.isOnline == false) {
            Text(summary.connection.value, style = MaterialTheme.typography.bodySmall, color = QaColors.negative)
        }
        Text(
            summary.access.value,
            style = MaterialTheme.typography.bodySmall,
            color = QaColors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
@ReadOnlyComposable
private fun standSummary(bike: BikeContext, now: Instant): StandSummary {
    val status = bike.status
    val offlineSince = status.offlineSince
    val connection = when (status.isOnline) {
        true -> StandFact("Связь", "В сети", QaColors.positive, null)
        false -> StandFact(
            label = "Связь",
            value = if (offlineSince == null) "Не в сети" else "Не в сети ${elapsedText(offlineSince, now)}",
            dotColor = QaColors.negative,
            task = null,
        )
        null -> StandFact("Связь", "Неизвестно", QaColors.disabledContent, null)
    }
    val gpsLostSince = status.gpsLostSince
    val gps = when {
        !status.isGpsLost -> StandFact("GPS", "в порядке", QaColors.positive, null)
        gpsLostSince == null -> StandFact("GPS", "ошибка", QaColors.negative, null)
        else -> StandFact("GPS", "ошибка ${elapsedText(gpsLostSince, now)}", QaColors.negative, null)
    }
    val percent = bike.chargePercent
    val state = bike.chargeState
    val charge = when {
        percent != null && state != null -> StandFact("Заряд", "${formatPercent(percent)} · ${chargeStateLabel(state)}", chargeStateColor(state), null)
        percent != null -> StandFact("Заряд", formatPercent(percent), QaColors.disabledContent, null)
        else -> StandFact("Заряд", "неизвестен", QaColors.disabledContent, null)
    }
    val access = when (status.isDisabled) {
        true -> StandFact("Доступ", "Снят: ${status.disabledReason ?: "причина не указана"}", QaColors.warningText, null)
        false -> StandFact("Доступ", "В доступе", QaColors.positive, null)
        null -> StandFact("Доступ", "Неизвестно", QaColors.disabledContent, null)
    }
    val task = status.driverTask
    val driverTask = if (task == null) {
        StandFact("Задача водителю", "Нет", null, null)
    } else {
        StandFact("Задача водителю", driverTaskLabel(task), null, task)
    }
    val location = bike.location
    return StandSummary(
        connection = connection,
        gps = gps,
        charge = charge,
        access = access,
        driverTask = driverTask,
        battery = StandFact("АКБ", batteryCategoryLabel(status.batteryCategory), null, null),
        coordinates = StandFact("Координаты", if (location == null) "нет" else formatCoordinates(location), null, null),
    )
}
