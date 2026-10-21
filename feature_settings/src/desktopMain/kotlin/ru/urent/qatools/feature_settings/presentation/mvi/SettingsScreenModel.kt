package ru.urent.qatools.feature_settings.presentation.mvi

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.launch
import ru.urent.qatools.base_bike_setup.domain.BikeSetupInteractor
import ru.urent.qatools.base_bike_setup.domain.TemplateInteractor
import ru.urent.qatools.base_stand.StandSessionInteractor
import ru.urent.qatools.core_base_feature.BaseScreenModel
import ru.urent.qatools.core_logger.AppLog
import ru.urent.qatools.core_settings.AppSettingsRepository
import ru.urent.qatools.feature_settings.domain.SettingsInteractor
import ru.urent.qatools.feature_settings.entities.LogArchiveResult

class SettingsScreenModel(
    componentContext: ComponentContext,
    private val interactor: SettingsInteractor,
    private val sessionInteractor: StandSessionInteractor,
    private val appSettingsRepository: AppSettingsRepository,
    private val templateInteractor: TemplateInteractor,
    private val bikeSetupInteractor: BikeSetupInteractor,
    log: AppLog,
) : BaseScreenModel<SettingsState, Nothing, SettingsEvent>(
    componentContext = componentContext,
    initialState = SettingsState(
        appSettings = null,
        email = null,
        currentBaseUrl = null,
        currentHost = null,
        savedStands = emptyList(),
        archiveMessage = null,
        isArchiveSaved = false,
        setupDefaults = null,
        reasons = emptyList(),
        dataDirectoryPath = interactor.dataDirectory.absolutePath,
    ),
    log = log,
) {
    init {
        onFetchData()
    }

    override fun dispatch(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.OnThemeSelected -> screenModelScope { appSettingsRepository.saveTheme(event.theme) }
            is SettingsEvent.OnUiScaleSelected -> screenModelScope { appSettingsRepository.saveUiScalePercent(event.uiScalePercent) }
            SettingsEvent.OnWindowedModeSelected -> screenModelScope { appSettingsRepository.saveBorderlessFullscreen(false) }
            SettingsEvent.OnBorderlessModeSelected -> screenModelScope { appSettingsRepository.saveBorderlessFullscreen(true) }
            is SettingsEvent.OnForgetStandClicked -> screenModelScope { sessionInteractor.forget(event.baseUrl) }
            SettingsEvent.OnLogoutClicked -> screenModelScope { sessionInteractor.logout() }
            SettingsEvent.OnOpenLogsFolderClicked -> interactor.openFolder(interactor.logsDirectory)
            SettingsEvent.OnOpenDataFolderClicked -> interactor.openFolder(interactor.dataDirectory)
            is SettingsEvent.OnLogArchiveFileChosen -> onLogArchiveFileChosen(event)
            is SettingsEvent.OnDefaultTargetChanged -> onDefaultTargetChanged(event)
            is SettingsEvent.OnDefaultAvailabilityChanged -> onDefaultAvailabilityChanged(event)
        }
    }

    override fun onFetchData() {
        onFetchDataScope {
            launch {
                sessionInteractor.session.collect { session ->
                    updateState { it.copy(email = session?.email, currentBaseUrl = session?.baseUrl, currentHost = session?.host) }
                }
            }
            launch {
                appSettingsRepository.settings.collect { appSettings -> updateState { it.copy(appSettings = appSettings) } }
            }
            launch {
                sessionInteractor.savedStands.collect { savedStands -> updateState { it.copy(savedStands = savedStands) } }
            }
            launch {
                templateInteractor.setupDefaults.collect { setupDefaults -> updateState { it.copy(setupDefaults = setupDefaults) } }
            }
            launch {
                val reasons = bikeSetupInteractor.getDisabledReasons()
                updateState { it.copy(reasons = reasons) }
            }
            sessionInteractor.refreshSavedStands()
        }
    }

    private fun onLogArchiveFileChosen(event: SettingsEvent.OnLogArchiveFileChosen) {
        val standHost = state.currentHost ?: NO_STAND_HOST
        screenModelScope {
            val (message, isSaved) = when (val result = interactor.saveLogArchive(target = event.target, standHost = standHost)) {
                is LogArchiveResult.Saved -> "Сохранено: ${result.path}" to true
                is LogArchiveResult.Failed -> "Не удалось сохранить архив: ${result.reason}" to false
            }
            updateState { it.copy(archiveMessage = message, isArchiveSaved = isSaved) }
        }
    }

    private fun onDefaultTargetChanged(event: SettingsEvent.OnDefaultTargetChanged) {
        val defaults = state.setupDefaults ?: return
        screenModelScope { templateInteractor.saveSetupDefaults(defaults.copy(target = event.target)) }
    }

    private fun onDefaultAvailabilityChanged(event: SettingsEvent.OnDefaultAvailabilityChanged) {
        val defaults = state.setupDefaults ?: return
        screenModelScope { templateInteractor.saveSetupDefaults(defaults.copy(availability = event.availability)) }
    }

    private companion object {
        const val NO_STAND_HOST = "не выбран"
    }
}
