package ru.urent.qatools.composeapp.root

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import ru.urent.qatools.composeapp.shell.mvi.ShellScreenModel
import ru.urent.qatools.composeapp.shell.mvi.ShellSideEffect
import ru.urent.qatools.composeapp.shell.ui.Section
import ru.urent.qatools.composeapp.shell.ui.ShellView
import ru.urent.qatools.composeapp.update.mvi.UpdateBannerScreenModel
import ru.urent.qatools.composeapp.update.mvi.UpdateBannerSideEffect
import ru.urent.qatools.composeapp.update.ui.UpdateBannerView
import ru.urent.qatools.navigation.LocalNavigator
import ru.urent.qatools.navigation.Navigator
import ru.urent.qatools.navigation.Screens
import ru.urent.qatools.uikit.theme.QaColors

@Composable
fun RootContent(root: RootComponent, onCloseApp: () -> Unit) {
    CompositionLocalProvider(LocalNavigator provides root) {
        Surface(color = QaColors.background, modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                UpdateBanner(updateBanner = root.updateBanner, onCloseApp = onCloseApp)
                Children(stack = root.stack, modifier = Modifier.fillMaxWidth().weight(1f)) { child ->
                    val screen = child.instance
                    val section = when (child.configuration) {
                        Screens.Login -> null
                        is Screens.Templates, is Screens.Template -> Section.Templates
                        is Screens.QuickSetup -> Section.QuickSetup
                        is Screens.QrCode -> Section.QrCode
                        Screens.Settings -> Section.Settings
                    }
                    if (section == null) {
                        screen.Content()
                    } else {
                        Shell(shell = root.shell, activeSection = section) { screen.Content() }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateBanner(updateBanner: UpdateBannerScreenModel, onCloseApp: () -> Unit) {
    val state by updateBanner.collectAsState()
    UpdateBannerView(state = state, onEvent = { updateBanner.dispatch(event = it) })
    updateBanner.collectSideEffect { effect ->
        when (effect) {
            UpdateBannerSideEffect.CloseApp -> onCloseApp()
        }
    }
}

@Composable
private fun Shell(shell: ShellScreenModel, activeSection: Section, content: @Composable () -> Unit) {
    val navigator = LocalNavigator.current
    val state by shell.collectAsState()
    ShellView(state = state, activeSection = activeSection, onEvent = { shell.dispatch(event = it) }, content = content)
    shell.collectSideEffect { handleSideEffect(effect = it, navigator = navigator) }
}

private fun handleSideEffect(effect: ShellSideEffect, navigator: Navigator) {
    when (effect) {
        is ShellSideEffect.OpenSection -> navigator.replaceAll(effect.screen)
    }
}
