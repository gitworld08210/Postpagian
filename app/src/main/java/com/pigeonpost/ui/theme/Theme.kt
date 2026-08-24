package com.pigeonpost.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Retained for reference only. It is intentionally NOT applied: the PigeonPost
 * aesthetic is aged light parchment, and [ParchmentBackground] always paints a
 * light cream sheet. Applying a dark scheme rendered cream ink on cream
 * parchment, making every label, placeholder and typed character invisible.
 */
@Suppress("unused")
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

/**
 * PigeonPost always renders the parchment (light) color scheme, regardless of the
 * system dark-mode setting. The parchment background is painted by the app itself
 * and is always light cream, so the ink must always be dark to stay readable.
 */
@Composable
fun PigeonPostTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            // The status bar is DeepBrown900 (dark), so its icons must be light.
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PigeonPostTypography,
        shapes = PigeonPostShapes,
        content = content
    )
}
