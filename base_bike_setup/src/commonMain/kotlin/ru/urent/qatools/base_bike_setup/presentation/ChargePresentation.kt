package ru.urent.qatools.base_bike_setup.presentation

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import ru.urent.qatools.base_bike_setup.entities.ApplyFailure
import ru.urent.qatools.base_bike_setup.entities.AvailabilityTarget
import ru.urent.qatools.base_bike_setup.entities.BatteryTask
import ru.urent.qatools.base_bike_setup.entities.BikeContext
import ru.urent.qatools.base_bike_setup.entities.ChargeBand
import ru.urent.qatools.core_entities.ChargeState
import ru.urent.qatools.base_bike_setup.entities.ChargeTarget
import ru.urent.qatools.base_bike_setup.entities.TaskZone
import ru.urent.qatools.core_entities.GeoPoint
import ru.urent.qatools.core_entities.TaskPriority

object ChargeColors {
    val empty = Color(0xFF7E1432)
    val extremelyLow = Color(0xFFAE1F4D)
    val low = Color(0xFFD7591B)
    val needToChange = Color(0xFFFFCE3B)
    val normal = Color(0xFF40A17E)
}

private const val TECHNIC_CRITICAL_TASK_PIN_ARGB = 0xFF7E1432
private const val TECHNIC_BURNING_TASK_PIN_ARGB = 0xFFAE1F4D
private const val TECHNIC_HIGH_TASK_PIN_ARGB = 0xFFD7591B
private const val TECHNIC_NORMAL_TASK_PIN_ARGB = 0xFF858C99
private const val POSITIVE_GREEN_ARGB = 0xFF40A17E
private const val EXACT_PERCENT_YELLOW_ARGB = 0xFFFFCE3B

object TaskColors {
    val critical = Color(TECHNIC_CRITICAL_TASK_PIN_ARGB)
    val burning = Color(TECHNIC_BURNING_TASK_PIN_ARGB)
    val high = Color(TECHNIC_HIGH_TASK_PIN_ARGB)
    val normal = Color(TECHNIC_NORMAL_TASK_PIN_ARGB)
    val noTask = Color(POSITIVE_GREEN_ARGB)
    val exactPercent = Color(EXACT_PERCENT_YELLOW_ARGB)
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

fun chargeStateColor(state: ChargeState): Color = when (state) {
    ChargeState.EMPTY -> ChargeColors.empty
    ChargeState.EXTREMELY_LOW -> ChargeColors.extremelyLow
    ChargeState.LOW -> ChargeColors.low
    ChargeState.NEED_TO_CHANGE -> ChargeColors.needToChange
    ChargeState.NORMAL -> ChargeColors.normal
}

fun taskPriorityLabel(priority: TaskPriority): String = when (priority) {
    TaskPriority.CRITICAL -> "Разряжен"
    TaskPriority.BURNING -> "Низкий заряд"
    TaskPriority.HIGH -> "Высокий"
    TaskPriority.NORMAL -> "Нормальный"
}

fun taskPriorityColor(priority: TaskPriority): Color = when (priority) {
    TaskPriority.CRITICAL -> TaskColors.critical
    TaskPriority.BURNING -> TaskColors.burning
    TaskPriority.HIGH -> TaskColors.high
    TaskPriority.NORMAL -> TaskColors.normal
}

fun chargeTargetLabel(target: ChargeTarget): String = when (target) {
    is ChargeTarget.Task -> taskPriorityLabel(target.priority)
    ChargeTarget.NoTask -> "Без задачи"
    is ChargeTarget.Percent -> "Точный ${formatPercent(target.percent)}"
}

fun chargeTargetShortLabel(target: ChargeTarget): String = when (target) {
    is ChargeTarget.Task -> taskPriorityShortLabel(target.priority)
    ChargeTarget.NoTask -> "Без задачи"
    is ChargeTarget.Percent -> formatPercent(target.percent)
}

private fun taskPriorityShortLabel(priority: TaskPriority): String = when (priority) {
    TaskPriority.CRITICAL -> "Разряжен"
    TaskPriority.BURNING -> "Низкий"
    TaskPriority.HIGH -> "Высокий"
    TaskPriority.NORMAL -> "Нормал."
}

fun chargeTargetColor(target: ChargeTarget): Color = when (target) {
    is ChargeTarget.Task -> taskPriorityColor(target.priority)
    ChargeTarget.NoTask -> TaskColors.noTask
    is ChargeTarget.Percent -> TaskColors.exactPercent
}

fun batteryTaskLabel(task: BatteryTask): String = when (task) {
    BatteryTask.Absent -> "Нет"
    is BatteryTask.Active -> taskPriorityLabel(task.priority)
    BatteryTask.Unknown -> "Не удалось узнать"
}

fun bandRangeText(band: ChargeBand): String {
    val from = formatNumber(band.fromPercent)
    return when {
        band.toPercent >= MAX_PERCENT && band.fromPercent > 0 -> "от $from%"
        band.toPercent == band.fromPercent -> "$from%"
        else -> "$from–${formatNumber(band.toPercent)}%"
    }
}

fun taskZoneText(zone: TaskZone, isZonePercentDivided: Boolean): String = when (zone) {
    is TaskZone.Limited -> {
        val dividedText = if (isZonePercentDivided) ", для усиленной АКБ — % техзоны с делителем стенда" else ""
        "Задачи на замену АКБ создаются при заряде до ${zone.taskPercent}%: техзона «${zone.zoneName}»$dividedText"
    }
    TaskZone.GeoIndependent -> "Место вне техзон с настройкой заряда — задача «Нормальный» здесь не создаётся"
    is TaskZone.SwitchedOff -> "В техзоне «${zone.zoneName}» настройка заряда выключена — задачи на замену АКБ здесь не создаются"
}

fun lowBatteryReasonWarning(target: ChargeTarget, availability: AvailabilityTarget, bike: BikeContext?): String? {
    val isRaisedTask = target == ChargeTarget.Task(TaskPriority.HIGH) || target == ChargeTarget.Task(TaskPriority.NORMAL)
    if (bike == null || !bike.status.hasLowBatteryReason || availability != AvailabilityTarget.Keep || !isRaisedTask) return null
    return "У байка причина снятия «Низкий заряд батареи» — с ней стенд даст задачу «Низкий заряд», а не «${chargeTargetLabel(target)}». " +
        "Выберите доступ «В доступ» или другую причину — Полигон заменит причину"
}

fun failureText(failure: ApplyFailure): String = when (failure) {
    ApplyFailure.BikeNotFound -> "Байка с таким номером нет на стенде"
    ApplyFailure.TrackerNotLinked -> "К байку не привязан трекер"
    ApplyFailure.TrackerNotFound -> "Трекера байка нет на стенде"
    is ApplyFailure.TrackerNotEmulated -> "Трекер ${failure.imei} не запущен ни в симуляторе, ни в эмуляторе стенда — Полигон им не управляет"
    is ApplyFailure.UnsupportedDriver -> "Драйвер ${failure.driver.ifBlank { "трекера" }} пока не поддерживается"
    ApplyFailure.NoVoltageRange -> "У трекера не заданы мин. и макс. напряжение"
    ApplyFailure.NoChargeThresholds -> "У модели байка нет порогов заряда"
    ApplyFailure.StandUnavailable -> "Стенд недоступен. Проверьте VPN"
    ApplyFailure.SessionExpired -> "Сессия истекла, войдите заново"
    ApplyFailure.NotConfirmed -> "Трекер не прислал новый заряд — проверьте симулятор или эмулятор"
    ApplyFailure.TargetUnreachableHere -> "В этом месте стенд не создаст задачу с таким приоритетом — смените заряд или место"
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
