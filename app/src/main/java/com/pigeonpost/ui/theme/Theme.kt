package com.pigeonpost.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = GoldAccent400,
    secondary = RoyalBlue800,
    tertiary = WaxSealRed500,
    background = ParchmentDark100,
    surface = ParchmentDark200,
    onPrimary = InkBlack,
    onSecondary = Parchment100,
    onTertiary = Parchment100,
    onBackground = Parchment100,
    onSurface = Parchment100,
    surfaceVariant = DeepBrown800,
    onSurfaceVariant = Parchment300
)

private val LightColorScheme = lightColorScheme(
    primary = DeepBrown900,
    secondary = RoyalBlue800,
    tertiary = WaxSealRed500,
    background = Parchment100,
    surface = Parchment200,
    onPrimary = Parchment100,
    onSecondary = Parchment100,
    onTertiary = Parchment100,
    onBackground = DeepBrown900,
    onSurface = DeepBrown900,
    surfaceVariant = Parchment300,
    onSurfaceVariant = DeepBrown700
)

@Composable
fun PigeonPostTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PigeonPostTypography,
        shapes = PigeonPostShapes,
        content = content
    )
}
