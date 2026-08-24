package com.pigeonpost.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pigeonpost.ui.animation.FeatherParticles
import com.pigeonpost.ui.animation.PigeonFlyingAnimation
import com.pigeonpost.ui.components.ParchmentBackground
import com.pigeonpost.ui.theme.GoldAccent400
import kotlinx.coroutines.delay

/**
 * Animated splash screen with pigeon silhouette, parchment background,
 * app title in medieval calligraphy style, fade-in animation with feather particles.
 */
@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    val titleAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val pigeonAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Staggered fade-in animation
        pigeonAlpha.animateTo(1f, animationSpec = tween(800))
        delay(200)
        titleAlpha.animateTo(1f, animationSpec = tween(1000))
        delay(200)
        subtitleAlpha.animateTo(1f, animationSpec = tween(800))
        delay(1500)
        onSplashComplete()
    }

    ParchmentBackground {
        // Feather particles floating in background
        FeatherParticles(
            modifier = Modifier.fillMaxSize(),
            particleCount = 15
        )

        // Main content
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Pigeon animation
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .alpha(pigeonAlpha.value)
            ) {
                PigeonFlyingAnimation(
                    size = 120.dp,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App title in medieval calligraphy style
            Text(
                text = "Pigeon Post",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(titleAlpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Messages delivered by wing",
                style = MaterialTheme.typography.titleMedium,
                color = GoldAccent400,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(subtitleAlpha.value)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Est. MMXXIV",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(subtitleAlpha.value)
            )
        }
    }
}
