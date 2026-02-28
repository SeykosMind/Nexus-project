package com.nexus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── HUD Color Palette ──────────────────────────────────────────────────────────
object NexusColors {
    val Background     = Color(0xFF020B12)
    val Surface        = Color(0xFF051520)
    val SurfaceVariant = Color(0xFF0A2030)
    val Primary        = Color(0xFF00E5FF)    // cyan eléctrico
    val PrimaryDim     = Color(0xFF0097A7)
    val Secondary      = Color(0xFF39FF14)    // verde neón
    val Accent         = Color(0xFFFF007F)    // magenta
    val Warning        = Color(0xFFFFD600)
    val Error          = Color(0xFFFF1744)
    val OnBackground   = Color(0xFFCCEEFF)
    val OnSurface      = Color(0xFF88BBCC)
    val Grid           = Color(0xFF0D2535)
    val Glow           = Color(0x4400E5FF)
}

data class NexusColorScheme(
    val background: Color     = NexusColors.Background,
    val surface: Color        = NexusColors.Surface,
    val surfaceVariant: Color = NexusColors.SurfaceVariant,
    val primary: Color        = NexusColors.Primary,
    val primaryDim: Color     = NexusColors.PrimaryDim,
    val secondary: Color      = NexusColors.Secondary,
    val accent: Color         = NexusColors.Accent,
    val warning: Color        = NexusColors.Warning,
    val error: Color          = NexusColors.Error,
    val onBackground: Color   = NexusColors.OnBackground,
    val onSurface: Color      = NexusColors.OnSurface,
    val grid: Color           = NexusColors.Grid,
    val glow: Color           = NexusColors.Glow,
)

val LocalNexusColors = staticCompositionLocalOf { NexusColorScheme() }

// ── Typography ────────────────────────────────────────────────────────────────
val ShareTechMono = FontFamily.Monospace   // fallback; swap with actual font if bundled

object NexusTypography {
    val displayLarge = TextStyle(fontFamily = ShareTechMono, fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
    val displayMedium = TextStyle(fontFamily = ShareTechMono, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
    val titleLarge = TextStyle(fontFamily = ShareTechMono, fontSize = 18.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
    val titleMedium = TextStyle(fontFamily = ShareTechMono, fontSize = 14.sp, letterSpacing = 1.5.sp)
    val bodyMedium = TextStyle(fontFamily = ShareTechMono, fontSize = 13.sp, letterSpacing = 0.5.sp)
    val bodySmall = TextStyle(fontFamily = ShareTechMono, fontSize = 11.sp, letterSpacing = 0.5.sp)
    val labelSmall = TextStyle(fontFamily = ShareTechMono, fontSize = 10.sp, letterSpacing = 1.sp)
}

val LocalNexusTypography = staticCompositionLocalOf { NexusTypography }

// ── Material Dark Scheme bridging ─────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary        = NexusColors.Primary,
    secondary      = NexusColors.Secondary,
    tertiary       = NexusColors.Accent,
    background     = NexusColors.Background,
    surface        = NexusColors.Surface,
    error          = NexusColors.Error,
    onPrimary      = NexusColors.Background,
    onBackground   = NexusColors.OnBackground,
    onSurface      = NexusColors.OnSurface,
)

@Composable
fun NexusTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalNexusColors provides NexusColorScheme(),
        LocalNexusTypography provides NexusTypography,
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            content = content
        )
    }
}

// Convenience accessor
object NexusTheme {
    val colors: NexusColorScheme
        @Composable get() = LocalNexusColors.current
    val typography: NexusTypography
        @Composable get() = LocalNexusTypography.current
}
