package ru.urent.qatools.feature_qr_code.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import ru.urent.qatools.core_database.DatabaseDriverProvider
import ru.urent.qatools.data_qr_code.QrCodeRepository
import ru.urent.qatools.data_qr_code.impl.DataQrCodeModule
import ru.urent.qatools.feature_qr_code.domain.QrCodeInteractor
import ru.urent.qatools.feature_qr_code.presentation.mvi.QrCodeScreenModel
import ru.urent.qatools.navigation.LocalNavigator
import ru.urent.qatools.testing_stand.InMemoryDatabaseDriverProvider
import ru.urent.qatools.testing_stand.RecordingNavigator
import ru.urent.qatools.testing_stand.SilentLog
import ru.urent.qatools.uikit.theme.QaDimens
import ru.urent.qatools.uikit.theme.QaTheme

@OptIn(ExperimentalTestApi::class)
class QrCodeScreenTest {
    private val repository = createRepository()
    private val navigator = RecordingNavigator()

    @Test
    fun generatedNumberReadsBackAsBikeLink() = runDesktopComposeUiTest(width = CONTENT_WIDTH, height = CONTENT_HEIGHT) {
        showScreen(this, initialContent = null)

        onNode(hasSetTextAction() and hasText(INPUT_LABEL)).performTextInput(FIRST_BIKE_NUMBER)
        onNodeWithText("Показать").performClick()

        assertEquals(FIRST_BIKE_URL, readQrCode(this, FIRST_BIKE_URL))
    }

    @Test
    fun enterGeneratesQrCodeWithCyrillicText() = runDesktopComposeUiTest(width = CONTENT_WIDTH, height = CONTENT_HEIGHT) {
        showScreen(this, initialContent = null)

        val input = onNode(hasSetTextAction() and hasText(INPUT_LABEL))
        input.performTextInput(CYRILLIC_TEXT)
        input.performKeyInput { pressKey(Key.Enter) }

        assertEquals(CYRILLIC_TEXT, readQrCode(this, CYRILLIC_TEXT))
    }

    @Test
    fun checkedHistoryEntriesStandSideBySide() = runDesktopComposeUiTest(width = CONTENT_WIDTH, height = CONTENT_HEIGHT) {
        showScreen(this, initialContent = null)
        val input = onNode(hasSetTextAction() and hasText(INPUT_LABEL))
        input.performTextInput(FIRST_BIKE_NUMBER)
        onNodeWithText("Показать").performClick()
        waitForQrCode(this, FIRST_BIKE_URL)
        input.performTextReplacement(SECOND_BIKE_NUMBER)
        onNodeWithText("Показать").performClick()
        waitForQrCode(this, SECOND_BIKE_URL)

        onNodeWithContentDescription("Показывать: $FIRST_BIKE_URL").performClick()

        assertEquals(FIRST_BIKE_URL, readQrCode(this, FIRST_BIKE_URL))
        assertEquals(SECOND_BIKE_URL, readQrCode(this, SECOND_BIKE_URL))
        val newer = onNodeWithContentDescription("QR-код $SECOND_BIKE_URL").getUnclippedBoundsInRoot()
        val older = onNodeWithContentDescription("QR-код $FIRST_BIKE_URL").getUnclippedBoundsInRoot()
        assertTrue(older.left >= newer.right, "QR должны стоять рядом в порядке истории: $newer, $older")
    }

    @Test
    fun tooLongTextKeepsLayoutInPlace() = runDesktopComposeUiTest(width = CONTENT_WIDTH, height = CONTENT_HEIGHT) {
        showScreen(this, initialContent = null)
        val buttonBefore = onNodeWithText("Показать").getUnclippedBoundsInRoot()
        val historyBefore = onNodeWithText("История").getUnclippedBoundsInRoot()

        onNode(hasSetTextAction() and hasText(INPUT_LABEL)).performTextInput(TOO_LONG_TEXT_CHAR.repeat(TOO_LONG_TEXT_LENGTH))
        onNodeWithText("Показать").performClick()

        onNodeWithText("Слишком длинная строка — QR-код её не вместит").assertIsDisplayed()
        assertEquals(buttonBefore, onNodeWithText("Показать").getUnclippedBoundsInRoot())
        assertEquals(historyBefore, onNodeWithText("История").getUnclippedBoundsInRoot())
    }

    @Test
    fun headerStaysInPlaceWhenOpenedForBike() = runDesktopComposeUiTest(width = CONTENT_WIDTH * 2, height = CONTENT_HEIGHT) {
        val forBike = screenModel(initialContent = FIRST_BIKE_URL)
        val fromSidebar = screenModel(initialContent = null)
        val forBikeScreen = QrCodeScreen(screenModel = forBike)
        val fromSidebarScreen = QrCodeScreen(screenModel = fromSidebar)
        setContent {
            CompositionLocalProvider(LocalNavigator provides navigator) {
                QaTheme(isDark = false) {
                    Row {
                        Box(modifier = Modifier.weight(1f)) { forBikeScreen.Content() }
                        Box(modifier = Modifier.weight(1f)) { fromSidebarScreen.Content() }
                    }
                }
            }
        }
        waitUntil(timeoutMillis = WAIT_TIMEOUT_MS) { forBike.stateFlow.value.isLoaded && fromSidebar.stateFlow.value.isLoaded }

        for (text in listOf(TITLE, SUBTITLE, "История")) {
            val forBikeBounds = onAllNodesWithText(text)[0].getUnclippedBoundsInRoot()
            val fromSidebarBounds = onAllNodesWithText(text)[1].getUnclippedBoundsInRoot()
            assertEquals(forBikeBounds.top, fromSidebarBounds.top, "«$text» не должен скакать по вертикали")
            assertEquals(forBikeBounds.left, fromSidebarBounds.left - CONTENT_WIDTH.dp, "«$text» не должен скакать по горизонтали")
        }
        assertEquals(QaDimens.screenPaddingVertical, onAllNodesWithText(TITLE)[1].getUnclippedBoundsInRoot().top)
    }

    @Test
    fun qrCodeOpenedForBikeReturnsBack() = runDesktopComposeUiTest(width = CONTENT_WIDTH, height = CONTENT_HEIGHT) {
        showScreen(this, initialContent = FIRST_BIKE_URL)

        assertEquals(FIRST_BIKE_URL, readQrCode(this, FIRST_BIKE_URL))
        onNodeWithText("← Вернуться к байку").performClick()
        waitForIdle()

        assertEquals(1, navigator.popCount)
    }

    private fun screenModel(initialContent: String?) = QrCodeScreenModel(
        componentContext = DefaultComponentContext(LifecycleRegistry()),
        initialContent = initialContent,
        interactor = QrCodeInteractor(repository = repository),
        repository = repository,
        log = SilentLog,
    )

    private fun showScreen(test: DesktopComposeUiTest, initialContent: String?) {
        val screenModel = screenModel(initialContent = initialContent)
        val screen = QrCodeScreen(screenModel = screenModel)
        test.setContent {
            CompositionLocalProvider(LocalNavigator provides navigator) {
                QaTheme(isDark = true) { screen.Content() }
            }
        }
        test.waitUntil(timeoutMillis = WAIT_TIMEOUT_MS) { screenModel.stateFlow.value.isLoaded }
    }

    private fun waitForQrCode(test: DesktopComposeUiTest, content: String) {
        test.waitUntil(timeoutMillis = WAIT_TIMEOUT_MS) {
            test.onAllNodesWithContentDescription("QR-код $content").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun readQrCode(test: DesktopComposeUiTest, content: String): String {
        waitForQrCode(test, content)
        test.waitForIdle()
        val image = test.onNodeWithContentDescription("QR-код $content").captureToImage().toAwtImage()
        val pixels = IntArray(image.width * image.height)
        image.getRGB(0, 0, image.width, image.height, pixels, 0, image.width)
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(image.width, image.height, pixels)))
        return QRCodeReader().decode(bitmap).text
    }

    private fun createRepository(): QrCodeRepository {
        val koin = koinApplication {
            modules(
                DataQrCodeModule().module,
                module { single<DatabaseDriverProvider> { InMemoryDatabaseDriverProvider } },
            )
        }.koin
        return koin.get()
    }

    private companion object {
        const val CONTENT_WIDTH = 1100
        const val CONTENT_HEIGHT = 800
        const val WAIT_TIMEOUT_MS = 5_000L
        const val INPUT_LABEL = "Номер ТС или ссылка"
        const val FIRST_BIKE_NUMBER = "4071"
        const val SECOND_BIKE_NUMBER = "4072"
        const val FIRST_BIKE_URL = "http://ure.su/a/S.4071"
        const val SECOND_BIKE_URL = "http://ure.su/a/S.4072"
        const val CYRILLIC_TEXT = "Байк S.4072 — проверка"
        const val TITLE = "QR-код"
        const val SUBTITLE = "Телефон считывает код прямо с экрана"
        const val TOO_LONG_TEXT_CHAR = "a"
        const val TOO_LONG_TEXT_LENGTH = 3_000
    }
}
