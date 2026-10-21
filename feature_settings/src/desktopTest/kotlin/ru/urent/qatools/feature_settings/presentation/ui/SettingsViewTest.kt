package ru.urent.qatools.feature_settings.presentation.ui

import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import ru.urent.qatools.base_bike_setup.entities.AvailabilityTarget
import ru.urent.qatools.base_bike_setup.entities.ChargeTarget
import ru.urent.qatools.base_bike_setup.entities.SetupDefaults
import ru.urent.qatools.base_stand.Stand
import ru.urent.qatools.core_settings.AppSettings
import ru.urent.qatools.core_settings.AppTheme
import ru.urent.qatools.feature_settings.presentation.mvi.SettingsEvent
import ru.urent.qatools.feature_settings.presentation.mvi.SettingsState
import ru.urent.qatools.uikit.theme.QaTheme

@OptIn(ExperimentalTestApi::class)
class SettingsViewTest {
    private val events = mutableListOf<SettingsEvent>()

    @Test
    fun borderlessSegmentSwitchesWindowMode() = runDesktopComposeUiTest(width = CONTENT_WIDTH, height = CONTENT_HEIGHT) {
        showSettings(this)

        onNodeWithText("Во весь экран без рамок").performClick()

        assertEquals(listOf<SettingsEvent>(SettingsEvent.OnBorderlessModeSelected), events)
    }

    @Test
    fun standsStandBesideAppearanceInWideWindow() = runDesktopComposeUiTest(width = CONTENT_WIDTH, height = CONTENT_HEIGHT) {
        showSettings(this)

        val appearance = onNodeWithText("Внешний вид и окно").getUnclippedBoundsInRoot()
        val stands = onNodeWithText("Стенды").getUnclippedBoundsInRoot()
        assertTrue(stands.left > appearance.right, "Стенды должны стоять во второй колонке: $stands, $appearance")
        assertEquals(appearance.top, stands.top)
    }

    @Test
    fun logoutButtonNamesThatItLeavesAllStands() = runDesktopComposeUiTest(width = CONTENT_WIDTH, height = CONTENT_HEIGHT) {
        showSettings(this)

        onNodeWithText("Выйти из всех стендов").performClick()

        assertEquals(listOf<SettingsEvent>(SettingsEvent.OnLogoutClicked), events)
    }

    private fun showSettings(test: DesktopComposeUiTest) {
        val state = SettingsState(
            appSettings = AppSettings(
                isBorderlessFullscreen = false,
                uiScalePercent = UI_SCALE_PERCENT,
                theme = AppTheme.LIGHT,
                isSidebarCollapsed = false,
                isWindowMaximized = false,
                windowBounds = null,
            ),
            email = EMAIL,
            currentBaseUrl = BASE_URL,
            currentHost = HOST,
            savedStands = listOf(Stand(baseUrl = BASE_URL, email = EMAIL, hasSavedLogin = true)),
            archiveMessage = null,
            isArchiveSaved = false,
            setupDefaults = SetupDefaults(target = ChargeTarget.Percent(DEFAULT_PERCENT), availability = AvailabilityTarget.Keep),
            reasons = emptyList(),
            dataDirectoryPath = DATA_DIRECTORY,
        )
        test.setContent {
            QaTheme(isDark = false) { SettingsView(state = state, onEvent = { event -> events.add(event) }) }
        }
        test.waitForIdle()
    }

    private companion object {
        const val CONTENT_WIDTH = 1180
        const val CONTENT_HEIGHT = 868
        const val UI_SCALE_PERCENT = 100
        const val DEFAULT_PERCENT = 50.0
        const val EMAIL = "qa.tester@urent.ru"
        const val HOST = "tm-0.dev.urent.ru"
        const val BASE_URL = "https://$HOST"
        const val DATA_DIRECTORY = "C:\\Users\\qa\\AppData\\Roaming\\Poligon"
    }
}
