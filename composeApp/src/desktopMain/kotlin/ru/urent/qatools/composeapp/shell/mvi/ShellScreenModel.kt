package ru.urent.qatools.composeapp.shell.mvi

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.launch
import ru.urent.qatools.base_stand.LoginResult
import ru.urent.qatools.base_stand.StandConnectionState
import ru.urent.qatools.base_stand.StandOption
import ru.urent.qatools.base_stand.StandOptions
import ru.urent.qatools.base_stand.StandSession
import ru.urent.qatools.base_stand.StandSessionInteractor
import ru.urent.qatools.core_base_feature.BaseScreenModel
import ru.urent.qatools.core_logger.AppLog
import ru.urent.qatools.core_settings.AppSettingsRepository
import ru.urent.qatools.navigation.Screens

class ShellScreenModel(
    componentContext: ComponentContext,
    private val sessionInteractor: StandSessionInteractor,
    private val appSettingsRepository: AppSettingsRepository,
    log: AppLog,
) : BaseScreenModel<ShellState, ShellSideEffect, ShellEvent>(
    componentContext = componentContext,
    initialState = ShellState(
        currentBaseUrl = null,
        standHost = "",
        standLabel = "",
        standOptions = StandOptions.build(emptyList()),
        isSwitchingStand = false,
        standSwitchError = null,
        isSidebarCollapsed = false,
        connectionState = StandConnectionState.CONNECTED,
    ),
    log = log,
) {
    private var session: StandSession? = null

    init {
        onFetchData()
    }

    override fun dispatch(event: ShellEvent) {
        when (event) {
            ShellEvent.OnTemplatesClicked -> openSection(Screens.Templates(standBaseUrl = state.currentBaseUrl ?: return))
            ShellEvent.OnQuickSetupClicked -> openSection(Screens.QuickSetup(standBaseUrl = state.currentBaseUrl ?: return))
            ShellEvent.OnQrCodeClicked -> openSection(Screens.QrCode(bikeQrText = null))
            ShellEvent.OnSettingsClicked -> openSection(Screens.Settings)
            is ShellEvent.OnStandSelected -> onStandSelected(event.baseUrl)
            ShellEvent.OnSidebarCollapseClicked -> screenModelScope { appSettingsRepository.saveSidebarCollapsed(true) }
            ShellEvent.OnSidebarExpandClicked -> screenModelScope { appSettingsRepository.saveSidebarCollapsed(false) }
        }
    }

    override fun onFetchData() {
        onFetchDataScope {
            launch {
                sessionInteractor.session.collect { current ->
                    session = current
                    publishStand(state.standOptions)
                }
            }
            launch {
                sessionInteractor.savedStands.collect { stands -> publishStand(StandOptions.build(stands)) }
            }
            launch {
                appSettingsRepository.settings.collect { settings -> updateState { it.copy(isSidebarCollapsed = settings.isSidebarCollapsed) } }
            }
            launch {
                sessionInteractor.connectionState.collect { connectionState -> updateState { it.copy(connectionState = connectionState) } }
            }
            sessionInteractor.refreshSavedStands()
        }
    }

    private fun publishStand(standOptions: List<StandOption>) {
        val current = session
        val standHost = current?.host.orEmpty()
        val currentOption = standOptions.firstOrNull { it.baseUrl == current?.baseUrl }
        val standLabel = currentOption?.label ?: standHost
        updateState {
            it.copy(currentBaseUrl = current?.baseUrl, standHost = standHost, standLabel = standLabel, standOptions = standOptions)
        }
    }

    private fun openSection(screen: Screens) {
        if (state.currentBaseUrl == null) return
        screenModelScope { postSideEffect(ShellSideEffect.OpenSection(screen)) }
    }

    private fun onStandSelected(baseUrl: String) {
        updateState { it.copy(isSwitchingStand = true, standSwitchError = null) }
        screenModelScope {
            val error = switchErrorOf(result = sessionInteractor.switchTo(baseUrl), baseUrl = baseUrl)
            updateState { it.copy(isSwitchingStand = false, standSwitchError = error) }
        }
    }

    private fun switchErrorOf(result: LoginResult, baseUrl: String): String? {
        val host = baseUrl.substringAfter(SCHEME_SEPARATOR)
        return when (result) {
            LoginResult.Success -> null
            LoginResult.InvalidCredentials -> "Почта или пароль не подошли для $host"
            LoginResult.SavedPasswordMissing -> "Пароль не сохранён — войдите в $host через «Выйти»"
            LoginResult.SavedLoginExpired -> "Сохранённый вход в $host больше не действует"
            LoginResult.VpnRequired, LoginResult.Unavailable -> "$host недоступен. Проверьте VPN"
            LoginResult.SessionExpired -> "Сессия истекла, войдите заново"
            is LoginResult.Misconfigured -> result.message
            is LoginResult.Unexpected -> {
                val statusCode = result.statusCode
                if (statusCode == null) "$host прислал непонятный ответ" else "$host ответил ошибкой $statusCode"
            }
        }
    }

    private companion object {
        const val SCHEME_SEPARATOR = "://"
    }
}
