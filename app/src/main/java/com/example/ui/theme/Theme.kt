package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                // App UI is permanently a dark cyber-HUD; status and navigation icons must ALWAYS be bright/light
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = HudColorScheme,
        typography = Typography,
        content = content
    )
}

