package ru.urent.qatools.core_settings

enum class AppTheme { LIGHT, DARK, SYSTEM }

data class WindowBounds(val x: Double, val y: Double, val width: Double, val height: Double)

data class AppSettings(
    val isBorderlessFullscreen: Boolean,
    val uiScalePercent: Int,
    val theme: AppTheme,
    val isSidebarCollapsed: Boolean,
    val isWindowMaximized: Boolean,
    val windowBounds: WindowBounds?,
)
