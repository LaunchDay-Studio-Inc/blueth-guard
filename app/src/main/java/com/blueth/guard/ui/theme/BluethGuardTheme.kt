package com.blueth.guard.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimary,
    onPrimary = TextPrimary,
    primaryContainer = BluePrimaryDark,
    onPrimaryContainer = TextPrimary,
    secondary = CyanSecondary,
    onSecondary = TextPrimary,
    secondaryContainer = CyanSecondaryDark,
    onSecondaryContainer = TextPrimary,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = RiskCritical,
    onError = TextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = LightSurface,
    primaryContainer = BluePrimaryLight,
    onPrimaryContainer = TextPrimaryLight,
    secondary = CyanSecondary,
    onSecondary = LightSurface,
    secondaryContainer = CyanSecondaryLight,
    onSecondaryContainer = TextPrimaryLight,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    error = RiskCritical,
    onError = LightSurface
)

private val AmoledColorScheme = darkColorScheme(
    primary = BluePrimary,
    onPrimary = TextPrimary,
    primaryContainer = BluePrimaryDark,
    onPrimaryContainer = TextPrimary,
    secondary = CyanSecondary,
    onSecondary = TextPrimary,
    secondaryContainer = CyanSecondaryDark,
    onSecondaryContainer = TextPrimary,
    background = AmoledBackground,
    onBackground = TextPrimary,
    surface = AmoledSurface,
    onSurface = TextPrimary,
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = RiskCritical,
    onError = TextPrimary
)

val BluethShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun BluethGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoledTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        amoledTheme -> AmoledColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme && !amoledTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BluethTypography,
        shapes = BluethShapes,
        content = content
    )
}
