package ru.urent.qatools.data_qr_code.impl

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ru.urent.qatools.data_qr_code.QrHistoryEntry
import ru.urent.qatools.data_qr_code.db.QrCodeDatabase

class QrCodeRepositoryTest {
    private val repository = QrCodeRepositoryImpl(
        database = QrCodeDatabase(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, schema = QrCodeDatabase.Schema)),
    )

    @Test
    fun shownAgainTextKeepsItsPlaceInHistory() = runBlocking<Unit> {
        repository.showOnly(FIRST_BIKE_URL)
        repository.showOnly(SECOND_BIKE_URL)

        repository.showOnly(FIRST_BIKE_URL)

        assertEquals(
            listOf(QrHistoryEntry(SECOND_BIKE_URL, isShown = false), QrHistoryEntry(FIRST_BIKE_URL, isShown = true)),
            repository.history.first(),
        )
    }

    @Test
    fun checkedEntriesAreShownTogether() = runBlocking<Unit> {
        repository.showOnly(FIRST_BIKE_URL)
        repository.showOnly(SECOND_BIKE_URL)

        repository.saveShown(FIRST_BIKE_URL, isShown = true)

        assertEquals(
            listOf(QrHistoryEntry(SECOND_BIKE_URL, isShown = true), QrHistoryEntry(FIRST_BIKE_URL, isShown = true)),
            repository.history.first(),
        )
    }

    @Test
    fun historyKeepsOnlyLatestTwenty() = runBlocking<Unit> {
        for (number in 1..SAVED_COUNT) repository.showOnly("$BIKE_URL_PREFIX$number")

        val history = repository.history.first()

        assertEquals(KEPT_COUNT, history.size)
        assertEquals("$BIKE_URL_PREFIX$SAVED_COUNT", history.first().content)
        assertEquals("$BIKE_URL_PREFIX${SAVED_COUNT - KEPT_COUNT + 1}", history.last().content)
    }

    @Test
    fun deletedTextLeavesHistory() = runBlocking<Unit> {
        repository.showOnly(FIRST_BIKE_URL)
        repository.showOnly(SECOND_BIKE_URL)

        repository.delete(FIRST_BIKE_URL)

        assertEquals(listOf(QrHistoryEntry(SECOND_BIKE_URL, isShown = true)), repository.history.first())
    }

    @Test
    fun numberPrefixIsMissingUntilSavedAndKeepsChanges() = runBlocking<Unit> {
        assertNull(repository.getNumberPrefix())

        repository.saveNumberPrefix(OTHER_PREFIX)

        assertEquals(OTHER_PREFIX, repository.getNumberPrefix())
    }

    @Test
    fun historyOfFirstVersionSurvivesUpgrade() = runBlocking<Unit> {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, FIRST_VERSION_HISTORY_TABLE, 0)
        driver.execute(null, "INSERT INTO qr_code_history(content, used_sequence) VALUES ('$FIRST_BIKE_URL', 1)", 0)

        QrCodeDatabase.Schema.migrate(driver, FIRST_SCHEMA_VERSION, QrCodeDatabase.Schema.version)
        val upgraded = QrCodeRepositoryImpl(database = QrCodeDatabase(driver))

        assertEquals(listOf(QrHistoryEntry(FIRST_BIKE_URL, isShown = false)), upgraded.history.first())
        assertNull(upgraded.getNumberPrefix())
    }

    private companion object {
        const val BIKE_URL_PREFIX = "http://ure.su/a/S."
        const val OTHER_PREFIX = "http://ure.su/a/E."
        const val FIRST_BIKE_URL = "http://ure.su/a/S.4071"
        const val SECOND_BIKE_URL = "http://ure.su/a/S.4072"
        const val SAVED_COUNT = 25
        const val KEPT_COUNT = 20
        const val FIRST_SCHEMA_VERSION = 1L
        const val FIRST_VERSION_HISTORY_TABLE = """
            CREATE TABLE qr_code_history (
                content TEXT NOT NULL PRIMARY KEY,
                used_sequence INTEGER NOT NULL
            )
        """
    }
}
