package ru.urent.qatools.feature_login.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import ru.urent.qatools.feature_login.presentation.mvi.LoginScreenModel
import ru.urent.qatools.navigation.Screen

class LoginScreen(private val screenModel: LoginScreenModel) : Screen {
    @Composable
    override fun Content() {
        val state by screenModel.collectAsState()
        LoginView(state = state, onEvent = { screenModel.dispatch(event = it) })
    }
}
