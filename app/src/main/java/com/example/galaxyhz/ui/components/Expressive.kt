package com.example.galaxyhz.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.galaxyhz.theme.Bg0
import com.example.galaxyhz.theme.ExpressiveShapes
import com.example.galaxyhz.theme.Line

@Composable
fun SectionHeader(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp
    )
}

/** Selectable card used for one panel mode / resolution / option. */
@Composable
fun ModeCard(
    title: String,
    subtitle: String,
    details: String,
    isSelected: Boolean,
    accent: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val container =
        if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ExpressiveShapes.card)
            .border(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected) accent else Line,
                ExpressiveShapes.card
            )
            .clickable(enabled = enabled, onClick = onClick),
        shape = ExpressiveShapes.card,
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (details.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(details, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
            RadioButton(selected = isSelected, onClick = onClick, enabled = enabled)
        }
    }
}

/** Big status card with the animated FPS readout. */
@Composable
fun StatusHero(
    fps: Int,
    suffix: String,
    line1: String,
    line2: String,
    accent: Color,
    badge: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = 0.4f), ExpressiveShapes.cardBig),
        shape = ExpressiveShapes.cardBig,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    badge,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "$fps",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent
                    )
                    Text(
                        suffix,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(line1, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(line2, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .background(
                        Brush.radialGradient(listOf(accent.copy(alpha = 0.35f), Color.Transparent)),
                        CircleShape
                    )
                    .border(2.dp, accent, CircleShape)
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    suffix.trim(),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }
    }
}

/** Expressive loading overlay using the M3 Expressive LoadingIndicator. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingOverlay(show: Boolean) {
    if (!show) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg0.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Box(Modifier.padding(28.dp), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
        }
    }
}

@Composable
fun InfoNote(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text, fontSize = 12.sp, color = color)
}

/**
 * M3 Expressive single-choice segmented button row - the signature control
 * for switching between the main rates.
 */
@Composable
fun HzSegmentedRow(
    options: List<Int>,
    selected: Int,
    enabled: Boolean = true,
    onSelect: (Int) -> Unit
) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, hz ->
            SegmentedButton(
                selected = selected == hz,
                onClick = { onSelect(hz) },
                enabled = enabled,
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primary,
                    activeContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                label = {
                    Text(
                        "$hz",
                        fontWeight = if (selected == hz) FontWeight.Black else FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            )
        }
    }
}

@Composable
fun TitledCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveShapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

/** Lock-rate card with a live verified/not-verified status line. */
@Composable
fun LockCard(
    locked: Boolean,
    busy: Boolean,
    hasRoot: Boolean,
    lang: String,
    onLock: () -> Unit
) {
    TitledCard("ANTI-FLICKER LOCK") {
        Text(
            if (locked) "Rate is locked (no idle/content downclocking)"
            else "Rate may drop when content is static (flicker risk)",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        androidx.compose.material3.Button(
            onClick = onLock,
            enabled = !busy && hasRoot,
            shape = ExpressiveShapes.pill,
            modifier = Modifier.fillMaxWidth()
        ) {
        Text(
            com.example.galaxyhz.ui.L10n.t("lock_rate", lang),
            fontWeight = FontWeight.Bold
        )
        }
    }
}
