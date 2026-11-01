package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Dedicated High-Tech HUD Color Scheme
private val HudColorScheme = darkColorScheme(
    primary = HudPrimary,
    onPrimary = HudBackground,
    primaryContainer = HudSurfaceLight,
    onPrimaryContainer = HudPrimary,
    secondary = HudSecondary,
    onSecondary = HudBackground,
    secondaryContainer = HudSurfaceElevated,
    onSecondaryContainer = HudSecondary,
    tertiary = HudAmber,
    onTertiary = HudBackground,
    background = HudBackground,
    onBackground = HudTextPrimary,
    surface = HudSurface,
    onSurface = HudTextPrimary,
    surfaceVariant = HudSurfaceElevated,
    onSurfaceVariant = HudTextSecondary,
    outline = HudBorder,
    outlineVariant = HudBorderGlow,
    error = HudAlert,
    onError = HudBackground
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HudColorScheme,
        typography = Typography,
        content = content
    )
}
