package com.gosms.gateway.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LightPrimary = Color(0xFF4F46E5)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFE0E7FF)
val LightOnPrimaryContainer = Color(0xFF312E81)
val LightBackground = Color(0xFFF6F7FB)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF0F172A)
val LightOnSurfaceMuted = Color(0xFF64748B)
val LightOutline = Color(0xFFE2E8F0)
val LightSuccess = Color(0xFF16A34A)
val LightSuccessContainer = Color(0xFFDCFCE7)
val LightWarning = Color(0xFFD97706)
val LightWarningContainer = Color(0xFFFEF3C7)
val LightError = Color(0xFFDC2626)
val LightErrorContainer = Color(0xFFFEE2E2)

val DarkPrimary = Color(0xFF818CF8)
val DarkOnPrimary = Color(0xFF1E1B4B)
val DarkPrimaryContainer = Color(0xFF312E81)
val DarkOnPrimaryContainer = Color(0xFFE0E7FF)
val DarkBackground = Color(0xFF0B1020)
val DarkSurface = Color(0xFF141B2D)
val DarkOnSurface = Color(0xFFE5E7EB)
val DarkOnSurfaceMuted = Color(0xFF94A3B8)
val DarkOutline = Color(0xFF243049)
val DarkSuccess = Color(0xFF4ADE80)
val DarkSuccessContainer = Color(0xFF14532D)
val DarkWarning = Color(0xFFFBBF24)
val DarkWarningContainer = Color(0xFF78350F)
val DarkError = Color(0xFFF87171)
val DarkErrorContainer = Color(0xFF7F1D1D)

@Immutable
data class ExtendedColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val background: Color,
    val surface: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val outline: Color,
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val error: Color,
    val errorContainer: Color
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        primary = LightPrimary,
        onPrimary = LightOnPrimary,
        primaryContainer = LightPrimaryContainer,
        onPrimaryContainer = LightOnPrimaryContainer,
        background = LightBackground,
        surface = LightSurface,
        onSurface = LightOnSurface,
        onSurfaceMuted = LightOnSurfaceMuted,
        outline = LightOutline,
        success = LightSuccess,
        successContainer = LightSuccessContainer,
        warning = LightWarning,
        warningContainer = LightWarningContainer,
        error = LightError,
        errorContainer = LightErrorContainer
    )
}
