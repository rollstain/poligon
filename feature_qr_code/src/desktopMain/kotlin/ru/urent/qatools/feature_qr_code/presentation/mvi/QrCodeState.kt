package ru.urent.qatools.feature_qr_code.presentation.mvi

import ru.urent.qatools.feature_qr_code.entities.QrCodeHistoryItem
import ru.urent.qatools.feature_qr_code.entities.ShownQrCode

data class QrCodeState(
    val input: String,
    val numberPrefix: String,
    val pendingContent: String?,
    val isInputTooLong: Boolean,
    val isLoaded: Boolean,
    val isBackShown: Boolean,
    val history: List<QrCodeHistoryItem>,
    val shownQrCodes: List<ShownQrCode>,
)
