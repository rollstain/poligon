package ru.urent.qatools.feature_templates.presentation.editor.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ru.urent.qatools.base_bike_setup.entities.ApplyStatus
import ru.urent.qatools.base_bike_setup.entities.AvailabilityTarget
import ru.urent.qatools.base_bike_setup.entities.BikeApplyResult
import ru.urent.qatools.base_bike_setup.entities.BikeSetup
import ru.urent.qatools.base_bike_setup.entities.ChargeTarget
import ru.urent.qatools.core_entities.ChargeState
import ru.urent.qatools.core_entities.GeoPoint
import ru.urent.qatools.feature_templates.presentation.editor.mvi.TemplateScreenModel
import ru.urent.qatools.navigation.LocalNavigator
import ru.urent.qatools.navigation.Screens
import ru.urent.qatools.testing_stand.RecordingNavigator
import ru.urent.qatools.testing_stand.ScreenTestStand
import ru.urent.qatools.testing_stand.SilentLog
import ru.urent.qatools.uikit.theme.QaTheme

@OptIn(ExperimentalTestApi::class)
class TemplateScreenTest {
    private val navigator = RecordingNavigator()

    @Test
    fun narrowWindowKeepsBikeListBesideMapWithHeaderActions() = runDesktopComposeUiTest(width = NARROW_CONTENT_WIDTH, height = CONTENT_HEIGHT) {
        val stand = ScreenTestStand()
        showTemplate(this, stand, createTemplate(stand))

        onNodeWithText("Применить все (2)").assertIsDisplayed()
        onNodeWithText("Обновить статусы").assertIsDisplayed()
        onNodeWithContentDescription("Ещё действия с шаблоном").assertIsDisplayed()
        val bikeRow = onNodeWithContentDescription(FOUND_BIKE_ROW).getUnclippedBoundsInRoot()
        val mapHint = onNodeWithText(MAP_HINT).getUnclippedBoundsInRoot()
        assertTrue(mapHint.left > bikeRow.right, "Карта должна стоять справа от списка байков: $mapHint, $bikeRow")
    }

    @Test
    fun fullScreenGivesMapMoreThanHalfOfWidth() = runDesktopComposeUiTest(width = FULL_SCREEN_CONTENT_WIDTH, height = FULL_SCREEN_CONTENT_HEIGHT) {
        val stand = ScreenTestStand()
        showTemplate(this, stand, createTemplate(stand))

        val mapHint = onNodeWithText(MAP_HINT).getUnclippedBoundsInRoot()
        assertTrue(mapHint.left.value < FULL_SCREEN_CONTENT_WIDTH / 2, "Карта должна занимать больше половины ширины: $mapHint")
    }

    @Test
    fun renamingInHeaderSavesTemplateName() = runDesktopComposeUiTest(width = COLLAPSED_SIDEBAR_CONTENT_WIDTH, height = CONTENT_HEIGHT) {
        val stand = ScreenTestStand()
        val templateId = createTemplate(stand)
        showTemplate(this, stand, templateId)

        onNodeWithContentDescription("Название шаблона").performTextReplacement(NEW_NAME)

        waitUntil(timeoutMillis = WAIT_TIMEOUT_MS) { runBlocking { stand.interactors.templateInteractor.getTemplate(templateId).first()?.name } == NEW_NAME }
    }

    @Test
    fun headerShowsWhenTemplateWasApplied() = runDesktopComposeUiTest(width = COLLAPSED_SIDEBAR_CONTENT_WIDTH, height = CONTENT_HEIGHT) {
        val stand = ScreenTestStand()
        val templateId = createTemplate(stand)
        val bike = checkNotNull(stand.interactors.templateInteractor.getTemplate(templateId).first()).bikes.first()
        val applied = ApplyStatus.Done(percent = NORMAL_PERCENT, state = ChargeState.NORMAL, isTargetReached = true, availability = AvailabilityTarget.Keep)
        val result = BikeApplyResult(appliedSetup = bike.setup, status = applied, appliedAt = Clock.System.now())
        stand.interactors.templateInteractor.saveApplyResult(templateBikeId = bike.id, result = result)
        showTemplate(this, stand, templateId)

        onNodeWithText("Применён", substring = true).assertIsDisplayed()
    }

    @Test
    fun clickingBikeOpensItsCardAndMovesMapToIt() = runDesktopComposeUiTest(width = NARROW_CONTENT_WIDTH, height = CONTENT_HEIGHT) {
        val stand = ScreenTestStand()
        val screenModel = showTemplate(this, stand, createTemplate(stand))
        val clickedBike = screenModel.stateFlow.value.rows.first { it.bike.setup.simulatorId == ScreenTestStand.FOUND_SIMULATOR_ID }.bike

        onNodeWithContentDescription(FOUND_BIKE_ROW).performClick()

        waitUntil(timeoutMillis = WAIT_TIMEOUT_MS) { screenModel.stateFlow.value.selectedBikeId == clickedBike.id }
        assertEquals(clickedBike.setup.location, screenModel.stateFlow.value.focus?.point)
        onNodeWithText(BACK_TO_LIST).assertIsDisplayed()
        onNodeWithText("Заряд после применения").assertIsDisplayed()
        onNodeWithContentDescription(FOUND_BIKE_ROW).assertDoesNotExist()
    }

    @Test
    fun backFromBikeCardReturnsToList() = runDesktopComposeUiTest(width = NARROW_CONTENT_WIDTH, height = CONTENT_HEIGHT) {
        val stand = ScreenTestStand()
        val screenModel = showTemplate(this, stand, createTemplate(stand))
        onNodeWithContentDescription(FOUND_BIKE_ROW).performClick()
        waitForIdle()

        onNodeWithText(BACK_TO_LIST).performClick()
        waitForIdle()

        assertEquals(null, screenModel.stateFlow.value.selectedBikeId)
        onNodeWithContentDescription(FOUND_BIKE_ROW).assertIsDisplayed()
        onNodeWithContentDescription(MISSING_BIKE_ROW).assertIsDisplayed()
    }

    @Test
    fun qrButtonInCardOpensQrCodeOfFoundBike() = runDesktopComposeUiTest(width = NARROW_CONTENT_WIDTH, height = CONTENT_HEIGHT) {
        val stand = ScreenTestStand()
        showTemplate(this, stand, createTemplate(stand))
        onNodeWithContentDescription(FOUND_BIKE_ROW).performClick()
        waitUntil(timeoutMillis = WAIT_TIMEOUT_MS) {
            onAllNodesWithContentDescription("QR-код S.${ScreenTestStand.FOUND_SIMULATOR_ID}").fetchSemanticsNodes().isNotEmpty()
        }

        onNodeWithContentDescription("QR-код S.${ScreenTestStand.FOUND_SIMULATOR_ID}").performClick()
        waitForIdle()

        assertEquals(listOf<Screens>(Screens.QrCode(bikeQrText = FOUND_BIKE_QR_TEXT)), navigator.pushedScreens)
    }

    @Test
    fun cardOfMissingBikeHasNoQrCode() = runDesktopComposeUiTest(width = NARROW_CONTENT_WIDTH, height = CONTENT_HEIGHT) {
        val stand = ScreenTestStand()
        showTemplate(this, stand, createTemplate(stand))

        onNodeWithContentDescription(MISSING_BIKE_ROW).performClick()
        waitForIdle()

        onNodeWithText(BACK_TO_LIST).assertIsDisplayed()
        onNodeWithContentDescription("QR-код S.${ScreenTestStand.MISSING_SIMULATOR_ID}").assertDoesNotExist()
    }

    @Test
    fun deletingTemplateStartsFromMoreMenu() = runDesktopComposeUiTest(width = NARROW_CONTENT_WIDTH, height = CONTENT_HEIGHT) {
        val stand = ScreenTestStand()
        val screenModel = showTemplate(this, stand, createTemplate(stand))

        onNodeWithContentDescription("Ещё действия с шаблоном").performClick()
        onNodeWithText("Удалить шаблон").performClick()
        waitForIdle()

        assertTrue(screenModel.stateFlow.value.isDeleteConfirmVisible)
    }

    private suspend fun createTemplate(stand: ScreenTestStand): Long {
        val templateId = stand.interactors.templateInteractor.create(standBaseUrl = ScreenTestStand.BASE_URL, name = "Тестовый шаблон")
        for (simulatorId in listOf(ScreenTestStand.FOUND_SIMULATOR_ID, ScreenTestStand.MISSING_SIMULATOR_ID)) {
            val setup = BikeSetup(
                simulatorId = simulatorId,
                location = BikeLocation,
                target = ChargeTarget.State(ChargeState.NORMAL),
                availability = AvailabilityTarget.Keep,
            )
            stand.interactors.templateInteractor.addBike(templateId = templateId, setup = setup)
        }
        return templateId
    }

    private fun showTemplate(test: DesktopComposeUiTest, stand: ScreenTestStand, templateId: Long): TemplateScreenModel {
        val screenModel = TemplateScreenModel(
            componentContext = DefaultComponentContext(LifecycleRegistry()),
            standBaseUrl = ScreenTestStand.BASE_URL,
            templateId = templateId,
            templateInteractor = stand.interactors.templateInteractor,
            locationInteractor = stand.interactors.locationInteractor,
            bikeSetupInteractor = stand.interactors.bikeSetupInteractor,
            runner = stand.interactors.runner,
            tileSource = ScreenTestStand.Tiles,
            reconnections = emptyFlow(),
            log = SilentLog,
        )
        val screen = TemplateScreen(screenModel = screenModel, tileLoader = stand.tileLoader)
        test.setContent {
            CompositionLocalProvider(LocalNavigator provides navigator) {
                QaTheme(isDark = false) { screen.Content() }
            }
        }
        test.waitUntil(timeoutMillis = WAIT_TIMEOUT_MS) { screenModel.stateFlow.value.rows.size == TEMPLATE_BIKE_COUNT }
        test.waitForIdle()
        return screenModel
    }

    private companion object {
        const val COLLAPSED_SIDEBAR_CONTENT_WIDTH = 1296
        const val NARROW_CONTENT_WIDTH = 1100
        const val CONTENT_HEIGHT = 868
        const val FULL_SCREEN_CONTENT_WIDTH = 2496
        const val FULL_SCREEN_CONTENT_HEIGHT = 1376
        const val WAIT_TIMEOUT_MS = 5_000L
        const val TEMPLATE_BIKE_COUNT = 2
        const val NORMAL_PERCENT = 62.5
        const val NEW_NAME = "Смоук QA"
        const val FOUND_BIKE_QR_TEXT = "http://ure.su/a/S.${ScreenTestStand.FOUND_SIMULATOR_ID}"
        const val MAP_HINT = "Нажмите на байк в списке или его пин, чтобы изменить заряд, доступ или место"
        const val BACK_TO_LIST = "← Все байки (2)"
        const val FOUND_BIKE_ROW = "Байк S.${ScreenTestStand.FOUND_SIMULATOR_ID}"
        const val MISSING_BIKE_ROW = "Байк S.${ScreenTestStand.MISSING_SIMULATOR_ID}"
        val BikeLocation = GeoPoint(latitude = 43.424838, longitude = 39.928128)
    }
}
