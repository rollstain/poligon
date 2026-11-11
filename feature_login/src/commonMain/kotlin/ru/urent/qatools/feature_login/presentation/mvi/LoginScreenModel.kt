package ru.urent.qatools.feature_login.presentation.mvi

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.launch
import ru.urent.qatools.base_stand.LoginResult
import ru.urent.qatools.base_stand.Stand
import ru.urent.qatools.base_stand.StandOptions
import ru.urent.qatools.base_stand.StandSessionInteractor
import ru.urent.qatools.base_update.domain.UpdateInteractor
import ru.urent.qatools.core_base_feature.BaseScreenModel
import ru.urent.qatools.core_logger.AppLog

class LoginScreenModel(
    componentContext: ComponentContext,
    private val sessionInteractor: StandSessionInteractor,
    private val updateInteractor: UpdateInteractor,
    log: AppLog,
) : BaseScreenModel<LoginState, Nothing, LoginEvent>(
    componentContext = componentContext,
    initialState = LoginState(
        baseUrl = "",
        email = "",
        password = "",
        isPasswordRemembered = true,
        standOptions = StandOptions.build(emptyList()),
        savedStands = emptyList(),
        isLoginSaved = false,
        isLoading = false,
        error = null,
        updateStatus = updateInteractor.status.value,
    ),
    log = log,
) {
    private var savedStands: List<Stand> = emptyList()

    init {
        onFetchData()
    }

    override fun dispatch(event: LoginEvent) {
        when (event) {
            is LoginEvent.OnBaseUrlChanged -> onFieldsChanged(baseUrl = event.baseUrl, email = state.email, password = state.password)
            is LoginEvent.OnEmailChanged -> onFieldsChanged(baseUrl = state.baseUrl, email = event.email, password = state.password)
            is LoginEvent.OnPasswordChanged -> onFieldsChanged(baseUrl = state.baseUrl, email = state.email, password = event.password)
            LoginEvent.OnRememberPasswordClicked -> updateState { it.copy(isPasswordRemembered = !it.isPasswordRemembered) }
            is LoginEvent.OnStandOptionSelected -> onStandOptionSelected(event)
            is LoginEvent.OnSavedLoginClicked -> onSavedLoginClicked(event.stand)
            LoginEvent.OnLoginClicked -> onLoginClicked()
            LoginEvent.OnCheckUpdatesClicked -> onCheckUpdatesClicked()
        }
    }

    override fun onFetchData() {
        onFetchDataScope {
            launch {
                updateInteractor.status.collect { updateStatus -> updateState { it.copy(updateStatus = updateStatus) } }
            }
            savedStands = sessionInteractor.refreshSavedStands()
            val last = savedStands.firstOrNull()
            val lastPassword = if (last == null) null else sessionInteractor.getSavedPassword(last.baseUrl)
            val baseUrl = state.baseUrl.ifEmpty { last?.baseUrl.orEmpty() }
            val email = state.email.ifEmpty { last?.email.orEmpty() }
            val password = state.password.ifEmpty { lastPassword.orEmpty() }
            val standOptions = StandOptions.build(savedStands)
            val isLoginSaved = isLoginSaved(baseUrl = baseUrl, email = email)
            updateState {
                it.copy(
                    baseUrl = baseUrl,
                    email = email,
                    password = password,
                    standOptions = standOptions,
                    savedStands = savedStands,
                    isLoginSaved = isLoginSaved,
                )
            }
            if (isLoginSaved) {
                updateState { it.copy(isLoading = true, error = null) }
                finishLogin(sessionInteractor.restore(baseUrl))
            }
        }
    }

    private fun onFieldsChanged(baseUrl: String, email: String, password: String) {
        val isLoginSaved = isLoginSaved(baseUrl = baseUrl, email = email)
        updateState { it.copy(baseUrl = baseUrl, email = email, password = password, isLoginSaved = isLoginSaved, error = null) }
    }

    private fun onStandOptionSelected(event: LoginEvent.OnStandOptionSelected) {
        screenModelScope {
            val option = event.option
            val savedPassword = sessionInteractor.getSavedPassword(option.baseUrl)
            val email = option.email ?: state.email
            val password = savedPassword ?: state.password
            onFieldsChanged(baseUrl = option.baseUrl, email = email, password = password)
        }
    }

    private fun onSavedLoginClicked(stand: Stand) {
        onFieldsChanged(baseUrl = stand.baseUrl, email = stand.email.orEmpty(), password = "")
        onLoginClicked()
    }

    private fun onLoginClicked() {
        val current = state
        val isSavedLoginUsed = current.password.isEmpty() && current.isLoginSaved
        val isFormFilled = current.baseUrl.isNotBlank() && current.email.isNotBlank() && current.password.isNotEmpty()
        if (!isSavedLoginUsed && !isFormFilled) {
            updateState { it.copy(error = "Заполните адрес стенда, почту и пароль") }
            return
        }
        updateState { it.copy(isLoading = true, error = null) }
        screenModelScope {
            val result = if (isSavedLoginUsed) {
                sessionInteractor.restore(current.baseUrl)
            } else {
                sessionInteractor.login(
                    rawBaseUrl = current.baseUrl,
                    email = current.email.trim(),
                    password = current.password,
                    rememberPassword = current.isPasswordRemembered,
                )
            }
            finishLogin(result)
        }
    }

    private fun onCheckUpdatesClicked() {
        updateInteractor.check()
    }

    private suspend fun finishLogin(result: LoginResult) {
        val error = errorOf(result)
        savedStands = sessionInteractor.refreshSavedStands()
        val standOptions = StandOptions.build(savedStands)
        val password = if (error == null) "" else state.password
        val isLoginSaved = isLoginSaved(baseUrl = state.baseUrl, email = state.email)
        updateState {
            it.copy(
                standOptions = standOptions,
                savedStands = savedStands,
                isLoginSaved = isLoginSaved,
                isLoading = false,
                password = password,
                error = error,
            )
        }
    }

    private fun isLoginSaved(baseUrl: String, email: String): Boolean =
        savedStands.any { it.baseUrl == baseUrl && it.email == email.trim() && it.hasSavedLogin }

    private fun errorOf(result: LoginResult): String? = when (result) {
        LoginResult.Success -> null
        LoginResult.SavedLoginExpired -> "Сохранённый вход больше не действует. Введите пароль"
        LoginResult.InvalidCredentials -> "Неверная почта или пароль"
        LoginResult.VpnRequired -> "VPN не работает: стенд не пускает без него. Включите VPN и нажмите «Войти»"
        LoginResult.Unavailable -> "Стенд недоступен. Проверьте адрес и VPN"
        LoginResult.SessionExpired -> "Сессия истекла, войдите заново"
        LoginResult.SavedPasswordMissing -> "Пароль не сохранён — введите его"
        is LoginResult.Misconfigured -> result.message
        is LoginResult.Unexpected -> {
            val statusCode = result.statusCode
            if (statusCode == null) "Стенд прислал непонятный ответ" else "Стенд ответил ошибкой $statusCode"
        }
    }
}
