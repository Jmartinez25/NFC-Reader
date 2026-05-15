package com.martzlabs.myapplication.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary        = PrimaryLight,
    secondary      = SecondaryLight,
    tertiary       = TertiaryLight,
    background     = NeutralLight,
    surface        = SurfaceLight,
    onPrimary      = Color.White,
    onSurface      = TextLight,
    onBackground   = Color.White,
    surfaceVariant = SecondaryLight,
)

private val DarkColors = darkColorScheme(
    primary        = PrimaryDark,
    secondary      = SecondaryDark,
    tertiary       = TertiaryDark,
    background     = NeutralDark,
    surface        = SurfaceDark,
    onPrimary      = Color.White,
    onSurface      = TextDark,
    onBackground   = TextDark,
    surfaceVariant = SecondaryDark,
)

@Composable
fun NFCTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content     = content
    )
}