package com.example.scrollersdashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// --- UI Constants ---
val SharpRoundedCorner = 28.dp // M3 ExtraLarge standard

// Base Colors (Can be used as fallbacks if not using MaterialTheme directly)
val DeepCharcoal = Color(0xFF18181B) 
val LightGrey = Color(0xFFE0E0E5)

// Element Base Colors
val ElementDark = Color(0xFF18181B)
val ElementLight = Color(0xFFE0E0E5)

val GlassColor = Color(0xFF1A1A1E)

// Neon Gradients
val InstaNeonGradient = listOf(Color(0xFFFF8C00), Color(0xFFE1306C), Color(0xFFC13584))
val YTNeonGradient = listOf(Color(0xFFFF0000), Color(0xFFFF4D4D))
val AnalysisButtonGradient = listOf(Color(0xFF6A11CB), Color(0xFF2575FC), Color(0xFFFF0000), Color(0xFFFF8C00))

// Custom Toggle Colors
val ToggleTrackOn = Color(0xFF34C759)
val ToggleTrackOff = Color(0xFF333333)

@Composable
fun Modifier.modernGlassy(): Modifier = this.then(
    Modifier
        .clip(MaterialTheme.shapes.extraLarge)
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .border(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            MaterialTheme.shapes.extraLarge
        )
)
