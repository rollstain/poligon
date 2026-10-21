package ru.urent.qatools.feature_templates.presentation.editor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import ru.urent.qatools.base_map.MapTileLoader
import ru.urent.qatools.feature_templates.presentation.editor.mvi.TemplateScreenModel
import ru.urent.qatools.feature_templates.presentation.editor.mvi.TemplateSideEffect
import ru.urent.qatools.navigation.LocalNavigator
import ru.urent.qatools.navigation.Navigator
import ru.urent.qatools.navigation.Screen
import ru.urent.qatools.navigation.Screens

class TemplateScreen(
    private val screenModel: TemplateScreenModel,
    private val tileLoader: MapTileLoader,
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val state by screenModel.collectAsState()
        TemplateView(state = state, tileLoader = tileLoader, onEvent = { screenModel.dispatch(event = it) })
        screenModel.collectSideEffect { handleSideEffect(effect = it, navigator = navigator) }
    }
}

private fun handleSideEffect(effect: TemplateSideEffect, navigator: Navigator) {
    when (effect) {
        TemplateSideEffect.Back -> navigator.pop()
        is TemplateSideEffect.OpenQrCode -> navigator.push(Screens.QrCode(bikeQrText = effect.text))
    }
}
