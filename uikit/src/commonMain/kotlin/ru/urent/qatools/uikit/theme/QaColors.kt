package ru.urent.qatools.uikit.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class QaPalette(
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textInvert: Color,
    val textInvertSecondary: Color,
    val brand: Color,
    val brandText: Color,
    val brandSoft: Color,
    val brandSelection: Color,
    val positive: Color,
    val positiveText: Color,
    val negative: Color,
    val warningText: Color,
    val stroke: Color,
    val disabledBackground: Color,
    val disabledContent: Color,
    val sidebar: Color,
    val sidebarControl: Color,
    val sidebarSelection: Color,
    val sidebarError: Color,
)

val LightQaPalette = QaPalette(
    background = Color(0xFFF0F3F7),
    surface = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF1F2631),
    textSecondary = Color(0xFF6C7482),
    textInvert = Color(0xFFFFFFFF),
    textInvertSecondary = Color(0xA3FFFFFF),
    brand = Color(0xFF804AFF),
    brandText = Color(0xFF692DEC),
    brandSoft = Color(0x211F11DF),
    brandSelection = Color(0xFFF2F2FF),
    positive = Color(0xFF40A17E),
    positiveText = Color(0xFF23705E),
    negative = Color(0xFFBD364D),
    warningText = Color(0xFF9A3F12),
    stroke = Color(0x1A002566),
    disabledBackground = Color(0x141F2631),
    disabledContent = Color(0x3D1F2631),
    sidebar = Color(0xFF1F2631),
    sidebarControl = Color(0x14FFFFFF),
    sidebarSelection = Color(0x1FFFFFFF),
    sidebarError = Color(0xFFF7AAAF),
)

val DarkQaPalette = QaPalette(
    background = Color(0xFF13171D),
    surface = Color(0xFF1C222B),
    textPrimary = Color(0xFFE7EAF0),
    textSecondary = Color(0xFF9AA3B2),
    textInvert = Color(0xFFFFFFFF),
    textInvertSecondary = Color(0xA3FFFFFF),
    brand = Color(0xFF8A5CFF),
    brandText = Color(0xFFB79CFF),
    brandSoft = Color(0xFF2E2750),
    brandSelection = Color(0xFF2A2445),
    positive = Color(0xFF40A17E),
    positiveText = Color(0xFF6FCBA6),
    negative = Color(0xFFE0566C),
    warningText = Color(0xFFF2A774),
    stroke = Color(0x1AFFFFFF),
    disabledBackground = Color(0x14FFFFFF),
    disabledContent = Color(0x4DE7EAF0),
    sidebar = Color(0xFF0F1318),
    sidebarControl = Color(0x0FFFFFFF),
    sidebarSelection = Color(0x1AFFFFFF),
    sidebarError = Color(0xFFF7AAAF),
)

val LocalQaPalette = staticCompositionLocalOf { LightQaPalette }

object QaColors {
    val background: Color
        @Composable @ReadOnlyComposable get() = LocalQaPalette.current.background
    val surface: Color
        @Composable @ReadOnlyComposable get() = LocalQaPalette.current.surface
    val textPrimary: Color
        @Composable @ReadOnlyComposable get() = LocalQaPalette.current.textPrimary
    val textSecondary: Color
        @Composable @ReadOnlyComposable get() = LocalQaPalette.current.textSecondary
    val textInvert: Color
        @Composable @ReadOnlyComposable get() = LocalQaPalette.current.textInvert
    val textInvertSecondary: Color
        @Composable @ReadOnlyComposable get() = LocalQaPalette.current.textInvertSecondary
    val brand: Color
        @Composable @ReadOnlyComposable get() = LocalQaPalette.current.brand
    val brandText: Color
        @Composable @ReadOnlyComposable get() = LocalQaPalette.current.brandText
    val brandSoft: Color
        @Composable @ReadOnlyComposable get() = LocalQaPalette.current.brandSoft
    val brandSelection: Color
        @Composable @ReadOnlyComposable get() = LocalQaPalette.current.brandSelection
    val positive: Color
        @Composable @ReadOnlyComposable get() = LocalQaPalette.current.positive
    val positiveText: Color
        @Composable @ReadOnlyComposable get() = LocalQaPalette.current.positiveText
    val negative: Color
        @Composable @ReadOnlyComposable get() = LocalQaPalette.current.negative
    val warningText: Color
        @Composable @ReadOnlyComposable get() = LocalQaPalette.current.warningText
    val stroke: Color
        @Composable @ReadOnlyComposable get() = LocalQaPalette.current.stroke
    val disabledBackground: Color
        @Composable @ReadOnlyComposable get() = LocalQaPalette.current.disabledBackground
    val disabledContent: Color
        @Composable @ReadOnlyComposable get() = LocalQaPalette.current.disabledContent
    val sidebar: Color
        @Composable @ReadOnlyComposable get() = LocalQaPalette.current.sidebar
    val sidebarControl: Color
        @Composable @ReadOnlyComposable get() = LocalQaPalette.current.sidebarControl
    val sidebarSelection: Color
        @Composable @ReadOnlyComposable get() = LocalQaPalette.current.sidebarSelection
    val sidebarError: Color
        @Composable @ReadOnlyComposable get() = LocalQaPalette.current.sidebarError
}
