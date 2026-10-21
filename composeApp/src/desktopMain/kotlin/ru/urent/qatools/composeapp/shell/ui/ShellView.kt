package ru.urent.qatools.composeapp.shell.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import ru.urent.qatools.base_stand.StandConnectionState
import ru.urent.qatools.composeapp.resources.Res
import ru.urent.qatools.composeapp.resources.ic_collapse
import ru.urent.qatools.composeapp.resources.ic_expand
import ru.urent.qatools.composeapp.resources.ic_qr_code
import ru.urent.qatools.composeapp.resources.ic_quick_setup
import ru.urent.qatools.composeapp.resources.ic_settings
import ru.urent.qatools.composeapp.resources.ic_templates
import ru.urent.qatools.composeapp.shell.mvi.ShellEvent
import ru.urent.qatools.composeapp.shell.mvi.ShellState
import ru.urent.qatools.core_platform.AppInfo
import ru.urent.qatools.uikit.components.QaButton
import ru.urent.qatools.uikit.components.QaButtonStyle
import ru.urent.qatools.uikit.components.QaTextField
import ru.urent.qatools.uikit.resources.Res as UikitRes
import ru.urent.qatools.uikit.resources.poligon_logo
import ru.urent.qatools.uikit.theme.QaColors
import ru.urent.qatools.uikit.theme.QaDimens

private const val RAIL_STAND_LABEL_MAX_LENGTH = 4
private const val OVERLAY_SCRIM_ALPHA = 0.85f
private val OverlayCardMaxWidth = 440.dp
private val OverlayProgressSize = 16.dp
private val OverlayProgressStroke = 2.dp
private val SidebarLogoSize = 28.dp
private val SidebarCollapsedWidth = 64.dp
private val SidebarAutoCollapseWindowWidth = 1200.dp
private val RailStandChipHeight = 36.dp
private val RailIconSize = 20.dp
private val RailDividerWidth = 28.dp

@Composable
fun ShellView(state: ShellState, activeSection: Section, onEvent: (ShellEvent) -> Unit, content: @Composable () -> Unit) {
    var isStandMenuOpen by remember { mutableStateOf(false) }
    var isCustomStandDialogOpen by remember { mutableStateOf(false) }
    val onStandMenuOpenChange: (Boolean) -> Unit = { isOpen -> isStandMenuOpen = isOpen }
    val onCustomStandClick = {
        isStandMenuOpen = false
        isCustomStandDialogOpen = true
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWindowNarrow = maxWidth < SidebarAutoCollapseWindowWidth
        Row(modifier = Modifier.fillMaxSize()) {
            if (isWindowNarrow || state.isSidebarCollapsed) {
                CollapsedSidebar(
                    state = state,
                    activeSection = activeSection,
                    isStandMenuOpen = isStandMenuOpen,
                    onStandMenuOpenChange = onStandMenuOpenChange,
                    onCustomStandClick = onCustomStandClick,
                    onEvent = onEvent,
                    canExpand = !isWindowNarrow,
                )
            } else {
                ExpandedSidebar(
                    state = state,
                    activeSection = activeSection,
                    isStandMenuOpen = isStandMenuOpen,
                    onStandMenuOpenChange = onStandMenuOpenChange,
                    onCustomStandClick = onCustomStandClick,
                    onEvent = onEvent,
                )
            }
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) { content() }
        }
        if (state.connectionState != StandConnectionState.CONNECTED) {
            StandConnectionLostOverlay(isVpnRequired = state.connectionState == StandConnectionState.VPN_REQUIRED)
        }
    }

    if (isCustomStandDialogOpen) {
        var customUrl by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { isCustomStandDialogOpen = false },
            containerColor = QaColors.surface,
            shape = RoundedCornerShape(QaDimens.radiusDialog),
            title = { Text("Другой стенд", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(QaDimens.spaceM)) {
                    Text(
                        "Войдём с той же почтой и паролем, что и на текущем стенде.",
                        color = QaColors.textSecondary,
                    )
                    QaTextField(
                        value = customUrl,
                        onValueChange = { customUrl = it },
                        label = "Адрес, например https://tm-7.dev.urent.ru",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                QaButton(
                    text = "Перейти",
                    enabled = customUrl.isNotBlank(),
                    onClick = {
                        isCustomStandDialogOpen = false
                        onEvent(ShellEvent.OnStandSelected(customUrl))
                    },
                )
            },
            dismissButton = {
                QaButton(text = "Отмена", onClick = { isCustomStandDialogOpen = false }, style = QaButtonStyle.Secondary)
            },
        )
    }
}

@Composable
private fun ExpandedSidebar(
    state: ShellState,
    activeSection: Section,
    isStandMenuOpen: Boolean,
    onStandMenuOpenChange: (Boolean) -> Unit,
    onCustomStandClick: () -> Unit,
    onEvent: (ShellEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(QaDimens.sidebarWidth)
            .fillMaxHeight()
            .background(QaColors.sidebar)
            .padding(horizontal = QaDimens.spaceL, vertical = QaDimens.spaceXxl),
        verticalArrangement = Arrangement.spacedBy(QaDimens.spaceXxl),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = QaDimens.spaceS),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceM),
        ) {
            Image(
                painter = painterResource(UikitRes.drawable.poligon_logo),
                contentDescription = null,
                modifier = Modifier.size(SidebarLogoSize),
            )
            Text(AppInfo.APP_NAME, color = QaColors.textInvert, style = MaterialTheme.typography.titleLarge)
        }
        Column(verticalArrangement = Arrangement.spacedBy(QaDimens.spaceS)) {
            Text(
                "Стенд",
                color = QaColors.textInvertSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = QaDimens.spaceS),
            )
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = QaDimens.controlHeight)
                        .background(QaColors.sidebarControl, RoundedCornerShape(QaDimens.radiusField))
                        .clickable(enabled = !state.isSwitchingStand, onClick = { onStandMenuOpenChange(true) })
                        .padding(horizontal = QaDimens.spaceM),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = state.standLabel, color = QaColors.textInvert, style = MaterialTheme.typography.labelLarge)
                        Text(text = state.standHost, color = QaColors.textInvertSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("▾", color = QaColors.textInvert)
                }
                StandMenu(
                    isExpanded = isStandMenuOpen,
                    state = state,
                    onDismiss = { onStandMenuOpenChange(false) },
                    onCustomStandClick = onCustomStandClick,
                    onEvent = onEvent,
                )
            }
            if (state.isSwitchingStand) {
                Text(
                    "Переключаемся…",
                    color = QaColors.textInvertSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = QaDimens.spaceS),
                )
            }
            val switchError = state.standSwitchError
            if (switchError != null) {
                Text(
                    switchError,
                    color = QaColors.sidebarError,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = QaDimens.spaceS),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(QaDimens.spaceXs)) {
            Text(
                "Заряд и геопозиция",
                color = QaColors.textInvertSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = QaDimens.spaceS),
            )
            SidebarItem(
                text = "Шаблоны",
                isActive = activeSection == Section.Templates,
                onClick = { onEvent(ShellEvent.OnTemplatesClicked) },
            )
            SidebarItem(
                text = "Разовая настройка",
                isActive = activeSection == Section.QuickSetup,
                onClick = { onEvent(ShellEvent.OnQuickSetupClicked) },
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(QaDimens.spaceXs)) {
            Text(
                "Инструменты",
                color = QaColors.textInvertSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = QaDimens.spaceS),
            )
            SidebarItem(text = "QR-код", isActive = activeSection == Section.QrCode, onClick = { onEvent(ShellEvent.OnQrCodeClicked) })
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceXs)) {
            Box(modifier = Modifier.weight(1f)) {
                SidebarItem(text = "Настройки", isActive = activeSection == Section.Settings, onClick = { onEvent(ShellEvent.OnSettingsClicked) })
            }
            RailButton(
                icon = Res.drawable.ic_collapse,
                label = "Свернуть панель",
                isActive = false,
                onClick = { onEvent(ShellEvent.OnSidebarCollapseClicked) },
            )
        }
    }
}

@Composable
private fun CollapsedSidebar(
    state: ShellState,
    activeSection: Section,
    isStandMenuOpen: Boolean,
    onStandMenuOpenChange: (Boolean) -> Unit,
    onCustomStandClick: () -> Unit,
    onEvent: (ShellEvent) -> Unit,
    canExpand: Boolean,
) {
    val switchError = state.standSwitchError
    Column(
        modifier = Modifier
            .width(SidebarCollapsedWidth)
            .fillMaxHeight()
            .background(QaColors.sidebar)
            .padding(vertical = QaDimens.spaceXxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(QaDimens.spaceM),
    ) {
        Image(
            painter = painterResource(UikitRes.drawable.poligon_logo),
            contentDescription = AppInfo.APP_NAME,
            modifier = Modifier.size(SidebarLogoSize),
        )
        Box {
            SidebarTooltip(text = switchError ?: state.standHost) {
                Box(
                    modifier = Modifier
                        .size(width = QaDimens.controlHeight, height = RailStandChipHeight)
                        .background(QaColors.sidebarControl, RoundedCornerShape(QaDimens.radiusField))
                        .clickable(enabled = !state.isSwitchingStand, onClick = { onStandMenuOpenChange(true) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (state.isSwitchingStand) "…" else state.standLabel.substringBefore('.').uppercase().take(RAIL_STAND_LABEL_MAX_LENGTH),
                        color = if (switchError == null) QaColors.textInvert else QaColors.sidebarError,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            StandMenu(
                isExpanded = isStandMenuOpen,
                state = state,
                onDismiss = { onStandMenuOpenChange(false) },
                onCustomStandClick = onCustomStandClick,
                onEvent = onEvent,
            )
        }
        HorizontalDivider(
            modifier = Modifier.width(RailDividerWidth),
            thickness = QaDimens.dividerThickness,
            color = QaColors.sidebarSelection,
        )
        RailButton(
            icon = Res.drawable.ic_templates,
            label = "Шаблоны",
            isActive = activeSection == Section.Templates,
            onClick = { onEvent(ShellEvent.OnTemplatesClicked) },
        )
        RailButton(
            icon = Res.drawable.ic_quick_setup,
            label = "Разовая настройка",
            isActive = activeSection == Section.QuickSetup,
            onClick = { onEvent(ShellEvent.OnQuickSetupClicked) },
        )
        HorizontalDivider(
            modifier = Modifier.width(RailDividerWidth),
            thickness = QaDimens.dividerThickness,
            color = QaColors.sidebarSelection,
        )
        RailButton(
            icon = Res.drawable.ic_qr_code,
            label = "QR-код",
            isActive = activeSection == Section.QrCode,
            onClick = { onEvent(ShellEvent.OnQrCodeClicked) },
        )
        Spacer(modifier = Modifier.weight(1f))
        RailButton(
            icon = Res.drawable.ic_settings,
            label = "Настройки",
            isActive = activeSection == Section.Settings,
            onClick = { onEvent(ShellEvent.OnSettingsClicked) },
        )
        if (canExpand) {
            RailButton(
                icon = Res.drawable.ic_expand,
                label = "Развернуть панель",
                isActive = false,
                onClick = { onEvent(ShellEvent.OnSidebarExpandClicked) },
            )
        }
    }
}

@Composable
private fun StandMenu(
    isExpanded: Boolean,
    state: ShellState,
    onDismiss: () -> Unit,
    onCustomStandClick: () -> Unit,
    onEvent: (ShellEvent) -> Unit,
) {
    DropdownMenu(expanded = isExpanded, onDismissRequest = onDismiss) {
        for (option in state.standOptions) {
            val isCurrent = option.baseUrl == state.currentBaseUrl
            DropdownMenuItem(
                text = {
                    Column {
                        Text(
                            option.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isCurrent) QaColors.brandText else QaColors.textPrimary,
                        )
                        Text(
                            option.baseUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = QaColors.textSecondary,
                        )
                    }
                },
                onClick = {
                    onDismiss()
                    onEvent(ShellEvent.OnStandSelected(option.baseUrl))
                },
            )
        }
        DropdownMenuItem(
            text = { Text("Другой адрес…", style = MaterialTheme.typography.labelLarge) },
            onClick = onCustomStandClick,
        )
    }
}

@Composable
private fun StandConnectionLostOverlay(isVpnRequired: Boolean) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(QaColors.background.copy(alpha = OVERLAY_SCRIM_ALPHA))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            }
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { true },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(QaDimens.radiusDialog),
            color = QaColors.surface,
            shadowElevation = QaDimens.spaceS,
            modifier = Modifier.widthIn(max = OverlayCardMaxWidth),
        ) {
            Column(modifier = Modifier.padding(QaDimens.spaceXxl), verticalArrangement = Arrangement.spacedBy(QaDimens.spaceM)) {
                Text(if (isVpnRequired) "VPN отвалился" else "Стенд не отвечает", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = if (isVpnRequired) {
                        "Стенд не пускает без VPN. Переподключите VPN — «Полигон» продолжит с того же места, всё введённое останется."
                    } else {
                        "Нет ответа от стенда. Проверьте интернет и VPN — «Полигон» продолжит с того же места, всё введённое останется."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = QaColors.textSecondary,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceS)) {
                    CircularProgressIndicator(
                        color = QaColors.brand,
                        strokeWidth = OverlayProgressStroke,
                        modifier = Modifier.size(OverlayProgressSize),
                    )
                    Text("Проверяем подключение…", style = MaterialTheme.typography.bodySmall, color = QaColors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun RailButton(icon: DrawableResource, label: String, isActive: Boolean, onClick: () -> Unit) {
    SidebarTooltip(text = label) {
        Box(
            modifier = Modifier
                .size(QaDimens.controlHeight)
                .background(if (isActive) QaColors.sidebarSelection else QaColors.sidebar, RoundedCornerShape(QaDimens.radiusField))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = label,
                tint = if (isActive) QaColors.textInvert else QaColors.textInvertSecondary,
                modifier = Modifier.size(RailIconSize),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SidebarTooltip(text: String, content: @Composable () -> Unit) {
    TooltipArea(
        tooltip = {
            Surface(shape = RoundedCornerShape(QaDimens.radiusField), color = QaColors.textPrimary, shadowElevation = QaDimens.spaceXs) {
                Text(
                    text,
                    color = QaColors.surface,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = QaDimens.spaceS, vertical = QaDimens.spaceXs),
                )
            }
        },
        content = content,
    )
}

@Composable
private fun SidebarItem(text: String, isActive: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = QaDimens.controlHeight)
            .background(if (isActive) QaColors.sidebarSelection else QaColors.sidebar, RoundedCornerShape(QaDimens.radiusField))
            .clickable(onClick = onClick)
            .padding(horizontal = QaDimens.spaceM),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            color = if (isActive) QaColors.textInvert else QaColors.textInvertSecondary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}
