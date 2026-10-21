package ru.urent.qatools.core_settings

import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun saveBorderlessFullscreen(isBorderlessFullscreen: Boolean)
    suspend fun saveUiScalePercent(uiScalePercent: Int)
    suspend fun saveTheme(theme: AppTheme)
    suspend fun saveSidebarCollapsed(isSidebarCollapsed: Boolean)
    suspend fun saveWindowMaximized()
    suspend fun saveWindowBounds(bounds: WindowBounds)
}
