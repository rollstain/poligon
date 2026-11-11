package ru.urent.qatools.composeapp.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.launch
import ru.urent.qatools.base_stand.StandSession
import ru.urent.qatools.base_stand.StandSessionInteractor
import ru.urent.qatools.composeapp.shell.mvi.ShellScreenModel
import ru.urent.qatools.composeapp.update.mvi.UpdateBannerScreenModel
import ru.urent.qatools.core_base_feature.componentScope
import ru.urent.qatools.navigation.Navigator
import ru.urent.qatools.navigation.Screen
import ru.urent.qatools.navigation.Screens

class RootComponent(
    componentContext: ComponentContext,
    sessionInteractor: StandSessionInteractor,
    screenFactory: ScreenFactory,
) : ComponentContext by componentContext, Navigator {
    private val scope = componentScope(lifecycle)
    private val navigation = StackNavigation<Screens>()

    val shell: ShellScreenModel = screenFactory.createShell(childContext(SHELL_CHILD_KEY))

    val updateBanner: UpdateBannerScreenModel = screenFactory.createUpdateBanner(childContext(UPDATE_BANNER_CHILD_KEY))

    val stack: Value<ChildStack<Screens, Screen>> = childStack(
        source = navigation,
        serializer = Screens.serializer(),
        initialConfiguration = Screens.Login,
        handleBackButton = false,
        childFactory = { screen, context -> screenFactory.createScreen(context = context, screen = screen) },
    )

    init {
        scope.launch { sessionInteractor.session.collect { session -> onSessionChanged(session) } }
    }

    override fun push(screen: Screens) = navigation.pushNew(screen)

    override fun replaceAll(screen: Screens) = navigation.replaceAll(screen)

    override fun pop() = navigation.pop()

    private fun onSessionChanged(session: StandSession?) {
        val screen = if (session == null) Screens.Login else Screens.Templates(standBaseUrl = session.baseUrl)
        navigation.replaceAll(screen)
    }

    private companion object {
        const val SHELL_CHILD_KEY = "shell"
        const val UPDATE_BANNER_CHILD_KEY = "update_banner"
    }
}
