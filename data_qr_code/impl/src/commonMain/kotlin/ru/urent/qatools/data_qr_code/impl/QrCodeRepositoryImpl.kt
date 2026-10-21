package ru.urent.qatools.data_qr_code.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import ru.urent.qatools.data_qr_code.QrCodeRepository
import ru.urent.qatools.data_qr_code.QrHistoryEntry
import ru.urent.qatools.data_qr_code.db.QrCodeDatabase

internal class QrCodeRepositoryImpl(database: QrCodeDatabase) : QrCodeRepository {
    private val historyQueries = database.qrCodeHistoryQueries
    private val settingsQueries = database.qrCodeSettingsQueries

    override val history: Flow<List<QrHistoryEntry>> = historyQueries
        .selectHistory { content, isShown -> QrHistoryEntry(content = content, isShown = isShown == TRUE_VALUE) }
        .asFlow()
        .mapToList(Dispatchers.IO)

    override suspend fun showOnly(content: String): Unit = withContext(Dispatchers.IO) {
        historyQueries.transaction {
            historyQueries.add(content)
            historyQueries.deleteBeyondRecent(HISTORY_SIZE)
            historyQueries.hideAll()
            historyQueries.saveShown(is_shown = TRUE_VALUE, content = content)
        }
    }

    override suspend fun saveShown(content: String, isShown: Boolean): Unit = withContext(Dispatchers.IO) {
        historyQueries.saveShown(is_shown = if (isShown) TRUE_VALUE else FALSE_VALUE, content = content)
    }

    override suspend fun delete(content: String): Unit = withContext(Dispatchers.IO) {
        historyQueries.delete(content)
    }

    override suspend fun getNumberPrefix(): String? = withContext(Dispatchers.IO) {
        settingsQueries.selectNumberPrefix().executeAsOneOrNull()
    }

    override suspend fun saveNumberPrefix(numberPrefix: String): Unit = withContext(Dispatchers.IO) {
        settingsQueries.saveNumberPrefix(numberPrefix)
    }

    private companion object {
        const val HISTORY_SIZE = 20L
        const val TRUE_VALUE = 1L
        const val FALSE_VALUE = 0L
    }
}
