package ru.urent.qatools.feature_settings.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import ru.urent.qatools.feature_settings.presentation.mvi.SettingsScreenModel
import ru.urent.qatools.navigation.Screen

class SettingsScreen(private val screenModel: SettingsScreenModel) : Screen {
    @Composable
    override fun Content() {
        val state by screenModel.collectAsState()
        SettingsView(state = state, onEvent = { screenModel.dispatch(event = it) })
    }
}
