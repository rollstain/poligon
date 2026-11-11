package ru.urent.qatools.feature_quick_setup.presentation.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.emptyFlow
import ru.urent.qatools.base_bike_setup.entities.BikeLookup
import ru.urent.qatools.feature_quick_setup.presentation.mvi.QuickSetupScreenModel
import ru.urent.qatools.navigation.LocalNavigator
import ru.urent.qatools.navigation.Screens
import ru.urent.qatools.testing_stand.RecordingNavigator
import ru.urent.qatools.testing_stand.ScreenTestStand
import ru.urent.qatools.testing_stand.SilentLog
import ru.urent.qatools.uikit.theme.QaTheme

@OptIn(ExperimentalTestApi::class)
class QuickSetupScreenTest {
    private val navigator = RecordingNavigator()

    @Test
    fun targetColumnStandsBesideMapWithApplyButtonInside() = runDesktopComposeUiTest(width = WIDE_CONTENT_WIDTH, height = CONTENT_HEIGHT) {
        showFoundBike(this)

        val target = onNodeWithText("Станет после применения").getUnclippedBoundsInRoot()
        val mapHint = onNodeWithText(MAP_HINT).getUnclippedBoundsInRoot()
        val apply = onNodeWithText("Применить сейчас").getUnclippedBoundsInRoot()
        assertTrue(mapHint.left > target.right, "Карта должна стоять справа от колонки цели: $mapHint, $target")
        assertTrue(apply.right < mapHint.left, "«Применить сейчас» должна быть в колонке цели, а не под картой: $apply")
        onNodeWithText("Доступ после применения").assertIsDisplayed()
        onNodeWithText("Применить сейчас").assertIsDisplayed()
    }

    @Test
    fun standFactsStandInStripRightOfBike() = runDesktopComposeUiTest(width = WIDE_CONTENT_WIDTH, height = CONTENT_HEIGHT) {
        showFoundBike(this)

        val bikeTitle = onNodeWithText("S.${ScreenTestStand.FOUND_BIKE_NUMBER}").getUnclippedBoundsInRoot()
        val target = onNodeWithText("Станет после применения").getUnclippedBoundsInRoot()
        for (fact in listOf("Связь", "GPS", "Доступ")) {
            val bounds = onNodeWithText(fact).getUnclippedBoundsInRoot()
            assertTrue(bounds.left > bikeTitle.right, "Факт «$fact» должен стоять в полосе справа от байка: $bounds, $bikeTitle")
            assertTrue(bounds.bottom < target.top, "Факт «$fact» должен стоять над колонкой цели: $bounds, $target")
        }
    }

    @Test
    fun currentChargeIsShownOnTargetScale() = runDesktopComposeUiTest(width = WIDE_CONTENT_WIDTH, height = CONTENT_HEIGHT) {
        showFoundBike(this)

        onNodeWithText("сейчас на стенде 62,5%").assertIsDisplayed()
    }

    @Test
    fun narrowWindowKeepsMapBesideTargetColumn() = runDesktopComposeUiTest(width = NARROW_CONTENT_WIDTH, height = CONTENT_HEIGHT) {
        showFoundBike(this)

        val target = onNodeWithText("Станет после применения").getUnclippedBoundsInRoot()
        val mapHint = onNodeWithText(MAP_HINT).getUnclippedBoundsInRoot()
        assertTrue(mapHint.left > target.right, "И в узком окне карта стоит справа от колонки цели: $mapHint, $target")
        onNodeWithText("Применить сейчас").assertIsDisplayed()
    }

    @Test
    fun qrButtonOpensQrCodeOfFoundBike() = runDesktopComposeUiTest(width = WIDE_CONTENT_WIDTH, height = CONTENT_HEIGHT) {
        showFoundBike(this)

        onNodeWithText("QR-код").performClick()
        waitForIdle()

        assertEquals(listOf<Screens>(Screens.QrCode(bikeQrText = FOUND_BIKE_QR_TEXT)), navigator.pushedScreens)
    }

    private fun showFoundBike(test: DesktopComposeUiTest) {
        val stand = ScreenTestStand()
        val screenModel = QuickSetupScreenModel(
            componentContext = DefaultComponentContext(LifecycleRegistry()),
            standBaseUrl = ScreenTestStand.BASE_URL,
            bikeSetupInteractor = stand.interactors.bikeSetupInteractor,
            templateInteractor = stand.interactors.templateInteractor,
            locationInteractor = stand.interactors.locationInteractor,
            tileSource = ScreenTestStand.Tiles,
            reconnections = emptyFlow(),
            log = SilentLog,
        )
        val screen = QuickSetupScreen(screenModel = screenModel, tileLoader = stand.tileLoader)
        test.setContent {
            CompositionLocalProvider(LocalNavigator provides navigator) {
                QaTheme(isDark = false) { screen.Content() }
            }
        }
        test.onNode(hasSetTextAction() and hasText("Номер байка")).performTextInput(ScreenTestStand.FOUND_BIKE_NUMBER.toString())
        test.waitUntil(timeoutMillis = LOOKUP_TIMEOUT_MS) { screenModel.stateFlow.value.lookup is BikeLookup.Found }
        test.waitForIdle()
    }

    private companion object {
        const val WIDE_CONTENT_WIDTH = 1100
        const val NARROW_CONTENT_WIDTH = 900
        const val CONTENT_HEIGHT = 868
        const val LOOKUP_TIMEOUT_MS = 5_000L
        const val MAP_HINT = "Перетащите пин или кликните по карте"
        const val FOUND_BIKE_QR_TEXT = "http://ure.su/a/S.${ScreenTestStand.FOUND_BIKE_NUMBER}"
    }
}
