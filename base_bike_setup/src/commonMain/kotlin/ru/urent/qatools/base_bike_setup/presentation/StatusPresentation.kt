package ru.urent.qatools.base_bike_setup.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import kotlin.time.Instant
import ru.urent.qatools.base_bike_setup.entities.AvailabilityTarget
import ru.urent.qatools.core_entities.BatteryCapacityCategory
import ru.urent.qatools.base_bike_setup.entities.DriverTask
import ru.urent.qatools.core_entities.DriverTaskReason
import ru.urent.qatools.uikit.theme.QaColors

private const val NEEDS_REPAIR_REASON_NAME = "NeedsRepair"
private const val READY_FOR_USE_REASON_NAME = "ReadyForUse"
private const val MINUTES_IN_HOUR = 60
private const val MINUTES_IN_DAY = 24 * MINUTES_IN_HOUR

fun availabilityTargetLabel(target: AvailabilityTarget): String = when (target) {
    AvailabilityTarget.Keep -> "Не менять доступ"
    AvailabilityTarget.Available -> "Поставить в доступ"
    is AvailabilityTarget.Disabled -> target.reasonTitle
}

@Composable
@ReadOnlyComposable
fun availabilityTargetColor(target: AvailabilityTarget): Color = when (target) {
    AvailabilityTarget.Keep -> QaColors.disabledContent
    AvailabilityTarget.Available -> QaColors.positive
    is AvailabilityTarget.Disabled -> {
        val taskReason = driverTaskReasonFor(target.reasonName)
        if (taskReason == null) QaColors.textSecondary else driverTaskColor(DriverTask(taskReason))
    }
}

fun driverTaskReasonFor(disabledReasonName: String): DriverTaskReason? = when (disabledReasonName) {
    NEEDS_REPAIR_REASON_NAME -> DriverTaskReason.NEED_REPAIR
    READY_FOR_USE_REASON_NAME -> DriverTaskReason.READY_FOR_USE
    else -> null
}

@Composable
@ReadOnlyComposable
fun driverTaskColor(task: DriverTask): Color = when (task.reason) {
    DriverTaskReason.READY_FOR_USE -> QaColors.positiveText
    DriverTaskReason.NEED_REPAIR -> QaColors.negative
    DriverTaskReason.OUT_OF_ZONE, DriverTaskReason.IDLE, null -> QaColors.textSecondary
}

fun driverTaskLabel(task: DriverTask): String = when (task.reason) {
    DriverTaskReason.READY_FOR_USE -> "готов к эксплуатации"
    DriverTaskReason.NEED_REPAIR -> "требует ремонта"
    DriverTaskReason.OUT_OF_ZONE -> "вне зоны"
    DriverTaskReason.IDLE -> "изъят"
    null -> "создана"
}

fun batteryCategoryLabel(category: BatteryCapacityCategory): String = when (category) {
    BatteryCapacityCategory.NORMAL -> "обычная"
    BatteryCapacityCategory.INCREASED -> "усиленная"
    BatteryCapacityCategory.NOT_SET -> "не привязана"
}

fun elapsedText(since: Instant, now: Instant): String {
    val elapsed = now - since
    val minutes = elapsed.inWholeMinutes
    return when {
        minutes < 1 -> "меньше минуты"
        minutes < MINUTES_IN_HOUR -> "$minutes мин"
        minutes < MINUTES_IN_DAY -> "${elapsed.inWholeHours} ч"
        else -> "${elapsed.inWholeDays} дн"
    }
}
