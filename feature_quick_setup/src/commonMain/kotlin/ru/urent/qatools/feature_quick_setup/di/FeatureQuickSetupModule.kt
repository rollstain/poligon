package ru.urent.qatools.feature_quick_setup.di

import com.arkivanov.decompose.ComponentContext
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.urent.qatools.base_stand.StandSessionInteractor
import ru.urent.qatools.feature_quick_setup.presentation.mvi.QuickSetupScreenModel
import ru.urent.qatools.feature_quick_setup.presentation.ui.QuickSetupScreen
import ru.urent.qatools.navigation.Screen
import ru.urent.qatools.navigation.Screens

class FeatureQuickSetupModule {
    val module = module {
        factory { (context: ComponentContext, screen: Screens.QuickSetup) ->
            QuickSetupScreenModel(
                componentContext = context,
                standBaseUrl = screen.standBaseUrl,
                bikeSetupInteractor = get(),
                templateInteractor = get(),
                locationInteractor = get(),
                tileSource = get<StandSessionInteractor>().session.value?.config?.tileSource,
                reconnections = get<StandSessionInteractor>().reconnections,
                log = get(),
            )
        }
        factory<Screen>(named<Screens.QuickSetup>()) { (context: ComponentContext, screen: Screens.QuickSetup) ->
            QuickSetupScreen(screenModel = get { parametersOf(context, screen) }, tileLoader = get())
        }
    }
}
