package ru.urent.qatools.composeapp.update.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.urent.qatools.base_update.entities.UpdateStatus
import ru.urent.qatools.composeapp.update.mvi.UpdateBannerEvent
import ru.urent.qatools.composeapp.update.mvi.UpdateBannerState
import ru.urent.qatools.uikit.components.QaButton
import ru.urent.qatools.uikit.theme.QaColors
import ru.urent.qatools.uikit.theme.QaDimens

@Composable
fun UpdateBannerView(state: UpdateBannerState, onEvent: (UpdateBannerEvent) -> Unit) {
    val status = state.status
    if (state.isHidden || status !is UpdateStatus.Found) return
    val version = status.installer.version
    val message = when (status) {
        is UpdateStatus.Available -> "Доступна новая версия Полигона $version"
        is UpdateStatus.Downloading -> "Скачиваем версию $version… ${status.percent}%"
        is UpdateStatus.DownloadFailed -> "Не удалось скачать версию $version. Проверьте интернет и место на диске"
        is UpdateStatus.Downloaded -> "Версия $version скачана. Полигон закроется, установит её и откроется снова"
        is UpdateStatus.Installing -> "Запускаем установку версии $version…"
        is UpdateStatus.InstallFailed -> "Не удалось запустить установку. Запустите вручную: ${status.file.absolutePath}"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(QaColors.brandSelection)
            .padding(horizontal = QaDimens.spaceXxl, vertical = QaDimens.spaceS)
            .heightIn(min = QaDimens.controlHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceM),
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = QaColors.textPrimary, modifier = Modifier.weight(1f))
        when (status) {
            is UpdateStatus.Available -> QaButton(text = "Обновить", onClick = { onEvent(UpdateBannerEvent.OnDownloadClicked) })
            is UpdateStatus.DownloadFailed -> QaButton(text = "Повторить", onClick = { onEvent(UpdateBannerEvent.OnDownloadClicked) })
            is UpdateStatus.Downloaded -> QaButton(text = "Установить и перезапустить", onClick = { onEvent(UpdateBannerEvent.OnInstallClicked) })
            is UpdateStatus.Downloading, is UpdateStatus.Installing, is UpdateStatus.InstallFailed -> Unit
        }
        if (status !is UpdateStatus.Downloading && status !is UpdateStatus.Installing) {
            TextButton(onClick = { onEvent(UpdateBannerEvent.OnLaterClicked) }) { Text("Позже") }
        }
    }
}
