package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// --- 1. Emerald Ledger Color Schemes ---
private val EmeraldDarkColorScheme = darkColorScheme(
    primary = PrimaryDarkTheme,
    onPrimary = Color(0xFF003731),
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = EmeraldSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF004D40),
    onSecondaryContainer = Color(0xFFC7F0E9),
    tertiary = EmeraldTertiary,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val EmeraldLightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = EmeraldOnPrimaryContainer,
    secondary = EmeraldSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7E8E4),
    onSecondaryContainer = Color(0xFF003731),
    tertiary = EmeraldTertiary,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = DebtRed,
    errorContainer = DebtRedContainer,
    onError = Color.White,
    onErrorContainer = DebtOnRedContainer
)

// --- 2. Sapphire Neo-Bank Color Schemes ---
private val SapphireLightColorScheme = lightColorScheme(
    primary = SapphirePrimary,
    onPrimary = Color.White,
    primaryContainer = SapphirePrimaryContainer,
    onPrimaryContainer = SapphireOnPrimaryContainer,
    secondary = SapphireSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = SapphireTertiary,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F5F9),
    surfaceContainer = Color(0xFFE2E8F0),
    surfaceContainerHigh = Color(0xFFCBD5E1),
    surfaceContainerHighest = Color(0xFF94A3B8),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFCBD5E1),
    error = DebtRed,
    errorContainer = DebtRedContainer,
    onError = Color.White,
    onErrorContainer = DebtOnRedContainer
)

private val SapphireDarkColorScheme = darkColorScheme(
    primary = SapphirePrimaryDark,
    onPrimary = Color(0xFF0B1120),
    primaryContainer = SapphirePrimaryContainerDark,
    onPrimaryContainer = SapphireOnPrimaryContainerDark,
    secondary = SapphireSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF075985),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = SapphireTertiary,
    background = SapphireBackgroundDark,
    onBackground = Color(0xFFF1F5F9),
    surface = SapphireSurfaceDark,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainer = Color(0xFF1E293B),
    surfaceContainerHigh = Color(0xFF334155),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFF334155),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6)
)

// --- 3. Warm Amber & Coffee Color Schemes ---
private val AmberLightColorScheme = lightColorScheme(
    primary = AmberPrimary,
    onPrimary = Color.White,
    primaryContainer = AmberPrimaryContainer,
    onPrimaryContainer = AmberOnPrimaryContainer,
    secondary = AmberSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFDE68A),
    onSecondaryContainer = Color(0xFF451A03),
    tertiary = AmberTertiary,
    background = Color(0xFFFAF8F5),
    onBackground = Color(0xFF291F18),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF291F18),
    surfaceVariant = Color(0xFFF3EDE4),
    onSurfaceVariant = Color(0xFF5A4E46),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5EFE7),
    surfaceContainer = Color(0xFFEFE8DE),
    surfaceContainerHigh = Color(0xFFE8DFC0),
    surfaceContainerHighest = Color(0xFFDDD2BE),
    outline = Color(0xFFA89A8E),
    outlineVariant = Color(0xFFD4C8BC),
    error = DebtRed,
    errorContainer = DebtRedContainer,
    onError = Color.White,
    onErrorContainer = DebtOnRedContainer
)

private val AmberDarkColorScheme = darkColorScheme(
    primary = AmberPrimaryDark,
    onPrimary = Color(0xFF451A03),
    primaryContainer = AmberPrimaryContainerDark,
    onPrimaryContainer = AmberOnPrimaryContainerDark,
    secondary = AmberSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF543D1A),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = Color(0xFFD97706),
    background = AmberBackgroundDark,
    onBackground = Color(0xFFF5EFE7),
    surface = AmberSurfaceDark,
    onSurface = Color(0xFFF5EFE7),
    surfaceVariant = Color(0xFF33271F),
    onSurfaceVariant = Color(0xFFC4B5A5),
    surfaceContainer = Color(0xFF2C221B),
    surfaceContainerHigh = Color(0xFF3B2E25),
    outline = Color(0xFF7E6E60),
    outlineVariant = Color(0xFF4A3C31),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun MyApplicationTheme(
    themeMode: String = "SYSTEM",
    themePalette: String = "EMERALD",
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    val selectedPalette = AppThemePalette.fromId(themePalette)

    val colorScheme = when (selectedPalette) {
        AppThemePalette.EMERALD -> if (isDark) EmeraldDarkColorScheme else EmeraldLightColorScheme
        AppThemePalette.SAPPHIRE -> if (isDark) SapphireDarkColorScheme else SapphireLightColorScheme
        AppThemePalette.AMBER -> if (isDark) AmberDarkColorScheme else AmberLightColorScheme
    }

    val themeAttrs = when (selectedPalette) {
        AppThemePalette.EMERALD -> AppThemeAttrs(
            heroGradient = if (isDark) HeroDarkGradient else HeroEmeraldGradient,
            heroSurfaceColor = EmeraldDark,
            heroOnColor = EmeraldLight,
            palette = selectedPalette
        )
        AppThemePalette.SAPPHIRE -> AppThemeAttrs(
            heroGradient = HeroSapphireGradient,
            heroSurfaceColor = SapphireDark,
            heroOnColor = SapphireLight,
            palette = selectedPalette
        )
        AppThemePalette.AMBER -> AppThemeAttrs(
            heroGradient = HeroAmberGradient,
            heroSurfaceColor = AmberDark,
            heroOnColor = AmberLight,
            palette = selectedPalette
        )
    }

    CompositionLocalProvider(LocalAppThemeAttrs provides themeAttrs) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}



