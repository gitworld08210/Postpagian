package com.pigeonpost.ui.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pigeonpost.ui.theme.DeepBrown700
import com.pigeonpost.ui.theme.DeepBrown900
import com.pigeonpost.ui.theme.GoldAccent400
import com.pigeonpost.ui.theme.Parchment100
import com.pigeonpost.ui.theme.Parchment200
import kotlinx.coroutines.delay

/**
 * Pigeon delivery animation overlay that shows when a received message
 * transitions from FLYING to DELIVERED. Displays a pigeon landing,
 * dropping a scroll that unrolls to reveal the message text.
 */
@Composable
fun PigeonDeliveryAnimation(
    messageText: String,
    visible: Boolean,
    onDismiss: () -> Unit
) {
    // Pigeon landing offset (starts from top, lands in center)
    val pigeonOffset = remember { Animatable(-200f) }
    // Scroll alpha (fades in after pigeon lands)
    val scrollAlpha = remember { Animatable(0f) }
    // Message text alpha (appears after scroll unrolls)
    val messageAlpha = remember { Animatable(0f) }

    var showOverlay by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            showOverlay = true
            // Reset animations
            pigeonOffset.snapTo(-200f)
            scrollAlpha.snapTo(0f)
            messageAlpha.snapTo(0f)

            // Phase 1: Pigeon lands (0.8s)
            pigeonOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 800, easing = LinearEasing)
            )

            // Phase 2: Scroll appears (0.4s)
            scrollAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400)
            )

            // Phase 3: Message text reveals (0.5s)
            delay(200)
            messageAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500)
            )

            // Hold for a moment then dismiss
            delay(1000)
            showOverlay = false
            onDismiss()
        } else {
            showOverlay = false
        }
    }

    AnimatedVisibility(
        visible = showOverlay,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 3.dp,
                        color = GoldAccent400,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .background(Parchment200)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Pigeon arriving animation
                Box(
                    modifier = Modifier
                        .offset { IntOffset(0, pigeonOffset.value.toInt()) }
                ) {
                    PigeonFlyingAnimation(
                        size = 64.dp,
                        tint = DeepBrown900
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // "Scroll dropped" text
                Text(
                    text = "\uD83D\uDCDC",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.alpha(scrollAlpha.value)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "A pigeon has arrived!",
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepBrown900,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.alpha(scrollAlpha.value)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Unrolled scroll with message
                Box(
                    modifier = Modifier
                        .alpha(messageAlpha.value)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Parchment100)
                        .border(
                            width = 1.dp,
                            color = GoldAccent400.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = messageText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = DeepBrown700,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
