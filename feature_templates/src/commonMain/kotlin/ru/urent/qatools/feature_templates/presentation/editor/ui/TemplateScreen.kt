package ru.urent.qatools.feature_templates.presentation.editor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ru.urent.qatools.base_map.BikeMapController
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
        var mapController by remember { mutableStateOf<BikeMapController?>(null) }
        TemplateView(
            state = state,
            tileLoader = tileLoader,
            onEvent = { screenModel.dispatch(event = it) },
            onMapControllerReady = { mapController = it },
        )
        screenModel.collectSideEffect { handleSideEffect(effect = it, navigator = navigator, mapController = mapController) }
    }
}

private fun handleSideEffect(effect: TemplateSideEffect, navigator: Navigator, mapController: BikeMapController?) {
    when (effect) {
        TemplateSideEffect.Back -> navigator.pop()
        is TemplateSideEffect.OpenQrCode -> navigator.push(Screens.QrCode(bikeQrText = effect.text))
        is TemplateSideEffect.FocusMap -> mapController?.focus(effect.point)
    }
}
