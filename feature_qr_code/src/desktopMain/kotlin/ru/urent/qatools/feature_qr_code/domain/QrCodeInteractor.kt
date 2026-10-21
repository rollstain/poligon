package ru.urent.qatools.feature_qr_code.domain

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.urent.qatools.data_qr_code.QrCodeRepository
import ru.urent.qatools.feature_qr_code.entities.QrCodeHistoryItem
import ru.urent.qatools.feature_qr_code.entities.ShownQrCode

class QrCodeInteractor(private val repository: QrCodeRepository) {
    private val writer = QRCodeWriter()

    val history: Flow<List<QrCodeHistoryItem>> = repository.history.map { entries ->
        entries.map { entry -> QrCodeHistoryItem(content = entry.content, isShown = entry.isShown) }
    }

    suspend fun getNumberPrefix(): String = repository.getNumberPrefix() ?: DEFAULT_NUMBER_PREFIX

    fun contentOf(input: String, numberPrefix: String): String? {
        val trimmedInput = input.trim()
        if (trimmedInput.isEmpty()) return null
        val isNumber = trimmedInput.all { it.isDigit() }
        return if (isNumber) "$numberPrefix$trimmedInput" else trimmedInput
    }

    fun encode(content: String): BitMatrix? {
        val hints = if (Charsets.ISO_8859_1.newEncoder().canEncode(content)) emptyMap() else Utf8Hints
        return try {
            writer.encode(content, BarcodeFormat.QR_CODE, UNSCALED_SIZE, UNSCALED_SIZE, hints)
        } catch (exception: WriterException) {
            null
        }
    }

    fun getShownQrCodes(history: List<QrCodeHistoryItem>): List<ShownQrCode> {
        val shownQrCodes = mutableListOf<ShownQrCode>()
        for (item in history) {
            if (!item.isShown) continue
            val matrix = encode(item.content) ?: continue
            shownQrCodes.add(ShownQrCode(content = item.content, matrix = matrix))
        }
        return shownQrCodes
    }

    companion object {
        const val DEFAULT_NUMBER_PREFIX = "http://ure.su/a/S."
        private const val UNSCALED_SIZE = 0
        private val Utf8Hints = mapOf(EncodeHintType.CHARACTER_SET to Charsets.UTF_8.name())
    }
}
