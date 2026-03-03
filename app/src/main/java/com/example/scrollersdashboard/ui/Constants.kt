package com.example.scrollersdashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// --- UI Constants ---
val SharpRoundedCorner = 24.dp 
val DeepCharcoal = Color(0xFF0F0F11) // Match image background
val GlassColor = Color(0xFF1A1A1E)   // Match image panel background
val LightGrey = Color(0xFFF2F2F7)

// Neon Gradients
val InstaNeonGradient = listOf(Color(0xFFFF8C00), Color(0xFFE1306C), Color(0xFFC13584))
val YTNeonGradient = listOf(Color(0xFFFF0000), Color(0xFFFF4D4D))
val AnalysisButtonGradient = listOf(Color(0xFF6A11CB), Color(0xFF2575FC), Color(0xFFFF0000), Color(0xFFFF8C00)) // Multi-stop gradient like image

// Custom Toggle Colors
val ToggleTrackOn = Color(0xFF34C759)
val ToggleTrackOff = Color(0xFF333333)

// --- Helper Extensions ---
fun Modifier.modernGlassy(isDarkMode: Boolean): Modifier = this
    .clip(RoundedCornerShape(SharpRoundedCorner))
    .background(
        if (isDarkMode) Color(0xFF1A1A1E) else Color.White
    )
    .border(
        1.dp,
        if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
        RoundedCornerShape(SharpRoundedCorner)
    )
