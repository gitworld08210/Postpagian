package com.pigeonpost.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pigeonpost.ui.theme.DeepBrown700
import com.pigeonpost.ui.theme.Parchment100
import com.pigeonpost.ui.theme.Parchment200
import com.pigeonpost.ui.theme.Parchment300
import kotlin.random.Random

/**
 * A reusable composable that renders a parchment-textured background
 * with aged edges and subtle paper grain effect using Canvas drawing.
 */
@Composable
fun ParchmentBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawParchmentTexture()
            drawAgedEdges()
        }
        content()
    }
}

private fun DrawScope.drawParchmentTexture() {
    // Base parchment gradient
    val gradientBrush = Brush.radialGradient(
        colors = listOf(
            Parchment200,
            Parchment100,
            Parchment300
        ),
        center = Offset(size.width * 0.5f, size.height * 0.4f),
        radius = size.maxDimension * 0.8f
    )
    drawRect(brush = gradientBrush)

    // Subtle grain effect - draw small semi-transparent specks
    val random = Random(42) // Fixed seed for consistent rendering
    val speckColor = Color(0x08000000)
    repeat(200) {
        val x = random.nextFloat() * size.width
        val y = random.nextFloat() * size.height
        val radius = random.nextFloat() * 2f + 0.5f
        drawCircle(
            color = speckColor,
            radius = radius,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawAgedEdges() {
    // Darkened edges to simulate aging
    val edgeWidth = size.width * 0.08f
    val edgeColor = DeepBrown700.copy(alpha = 0.15f)

    // Top edge
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(edgeColor, Color.Transparent),
            startY = 0f,
            endY = edgeWidth
        )
    )

    // Bottom edge
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, edgeColor),
            startY = size.height - edgeWidth,
            endY = size.height
        )
    )

    // Left edge
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(edgeColor, Color.Transparent),
            startX = 0f,
            endX = edgeWidth
        )
    )

    // Right edge
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, edgeColor),
            startX = size.width - edgeWidth,
            endX = size.width
        )
    )
}
