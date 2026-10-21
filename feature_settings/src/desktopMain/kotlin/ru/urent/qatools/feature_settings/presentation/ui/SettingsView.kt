package ru.urent.qatools.feature_settings.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.time.LocalDate
import ru.urent.qatools.base_bike_setup.presentation.AvailabilityField
import ru.urent.qatools.base_bike_setup.presentation.ChargeField
import ru.urent.qatools.core_platform.AppInfo
import ru.urent.qatools.core_settings.AppTheme
import ru.urent.qatools.feature_settings.presentation.mvi.SettingsEvent
import ru.urent.qatools.feature_settings.presentation.mvi.SettingsState
import ru.urent.qatools.uikit.components.QaButton
import ru.urent.qatools.uikit.components.QaButtonStyle
import ru.urent.qatools.uikit.theme.QaColors
import ru.urent.qatools.uikit.theme.QaDimens

private const val TWO_COLUMNS_MIN_WIDTH_DP = 960
private const val SCHEME_SEPARATOR = "://"
private const val LOG_ARCHIVE_PREFIX = "Poligon-logs-"
private const val LOG_ARCHIVE_EXTENSION = ".zip"
private val UiScaleOptions = listOf(90, 100, 110, 125, 150)
private val SegmentedShape = RoundedCornerShape(14.dp)
private val SegmentShape = RoundedCornerShape(10.dp)
private val SegmentMinHeight = 40.dp
private val BadgeShape = RoundedCornerShape(8.dp)

@Composable
fun SettingsView(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = QaDimens.screenPaddingHorizontal, vertical = QaDimens.screenPaddingVertical),
        verticalArrangement = Arrangement.spacedBy(QaDimens.spaceL),
    ) {
        Text("Настройки", style = MaterialTheme.typography.headlineSmall)
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth.value >= TWO_COLUMNS_MIN_WIDTH_DP) {
                Row(horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceXl)) {
                    AppGroups(modifier = Modifier.weight(1f), state = state, onEvent = onEvent)
                    StandAndSupportGroups(modifier = Modifier.weight(1f), state = state, onEvent = onEvent)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(QaDimens.spaceXl)) {
                    AppGroups(modifier = Modifier.fillMaxWidth(), state = state, onEvent = onEvent)
                    StandAndSupportGroups(modifier = Modifier.fillMaxWidth(), state = state, onEvent = onEvent)
                }
            }
        }
    }
}

@Composable
private fun AppGroups(modifier: Modifier, state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(QaDimens.spaceXl)) {
        val appSettings = state.appSettings
        if (appSettings != null) {
            SettingsGroup(title = "Внешний вид и окно", note = null) {
                SegmentedSetting(title = "Тема", description = "Подложка карты 2GIS остаётся светлой") {
                    SegmentedChoice(
                        options = listOf(AppTheme.LIGHT to "Светлая", AppTheme.DARK to "Тёмная", AppTheme.SYSTEM to "Как в системе"),
                        selected = appSettings.theme,
                        onSelect = { theme -> onEvent(SettingsEvent.OnThemeSelected(theme)) },
                    )
                }
                HorizontalDivider(thickness = QaDimens.dividerThickness, color = QaColors.stroke)
                SegmentedSetting(title = "Масштаб интерфейса", description = "Текст, поля и карта крупнее на большом мониторе") {
                    SegmentedChoice(
                        options = UiScaleOptions.map { percent -> percent to "$percent%" },
                        selected = appSettings.uiScalePercent,
                        onSelect = { uiScalePercent -> onEvent(SettingsEvent.OnUiScaleSelected(uiScalePercent)) },
                    )
                }
                HorizontalDivider(thickness = QaDimens.dividerThickness, color = QaColors.stroke)
                SegmentedSetting(
                    title = "Окно",
                    description = "Без рамок окно закрывает весь монитор и панель задач. F11 переключает с любого экрана, " +
                        "Alt+F4 закрывает ${AppInfo.APP_NAME} без рамки",
                ) {
                    SegmentedChoice(
                        options = listOf(false to "Обычное", true to "Во весь экран без рамок"),
                        selected = appSettings.isBorderlessFullscreen,
                        onSelect = { isBorderless ->
                            onEvent(if (isBorderless) SettingsEvent.OnBorderlessModeSelected else SettingsEvent.OnWindowedModeSelected)
                        },
                    )
                }
            }
        }
        SettingsGroup(title = "Новые байки", note = "С этих значений начинается разовая настройка и каждый новый байк в шаблоне") {
            val defaults = state.setupDefaults
            if (defaults != null) {
                Row(
                    modifier = Modifier.padding(horizontal = QaDimens.spaceXxl, vertical = QaDimens.spaceL),
                    horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceM),
                ) {
                    ChargeField(
                        target = defaults.target,
                        thresholds = null,
                        onTargetChange = { target -> onEvent(SettingsEvent.OnDefaultTargetChanged(target)) },
                        label = "Заряд",
                        modifier = Modifier.weight(1f),
                    )
                    AvailabilityField(
                        target = defaults.availability,
                        reasons = state.reasons,
                        onTargetChange = { availability -> onEvent(SettingsEvent.OnDefaultAvailabilityChanged(availability)) },
                        label = "Доступ",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StandAndSupportGroups(modifier: Modifier, state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(QaDimens.spaceXl)) {
        SettingsGroup(title = "Стенды", note = "«Забыть» убирает стенд из списка вместе с паролем и токеном. Забыть текущий — значит выйти из него") {
            Row(
                modifier = Modifier.fillMaxWidth().background(QaColors.background).padding(start = QaDimens.spaceXxl, end = QaDimens.spaceL, top = QaDimens.spaceM, bottom = QaDimens.spaceM),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceL),
            ) {
                val email = state.email
                val currentHost = state.currentHost
                Text(
                    if (email == null || currentHost == null) "Вход не выполнен" else "Вы вошли как $email в $currentHost",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                QaButton(text = "Выйти из всех стендов", onClick = { onEvent(SettingsEvent.OnLogoutClicked) }, style = QaButtonStyle.Secondary)
            }
            for (stand in state.savedStands) {
                HorizontalDivider(thickness = QaDimens.dividerThickness, color = QaColors.stroke)
                Row(
                    modifier = Modifier.padding(start = QaDimens.spaceXxl, end = QaDimens.spaceS, top = QaDimens.spaceM, bottom = QaDimens.spaceM),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(QaDimens.spaceXs)) {
                        Text(stand.baseUrl.substringAfter(SCHEME_SEPARATOR), style = MaterialTheme.typography.labelLarge)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceS)) {
                            val standEmail = stand.email
                            if (standEmail != null) {
                                Text(
                                    standEmail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = QaColors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                            }
                            if (stand.baseUrl == state.currentBaseUrl) {
                                Text(
                                    "текущий",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = QaColors.brandText,
                                    modifier = Modifier.background(QaColors.brandSelection, BadgeShape).padding(horizontal = QaDimens.spaceS, vertical = QaDimens.spaceXs),
                                )
                            }
                            Text(
                                if (stand.hasSavedLogin) "вход сохранён" else "только почта",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (stand.hasSavedLogin) QaColors.positiveText else QaColors.textSecondary,
                                modifier = Modifier.background(QaColors.background, BadgeShape).padding(horizontal = QaDimens.spaceS, vertical = QaDimens.spaceXs),
                            )
                        }
                    }
                    TextButton(onClick = { onEvent(SettingsEvent.OnForgetStandClicked(stand.baseUrl)) }) { Text("Забыть") }
                }
            }
        }
        SettingsGroup(title = "Логи для баг-репорта", note = "Приложите архив к задаче: в нём логи, версия ${AppInfo.APP_NAME}, Windows и текущий стенд") {
            Column(
                modifier = Modifier.padding(horizontal = QaDimens.spaceXxl, vertical = QaDimens.spaceL),
                verticalArrangement = Arrangement.spacedBy(QaDimens.spaceM),
            ) {
                Text(
                    "Запросы к стенду и симулятору, шаги применения по каждому байку и ошибки за 7 дней. " +
                        "Пароли, токены и ключи карт в логи не попадают",
                    style = MaterialTheme.typography.bodyMedium,
                    color = QaColors.textSecondary,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceS)) {
                    QaButton(
                        text = "Сохранить архив…",
                        onClick = {
                            val target = chooseArchiveFile()
                            if (target != null) onEvent(SettingsEvent.OnLogArchiveFileChosen(target))
                        },
                    )
                    TextButton(onClick = { onEvent(SettingsEvent.OnOpenLogsFolderClicked) }) { Text("Открыть папку с логами") }
                }
                val archiveMessage = state.archiveMessage
                if (archiveMessage != null) {
                    Text(
                        archiveMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.isArchiveSaved) QaColors.positiveText else QaColors.negative,
                    )
                }
            }
        }
        SettingsGroup(title = "О приложении", note = null) {
            Row(
                modifier = Modifier.padding(horizontal = QaDimens.spaceXxl, vertical = QaDimens.spaceL),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Версия", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text("${AppInfo.APP_NAME} ${AppInfo.version}", style = MaterialTheme.typography.bodyMedium)
            }
            HorizontalDivider(thickness = QaDimens.dividerThickness, color = QaColors.stroke)
            Row(
                modifier = Modifier.padding(start = QaDimens.spaceXxl, end = QaDimens.spaceS, top = QaDimens.spaceM, bottom = QaDimens.spaceM),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(QaDimens.spaceXs)) {
                    Text("Данные", style = MaterialTheme.typography.titleSmall)
                    Text(state.dataDirectoryPath, style = MaterialTheme.typography.bodySmall, color = QaColors.textSecondary)
                }
                TextButton(onClick = { onEvent(SettingsEvent.OnOpenDataFolderClicked) }) { Text("Открыть папку") }
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, note: String?, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(QaDimens.spaceS)) {
        Column(modifier = Modifier.padding(horizontal = QaDimens.spaceS), verticalArrangement = Arrangement.spacedBy(QaDimens.spaceXs)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (note != null) {
                Text(note, style = MaterialTheme.typography.bodySmall, color = QaColors.textSecondary)
            }
        }
        Surface(shape = RoundedCornerShape(QaDimens.radiusCard), color = QaColors.surface, modifier = Modifier.fillMaxWidth()) {
            Column { content() }
        }
    }
}

@Composable
private fun SegmentedSetting(title: String, description: String, control: @Composable () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = QaDimens.spaceXxl, vertical = QaDimens.spaceL),
        verticalArrangement = Arrangement.spacedBy(QaDimens.spaceS),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(QaDimens.spaceXs)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall, color = QaColors.textSecondary)
        }
        control()
    }
}

@Composable
private fun <T> SegmentedChoice(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(
        modifier = Modifier.background(QaColors.background, SegmentedShape).padding(QaDimens.spaceXs),
        horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceXs),
    ) {
        for ((value, title) in options) {
            val isSelected = value == selected
            Surface(
                onClick = { onSelect(value) },
                shape = SegmentShape,
                color = if (isSelected) QaColors.brandSelection else QaColors.background,
                contentColor = if (isSelected) QaColors.brandText else QaColors.textSecondary,
                modifier = Modifier.heightIn(min = SegmentMinHeight),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = QaDimens.spaceM, vertical = QaDimens.spaceS),
                )
            }
        }
    }
}

private fun chooseArchiveFile(): File? {
    val dialog = FileDialog(null as Frame?, "Сохранить архив логов", FileDialog.SAVE)
    dialog.file = "$LOG_ARCHIVE_PREFIX${LocalDate.now()}$LOG_ARCHIVE_EXTENSION"
    dialog.isVisible = true
    val directory = dialog.directory
    val fileName = dialog.file
    if (directory == null || fileName == null) return null
    val name = if (fileName.endsWith(LOG_ARCHIVE_EXTENSION)) fileName else "$fileName$LOG_ARCHIVE_EXTENSION"
    return File(directory, name)
}
