package com.pigeonpost.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pigeonpost.ui.theme.GoldAccent400
import com.pigeonpost.ui.theme.GoldAccent500
import com.pigeonpost.ui.theme.WaxSealRed300
import com.pigeonpost.ui.theme.WaxSealRed400
import com.pigeonpost.ui.theme.WaxSealRed500
import kotlin.math.cos
import kotlin.math.sin

/**
 * Composable that draws a wax seal decoration - a circular red/gold seal
 * with a ribbon effect and embossed appearance.
 */
@Composable
fun WaxSeal(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showRibbon: Boolean = true
) {
    Canvas(modifier = modifier.size(size)) {
        if (showRibbon) {
            drawRibbon()
        }
        drawSealBody()
        drawSealEmbossing()
    }
}

private fun DrawScope.drawRibbon() {
    val centerX = size.width / 2
    val centerY = size.height / 2
    val ribbonWidth = size.width * 0.12f

    val ribbonPath = Path().apply {
        // Left ribbon tail
        moveTo(centerX - ribbonWidth, centerY)
        lineTo(centerX - size.width * 0.4f, size.height * 0.85f)
        lineTo(centerX - size.width * 0.4f + ribbonWidth, size.height * 0.85f)
        lineTo(centerX, centerY)
        close()

        // Right ribbon tail
        moveTo(centerX + ribbonWidth, centerY)
        lineTo(centerX + size.width * 0.4f, size.height * 0.85f)
        lineTo(centerX + size.width * 0.4f - ribbonWidth, size.height * 0.85f)
        lineTo(centerX, centerY)
        close()
    }

    drawPath(
        path = ribbonPath,
        color = WaxSealRed300
    )
}

private fun DrawScope.drawSealBody() {
    val centerX = size.width / 2
    val centerY = size.height / 2
    val radius = size.minDimension * 0.35f

    // Outer irregular edge (wax drip effect)
    val outerPath = Path()
    val points = 24
    for (i in 0..points) {
        val angle = (i.toFloat() / points) * 2 * Math.PI.toFloat()
        val variation = if (i % 3 == 0) 0.92f else if (i % 2 == 0) 1.05f else 1.0f
        val r = radius * variation
        val x = centerX + cos(angle) * r
        val y = centerY + sin(angle) * r
        if (i == 0) outerPath.moveTo(x, y) else outerPath.lineTo(x, y)
    }
    outerPath.close()

    // Draw seal with gradient
    drawPath(
        path = outerPath,
        brush = Brush.radialGradient(
            colors = listOf(WaxSealRed400, WaxSealRed500),
            center = Offset(centerX * 0.9f, centerY * 0.9f),
            radius = radius
        )
    )
}

private fun DrawScope.drawSealEmbossing() {
    val centerX = size.width / 2
    val centerY = size.height / 2
    val innerRadius = size.minDimension * 0.2f

    // Inner circle emboss effect
    drawCircle(
        color = GoldAccent400.copy(alpha = 0.6f),
        radius = innerRadius,
        center = Offset(centerX, centerY)
    )

    // Pigeon silhouette hint (simplified bird shape)
    val birdPath = Path().apply {
        val bx = centerX
        val by = centerY
        val scale = innerRadius * 0.5f

        // Bird body (simple curved shape)
        moveTo(bx - scale * 0.8f, by)
        quadraticBezierTo(bx, by - scale * 0.6f, bx + scale * 0.8f, by)
        quadraticBezierTo(bx, by + scale * 0.3f, bx - scale * 0.8f, by)
        close()

        // Wing
        moveTo(bx, by - scale * 0.1f)
        quadraticBezierTo(bx + scale * 0.3f, by - scale * 0.8f, bx + scale * 0.6f, by - scale * 0.3f)
    }

    drawPath(
        path = birdPath,
        color = GoldAccent500.copy(alpha = 0.8f)
    )
}
