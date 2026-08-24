package com.pigeonpost.ui.screens.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pigeonpost.data.model.MessageStatus
import com.pigeonpost.ui.components.ParchmentBackground
import com.pigeonpost.ui.theme.DeepBrown700
import com.pigeonpost.ui.theme.DeepBrown800
import com.pigeonpost.ui.theme.GoldAccent400
import com.pigeonpost.ui.theme.GoldAccent500
import com.pigeonpost.ui.theme.Parchment100
import com.pigeonpost.ui.theme.Parchment300
import com.pigeonpost.ui.theme.RoyalBlue800
import com.pigeonpost.ui.theme.WaxSealRed400
import com.pigeonpost.ui.theme.WaxSealRed500
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shows pigeon's current position on a stylized old-world map background.
 * Features animated pigeon icon moving along the path, dotted trail line,
 * and origin/destination markers as medieval map pins.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PigeonMapScreen(
    messageId: String,
    onNavigateBack: () -> Unit,
    viewModel: PigeonMapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ParchmentBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Pigeon Tracker",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                // Old-world map canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    OldWorldMap(
                        pigeonProgress = uiState.progress.toFloat(),
                        status = uiState.status,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (uiState.status) {
                                MessageStatus.FLYING -> "Thy pigeon soars through the skies"
                                MessageStatus.DELIVERED -> "Message delivered by faithful pigeon!"
                                MessageStatus.LOST -> "Alas! Thy pigeon has perished in transit"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = when (uiState.status) {
                                MessageStatus.FLYING -> GoldAccent400
                                MessageStatus.DELIVERED -> MaterialTheme.colorScheme.onSurface
                                MessageStatus.LOST -> WaxSealRed500
                            },
                            fontStyle = FontStyle.Italic,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (uiState.status == MessageStatus.FLYING) {
                            Text(
                                text = "Progress: ${(uiState.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "~${String.format("%.1f", uiState.estimatedHoursRemaining)} hours remaining",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom Canvas-based map composable that draws a stylized old-world map appearance
 * with the pigeon path, since we don't have a Google Maps API key.
 */
@Composable
private fun OldWorldMap(
    pigeonProgress: Float,
    status: MessageStatus,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawOldWorldBackground()
        drawMapRoute(pigeonProgress)
        drawOriginMarker()
        drawDestinationMarker()
        if (status != MessageStatus.LOST) {
            drawPigeonOnMap(pigeonProgress)
        } else {
            drawDeathMarker(pigeonProgress)
        }
    }
}

private fun DrawScope.drawOldWorldBackground() {
    // Parchment base
    drawRect(color = Parchment300)

    // Map grid lines (stylized latitude/longitude)
    val gridColor = DeepBrown700.copy(alpha = 0.1f)
    val gridSpacing = size.width / 8

    repeat(9) { i ->
        val x = i * gridSpacing
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 0.5f
        )
    }
    repeat(7) { i ->
        val y = i * (size.height / 6)
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 0.5f
        )
    }

    // Compass rose in corner
    val compassCenter = Offset(size.width * 0.85f, size.height * 0.15f)
    val compassRadius = size.minDimension * 0.06f
    drawCircle(color = GoldAccent400.copy(alpha = 0.3f), radius = compassRadius, center = compassCenter)
    // N pointer
    drawLine(
        color = WaxSealRed400,
        start = compassCenter,
        end = Offset(compassCenter.x, compassCenter.y - compassRadius),
        strokeWidth = 2f
    )
    // S pointer
    drawLine(
        color = DeepBrown800,
        start = compassCenter,
        end = Offset(compassCenter.x, compassCenter.y + compassRadius),
        strokeWidth = 1.5f
    )
    // E and W pointers
    drawLine(
        color = DeepBrown800,
        start = Offset(compassCenter.x - compassRadius, compassCenter.y),
        end = Offset(compassCenter.x + compassRadius, compassCenter.y),
        strokeWidth = 1.5f
    )

    // Stylized land masses (simplified continent shapes)
    val landColor = DeepBrown700.copy(alpha = 0.15f)
    drawCircle(color = landColor, radius = size.width * 0.15f, center = Offset(size.width * 0.2f, size.height * 0.4f))
    drawCircle(color = landColor, radius = size.width * 0.12f, center = Offset(size.width * 0.8f, size.height * 0.5f))

    // Decorative border
    val borderColor = DeepBrown800.copy(alpha = 0.4f)
    drawRect(
        color = borderColor,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
    )
    drawRect(
        color = borderColor.copy(alpha = 0.2f),
        topLeft = Offset(6f, 6f),
        size = androidx.compose.ui.geometry.Size(size.width - 12f, size.height - 12f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
    )
}

private fun DrawScope.drawMapRoute(progress: Float) {
    val startX = size.width * 0.15f
    val startY = size.height * 0.55f
    val endX = size.width * 0.85f
    val endY = size.height * 0.45f

    // Full route (dotted line)
    drawLine(
        color = DeepBrown700.copy(alpha = 0.4f),
        start = Offset(startX, startY),
        end = Offset(endX, endY),
        strokeWidth = 2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
    )

    // Traveled route (solid gold line)
    val currentX = startX + (endX - startX) * progress
    val currentY = startY + (endY - startY) * progress
    drawLine(
        color = GoldAccent500,
        start = Offset(startX, startY),
        end = Offset(currentX, currentY),
        strokeWidth = 3f
    )
}

private fun DrawScope.drawOriginMarker() {
    val x = size.width * 0.15f
    val y = size.height * 0.55f

    // Pin base
    drawCircle(color = RoyalBlue800, radius = 10f, center = Offset(x, y))
    drawCircle(color = Parchment100, radius = 4f, center = Offset(x, y))

    // Pin pole
    drawLine(
        color = RoyalBlue800,
        start = Offset(x, y),
        end = Offset(x, y + 15f),
        strokeWidth = 2f
    )
}

private fun DrawScope.drawDestinationMarker() {
    val x = size.width * 0.85f
    val y = size.height * 0.45f

    // Pin base
    drawCircle(color = WaxSealRed500, radius = 10f, center = Offset(x, y))
    drawCircle(color = Parchment100, radius = 4f, center = Offset(x, y))

    // Pin pole
    drawLine(
        color = WaxSealRed500,
        start = Offset(x, y),
        end = Offset(x, y + 15f),
        strokeWidth = 2f
    )
}

private fun DrawScope.drawPigeonOnMap(progress: Float) {
    val startX = size.width * 0.15f
    val startY = size.height * 0.55f
    val endX = size.width * 0.85f
    val endY = size.height * 0.45f

    val x = startX + (endX - startX) * progress
    val y = startY + (endY - startY) * progress

    // Pigeon icon (simplified bird shape)
    drawCircle(color = DeepBrown800, radius = 8f, center = Offset(x, y))

    // Wings
    val wingSpread = 12f
    drawLine(
        color = DeepBrown800,
        start = Offset(x - wingSpread, y - 4f),
        end = Offset(x, y),
        strokeWidth = 2f
    )
    drawLine(
        color = DeepBrown800,
        start = Offset(x + wingSpread, y - 4f),
        end = Offset(x, y),
        strokeWidth = 2f
    )

    // Glow effect
    drawCircle(
        color = GoldAccent400.copy(alpha = 0.3f),
        radius = 16f,
        center = Offset(x, y)
    )
}

private fun DrawScope.drawDeathMarker(progress: Float) {
    val startX = size.width * 0.15f
    val startY = size.height * 0.55f
    val endX = size.width * 0.85f
    val endY = size.height * 0.45f

    val x = startX + (endX - startX) * progress
    val y = startY + (endY - startY) * progress

    // X mark for death location
    val crossSize = 10f
    drawLine(
        color = WaxSealRed500,
        start = Offset(x - crossSize, y - crossSize),
        end = Offset(x + crossSize, y + crossSize),
        strokeWidth = 3f
    )
    drawLine(
        color = WaxSealRed500,
        start = Offset(x + crossSize, y - crossSize),
        end = Offset(x - crossSize, y + crossSize),
        strokeWidth = 3f
    )
}
