package com.salahmate.app.core.design

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.salahmate.app.core.prefs.ThemeMode

private val LightScheme = lightColorScheme(
    primary = Emerald800,
    onPrimary = Color.White,
    primaryContainer = Emerald100,
    onPrimaryContainer = Emerald900,
    secondary = Gold700,
    onSecondary = Color.White,
    secondaryContainer = Gold100,
    onSecondaryContainer = Gold800,
    tertiary = Emerald600,
    onTertiary = Color.White,
    background = Parchment,
    onBackground = Ink,
    surface = Color(0xFFFDFBF5),
    onSurface = Ink,
    surfaceVariant = ParchmentDark,
    onSurfaceVariant = InkMuted,
    outline = Color(0xFF8C9A94),
    outlineVariant = Color(0xFFD3DCD7),
    error = DangerRed,
    onError = Color.White,
    errorContainer = DangerRedLight,
    onErrorContainer = Color(0xFF410E0B),
)

private val DarkScheme = darkColorScheme(
    primary = Emerald200,
    onPrimary = Emerald900,
    primaryContainer = Emerald800,
    onPrimaryContainer = Emerald50,
    secondary = Gold300,
    onSecondary = Color(0xFF3A2C00),
    secondaryContainer = Color(0xFF574208),
    onSecondaryContainer = Gold100,
    tertiary = Emerald200,
    onTertiary = Emerald900,
    background = NightSurface,
    onBackground = NightOnSurface,
    surface = NightSurfaceHigh,
    onSurface = NightOnSurface,
    surfaceVariant = NightSurfaceHigher,
    onSurfaceVariant = NightOnSurfaceMuted,
    outline = Color(0xFF7E8F89),
    outlineVariant = Color(0xFF2A3D38),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

/**
 * Arabic typography. Arabic script in the Quran is rendered with a Naskh-style serif so
 * diacritics stay legible at every font scale; the app never applies letter-spacing to
 * Arabic because that breaks shaping.
 */
val ArabicFontFamily = FontFamily.Serif

val AppTypography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 52.sp, lineHeight = 60.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 21.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp),
)

/**
 * Density/appearance values that do not map onto Material's [ColorScheme], exposed through a
 * CompositionLocal so screens can read consistent spacing and the user's Arabic scale factor.
 */
data class SalahMateDimens(
    val screenPadding: androidx.compose.ui.unit.Dp = 16.dp,
    val cardSpacing: androidx.compose.ui.unit.Dp = 12.dp,
    val cornerRadius: androidx.compose.ui.unit.Dp = 20.dp,
    val arabicScale: Float = 1f,
    val showTranslation: Boolean = true,
    val isDark: Boolean = false,
)

val LocalSalahMateDimens = staticCompositionLocalOf { SalahMateDimens() }

@Composable
fun SalahMateTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    arabicScale: Float = 1f,
    showTranslation: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val scheme = if (dark) DarkScheme else LightScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
            }
        }
    }
    CompositionLocalProvider(
        LocalSalahMateDimens provides SalahMateDimens(
            arabicScale = arabicScale,
            showTranslation = showTranslation,
            isDark = dark,
        ),
    ) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
    }
}

/** Arabic text style at the user's chosen scale. */
@Composable
fun arabicStyle(sizeSp: Float = 28f, bold: Boolean = false): TextStyle {
    val scale = LocalSalahMateDimens.current.arabicScale
    return TextStyle(
        fontFamily = ArabicFontFamily,
        fontSize = (sizeSp * scale).sp,
        lineHeight = (sizeSp * scale * 1.85f).sp,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        letterSpacing = 0.sp,
        textDirection = TextDirection.Rtl,
    )
}

@Composable
fun prayerAccent(key: String): Color {
    val dark = LocalSalahMateDimens.current.isDark
    val base = when (key) {
        "FAJR" -> FajrAccent
        "SUNRISE" -> SunriseAccent
        "DHUHR" -> DhuhrAccent
        "ASR" -> AsrAccent
        "MAGHRIB" -> MaghribAccent
        "ISHA" -> IshaAccent
        else -> Emerald600
    }
    return if (dark) base.copy(alpha = 0.95f).lighten(0.35f) else base
}

private fun Color.lighten(fraction: Float): Color = Color(
    red = (red + (1f - red) * fraction).coerceIn(0f, 1f),
    green = (green + (1f - green) * fraction).coerceIn(0f, 1f),
    blue = (blue + (1f - blue) * fraction).coerceIn(0f, 1f),
    alpha = alpha,
)
