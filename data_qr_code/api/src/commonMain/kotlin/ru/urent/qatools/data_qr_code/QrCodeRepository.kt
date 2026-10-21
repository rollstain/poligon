package ru.urent.qatools.data_qr_code

import kotlinx.coroutines.flow.Flow

data class QrHistoryEntry(val content: String, val isShown: Boolean)

interface QrCodeRepository {
    val history: Flow<List<QrHistoryEntry>>
    suspend fun showOnly(content: String)
    suspend fun saveShown(content: String, isShown: Boolean)
    suspend fun delete(content: String)
    suspend fun getNumberPrefix(): String?
    suspend fun saveNumberPrefix(numberPrefix: String)
}
