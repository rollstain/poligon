package ru.urent.qatools.feature_login.presentation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import ru.urent.qatools.core_platform.AppInfo
import ru.urent.qatools.feature_login.presentation.mvi.LoginEvent
import ru.urent.qatools.feature_login.presentation.mvi.LoginState
import ru.urent.qatools.uikit.components.QaButton
import ru.urent.qatools.uikit.components.QaTextField
import ru.urent.qatools.uikit.resources.Res
import ru.urent.qatools.uikit.resources.poligon_logo
import ru.urent.qatools.uikit.theme.QaColors
import ru.urent.qatools.uikit.theme.QaDimens

private const val SCHEME_SEPARATOR = "://"
private const val BRAND_PANEL_MIN_WINDOW_WIDTH_DP = 960
private val BrandPanelWidth = 480.dp
private val BrandPanelPadding = 48.dp
private val LoginLogoSize = 40.dp
private val FormWidth = 440.dp
private val SavedLoginShape = RoundedCornerShape(16.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginView(state: LoginState, onEvent: (LoginEvent) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isBrandPanelShown = maxWidth.value >= BRAND_PANEL_MIN_WINDOW_WIDTH_DP
        Row(modifier = Modifier.fillMaxSize()) {
            if (isBrandPanelShown) {
                Column(
                    modifier = Modifier.width(BrandPanelWidth).fillMaxHeight().background(QaColors.sidebar).padding(BrandPanelPadding),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceM)) {
                        Image(painter = painterResource(Res.drawable.poligon_logo), contentDescription = null, modifier = Modifier.size(LoginLogoSize))
                        Text(AppInfo.APP_NAME, style = MaterialTheme.typography.headlineSmall, color = QaColors.textInvert)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(QaDimens.spaceL)) {
                        Text(
                            "Заряд, доступ и место байков на тестовом стенде",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = QaColors.textInvert,
                        )
                        Text(
                            "По одному байку или целым шаблоном. Заряд считается по трекеру и модели каждого байка, " +
                                "поэтому пороги совпадают со стендом.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = QaColors.textInvertSecondary,
                        )
                    }
                    Text(
                        "Стенды открываются только через корпоративный VPN",
                        style = MaterialTheme.typography.bodyMedium,
                        color = QaColors.textInvertSecondary,
                    )
                }
            }
            Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier.width(FormWidth).verticalScroll(rememberScrollState()).padding(vertical = QaDimens.spaceXxl),
                    verticalArrangement = Arrangement.spacedBy(QaDimens.spaceL),
                ) {
                    Text("Вход в стенд", style = MaterialTheme.typography.headlineSmall)
                    val error = state.error
                    if (error != null) Text(error, color = QaColors.negative)
                    val savedLogins = state.savedStands.filter { it.email != null }
                    if (savedLogins.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(QaDimens.spaceS)) {
                            Text("Сохранённые входы", style = MaterialTheme.typography.bodyMedium, color = QaColors.textSecondary)
                            for (stand in savedLogins) {
                                val option = state.standOptions.firstOrNull { it.baseUrl == stand.baseUrl }
                                val host = stand.baseUrl.substringAfter(SCHEME_SEPARATOR)
                                Surface(
                                    onClick = {
                                        if (stand.hasSavedLogin) {
                                            onEvent(LoginEvent.OnSavedLoginClicked(stand))
                                        } else if (option != null) {
                                            onEvent(LoginEvent.OnStandOptionSelected(option))
                                        }
                                    },
                                    enabled = !state.isLoading,
                                    shape = SavedLoginShape,
                                    color = QaColors.surface,
                                    border = BorderStroke(QaDimens.dividerThickness, QaColors.stroke),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = QaDimens.spaceL, vertical = QaDimens.spaceM),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceM),
                                    ) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(QaDimens.spaceXs)) {
                                            Text(option?.label ?: host, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text(
                                                "$host, ${stand.email.orEmpty()}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = QaColors.textSecondary,
                                            )
                                        }
                                        Text(
                                            if (stand.hasSavedLogin) "Войти" else "Ввести пароль",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = QaColors.brandText,
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(thickness = QaDimens.dividerThickness, color = QaColors.stroke)
                        Text("Другой стенд", style = MaterialTheme.typography.bodyMedium, color = QaColors.textSecondary)
                    }
                    var isStandMenuOpen by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = isStandMenuOpen, onExpandedChange = { isStandMenuOpen = it }) {
                        QaTextField(
                            value = state.baseUrl,
                            onValueChange = { baseUrl -> onEvent(LoginEvent.OnBaseUrlChanged(baseUrl)) },
                            label = "Адрес стенда",
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isStandMenuOpen) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                        )
                        ExposedDropdownMenu(
                            expanded = isStandMenuOpen,
                            onDismissRequest = { isStandMenuOpen = false },
                            containerColor = QaColors.surface,
                        ) {
                            for (option in state.standOptions) {
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(option.label, style = MaterialTheme.typography.labelLarge)
                                            Text(option.baseUrl, style = MaterialTheme.typography.bodySmall, color = QaColors.textSecondary)
                                        }
                                    },
                                    onClick = {
                                        onEvent(LoginEvent.OnStandOptionSelected(option))
                                        isStandMenuOpen = false
                                    },
                                )
                            }
                        }
                    }
                    QaTextField(
                        value = state.email,
                        onValueChange = { email -> onEvent(LoginEvent.OnEmailChanged(email)) },
                        label = "Эл. почта",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    QaTextField(
                        value = state.password,
                        onValueChange = { password -> onEvent(LoginEvent.OnPasswordChanged(password)) },
                        label = if (state.isLoginSaved && state.password.isEmpty()) "Пароль — можно не вводить" else "Пароль",
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.toggleable(
                            value = state.isPasswordRemembered,
                            role = Role.Checkbox,
                            onValueChange = { onEvent(LoginEvent.OnRememberPasswordClicked) },
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(QaDimens.spaceS),
                    ) {
                        Checkbox(
                            checked = state.isPasswordRemembered,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(checkedColor = QaColors.brand),
                        )
                        Text("Запомнить пароль", style = MaterialTheme.typography.bodyMedium)
                    }
                    QaButton(
                        text = if (state.isLoading) "Входим…" else "Войти",
                        onClick = { onEvent(LoginEvent.OnLoginClicked) },
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Почта и пароль от админки стенда. Пароль и токен шифруются для вашей учётной записи Windows, " +
                            "«Выйти из всех стендов» в настройках стирает их",
                        style = MaterialTheme.typography.bodySmall,
                        color = QaColors.textSecondary,
                    )
                }
            }
        }
    }
}
