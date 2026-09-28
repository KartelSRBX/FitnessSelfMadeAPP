package com.kartel.fitness.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

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

// Более скруглённые, "живые" формы вместо стандартных плоских Material-углов.
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun FitnessTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        shapes = AppShapes,
        content = content,
    )
}
