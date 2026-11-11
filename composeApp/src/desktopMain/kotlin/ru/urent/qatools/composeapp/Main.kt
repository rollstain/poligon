package ru.urent.qatools.composeapp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.Rectangle
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.skiko.setupSkikoLoggerFactory
import org.koin.core.context.startKoin
import ru.urent.qatools.composeapp.di.AppModule
import ru.urent.qatools.composeapp.root.RootComponent
import ru.urent.qatools.composeapp.root.RootContent
import ru.urent.qatools.core_logger.AppLog
import ru.urent.qatools.core_platform.AppInfo
import ru.urent.qatools.core_settings.AppSettings
import ru.urent.qatools.core_settings.AppSettingsRepository
import ru.urent.qatools.core_settings.AppTheme
import ru.urent.qatools.core_settings.WindowBounds
import ru.urent.qatools.uikit.resources.Res
import ru.urent.qatools.uikit.resources.poligon_logo
import ru.urent.qatools.uikit.resources.poligon_logo_small
import ru.urent.qatools.uikit.theme.QaTheme

private const val SMALL_LOGO_MAX_PX = 24
private const val PERCENT_IN_UNIT = 100f
private const val WINDOW_SAVE_DELAY_MS = 500
private const val SKIKO_HARDWARE_INFO_PROPERTY = "skiko.hardwareInfo.enabled"
private val WindowIconSizesPx = listOf(16, 20, 24, 32, 40, 48, 64)
private val InitialWindowSize = DpSize(1360.dp, 900.dp)

fun main() {
    val koin = startKoin { modules(AppModule().modules) }.koin
    val log = koin.get<AppLog>()
    setupSkikoLoggerFactory { SkikoAppLog(log) }
    System.setProperty(SKIKO_HARDWARE_INFO_PROPERTY, "true")
    log.info("${AppInfo.APP_NAME} ${AppInfo.version} запущен · ${AppInfo.systemDescription}")
    log.info("Видеоадаптеры: ${AppInfo.graphicsAdapters}")
    val previousExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
        log.error("Необработанная ошибка в потоке ${thread.name}", exception)
        if (previousExceptionHandler == null) exception.printStackTrace() else previousExceptionHandler.uncaughtException(thread, exception)
    }
    val settingsRepository = koin.get<AppSettingsRepository>()
    val settingsAtStart = runBlocking { settingsRepository.settings.first() }
    val lifecycle = LifecycleRegistry()
    val root = runOnUiThread {
        RootComponent(
            componentContext = DefaultComponentContext(lifecycle = lifecycle),
            sessionInteractor = koin.get(),
            screenFactory = koin.get(),
        )
    }
    application {
        val settings by settingsRepository.settings.collectAsState(settingsAtStart)
        val isBorderlessFullscreen = settings.isBorderlessFullscreen
        val scope = rememberCoroutineScope()
        val windowedState = remember { restoredWindowState(settingsAtStart) }
        LaunchedEffect(windowedState) {
            snapshotFlow { WindowGeometry(windowedState.placement, windowedState.position, windowedState.size) }
                .collectLatest { geometry ->
                    delay(WINDOW_SAVE_DELAY_MS.milliseconds)
                    saveWindowGeometry(settingsRepository, geometry)
                }
        }
        val windowState = remember(isBorderlessFullscreen) {
            if (isBorderlessFullscreen) borderlessWindowState(windowedState) else windowedState
        }
        LifecycleController(lifecycleRegistry = lifecycle, windowState = windowState)
        key(isBorderlessFullscreen) {
            Window(
                onCloseRequest = ::exitApplication,
                state = windowState,
                title = AppInfo.APP_NAME,
                undecorated = isBorderlessFullscreen,
                resizable = !isBorderlessFullscreen,
                onPreviewKeyEvent = { event ->
                    val isFullscreenToggle = event.key == Key.F11 && event.type == KeyEventType.KeyDown
                    if (isFullscreenToggle) scope.launch { settingsRepository.saveBorderlessFullscreen(!isBorderlessFullscreen) }
                    isFullscreenToggle
                },
            ) {
                val logo = painterResource(Res.drawable.poligon_logo)
                val smallLogo = painterResource(Res.drawable.poligon_logo_small)
                val density = LocalDensity.current
                LaunchedEffect(logo, smallLogo) {
                    window.iconImages = WindowIconSizesPx.map { sizePx ->
                        val painter = if (sizePx <= SMALL_LOGO_MAX_PX) smallLogo else logo
                        painter.toAwtImage(density, LayoutDirection.Ltr, Size(sizePx.toFloat(), sizePx.toFloat()))
                    }
                }
                val scaledDensity = Density(
                    density = density.density * settings.uiScalePercent / PERCENT_IN_UNIT,
                    fontScale = density.fontScale,
                )
                val isDarkTheme = when (settings.theme) {
                    AppTheme.LIGHT -> false
                    AppTheme.DARK -> true
                    AppTheme.SYSTEM -> isSystemInDarkTheme()
                }
                CompositionLocalProvider(LocalDensity provides scaledDensity) {
                    QaTheme(isDark = isDarkTheme) { RootContent(root = root, onCloseApp = ::exitApplication) }
                }
            }
        }
    }
}

private data class WindowGeometry(val placement: WindowPlacement, val position: WindowPosition, val size: DpSize)

private fun restoredWindowState(settings: AppSettings): WindowState {
    val bounds = settings.windowBounds
    val center = if (bounds == null) null else Point((bounds.x + bounds.width / 2).roundToInt(), (bounds.y + bounds.height / 2).roundToInt())
    val screens = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.map { it.defaultConfiguration.bounds }
    val isOnScreen = center != null && screens.any { it.contains(center) }
    return WindowState(
        placement = if (settings.isWindowMaximized) WindowPlacement.Maximized else WindowPlacement.Floating,
        position = if (bounds != null && isOnScreen) WindowPosition(bounds.x.dp, bounds.y.dp) else WindowPosition.PlatformDefault,
        size = if (bounds == null) InitialWindowSize else DpSize(bounds.width.dp, bounds.height.dp),
    )
}

private suspend fun saveWindowGeometry(repository: AppSettingsRepository, geometry: WindowGeometry) {
    val position = geometry.position
    when {
        geometry.placement == WindowPlacement.Maximized -> repository.saveWindowMaximized()
        geometry.placement == WindowPlacement.Floating && position.isSpecified -> repository.saveWindowBounds(
            WindowBounds(
                x = position.x.value.toDouble(),
                y = position.y.value.toDouble(),
                width = geometry.size.width.value.toDouble(),
                height = geometry.size.height.value.toDouble(),
            ),
        )
    }
}

private fun borderlessWindowState(windowedState: WindowState): WindowState {
    val screen = screenBoundsUnder(windowedState)
    return WindowState(
        placement = WindowPlacement.Floating,
        position = WindowPosition(screen.x.dp, screen.y.dp),
        size = DpSize(screen.width.dp, screen.height.dp),
    )
}

private fun screenBoundsUnder(windowedState: WindowState): Rectangle {
    val environment = GraphicsEnvironment.getLocalGraphicsEnvironment()
    val defaultScreen = environment.defaultScreenDevice.defaultConfiguration.bounds
    val position = windowedState.position
    if (!position.isSpecified) return defaultScreen
    val centerX = (position.x + windowedState.size.width / 2).value.roundToInt()
    val centerY = (position.y + windowedState.size.height / 2).value.roundToInt()
    val screens = environment.screenDevices.map { it.defaultConfiguration.bounds }
    return screens.firstOrNull { it.contains(centerX, centerY) } ?: defaultScreen
}

private fun <T> runOnUiThread(block: () -> T): T {
    if (SwingUtilities.isEventDispatchThread()) return block()
    val result = AtomicReference<T>()
    SwingUtilities.invokeAndWait { result.set(block()) }
    return result.get()
}
