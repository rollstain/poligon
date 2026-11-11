package ru.urent.qatools.composeapp.root

import com.arkivanov.decompose.ComponentContext
import org.koin.core.Koin
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.TypeQualifier
import ru.urent.qatools.composeapp.shell.mvi.ShellScreenModel
import ru.urent.qatools.composeapp.update.mvi.UpdateBannerScreenModel
import ru.urent.qatools.navigation.Screen
import ru.urent.qatools.navigation.Screens

class ScreenFactory(private val koin: Koin) {

    fun createScreen(context: ComponentContext, screen: Screens): Screen =
        koin.get(qualifier = TypeQualifier(screen::class)) { parametersOf(context, screen) }

    fun createShell(context: ComponentContext): ShellScreenModel = koin.get { parametersOf(context) }

    fun createUpdateBanner(context: ComponentContext): UpdateBannerScreenModel = koin.get { parametersOf(context) }
}
