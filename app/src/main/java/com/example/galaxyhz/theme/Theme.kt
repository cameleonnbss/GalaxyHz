package com.example.galaxyhz.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val GalaxyHzColors = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    secondary = NeonEmerald,
    onSecondary = Color.Black,
    tertiary = NeonViolet,
    error = ErrorRed,
    background = Bg0,
    onBackground = Color.White,
    surface = Bg1,
    onSurface = Color.White,
    surfaceVariant = Bg2,
    onSurfaceVariant = Color(0xFFB9C2D8),
    surfaceContainer = Bg2,
    surfaceContainerHigh = Bg3,
    surfaceContainerHighest = Bg3,
    surfaceContainerLow = Bg1,
    surfaceContainerLowest = Bg0,
    outline = Line,
    outlineVariant = Line
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object ExpressiveShapes {
    val card = RoundedCornerShape(24.dp)
    val cardBig = RoundedCornerShape(32.dp)
    val chip = RoundedCornerShape(16.dp)
    val pill = RoundedCornerShape(100)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GalaxyHzTheme(content: @Composable () -> Unit) {
    // Real Material 3 Expressive: physics-based spring motion for every component.
    MaterialTheme(
        colorScheme = GalaxyHzColors,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}
