package ru.urent.qatools.feature_settings.di

import com.arkivanov.decompose.ComponentContext
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.urent.qatools.feature_settings.domain.SettingsInteractor
import ru.urent.qatools.feature_settings.presentation.mvi.SettingsScreenModel
import ru.urent.qatools.feature_settings.presentation.ui.SettingsScreen
import ru.urent.qatools.navigation.Screen
import ru.urent.qatools.navigation.Screens

class FeatureSettingsModule {
    val module = module {
        single { SettingsInteractor(appFiles = get(), log = get()) }
        factory { (context: ComponentContext) ->
            SettingsScreenModel(
                componentContext = context,
                interactor = get(),
                sessionInteractor = get(),
                appSettingsRepository = get(),
                templateInteractor = get(),
                bikeSetupInteractor = get(),
                log = get(),
            )
        }
        factory<Screen>(named<Screens.Settings>()) { (context: ComponentContext, _: Screens.Settings) ->
            SettingsScreen(screenModel = get { parametersOf(context) })
        }
    }
}
