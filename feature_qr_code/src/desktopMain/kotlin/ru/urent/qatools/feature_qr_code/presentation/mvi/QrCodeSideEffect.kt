package ru.urent.qatools.feature_qr_code.presentation.mvi

sealed interface QrCodeSideEffect {
    data object Back : QrCodeSideEffect
}
