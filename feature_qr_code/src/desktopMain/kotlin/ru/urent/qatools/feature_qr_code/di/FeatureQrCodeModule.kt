package ru.urent.qatools.feature_qr_code.di

import com.arkivanov.decompose.ComponentContext
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.urent.qatools.feature_qr_code.domain.QrCodeInteractor
import ru.urent.qatools.feature_qr_code.presentation.mvi.QrCodeScreenModel
import ru.urent.qatools.feature_qr_code.presentation.ui.QrCodeScreen
import ru.urent.qatools.navigation.Screen
import ru.urent.qatools.navigation.Screens

class FeatureQrCodeModule {
    val module = module {
        single { QrCodeInteractor(repository = get()) }
        factory { (context: ComponentContext, screen: Screens.QrCode) ->
            QrCodeScreenModel(
                componentContext = context,
                initialContent = screen.bikeQrText,
                interactor = get(),
                repository = get(),
                log = get(),
            )
        }
        factory<Screen>(named<Screens.QrCode>()) { (context: ComponentContext, screen: Screens.QrCode) ->
            QrCodeScreen(screenModel = get { parametersOf(context, screen) })
        }
    }
}
