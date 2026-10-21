package ru.urent.qatools.feature_templates.presentation.list.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import ru.urent.qatools.feature_templates.presentation.list.mvi.TemplatesScreenModel
import ru.urent.qatools.feature_templates.presentation.list.mvi.TemplatesSideEffect
import ru.urent.qatools.navigation.LocalNavigator
import ru.urent.qatools.navigation.Navigator
import ru.urent.qatools.navigation.Screen
import ru.urent.qatools.navigation.Screens

class TemplatesScreen(
    private val arguments: Screens.Templates,
    private val screenModel: TemplatesScreenModel,
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val state by screenModel.collectAsState()
        TemplatesView(state = state, onEvent = { screenModel.dispatch(event = it) })
        screenModel.collectSideEffect { handleSideEffect(effect = it, navigator = navigator, arguments = arguments) }
    }
}

private fun handleSideEffect(effect: TemplatesSideEffect, navigator: Navigator, arguments: Screens.Templates) {
    when (effect) {
        is TemplatesSideEffect.OpenTemplate -> navigator.push(Screens.Template(standBaseUrl = arguments.standBaseUrl, templateId = effect.templateId))
    }
}
