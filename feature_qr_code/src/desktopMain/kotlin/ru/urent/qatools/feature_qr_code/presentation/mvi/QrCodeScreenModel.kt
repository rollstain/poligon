package ru.urent.qatools.feature_qr_code.presentation.mvi

import com.arkivanov.decompose.ComponentContext
import ru.urent.qatools.core_base_feature.BaseScreenModel
import ru.urent.qatools.core_logger.AppLog
import ru.urent.qatools.data_qr_code.QrCodeRepository
import ru.urent.qatools.feature_qr_code.domain.QrCodeInteractor
import ru.urent.qatools.feature_qr_code.entities.QrCodeHistoryItem

class QrCodeScreenModel(
    componentContext: ComponentContext,
    private val initialContent: String?,
    private val interactor: QrCodeInteractor,
    private val repository: QrCodeRepository,
    log: AppLog,
) : BaseScreenModel<QrCodeState, QrCodeSideEffect, QrCodeEvent>(
    componentContext = componentContext,
    initialState = QrCodeState(
        input = "",
        numberPrefix = QrCodeInteractor.DEFAULT_NUMBER_PREFIX,
        pendingContent = null,
        isInputTooLong = false,
        isLoaded = false,
        isBackShown = initialContent != null,
        history = emptyList(),
        shownQrCodes = emptyList(),
    ),
    log = log,
) {
    init {
        onFetchData()
    }

    override fun dispatch(event: QrCodeEvent) {
        when (event) {
            is QrCodeEvent.OnInputChanged -> onInputChanged(event.input)
            is QrCodeEvent.OnNumberPrefixChanged -> onNumberPrefixChanged(event.numberPrefix)
            QrCodeEvent.OnGenerateClicked -> onGenerateClicked()
            is QrCodeEvent.OnHistoryEntryClicked -> screenModelScope { repository.showOnly(event.content) }
            is QrCodeEvent.OnHistoryEntryCheckClicked -> onHistoryEntryCheckClicked(event.content)
            is QrCodeEvent.OnHistoryEntryDeleteClicked -> screenModelScope { repository.delete(event.content) }
            QrCodeEvent.OnBackClicked -> screenModelScope { postSideEffect(QrCodeSideEffect.Back) }
        }
    }

    override fun onFetchData() {
        onFetchDataScope {
            val numberPrefix = interactor.getNumberPrefix()
            val pendingContent = interactor.contentOf(input = state.input, numberPrefix = numberPrefix)
            updateState { it.copy(numberPrefix = numberPrefix, pendingContent = pendingContent) }
            if (initialContent != null) repository.showOnly(initialContent)
            interactor.history.collect { history -> onHistoryLoaded(history) }
        }
    }

    private fun onHistoryLoaded(history: List<QrCodeHistoryItem>) {
        updateState { it.copy(isLoaded = true, history = history, shownQrCodes = interactor.getShownQrCodes(history)) }
    }

    private fun onInputChanged(input: String) {
        val pendingContent = interactor.contentOf(input = input, numberPrefix = state.numberPrefix)
        updateState { it.copy(input = input, pendingContent = pendingContent, isInputTooLong = false) }
    }

    private fun onNumberPrefixChanged(numberPrefix: String) {
        val pendingContent = interactor.contentOf(input = state.input, numberPrefix = numberPrefix)
        updateState { it.copy(numberPrefix = numberPrefix, pendingContent = pendingContent) }
        screenModelScope { repository.saveNumberPrefix(numberPrefix) }
    }

    private fun onGenerateClicked() {
        val content = state.pendingContent ?: return
        if (interactor.encode(content) == null) {
            updateState { it.copy(isInputTooLong = true) }
            return
        }
        screenModelScope { repository.showOnly(content) }
    }

    private fun onHistoryEntryCheckClicked(content: String) {
        val item = state.history.firstOrNull { it.content == content } ?: return
        screenModelScope { repository.saveShown(content = content, isShown = !item.isShown) }
    }
}
