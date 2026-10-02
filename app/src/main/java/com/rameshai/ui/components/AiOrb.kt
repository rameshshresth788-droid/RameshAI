package com.rameshai.ui.components

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.rameshai.ui.theme.OrbError
import com.rameshai.ui.theme.OrbIdle
import com.rameshai.ui.theme.OrbListening
import com.rameshai.ui.theme.OrbSpeaking
import com.rameshai.ui.theme.OrbThinking
import com.rameshai.voice.AssistantState

/**
 * Lightweight, battery-conscious orb: a single Canvas draw call per frame using
 * radial gradients and simple trig, no bitmaps/3D/particle systems. All motion
 * is driven by one [rememberInfiniteTransition], which Compose pauses
 * automatically when the composable leaves composition (e.g. app backgrounded).
 */
@Composable
fun AiOrb(state: AssistantState, amplitude: Float = 0f, modifier: Modifier = Modifier) {
    val color = when (state) {
        AssistantState.IDLE -> OrbIdle
        AssistantState.LISTENING -> OrbListening
        AssistantState.THINKING -> OrbThinking
        AssistantState.SPEAKING -> OrbSpeaking
        AssistantState.ERROR -> OrbError
    }

    val infinite = rememberInfiniteTransition(label = "orb")

    val pulse by infinite.animateFloat(
        initialValue = 0.9f,
        targetValue = if (state == AssistantState.LISTENING) 1f + amplitude.coerceIn(0f, 0.4f) else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state == AssistantState.IDLE) 1800 else 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state == AssistantState.THINKING) 1200 else 6000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Canvas(modifier = modifier.size(220.dp)) {
        val radius = (size.minDimension / 2.4f) * pulse.toFloat()
        val center = Offset(size.width / 2f, size.height / 2f)

        rotate(rotation, pivot = center) {
            // Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0f)),
                    center = center,
                    radius = radius * 1.8f
                ),
                radius = radius * 1.8f,
                center = center
            )
            // Core orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.95f), color.copy(alpha = 0.55f)),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
            // Thinking-state ring of dots
            if (state == AssistantState.THINKING) {
                for (i in 0 until 8) {
                    val angle = (i * 45f) * (Math.PI / 180f)
                    val dotRadius = radius * 1.35f
                    val dx = center.x + dotRadius * kotlin.math.cos(angle).toFloat()
                    val dy = center.y + dotRadius * kotlin.math.sin(angle).toFloat()
                    drawCircle(color = color.copy(alpha = 0.8f), radius = 6f, center = Offset(dx, dy))
                }
            }
        }
    }
}
