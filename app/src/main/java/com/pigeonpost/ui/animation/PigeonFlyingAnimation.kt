package com.pigeonpost.ui.animation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pigeonpost.ui.theme.DeepBrown800
import com.pigeonpost.ui.theme.Parchment100

/**
 * Animated pigeon composable that simulates wing flap keyframes
 * using Compose infinite transition animation APIs.
 */
@Composable
fun PigeonFlyingAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tint: Color = DeepBrown800
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pigeon_flight")

    // Wing flap animation
    val wingAngle by infiniteTransition.animateFloat(
        initialValue = -20f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wing_flap"
    )

    // Subtle body bob
    val bodyOffset by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "body_bob"
    )

    Canvas(modifier = modifier.size(size)) {
        val centerX = this.size.width / 2
        val centerY = this.size.height / 2 + bodyOffset

        // Draw body
        drawPigeonBody(centerX, centerY, tint)

        // Draw wings with flap animation
        rotate(wingAngle, pivot = Offset(centerX, centerY)) {
            drawPigeonWing(centerX, centerY, tint, isLeft = true)
        }
        rotate(-wingAngle, pivot = Offset(centerX, centerY)) {
            drawPigeonWing(centerX, centerY, tint, isLeft = false)
        }

        // Draw tail
        drawPigeonTail(centerX, centerY, tint)
    }
}

private fun DrawScope.drawPigeonBody(centerX: Float, centerY: Float, tint: Color) {
    val bodyWidth = size.width * 0.35f
    val bodyHeight = size.height * 0.25f

    // Body (oval)
    val bodyPath = Path().apply {
        moveTo(centerX - bodyWidth, centerY)
        quadraticBezierTo(centerX - bodyWidth, centerY - bodyHeight, centerX, centerY - bodyHeight * 0.8f)
        quadraticBezierTo(centerX + bodyWidth, centerY - bodyHeight, centerX + bodyWidth, centerY)
        quadraticBezierTo(centerX + bodyWidth, centerY + bodyHeight, centerX, centerY + bodyHeight * 0.8f)
        quadraticBezierTo(centerX - bodyWidth, centerY + bodyHeight, centerX - bodyWidth, centerY)
        close()
    }
    drawPath(path = bodyPath, color = tint)

    // Head
    val headRadius = size.minDimension * 0.1f
    drawCircle(
        color = tint,
        radius = headRadius,
        center = Offset(centerX + bodyWidth * 0.9f, centerY - bodyHeight * 0.5f)
    )

    // Beak
    val beakPath = Path().apply {
        val bx = centerX + bodyWidth * 0.9f + headRadius
        val by = centerY - bodyHeight * 0.5f
        moveTo(bx, by - headRadius * 0.2f)
        lineTo(bx + headRadius * 0.8f, by)
        lineTo(bx, by + headRadius * 0.2f)
        close()
    }
    drawPath(path = beakPath, color = Color(0xFFFF8F00))
}

private fun DrawScope.drawPigeonWing(centerX: Float, centerY: Float, tint: Color, isLeft: Boolean) {
    val wingLength = size.width * 0.35f
    val wingWidth = size.height * 0.15f
    val direction = if (isLeft) -1f else 1f

    val wingPath = Path().apply {
        moveTo(centerX, centerY)
        quadraticBezierTo(
            centerX, centerY + direction * wingLength * 0.5f,
            centerX - wingWidth, centerY + direction * wingLength
        )
        lineTo(centerX + wingWidth, centerY + direction * wingLength * 0.8f)
        quadraticBezierTo(
            centerX + wingWidth * 0.5f, centerY + direction * wingLength * 0.3f,
            centerX, centerY
        )
        close()
    }
    drawPath(path = wingPath, color = tint.copy(alpha = 0.8f))
}

private fun DrawScope.drawPigeonTail(centerX: Float, centerY: Float, tint: Color) {
    val bodyWidth = size.width * 0.35f
    val tailPath = Path().apply {
        moveTo(centerX - bodyWidth * 0.7f, centerY)
        lineTo(centerX - bodyWidth * 1.3f, centerY - size.height * 0.08f)
        lineTo(centerX - bodyWidth * 1.3f, centerY + size.height * 0.08f)
        close()
    }
    drawPath(path = tailPath, color = tint.copy(alpha = 0.7f))
}
