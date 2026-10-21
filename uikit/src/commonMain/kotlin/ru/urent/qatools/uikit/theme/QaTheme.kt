package ru.urent.qatools.uikit.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import ru.urent.qatools.uikit.resources.Res
import ru.urent.qatools.uikit.resources.roboto_bold
import ru.urent.qatools.uikit.resources.roboto_medium
import ru.urent.qatools.uikit.resources.roboto_regular
import ru.urent.qatools.uikit.resources.roboto_semibold

@Composable
fun QaTheme(isDark: Boolean, content: @Composable () -> Unit) {
    val roboto = FontFamily(
        Font(Res.font.roboto_regular, FontWeight.Normal),
        Font(Res.font.roboto_medium, FontWeight.Medium),
        Font(Res.font.roboto_semibold, FontWeight.SemiBold),
        Font(Res.font.roboto_bold, FontWeight.Bold),
    )
    val typography = Typography(
        headlineSmall = TextStyle(fontFamily = roboto, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
        titleLarge = TextStyle(fontFamily = roboto, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
        titleMedium = TextStyle(fontFamily = roboto, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
        bodyLarge = TextStyle(fontFamily = roboto, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
        bodyMedium = TextStyle(fontFamily = roboto, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontFamily = roboto, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
        labelLarge = TextStyle(fontFamily = roboto, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
        labelMedium = TextStyle(fontFamily = roboto, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    )
    val palette = if (isDark) DarkQaPalette else LightQaPalette
    val colors = if (isDark) {
        darkColorScheme(
            primary = palette.brand,
            onPrimary = palette.textInvert,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            onSurfaceVariant = palette.textSecondary,
            surfaceContainer = palette.surface,
            error = palette.negative,
        )
    } else {
        lightColorScheme(
            primary = palette.brand,
            onPrimary = palette.textInvert,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            onSurfaceVariant = palette.textSecondary,
            surfaceContainer = palette.surface,
            error = palette.negative,
        )
    }
    CompositionLocalProvider(LocalQaPalette provides palette) {
        MaterialTheme(colorScheme = colors, typography = typography, content = content)
    }
}
