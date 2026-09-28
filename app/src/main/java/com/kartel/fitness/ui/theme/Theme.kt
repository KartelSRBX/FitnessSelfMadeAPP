package com.kartel.fitness.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Приложение всегда тёмное: палитра синий/чёрный/фиолетовый/сиреневый
// задумана как тёмная тема, светлая схема оставлена про запас.
private val DarkColors = darkColorScheme(
    primary = Blue,
    onPrimary = OnDark,
    primaryContainer = BlueContainerDark,
    onPrimaryContainer = OnDark,
    secondary = Purple,
    onSecondary = OnDark,
    secondaryContainer = PurpleContainerDark,
    onSecondaryContainer = OnDark,
    tertiary = Lilac,
    onTertiary = BackgroundDark,
    tertiaryContainer = LilacContainerDark,
    onTertiaryContainer = OnDark,
    background = BackgroundDark,
    onBackground = OnDark,
    surface = SurfaceDark,
    onSurface = OnDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnDark,
)

private val LightColors = lightColorScheme(
    primary = Blue,
    primaryContainer = BlueContainerLight,
    secondary = Purple,
    secondaryContainer = PurpleContainerLight,
    tertiary = Lilac,
    tertiaryContainer = LilacContainerLight,
    background = BackgroundLight,
    onBackground = OnLight,
    surface = SurfaceLight,
    onSurface = OnLight,
)

@Composable
fun FitnessTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
