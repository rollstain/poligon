package ru.urent.qatools.feature_qr_code.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import ru.urent.qatools.feature_qr_code.presentation.mvi.QrCodeScreenModel
import ru.urent.qatools.feature_qr_code.presentation.mvi.QrCodeSideEffect
import ru.urent.qatools.navigation.LocalNavigator
import ru.urent.qatools.navigation.Navigator
import ru.urent.qatools.navigation.Screen

class QrCodeScreen(private val screenModel: QrCodeScreenModel) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val state by screenModel.collectAsState()
        QrCodeView(state = state, onEvent = { screenModel.dispatch(event = it) })
        screenModel.collectSideEffect { handleSideEffect(effect = it, navigator = navigator) }
    }
}

private fun handleSideEffect(effect: QrCodeSideEffect, navigator: Navigator) {
    when (effect) {
        QrCodeSideEffect.Back -> navigator.pop()
    }
}
