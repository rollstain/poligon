package ru.urent.qatools.core_settings.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.urent.qatools.core_database.CoreDatabase
import ru.urent.qatools.core_settings.AppSettings
import ru.urent.qatools.core_settings.AppSettingsRepository
import ru.urent.qatools.core_settings.AppTheme
import ru.urent.qatools.core_settings.WindowBounds

internal class AppSettingsRepositoryImpl(database: CoreDatabase) : AppSettingsRepository {
    private val queries = database.appSettingsQueries

    override val settings: Flow<AppSettings> = queries.selectSettings().asFlow().mapToOneOrNull(Dispatchers.IO).map { row ->
        val themeName = row?.theme
        val theme = if (themeName == null) null else runCatching { AppTheme.valueOf(themeName) }.getOrNull()
        val windowX = row?.window_x
        val windowY = row?.window_y
        val windowWidth = row?.window_width
        val windowHeight = row?.window_height
        val windowBounds = if (windowX == null || windowY == null || windowWidth == null || windowHeight == null) {
            null
        } else {
            WindowBounds(x = windowX, y = windowY, width = windowWidth, height = windowHeight)
        }
        AppSettings(
            isBorderlessFullscreen = row?.is_borderless_fullscreen == TRUE_VALUE,
            uiScalePercent = row?.ui_scale_percent?.toInt() ?: DEFAULT_UI_SCALE_PERCENT,
            theme = theme ?: AppTheme.LIGHT,
            isSidebarCollapsed = row?.is_sidebar_collapsed == TRUE_VALUE,
            isWindowMaximized = row?.is_window_maximized != FALSE_VALUE,
            windowBounds = windowBounds,
        )
    }

    override suspend fun saveBorderlessFullscreen(isBorderlessFullscreen: Boolean) = withContext(Dispatchers.IO) {
        queries.transaction {
            queries.ensureSettingsRow()
            queries.saveBorderlessFullscreen(if (isBorderlessFullscreen) TRUE_VALUE else FALSE_VALUE)
        }
    }

    override suspend fun saveUiScalePercent(uiScalePercent: Int) = withContext(Dispatchers.IO) {
        queries.transaction {
            queries.ensureSettingsRow()
            queries.saveUiScalePercent(uiScalePercent.toLong())
        }
    }

    override suspend fun saveTheme(theme: AppTheme) = withContext(Dispatchers.IO) {
        queries.transaction {
            queries.ensureSettingsRow()
            queries.saveTheme(theme.name)
        }
    }

    override suspend fun saveSidebarCollapsed(isSidebarCollapsed: Boolean) = withContext(Dispatchers.IO) {
        queries.transaction {
            queries.ensureSettingsRow()
            queries.saveSidebarCollapsed(if (isSidebarCollapsed) TRUE_VALUE else FALSE_VALUE)
        }
    }

    override suspend fun saveWindowMaximized() = withContext(Dispatchers.IO) {
        queries.transaction {
            queries.ensureSettingsRow()
            queries.saveWindowMaximized(TRUE_VALUE)
        }
    }

    override suspend fun saveWindowBounds(bounds: WindowBounds) = withContext(Dispatchers.IO) {
        queries.transaction {
            queries.ensureSettingsRow()
            queries.saveWindowBounds(window_x = bounds.x, window_y = bounds.y, window_width = bounds.width, window_height = bounds.height)
        }
    }

    private companion object {
        const val TRUE_VALUE = 1L
        const val FALSE_VALUE = 0L
        const val DEFAULT_UI_SCALE_PERCENT = 100
    }
}
