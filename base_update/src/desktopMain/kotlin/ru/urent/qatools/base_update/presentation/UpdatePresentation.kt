package ru.urent.qatools.base_update.presentation

import ru.urent.qatools.base_update.entities.UpdateStatus

fun updateCheckText(status: UpdateStatus): String? = when (status) {
    UpdateStatus.NotChecked -> null
    UpdateStatus.Checking -> "Проверяем обновления…"
    UpdateStatus.UpToDate -> "Установлена последняя версия"
    UpdateStatus.CheckFailed -> "Не удалось проверить обновления, подробности в логах"
    UpdateStatus.DevelopmentBuild -> "Сборка разработчика не обновляется"
    is UpdateStatus.Found -> "Доступна версия ${status.installer.version} — обновление в полосе вверху окна"
}

fun isUpdateCheckAllowed(status: UpdateStatus): Boolean =
    status != UpdateStatus.Checking && status !is UpdateStatus.Downloading && status !is UpdateStatus.Installing
