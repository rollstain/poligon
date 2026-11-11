package ru.urent.qatools.feature_login.presentation.ui

import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import ru.urent.qatools.base_stand.Stand
import ru.urent.qatools.base_stand.StandOptions
import ru.urent.qatools.base_update.entities.UpdateStatus
import ru.urent.qatools.feature_login.presentation.mvi.LoginEvent
import ru.urent.qatools.feature_login.presentation.mvi.LoginState
import ru.urent.qatools.uikit.theme.QaTheme

@OptIn(ExperimentalTestApi::class)
class LoginViewTest {
    private val events = mutableListOf<LoginEvent>()

    @Test
    fun savedLoginSignsInWithOneClick() = runDesktopComposeUiTest(width = WINDOW_WIDTH, height = WINDOW_HEIGHT) {
        showLogin(this)

        onNodeWithText("TM-0").performClick()

        assertEquals(listOf<LoginEvent>(LoginEvent.OnSavedLoginClicked(SavedLogin)), events)
    }

    @Test
    fun emailOnlyStandFillsFormInsteadOfSigningIn() = runDesktopComposeUiTest(width = WINDOW_WIDTH, height = WINDOW_HEIGHT) {
        showLogin(this)

        onNodeWithText("Ввести пароль").performClick()

        val option = StandOptions.build(Stands).first { it.baseUrl == EmailOnly.baseUrl }
        assertEquals(listOf<LoginEvent>(LoginEvent.OnStandOptionSelected(option)), events)
    }

    @Test
    fun narrowWindowHidesBrandPanelButKeepsForm() = runDesktopComposeUiTest(width = NARROW_WINDOW_WIDTH, height = WINDOW_HEIGHT) {
        showLogin(this)

        onNodeWithText(BRAND_TITLE).assertDoesNotExist()
        onNodeWithText("Вход в стенд").assertIsDisplayed()
    }

    @Test
    fun wideWindowShowsBrandPanel() = runDesktopComposeUiTest(width = WINDOW_WIDTH, height = WINDOW_HEIGHT) {
        showLogin(this)

        onNodeWithText(BRAND_TITLE).assertIsDisplayed()
    }

    @Test
    fun checkUpdatesButtonAsksForCheck() = runDesktopComposeUiTest(width = WINDOW_WIDTH, height = WINDOW_HEIGHT) {
        showLogin(this)

        onNodeWithText(CHECK_UPDATES).performClick()

        assertEquals(listOf<LoginEvent>(LoginEvent.OnCheckUpdatesClicked), events)
    }

    @Test
    fun checkUpdatesButtonWaitsForRunningCheck() = runDesktopComposeUiTest(width = WINDOW_WIDTH, height = WINDOW_HEIGHT) {
        showLogin(this, updateStatus = UpdateStatus.Checking)

        onNodeWithText(CHECK_UPDATES).assertIsNotEnabled()
        onNodeWithText("Проверяем обновления…").assertIsDisplayed()
    }

    private fun showLogin(test: DesktopComposeUiTest, updateStatus: UpdateStatus = UpdateStatus.NotChecked) {
        val state = LoginState(
            baseUrl = SavedLogin.baseUrl,
            email = EMAIL,
            password = "",
            isPasswordRemembered = true,
            standOptions = StandOptions.build(Stands),
            savedStands = Stands,
            isLoginSaved = true,
            isLoading = false,
            error = null,
            updateStatus = updateStatus,
        )
        test.setContent {
            QaTheme(isDark = false) { LoginView(state = state, onEvent = { event -> events.add(event) }) }
        }
        test.waitForIdle()
    }

    private companion object {
        const val WINDOW_WIDTH = 1440
        const val NARROW_WINDOW_WIDTH = 800
        const val WINDOW_HEIGHT = 900
        const val EMAIL = "qa.tester@urent.ru"
        const val BRAND_TITLE = "Заряд, доступ и место байков на тестовом стенде"
        const val CHECK_UPDATES = "Проверить обновления"
        val SavedLogin = Stand(baseUrl = "https://tm-0.dev.urent.ru", email = EMAIL, hasSavedLogin = true)
        val EmailOnly = Stand(baseUrl = "https://tm-3.dev.urent.ru", email = EMAIL, hasSavedLogin = false)
        val Stands = listOf(SavedLogin, EmailOnly)
    }
}
