package ru.urent.qatools.feature_qr_code.entities

import com.google.zxing.common.BitMatrix

data class QrCodeHistoryItem(val content: String, val isShown: Boolean)

data class ShownQrCode(val content: String, val matrix: BitMatrix)
