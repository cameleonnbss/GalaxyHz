package com.example.galaxyhz.ui.components

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.galaxyhz.theme.GradCyan
import com.example.galaxyhz.theme.GradEmerald
import com.example.galaxyhz.theme.GradViolet
import com.example.galaxyhz.theme.NeonCyan

/**
 * Material 3 Expressive shape assets (MaterialShapes API is newer than our
 * compose pin, so the iconic shapes are reproduced here with equivalent
 * geometry - same silhouettes, spring-animated by MotionScheme.expressive()).
 */
object MaterialShapes {
    /** The iconic M3 Expressive "cookie" / flower with 12 scallops. */
    val cookie12: Shape = GenericShape { size, _ ->
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = minOf(cx, cy)
        val petals = 12
        val steps = 240
        for (i in 0..steps) {
            val t = i * (2.0 * Math.PI / steps)
            val wobble = 1.0 + 0.14 * Math.cos(petals * t)
            val x = (cx + r * wobble * Math.cos(t)).toFloat()
            val y = (cy + r * wobble * Math.sin(t)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    /** Soft squircle used for hero badges. */
    val squircle = RoundedCornerShape(28.dp)

    /** Angular "burst" corner style for chips. */
    val burst = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp)
}

/** Gradient accent wash painted behind expressive sections. */
@Composable
fun AccentWash(
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val partner = when (accent) {
        NeonCyan -> GradCyan
        else -> GradViolet
    }
    Box(
        modifier
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.10f), partner.copy(alpha = 0.05f), Color.Transparent)
                )
            )
    ) { content() }
}

/** Soft pulsing glow dot - draws the eye without being loud. */
@Composable
fun PulsingGlow(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "glow")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "glowAlpha"
    )
    Box(modifier.size(10.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(22.dp)) {
            drawCircle(color.copy(alpha = alpha * 0.35f), radius = size.minDimension / 2f)
        }
        Box(
            Modifier
                .size(8.dp)
                .background(color.copy(alpha = 0.5f + alpha * 0.5f), CircleShape)
                .border(1.dp, color, CircleShape)
        )
    }
}

/**
 * Animated FPS readout: the number springs between values (Expressive motion)
 * and a thin progress ring fills to the rate relative to the panel's max.
 */
@Composable
fun AnimatedFpsBar(
    fps: Int,
    maxHz: Int,
    accent: Color
) {
    val target = if (maxHz > 0) (fps.coerceAtLeast(0)) / maxHz.toFloat() else 0f
    val progress by animateFloatAsState(
        targetValue = target.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 120f, visibilityThreshold = 0.001f),
        label = "fpsProgress"
    )
    Column {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
        ) {
            val bar = size.width * progress
            drawRoundRect(
                color = accent.copy(alpha = 0.22f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
            )
            if (bar > 0) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.6f))),
                    size = androidx.compose.ui.geometry.Size(bar, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
                )
            }
        }
    }
}

/** Small rotating "scalloped cookie" used as an expressive decorative accent. */
@Composable
fun RotatingCookie(accent: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "cookie")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing)),
        label = "cookieAngle"
    )
    Box(modifier.rotate(angle)) {
        Box(
            Modifier
                .size(40.dp)
                .background(accent.copy(alpha = 0.16f), MaterialShapes.cookie12)
        )
    }
}

/** Live rate chip with the Expressive burst corner silhouette. */
@Composable
fun SpringyHzChip(hz: Int, accent: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(accent.copy(alpha = 0.14f), MaterialShapes.burst)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            "$hz",
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = accent
        )
        Text(" Hz", fontSize = 11.sp, color = accent.copy(alpha = 0.8f))
    }
}

/** Shared accent-derived gradient for status heroes. */
fun accentGradient(accent: Color): Brush =
    Brush.linearGradient(listOf(accent.copy(alpha = 0.16f), GradViolet.copy(alpha = 0.05f), Color.Transparent))

/** Expressive section wrapper: cookie-gradient wash with soft border. */
@Composable
fun ExpressiveSection(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(accentGradient(NeonCyan))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
            .padding(12.dp)
    ) { content() }
}

/** Row of expressive burst-cornered tinted action buttons. */
@Composable
fun ExpressiveButtons(
    actions: List<Triple<String, Color, () -> Unit>>,
    enabled: Boolean = true
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.forEach { (label, color, action) ->
            Button(
                onClick = action,
                enabled = enabled,
                shape = MaterialShapes.burst,
                colors = ButtonDefaults.buttonColors(
                    containerColor = color.copy(alpha = 0.16f),
                    contentColor = color
                )
            ) {
                Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
