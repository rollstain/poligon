package ru.urent.qatools.feature_login.di

import com.arkivanov.decompose.ComponentContext
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.urent.qatools.feature_login.presentation.mvi.LoginScreenModel
import ru.urent.qatools.feature_login.presentation.ui.LoginScreen
import ru.urent.qatools.navigation.Screen
import ru.urent.qatools.navigation.Screens

class FeatureLoginModule {
    val module = module {
        factory { (context: ComponentContext) ->
            LoginScreenModel(componentContext = context, sessionInteractor = get(), updateInteractor = get(), log = get())
        }
        factory<Screen>(named<Screens.Login>()) { (context: ComponentContext, _: Screens.Login) ->
            LoginScreen(screenModel = get { parametersOf(context) })
        }
    }
}
