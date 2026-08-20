package com.example.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
    MaterialTheme(
        colorScheme = LaborColorScheme,
        typography = Typography,
        content = content
    )
}

