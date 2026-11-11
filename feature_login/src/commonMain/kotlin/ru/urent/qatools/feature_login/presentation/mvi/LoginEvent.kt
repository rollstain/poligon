package ru.urent.qatools.feature_login.presentation.mvi

import ru.urent.qatools.base_stand.Stand
import ru.urent.qatools.base_stand.StandOption

sealed interface LoginEvent {
    data class OnBaseUrlChanged(val baseUrl: String) : LoginEvent

    data class OnEmailChanged(val email: String) : LoginEvent

    data class OnPasswordChanged(val password: String) : LoginEvent

    data object OnRememberPasswordClicked : LoginEvent

    data class OnStandOptionSelected(val option: StandOption) : LoginEvent

    data class OnSavedLoginClicked(val stand: Stand) : LoginEvent

    data object OnLoginClicked : LoginEvent

    data object OnCheckUpdatesClicked : LoginEvent
}
