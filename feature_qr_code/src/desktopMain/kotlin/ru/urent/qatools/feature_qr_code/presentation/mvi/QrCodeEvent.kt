package ru.urent.qatools.feature_qr_code.presentation.mvi

sealed interface QrCodeEvent {
    data class OnInputChanged(val input: String) : QrCodeEvent

    data class OnNumberPrefixChanged(val numberPrefix: String) : QrCodeEvent

    data object OnGenerateClicked : QrCodeEvent

    data class OnHistoryEntryClicked(val content: String) : QrCodeEvent

    data class OnHistoryEntryCheckClicked(val content: String) : QrCodeEvent

    data class OnHistoryEntryDeleteClicked(val content: String) : QrCodeEvent

    data object OnBackClicked : QrCodeEvent
}
