package ru.urent.qatools.feature_login.presentation.mvi

import ru.urent.qatools.base_stand.Stand
import ru.urent.qatools.base_stand.StandOption
import ru.urent.qatools.base_update.entities.UpdateStatus

data class LoginState(
    val baseUrl: String,
    val email: String,
    val password: String,
    val isPasswordRemembered: Boolean,
    val standOptions: List<StandOption>,
    val savedStands: List<Stand>,
    val isLoginSaved: Boolean,
    val isLoading: Boolean,
    val error: String?,
    val updateStatus: UpdateStatus,
)
