package ru.urent.qatools.core_settings.impl

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ru.urent.qatools.core_database.CoreDatabase
import ru.urent.qatools.core_settings.WindowBounds

class AppSettingsRepositoryTest {
    private val repository = AppSettingsRepositoryImpl(
        database = CoreDatabase(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, schema = CoreDatabase.Schema)),
    )

    @Test
    fun firstLaunchOpensMaximizedWindowWithExpandedSidebar() = runTest {
        val settings = repository.settings.first()

        assertTrue(settings.isWindowMaximized)
        assertNull(settings.windowBounds)
        assertFalse(settings.isSidebarCollapsed)
    }

    @Test
    fun movedWindowIsRestoredFloatingAtSavedBounds() = runTest {
        repository.saveWindowMaximized()

        repository.saveWindowBounds(SavedBounds)

        val settings = repository.settings.first()
        assertFalse(settings.isWindowMaximized)
        assertEquals(SavedBounds, settings.windowBounds)
    }

    @Test
    fun maximizingKeepsBoundsForRestoringWindowLater() = runTest {
        repository.saveWindowBounds(SavedBounds)

        repository.saveWindowMaximized()

        val settings = repository.settings.first()
        assertTrue(settings.isWindowMaximized)
        assertEquals(SavedBounds, settings.windowBounds)
    }

    @Test
    fun collapsedSidebarStaysCollapsed() = runTest {
        repository.saveSidebarCollapsed(true)

        assertTrue(repository.settings.first().isSidebarCollapsed)
    }

    private companion object {
        val SavedBounds = WindowBounds(x = 120.0, y = 80.0, width = 1440.0, height = 960.0)
    }
}
