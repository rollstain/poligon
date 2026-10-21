package ru.urent.qatools.feature_qr_code.presentation.mvi

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import ru.urent.qatools.core_database.DatabaseDriverProvider
import ru.urent.qatools.data_qr_code.QrCodeRepository
import ru.urent.qatools.data_qr_code.impl.DataQrCodeModule
import ru.urent.qatools.feature_qr_code.domain.QrCodeInteractor
import ru.urent.qatools.testing_stand.InMemoryDatabaseDriverProvider
import ru.urent.qatools.testing_stand.SilentLog

class QrCodeScreenModelTest {
    private val repository = createRepository()

    @Test
    fun numberGoesAfterBikeLinkTemplate() = runBlocking<Unit> {
        val screenModel = screenModel(initialContent = null)

        screenModel.dispatch(QrCodeEvent.OnInputChanged(BIKE_NUMBER))
        screenModel.dispatch(QrCodeEvent.OnGenerateClicked)

        assertEquals(listOf(BIKE_URL), shownContents(screenModel))
        assertEquals(listOf(BIKE_URL), repository.history.first().map { it.content })
    }

    @Test
    fun linkIsTakenAsIs() = runBlocking<Unit> {
        val screenModel = screenModel(initialContent = null)

        screenModel.dispatch(QrCodeEvent.OnInputChanged(" $OTHER_LINK "))
        screenModel.dispatch(QrCodeEvent.OnGenerateClicked)

        assertEquals(listOf(OTHER_LINK), shownContents(screenModel))
    }

    @Test
    fun changedTemplateIsUsedForNumbersAndRemembered() = runBlocking<Unit> {
        val screenModel = screenModel(initialContent = null)
        withTimeout(WAIT_TIMEOUT_MS) { screenModel.stateFlow.first { it.isLoaded } }

        screenModel.dispatch(QrCodeEvent.OnNumberPrefixChanged(OTHER_PREFIX))
        screenModel.dispatch(QrCodeEvent.OnInputChanged(BIKE_NUMBER))
        screenModel.dispatch(QrCodeEvent.OnGenerateClicked)

        assertEquals(listOf("$OTHER_PREFIX$BIKE_NUMBER"), shownContents(screenModel))
        val reopened = screenModel(initialContent = null)
        withTimeout(WAIT_TIMEOUT_MS) { reopened.stateFlow.first { it.numberPrefix == OTHER_PREFIX } }
    }

    @Test
    fun pendingContentShowsWhatGoesIntoQrCode() {
        val screenModel = screenModel(initialContent = null)

        screenModel.dispatch(QrCodeEvent.OnInputChanged(" $BIKE_NUMBER "))
        assertEquals(BIKE_URL, screenModel.stateFlow.value.pendingContent)

        screenModel.dispatch(QrCodeEvent.OnInputChanged(" "))
        assertEquals(null, screenModel.stateFlow.value.pendingContent)
    }

    @Test
    fun bikeQrCodeIsShownOnOpen() = runBlocking<Unit> {
        val screenModel = screenModel(initialContent = BIKE_URL)

        assertEquals(listOf(BIKE_URL), shownContents(screenModel))
    }

    @Test
    fun checkedHistoryEntriesAreShownTogetherInHistoryOrder() = runBlocking<Unit> {
        repository.showOnly(OTHER_LINK)
        repository.showOnly(BIKE_URL)
        val screenModel = screenModel(initialContent = null)
        withTimeout(WAIT_TIMEOUT_MS) { screenModel.stateFlow.first { it.history.size == HISTORY_SIZE } }

        screenModel.dispatch(QrCodeEvent.OnHistoryEntryCheckClicked(OTHER_LINK))

        val state = withTimeout(WAIT_TIMEOUT_MS) { screenModel.stateFlow.first { it.shownQrCodes.size == HISTORY_SIZE } }
        assertEquals(listOf(BIKE_URL, OTHER_LINK), state.shownQrCodes.map { it.content })
    }

    @Test
    fun textBeyondQrCapacityIsReportedAndNotSaved() = runBlocking<Unit> {
        val screenModel = screenModel(initialContent = null)

        screenModel.dispatch(QrCodeEvent.OnInputChanged(TOO_LONG_TEXT_CHAR.repeat(TOO_LONG_TEXT_LENGTH)))
        screenModel.dispatch(QrCodeEvent.OnGenerateClicked)

        assertTrue(screenModel.stateFlow.value.isInputTooLong)
        assertEquals(emptyList(), repository.history.first())
    }

    private fun screenModel(initialContent: String?) = QrCodeScreenModel(
        componentContext = DefaultComponentContext(LifecycleRegistry()),
        initialContent = initialContent,
        interactor = QrCodeInteractor(repository = repository),
        repository = repository,
        log = SilentLog,
    )

    private suspend fun shownContents(screenModel: QrCodeScreenModel): List<String> {
        val state = withTimeout(WAIT_TIMEOUT_MS) { screenModel.stateFlow.first { it.shownQrCodes.isNotEmpty() } }
        return state.shownQrCodes.map { it.content }
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
        const val BIKE_NUMBER = "4072"
        const val BIKE_URL = "http://ure.su/a/S.4072"
        const val OTHER_PREFIX = "http://ure.su/a/E."
        const val OTHER_LINK = "https://example.com/qr?id=7"
        const val HISTORY_SIZE = 2
        const val TOO_LONG_TEXT_CHAR = "a"
        const val TOO_LONG_TEXT_LENGTH = 3_000
        const val WAIT_TIMEOUT_MS = 5_000L
    }
}
