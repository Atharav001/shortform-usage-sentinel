package com.example.scrollersdashboard.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object AppGradients {
    val WarmBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF2D1B2E),
            Color(0xFF1A1625),
            Color(0xFF0F0D1E)
        )
    )

    val EmberGlow = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF3D2C3D),
            Color(0xFF1F1625),
            Color(0xFF0D0A14)
        )
    )

    val HeroCard = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.10f),
            Color.White.copy(alpha = 0.05f)
        )
    )

    val InstagramRing = Brush.sweepGradient(
        colors = listOf(
            Color(0xFFF56040),
            Color(0xFFFCAF45),
            Color(0xFFF56040)
        )
    )

    val YouTubeRing = Brush.sweepGradient(
        colors = listOf(
            Color(0xFFFF0000),
            Color(0xFFCC0000),
            Color(0xFFFF0000)
        )
    )

    val HabitsAccent = Brush.linearGradient(
        colors = listOf(Color(0xFF6EE7B7), Color(0xFF34D399))
    )

    val TasksAccent = Brush.linearGradient(
        colors = listOf(Color(0xFFFCD34D), Color(0xFFF59E0B))
    )

    val SoftDelete = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF78716C).copy(alpha = 0.35f),
            Color(0xFFA8A29E).copy(alpha = 0.55f)
        )
    )
}
