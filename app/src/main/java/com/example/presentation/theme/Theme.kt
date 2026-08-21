package com.example.presentation.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LaborColorScheme = lightColorScheme(
    primary = LaborBlue,
    onPrimary = Color.White,
    primaryContainer = LaborBlueLight,
    onPrimaryContainer = LaborBlueDark,
    secondary = LaborBlueSecondary,
    onSecondary = Color.White,
    tertiary = LaborPurple,
    onTertiary = Color.White,
    background = LaborBackground,
    onBackground = LaborTextPrimary,
    surface = LaborSurface,
    onSurface = LaborTextPrimary,
    surfaceVariant = Color(0xFFEFEFEF),
    onSurfaceVariant = LaborTextSecondary,
    error = LaborError,
    onError = Color.White,
    outline = LaborDivider
)

@Composable
fun LaborbookTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.statusBarColor = LaborBlue.toArgb()
            window?.let {
                WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = LaborColorScheme,
        typography = Typography,
        content = content
    )
}

