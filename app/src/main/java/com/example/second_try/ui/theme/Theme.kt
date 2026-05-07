package com.example.second_try.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = AppColors.PrimaryGreen,
    onPrimary = AppColors.Background,

    secondary = AppColors.DarkGreen,
    onSecondary = AppColors.Background,

    tertiary = AppColors.Coral,
    onTertiary = AppColors.Background,

    background = AppColors.Background,
    onBackground = AppColors.TextDark,

    surface = AppColors.Background,
    onSurface = AppColors.TextDark,

    surfaceVariant = AppColors.WarmCard,
    onSurfaceVariant = AppColors.TextDark,

    error = AppColors.Coral,
    onError = AppColors.Background
)

@Composable
fun Second_tryTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}