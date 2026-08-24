package com.pigeonpost.ui.animation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.pigeonpost.ui.theme.DeepBrown700
import com.pigeonpost.ui.theme.GoldAccent300
import com.pigeonpost.ui.theme.Parchment300
import kotlin.random.Random

/**
 * Particle system composable that renders floating feather effects.
 * Used on splash screen and delivery completion screens.
 */
@Composable
fun FeatherParticles(
    modifier: Modifier = Modifier,
    particleCount: Int = 12,
    colors: List<Color> = listOf(
        Parchment300,
        GoldAccent300,
        DeepBrown700.copy(alpha = 0.5f)
    )
) {
    val particles = remember {
        List(particleCount) { FeatherParticle.create(it, colors) }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "feather_particles")

    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "feather_progress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { particle ->
            drawFeather(particle, animProgress)
        }
    }
}

private data class FeatherParticle(
    val startX: Float,
    val startY: Float,
    val speed: Float,
    val amplitude: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val color: Color,
    val featherSize: Float,
    val phase: Float
) {
    companion object {
        fun create(index: Int, colors: List<Color>): FeatherParticle {
            val random = Random(index * 137)
            return FeatherParticle(
                startX = random.nextFloat(),
                startY = random.nextFloat() * -0.2f - 0.1f,
                speed = random.nextFloat() * 0.3f + 0.2f,
                amplitude = random.nextFloat() * 0.1f + 0.03f,
                rotation = random.nextFloat() * 360f,
                rotationSpeed = random.nextFloat() * 180f + 90f,
                color = colors[index % colors.size],
                featherSize = random.nextFloat() * 0.02f + 0.015f,
                phase = random.nextFloat() * 2f * Math.PI.toFloat()
            )
        }
    }
}

private fun DrawScope.drawFeather(particle: FeatherParticle, progress: Float) {
    val adjustedProgress = (progress + particle.speed) % 1.0f

    val x = size.width * (particle.startX +
            kotlin.math.sin((adjustedProgress * 4f + particle.phase).toDouble()).toFloat() * particle.amplitude)
    val y = size.height * (adjustedProgress * 1.2f + particle.startY)

    if (y < 0 || y > size.height) return

    val featherLength = size.minDimension * particle.featherSize
    val currentRotation = particle.rotation + adjustedProgress * particle.rotationSpeed

    rotate(currentRotation, pivot = Offset(x, y)) {
        val featherPath = Path().apply {
            moveTo(x - featherLength, y)
            quadraticBezierTo(x, y - featherLength * 0.3f, x + featherLength, y)
            quadraticBezierTo(x, y + featherLength * 0.15f, x - featherLength, y)
            close()
        }
        drawPath(
            path = featherPath,
            color = particle.color.copy(alpha = 0.6f * (1f - adjustedProgress * 0.5f))
        )

        // Feather spine
        drawLine(
            color = particle.color.copy(alpha = 0.4f),
            start = Offset(x - featherLength, y),
            end = Offset(x + featherLength, y),
            strokeWidth = 0.5f
        )
    }
}
