package ru.urent.qatools.composeapp.update.mvi

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.launch
import ru.urent.qatools.base_update.domain.UpdateInteractor
import ru.urent.qatools.core_base_feature.BaseScreenModel
import ru.urent.qatools.core_logger.AppLog

class UpdateBannerScreenModel(
    componentContext: ComponentContext,
    private val updateInteractor: UpdateInteractor,
    log: AppLog,
) : BaseScreenModel<UpdateBannerState, UpdateBannerSideEffect, UpdateBannerEvent>(
    componentContext = componentContext,
    initialState = UpdateBannerState(status = updateInteractor.status.value, isHidden = false),
    log = log,
) {
    init {
        onFetchData()
    }

    override fun dispatch(event: UpdateBannerEvent) {
        when (event) {
            UpdateBannerEvent.OnDownloadClicked -> onDownloadClicked()
            UpdateBannerEvent.OnInstallClicked -> onInstallClicked()
            UpdateBannerEvent.OnLaterClicked -> onLaterClicked()
        }
    }

    override fun onFetchData() {
        onFetchDataScope {
            launch {
                updateInteractor.status.collect { status -> updateState { it.copy(status = status, isHidden = false) } }
            }
            updateInteractor.deleteDownloadedInstallers()
            updateInteractor.check()
        }
    }

    private fun onDownloadClicked() {
        updateInteractor.download()
    }

    private fun onInstallClicked() {
        screenModelScope {
            if (updateInteractor.install()) postSideEffect(UpdateBannerSideEffect.CloseApp)
        }
    }

    private fun onLaterClicked() {
        updateState { it.copy(isHidden = true) }
    }
}
