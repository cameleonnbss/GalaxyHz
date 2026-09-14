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
    primaryContainer = GradCyan,
    onPrimaryContainer = Color.White,
    secondary = NeonEmerald,
    onSecondary = Color.Black,
    secondaryContainer = GradEmerald,
    onSecondaryContainer = Color.White,
    tertiary = NeonViolet,
    onTertiary = Color.Black,
    tertiaryContainer = GradViolet,
    onTertiaryContainer = Color.White,
    error = ErrorRed,
    errorContainer = Color(0xFF5C1A1A),
    onErrorContainer = Color(0xFFFFDAD6),
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
    surfaceTint = NeonCyan,
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
