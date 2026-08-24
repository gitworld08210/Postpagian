package com.pigeonpost.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.pigeonpost.ui.theme.DeepBrown700
import com.pigeonpost.ui.theme.DeepBrown800
import com.pigeonpost.ui.theme.Parchment100
import com.pigeonpost.ui.theme.Parchment200
import com.pigeonpost.ui.theme.Parchment300

/**
 * A composable that wraps content in a scroll/parchment-styled container
 * with rolled edges at top and bottom to simulate an ancient scroll.
 */
@Composable
fun ScrollContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawScrollBody()
            drawScrollRolls()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            content()
        }
    }
}

private fun DrawScope.drawScrollBody() {
    val rollHeight = 20.dp.toPx()

    // Main parchment body
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Parchment300,
                Parchment200,
                Parchment100,
                Parchment200,
                Parchment300
            )
        ),
        topLeft = Offset(8.dp.toPx(), rollHeight),
        size = Size(
            size.width - 16.dp.toPx(),
            size.height - rollHeight * 2
        ),
        cornerRadius = CornerRadius(4.dp.toPx())
    )

    // Subtle border
    drawRoundRect(
        color = DeepBrown700.copy(alpha = 0.3f),
        topLeft = Offset(8.dp.toPx(), rollHeight),
        size = Size(
            size.width - 16.dp.toPx(),
            size.height - rollHeight * 2
        ),
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
    )
}

private fun DrawScope.drawScrollRolls() {
    val rollHeight = 20.dp.toPx()
    val rollInset = 4.dp.toPx()

    // Top roll
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                DeepBrown700,
                DeepBrown800,
                DeepBrown700
            )
        ),
        topLeft = Offset(rollInset, 0f),
        size = Size(size.width - rollInset * 2, rollHeight),
        cornerRadius = CornerRadius(rollHeight / 2)
    )

    // Top roll highlight
    drawRoundRect(
        color = Color.White.copy(alpha = 0.15f),
        topLeft = Offset(rollInset, 2.dp.toPx()),
        size = Size(size.width - rollInset * 2, rollHeight * 0.4f),
        cornerRadius = CornerRadius(rollHeight / 2)
    )

    // Bottom roll
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                DeepBrown700,
                DeepBrown800,
                DeepBrown700
            )
        ),
        topLeft = Offset(rollInset, size.height - rollHeight),
        size = Size(size.width - rollInset * 2, rollHeight),
        cornerRadius = CornerRadius(rollHeight / 2)
    )

    // Bottom roll highlight
    drawRoundRect(
        color = Color.White.copy(alpha = 0.15f),
        topLeft = Offset(rollInset, size.height - rollHeight + 2.dp.toPx()),
        size = Size(size.width - rollInset * 2, rollHeight * 0.4f),
        cornerRadius = CornerRadius(rollHeight / 2)
    )
}
