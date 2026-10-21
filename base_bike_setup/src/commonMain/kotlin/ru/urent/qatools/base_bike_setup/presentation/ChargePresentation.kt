package ru.urent.qatools.base_bike_setup.presentation

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import ru.urent.qatools.base_bike_setup.entities.ApplyFailure
import ru.urent.qatools.core_entities.ChargeState
import ru.urent.qatools.base_bike_setup.entities.ChargeTarget
import ru.urent.qatools.base_bike_setup.entities.ChargeThresholds
import ru.urent.qatools.core_entities.GeoPoint

object ChargeColors {
    val empty = Color(0xFF7E1432)
    val extremelyLow = Color(0xFFAE1F4D)
    val low = Color(0xFFD7591B)
    val needToChange = Color(0xFFFFCE3B)
    val normal = Color(0xFF40A17E)
    val exact = Color(0xFF6C7482)
}

private const val TENTHS_IN_UNIT = 10
private const val COORDINATE_DECIMALS = 5
private const val COORDINATE_SCALE = 100_000L
private const val MAX_LATITUDE = 90.0
private const val MAX_LONGITUDE = 180.0
private const val MAX_PERCENT = 100.0
private const val COORDINATE_PARTS = 2

fun chargeStateLabel(state: ChargeState): String = when (state) {
    ChargeState.EMPTY -> "Пустая"
    ChargeState.EXTREMELY_LOW -> "Сверхнизкий заряд"
    ChargeState.LOW -> "Низкий заряд"
    ChargeState.NEED_TO_CHANGE -> "Рекомендуемая замена"
    ChargeState.NORMAL -> "Норма"
}

fun chargeStateShortLabel(state: ChargeState): String = when (state) {
    ChargeState.EMPTY -> "Пустая"
    ChargeState.EXTREMELY_LOW -> "Сверхнизкий"
    ChargeState.LOW -> "Низкий"
    ChargeState.NEED_TO_CHANGE -> "Рекомендуемая замена"
    ChargeState.NORMAL -> "Норма"
}

fun chargeStateColor(state: ChargeState): Color = when (state) {
    ChargeState.EMPTY -> ChargeColors.empty
    ChargeState.EXTREMELY_LOW -> ChargeColors.extremelyLow
    ChargeState.LOW -> ChargeColors.low
    ChargeState.NEED_TO_CHANGE -> ChargeColors.needToChange
    ChargeState.NORMAL -> ChargeColors.normal
}

fun chargeTargetLabel(target: ChargeTarget): String = when (target) {
    is ChargeTarget.State -> chargeStateLabel(target.state)
    is ChargeTarget.Percent -> "Точный ${formatPercent(target.percent)}"
}

fun chargeTargetColor(target: ChargeTarget): Color = when (target) {
    is ChargeTarget.State -> chargeStateColor(target.state)
    is ChargeTarget.Percent -> ChargeColors.exact
}

fun thresholdRangeText(state: ChargeState, thresholds: ChargeThresholds): String = when (state) {
    ChargeState.EMPTY -> "${formatNumber(thresholds.emptyPercent)}%"
    ChargeState.EXTREMELY_LOW -> "${formatNumber(thresholds.emptyPercent)}–${formatNumber(thresholds.powerOffPercent)}%"
    ChargeState.LOW -> "${formatNumber(thresholds.powerOffPercent)}–${formatNumber(thresholds.disablePercent)}%"
    ChargeState.NEED_TO_CHANGE ->
        "${formatNumber(thresholds.disablePercent)}–${formatNumber(thresholds.needToChangePercent)}%"
    ChargeState.NORMAL -> "от ${formatNumber(thresholds.needToChangePercent)}%"
}

fun failureText(failure: ApplyFailure): String = when (failure) {
    ApplyFailure.TrackerNotFound -> "Трекера с таким номером нет на стенде"
    ApplyFailure.BikeNotLinked -> "К трекеру не привязан байк"
    is ApplyFailure.UnsupportedDriver -> "Драйвер ${failure.driver.ifBlank { "трекера" }} пока не поддерживается"
    ApplyFailure.NoVoltageRange -> "У трекера не заданы мин. и макс. напряжение"
    ApplyFailure.NoChargeThresholds -> "У модели байка нет порогов заряда"
    ApplyFailure.StandUnavailable -> "Стенд недоступен. Проверьте VPN"
    ApplyFailure.SessionExpired -> "Сессия истекла, войдите заново"
    ApplyFailure.NotConfirmed -> "Трекер не прислал новый заряд — проверьте симулятор"
    is ApplyFailure.ReasonNotFound -> "Заряд выставлен, но причины «${failure.reasonTitle}» нет на этом стенде"
    is ApplyFailure.AvailabilityNotChanged -> "Заряд выставлен, но доступ не изменился: ${failureText(failure.cause)}"
    is ApplyFailure.AvailabilityIgnored -> {
        val current = when {
            !failure.isDisabled -> "в доступе"
            failure.currentReason == null -> "снят с доступа"
            else -> "снят «${failure.currentReason}»"
        }
        "Заряд выставлен, но стенд не применил доступ: байк сейчас $current"
    }
    is ApplyFailure.Unexpected -> "Стенд ответил ошибкой ${failure.statusCode ?: ""}".trimEnd()
}

fun formatPercent(value: Double): String = "${formatTenths(value)}%"

fun formatNumber(value: Double): String {
    val tenths = (value * TENTHS_IN_UNIT).roundToInt()
    return if (tenths % TENTHS_IN_UNIT == 0) (tenths / TENTHS_IN_UNIT).toString() else formatTenths(value)
}

fun parsePercent(text: String): Double? {
    val value = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
    return if (value in 0.0..MAX_PERCENT) value else null
}

fun formatCoordinates(point: GeoPoint): String =
    "${formatDegrees(point.latitude)}, ${formatDegrees(point.longitude)}"

fun parseCoordinates(text: String): GeoPoint? {
    val byComma = text.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    val parts = if (byComma.size == COORDINATE_PARTS) byComma else text.trim().split(Regex("\\s+"))
    if (parts.size != COORDINATE_PARTS) return null
    val latitude = parts[0].toDoubleOrNull() ?: return null
    val longitude = parts[1].toDoubleOrNull() ?: return null
    if (abs(latitude) > MAX_LATITUDE || abs(longitude) > MAX_LONGITUDE) return null
    return GeoPoint(latitude = latitude, longitude = longitude)
}

private fun formatTenths(value: Double): String {
    val tenths = (value * TENTHS_IN_UNIT).roundToInt()
    val sign = if (tenths < 0) "-" else ""
    val absolute = abs(tenths)
    return "$sign${absolute / TENTHS_IN_UNIT},${absolute % TENTHS_IN_UNIT}"
}

private fun formatDegrees(value: Double): String {
    val scaled = (value * COORDINATE_SCALE).roundToLong()
    val sign = if (scaled < 0) "-" else ""
    val absolute = abs(scaled)
    val fraction = (absolute % COORDINATE_SCALE).toString().padStart(COORDINATE_DECIMALS, '0')
    return "$sign${absolute / COORDINATE_SCALE}.$fraction"
}
