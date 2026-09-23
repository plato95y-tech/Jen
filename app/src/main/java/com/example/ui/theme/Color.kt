package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Primary Emerald / Modern Teal Brand (M3 Tone 40 / Tone 80)
val EmeraldPrimary = Color(0xFF006A60)
val EmeraldDark = Color(0xFF003731)
val EmeraldLight = Color(0xFFE8F5F3)
val EmeraldSecondary = Color(0xFF00897B)
val EmeraldTertiary = Color(0xFF26A69A)
val EmeraldPrimaryContainer = Color(0xFFC7F0E9)
val EmeraldOnPrimaryContainer = Color(0xFF00201D)

// Semantic Accents (Debts, Payments, Warnings)
val DebtRed = Color(0xFFBA1A1A)
val DebtRedContainer = Color(0xFFFFDAD6)
val DebtOnRedContainer = Color(0xFF410002)

val PaymentGreen = Color(0xFF1B6D2F)
val PaymentGreenContainer = Color(0xFFD3E8D3)
val PaymentOnGreenContainer = Color(0xFF002108)

val WarningAmber = Color(0xFF8A5100)
val WarningAmberContainer = Color(0xFFFFDCBE)
val WarningOnAmberContainer = Color(0xFF2C1600)

// Modern Light Palette (M3 Surfaces & Containers)
val BackgroundLight = Color(0xFFF6F8F7)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFDAE5E2)
val SurfaceContainerLowest = Color(0xFFFFFFFF)
val SurfaceContainerLow = Color(0xFFF0F4F3)
val SurfaceContainer = Color(0xFFEAEFEB)
val SurfaceContainerHigh = Color(0xFFE4E9E6)
val SurfaceContainerHighest = Color(0xFFDEE4E1)
val OutlineLight = Color(0xFFB0B9B7)
val OutlineVariantLight = Color(0xFFD6DEDC)
val TextPrimaryLight = Color(0xFF161D1C)
val TextSecondaryLight = Color(0xFF3F4947)

// Modern Dark Palette (M3 Tone 10 - Tone 90)
val BackgroundDark = Color(0xFF0F1514)
val SurfaceDark = Color(0xFF161D1C)
val SurfaceVariantDark = Color(0xFF232D2C)
val SurfaceContainerDark = Color(0xFF1B2423)
val SurfaceContainerHighDark = Color(0xFF212C2B)
val PrimaryDarkTheme = Color(0xFF53DBC9)
val PrimaryContainerDark = Color(0xFF005048)
val OnPrimaryContainerDark = Color(0xFF73F8E5)
val TextPrimaryDark = Color(0xFFDEE4E2)
val TextSecondaryDark = Color(0xFFA1ADA9)
val OutlineDark = Color(0xFF4C5855)
val OutlineVariantDark = Color(0xFF2F3B39)

// Modern Gradient Brushes for Hero Cards and Dynamic Highlights
val HeroEmeraldGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF004D40),
        Color(0xFF00695C),
        Color(0xFF00796B),
        Color(0xFF00897B)
    )
)

val HeroDarkGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF102A26),
        Color(0xFF163C36),
        Color(0xFF1B4E47)
    )
)

val SoftGreenBadgeGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFFE8F5E9),
        Color(0xFFC8E6C9)
    )
)

val SoftRedBadgeGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFFFFEBEE),
        Color(0xFFFFCDD2)
    )
)

// 2. Sapphire Neo-Bank Palette (الأزرق المصرفي)
val SapphirePrimary = Color(0xFF1D4ED8)
val SapphireDark = Color(0xFF0F172A)
val SapphireLight = Color(0xFFEFF6FF)
val SapphireSecondary = Color(0xFF0284C7)
val SapphireTertiary = Color(0xFF0D9488)
val SapphirePrimaryContainer = Color(0xFFDBEAFE)
val SapphireOnPrimaryContainer = Color(0xFF1E3A8A)

val SapphirePrimaryDark = Color(0xFF93C5FD)
val SapphirePrimaryContainerDark = Color(0xFF1E40AF)
val SapphireOnPrimaryContainerDark = Color(0xFFDBEAFE)
val SapphireSurfaceDark = Color(0xFF111827)
val SapphireBackgroundDark = Color(0xFF0B1120)

val HeroSapphireGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF0F172A),
        Color(0xFF1E3A8A),
        Color(0xFF1D4ED8),
        Color(0xFF2563EB)
    )
)

// 3. Warm Amber & Coffee Palette (الكهرمان والتجارة الأصيلة)
val AmberPrimary = Color(0xFFB45309)
val AmberDark = Color(0xFF451A03)
val AmberLight = Color(0xFFFFFBEB)
val AmberSecondary = Color(0xFFD97706)
val AmberTertiary = Color(0xFF78350F)
val AmberPrimaryContainer = Color(0xFFFEF3C7)
val AmberOnPrimaryContainer = Color(0xFF78350F)

val AmberPrimaryDark = Color(0xFFFBBF24)
val AmberPrimaryContainerDark = Color(0xFF78350F)
val AmberOnPrimaryContainerDark = Color(0xFFFEF3C7)
val AmberSurfaceDark = Color(0xFF241C15)
val AmberBackgroundDark = Color(0xFF19130D)

val HeroAmberGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF451A03),
        Color(0xFF78350F),
        Color(0xFF92400E),
        Color(0xFFB45309)
    )
)

enum class AppThemePalette(
    val id: String,
    val titleAr: String,
    val subtitleAr: String,
    val primaryColor: Color,
    val secondaryColor: Color
) {
    EMERALD(
        id = "EMERALD",
        titleAr = "الزمرد المالي",
        subtitleAr = "أخضر زمردي ونعناعي هادئ",
        primaryColor = EmeraldPrimary,
        secondaryColor = EmeraldSecondary
    ),
    SAPPHIRE(
        id = "SAPPHIRE",
        titleAr = "الأزرق المصرفي",
        subtitleAr = "كحلي نيلي وأزرق ملكي",
        primaryColor = SapphirePrimary,
        secondaryColor = SapphireSecondary
    ),
    AMBER(
        id = "AMBER",
        titleAr = "الكهرمان والتجارة الأصيلة",
        subtitleAr = "كهرماني برونزي وقهوة دافئة",
        primaryColor = AmberPrimary,
        secondaryColor = AmberSecondary
    );

    companion object {
        fun fromId(id: String): AppThemePalette {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: EMERALD
        }
    }
}

data class AppThemeAttrs(
    val heroGradient: Brush,
    val heroSurfaceColor: Color,
    val heroOnColor: Color,
    val palette: AppThemePalette
)

val LocalAppThemeAttrs = androidx.compose.runtime.staticCompositionLocalOf {
    AppThemeAttrs(
        heroGradient = HeroEmeraldGradient,
        heroSurfaceColor = EmeraldDark,
        heroOnColor = EmeraldLight,
        palette = AppThemePalette.EMERALD
    )
}

