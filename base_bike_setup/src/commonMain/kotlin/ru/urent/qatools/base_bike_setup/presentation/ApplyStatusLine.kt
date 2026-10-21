package ru.urent.qatools.base_bike_setup.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.font.FontWeight
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import ru.urent.qatools.base_bike_setup.entities.ApplyStatus
import ru.urent.qatools.base_bike_setup.entities.AvailabilityTarget
import ru.urent.qatools.base_bike_setup.entities.BikeLookup
import ru.urent.qatools.core_entities.ChargeState
import ru.urent.qatools.base_bike_setup.entities.Placement
import ru.urent.qatools.uikit.theme.QaColors
import ru.urent.qatools.uikit.theme.QaDimens

data class StatusLine(val text: String, val color: Color, val dotColor: Color)

private val AppliedAtFormat = LocalDateTime.Format {
    day()
    char('.')
    monthNumber()
    chars(" в ")
    hour()
    char(':')
    minute()
}

@Composable
@ReadOnlyComposable
fun applyStatusLines(status: ApplyStatus?): List<StatusLine> = when (status) {
    null -> emptyList()
    ApplyStatus.Resolving -> listOf(StatusLine("Ищем байк на стенде…", QaColors.brandText, QaColors.brand))
    is ApplyStatus.WaitingForTracker ->
        listOf(StatusLine("Заряд ${formatPercent(status.percent)} отправлен, ждём пинг трекера", QaColors.brandText, QaColors.brand))
    is ApplyStatus.Done -> {
        val availabilityLine = when (val availability = status.availability) {
            AvailabilityTarget.Keep -> null
            AvailabilityTarget.Available -> StatusLine("Доступ применён: поставлен в доступ", QaColors.positiveText, QaColors.positive)
            is AvailabilityTarget.Disabled ->
                StatusLine("Доступ применён: снят «${availability.reasonTitle}»", QaColors.positiveText, QaColors.positive)
        }
        val state = status.state
        val chargeLine = if (status.isTargetReached) {
            val stateText = if (state == null) "" else "${chargeStateLabel(state)}, "
            StatusLine("Заряд подтверждён: $stateText${formatPercent(status.percent)}", QaColors.positiveText, QaColors.positive)
        } else {
            val currentStateText = if (state == null) "" else " (сейчас «${chargeStateLabel(state)}»)"
            StatusLine(
                "Заряд ${formatPercent(status.percent)} отправлен · статус заряда стенд пересчитает за 5–15 минут$currentStateText, " +
                    "потом нажмите «Обновить» в блоке «Сейчас на стенде»",
                QaColors.brandText,
                QaColors.brand,
            )
        }
        listOfNotNull(availabilityLine, chargeLine)
    }
    is ApplyStatus.Failed -> listOf(StatusLine(failureText(status.failure), QaColors.negative, QaColors.negative))
}

@Composable
@ReadOnlyComposable
fun bikeResultLine(status: ApplyStatus?, isChangedSinceApply: Boolean): StatusLine {
    if (isChangedSinceApply) return StatusLine("Изменён — не применён", QaColors.textSecondary, QaColors.disabledContent)
    return when (status) {
        null -> StatusLine("Ещё не применялся", QaColors.textSecondary, QaColors.disabledContent)
        ApplyStatus.Resolving -> StatusLine("Ищем байк на стенде…", QaColors.brandText, QaColors.brand)
        is ApplyStatus.WaitingForTracker -> StatusLine("Ждём пинг трекера", QaColors.brandText, QaColors.brand)
        is ApplyStatus.Done -> if (status.isTargetReached) {
            StatusLine("Подтверждён", QaColors.positiveText, QaColors.positive)
        } else {
            StatusLine("Ждёт стенд: заряд пересчитается за 5–15 мин, потом «Обновить статусы»", QaColors.brandText, QaColors.brand)
        }
        is ApplyStatus.Failed -> StatusLine(failureText(status.failure), QaColors.negative, QaColors.negative)
    }
}

@Composable
@ReadOnlyComposable
fun templateStatusLine(
    bikeStatuses: List<ApplyStatus?>,
    isApplying: Boolean,
    changedCount: Int,
    appliedAt: Instant?,
): StatusLine {
    val total = bikeStatuses.size
    val confirmed = bikeStatuses.count { it is ApplyStatus.Done && it.isTargetReached }
    val waitingForRecalculation = bikeStatuses.count { it is ApplyStatus.Done && !it.isTargetReached }
    val failed = bikeStatuses.count { it is ApplyStatus.Failed }
    val details = mutableListOf("подтверждено $confirmed из $total")
    if (waitingForRecalculation > 0) details.add("ждут пересчёта заряда: $waitingForRecalculation")
    if (failed > 0) details.add("с ошибкой: $failed")
    if (changedCount > 0) details.add("изменены после применения: $changedCount")
    val appliedAtText = if (appliedAt == null) "" else " ${appliedAt.toLocalDateTime(TimeZone.currentSystemDefault()).format(AppliedAtFormat)}"
    val text = "Применён$appliedAtText · ${details.joinToString(" · ")}"
    return when {
        isApplying -> StatusLine("Применяется · подтверждено $confirmed из $total", QaColors.brandText, QaColors.brand)
        bikeStatuses.all { it == null } -> StatusLine("Ещё не применялся", QaColors.textSecondary, QaColors.disabledContent)
        failed > 0 -> StatusLine(text, QaColors.negative, QaColors.negative)
        waitingForRecalculation > 0 || changedCount > 0 -> StatusLine(text, QaColors.brandText, QaColors.brand)
        else -> StatusLine(text, QaColors.positiveText, QaColors.positive)
    }
}

@Composable
@ReadOnlyComposable
fun lookupLine(lookup: BikeLookup?, isLookingUp: Boolean): StatusLine? = when {
    isLookingUp -> StatusLine("Ищем байк на стенде…", QaColors.brandText, QaColors.brand)
    lookup is BikeLookup.Failed -> StatusLine(failureText(lookup.failure), QaColors.negative, QaColors.negative)
    lookup is BikeLookup.Found -> {
        val bike = lookup.bike
        val percent = bike.chargePercent
        val state = bike.chargeState
        val charge = when {
            percent != null && state != null -> "сейчас ${formatPercent(percent)} · ${chargeStateLabel(state)}"
            percent != null -> "сейчас ${formatPercent(percent)}"
            else -> "заряд неизвестен"
        }
        StatusLine("${bike.identifier} · ${bike.modelName} · $charge", QaColors.textPrimary, chargeDotColor(state))
    }
    else -> null
}

@Composable
@ReadOnlyComposable
fun placementLine(placement: Placement, hasRentZones: Boolean, techZoneName: String?): StatusLine {
    val parkingText = if (placement.parkingName == null) "Вне парковки" else "На парковке «${placement.parkingName}»"
    val rentText = when {
        !hasRentZones -> ""
        placement.isInsideRentZone -> ", внутри зоны аренды"
        else -> ", вне зоны аренды"
    }
    val restrictionText = when {
        placement.isInsideNoRidingZone -> " · в зоне запрета езды"
        placement.isInsideNoParkingZone -> " · в зоне запрета парковки"
        else -> ""
    }
    val techZoneText = if (techZoneName == null) "" else " · техзона «$techZoneName»"
    val text = "$parkingText$rentText$restrictionText$techZoneText"
    val isGood = placement.parkingName != null && (placement.isInsideRentZone || !hasRentZones) && restrictionText.isEmpty()
    return if (isGood) {
        StatusLine(text, QaColors.positiveText, QaColors.positive)
    } else {
        StatusLine(text, QaColors.textSecondary, QaColors.disabledContent)
    }
}

@Composable
@ReadOnlyComposable
private fun chargeDotColor(state: ChargeState?): Color =
    if (state == null) QaColors.disabledContent else chargeStateColor(state)

@Composable
fun StatusLineView(line: StatusLine, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceS),
    ) {
        Box(modifier = Modifier.size(QaDimens.chipDotSize).background(line.dotColor, CircleShape))
        Text(
            text = line.text,
            color = line.color,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}
