package ru.urent.qatools.feature_quick_setup.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import ru.urent.qatools.base_map.MapTileLoader
import ru.urent.qatools.feature_quick_setup.presentation.mvi.QuickSetupScreenModel
import ru.urent.qatools.feature_quick_setup.presentation.mvi.QuickSetupSideEffect
import ru.urent.qatools.navigation.LocalNavigator
import ru.urent.qatools.navigation.Navigator
import ru.urent.qatools.navigation.Screen
import ru.urent.qatools.navigation.Screens

class QuickSetupScreen(
    private val screenModel: QuickSetupScreenModel,
    private val tileLoader: MapTileLoader,
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val state by screenModel.collectAsState()
        QuickSetupView(state = state, tileLoader = tileLoader, onEvent = { screenModel.dispatch(event = it) })
        screenModel.collectSideEffect { handleSideEffect(effect = it, navigator = navigator) }
    }
}

private fun handleSideEffect(effect: QuickSetupSideEffect, navigator: Navigator) {
    when (effect) {
        is QuickSetupSideEffect.OpenQrCode -> navigator.push(Screens.QrCode(bikeQrText = effect.text))
    }
}
