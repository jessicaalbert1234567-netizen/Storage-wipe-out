package com.galaxya12.cleaner.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = OneBlueDark,
    onPrimary = OneSurfaceDark,
    primaryContainer = OneBlueContainerDark,
    onPrimaryContainer = OneBlueDark,
    background = OneBackgroundDark,
    surface = OneSurfaceDark,
    onBackground = OneTextPrimaryDark,
    onSurface = OneTextPrimaryDark,
    surfaceVariant = OneCardStrokeDark,
    onSurfaceVariant = OneTextSecondaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = OneBlue,
    onPrimary = OneSurfaceLight,
    primaryContainer = OneBlueContainer,
    onPrimaryContainer = OneBlue,
    background = OneBackgroundLight,
    surface = OneSurfaceLight,
    onBackground = OneTextPrimaryLight,
    onSurface = OneTextPrimaryLight,
    surfaceVariant = OneCardStrokeLight,
    onSurfaceVariant = OneTextSecondaryLight
)

@Composable
fun GalaxyCleanerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
