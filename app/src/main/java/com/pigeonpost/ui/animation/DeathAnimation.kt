package com.pigeonpost.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.pigeonpost.ui.theme.DeepBrown800
import com.pigeonpost.ui.theme.GoldAccent400
import com.pigeonpost.ui.theme.WaxSealRed400
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Animation for when a pigeon dies during transit.
 * Shows feathers scattering, pigeon falling, and a message burn effect.
 */
@Composable
fun DeathAnimation(
    modifier: Modifier = Modifier,
    onAnimationComplete: () -> Unit = {}
) {
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(2500, easing = LinearEasing)
        )
        onAnimationComplete()
    }

    val scatterParticles = remember {
        List(16) { ScatterFeather.create(it) }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val progress = animProgress.value

        // Draw falling pigeon
        drawFallingPigeon(progress)

        // Draw scattering feathers
        scatterParticles.forEach { feather ->
            drawScatterFeather(feather, progress)
        }

        // Draw message burn effect
        if (progress > 0.4f) {
            drawBurnEffect((progress - 0.4f) / 0.6f)
        }
    }
}

private data class ScatterFeather(
    val angle: Float,
    val speed: Float,
    val rotationSpeed: Float,
    val color: Color,
    val size: Float
) {
    companion object {
        fun create(index: Int): ScatterFeather {
            val random = Random(index * 97)
            val colors = listOf(DeepBrown800, Color.Gray, Color.DarkGray)
            return ScatterFeather(
                angle = random.nextFloat() * 360f,
                speed = random.nextFloat() * 0.3f + 0.1f,
                rotationSpeed = random.nextFloat() * 720f - 360f,
                color = colors[index % colors.size],
                size = random.nextFloat() * 0.03f + 0.01f
            )
        }
    }
}

private fun DrawScope.drawFallingPigeon(progress: Float) {
    val centerX = size.width / 2
    val startY = size.height * 0.3f
    val fallDistance = size.height * 0.5f
    val currentY = startY + fallDistance * progress * progress // Gravity acceleration
    val rotation = progress * 180f

    if (progress < 0.8f) {
        rotate(rotation, pivot = Offset(centerX, currentY)) {
            // Simplified pigeon silhouette
            val pigeonPath = Path().apply {
                val s = size.minDimension * 0.06f
                moveTo(centerX - s, currentY)
                quadraticBezierTo(centerX, currentY - s, centerX + s, currentY)
                quadraticBezierTo(centerX, currentY + s * 0.5f, centerX - s, currentY)
                close()
            }
            drawPath(path = pigeonPath, color = DeepBrown800.copy(alpha = 1f - progress))
        }
    }
}

private fun DrawScope.drawScatterFeather(feather: ScatterFeather, progress: Float) {
    if (progress < 0.1f) return

    val adjustedProgress = ((progress - 0.1f) / 0.9f).coerceIn(0f, 1f)
    val centerX = size.width / 2
    val centerY = size.height * 0.3f

    val distance = size.minDimension * feather.speed * adjustedProgress
    val x = centerX + cos(Math.toRadians(feather.angle.toDouble())).toFloat() * distance
    val y = centerY + sin(Math.toRadians(feather.angle.toDouble())).toFloat() * distance +
            size.height * 0.2f * adjustedProgress * adjustedProgress // gravity

    val featherLength = size.minDimension * feather.size
    val rotation = feather.rotationSpeed * adjustedProgress

    rotate(rotation, pivot = Offset(x, y)) {
        val path = Path().apply {
            moveTo(x - featherLength, y)
            quadraticBezierTo(x, y - featherLength * 0.3f, x + featherLength, y)
            quadraticBezierTo(x, y + featherLength * 0.15f, x - featherLength, y)
            close()
        }
        drawPath(
            path = path,
            color = feather.color.copy(alpha = (1f - adjustedProgress * 0.8f).coerceAtLeast(0f))
        )
    }
}

private fun DrawScope.drawBurnEffect(progress: Float) {
    val centerX = size.width / 2
    val centerY = size.height * 0.65f
    val maxRadius = size.minDimension * 0.15f

    // Outer glow
    drawCircle(
        color = WaxSealRed400.copy(alpha = (0.3f * (1f - progress)).coerceAtLeast(0f)),
        radius = maxRadius * progress * 1.5f,
        center = Offset(centerX, centerY)
    )

    // Inner burn
    drawCircle(
        color = GoldAccent400.copy(alpha = (0.5f * (1f - progress)).coerceAtLeast(0f)),
        radius = maxRadius * progress,
        center = Offset(centerX, centerY)
    )

    // Smoke wisps
    val smokeAlpha = (0.2f * (1f - progress * 0.5f)).coerceAtLeast(0f)
    repeat(3) { i ->
        val smokeOffset = (i - 1) * maxRadius * 0.4f
        drawCircle(
            color = Color.Gray.copy(alpha = smokeAlpha),
            radius = maxRadius * 0.3f * progress,
            center = Offset(
                centerX + smokeOffset,
                centerY - maxRadius * progress * (0.5f + i * 0.3f)
            )
        )
    }
}
