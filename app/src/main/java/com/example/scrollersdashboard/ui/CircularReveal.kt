package com.example.scrollersdashboard.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.hypot

class CircularRevealShape(val progress: Float, val center: Offset) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        if (progress <= 0f) return Outline.Generic(Path())
        val maxRadius = hypot(
            maxOf(center.x, size.width - center.x),
            maxOf(center.y, size.height - center.y)
        )
        val path = Path().apply {
            val r = maxRadius * progress
            addOval(Rect(center.x - r, center.y - r, center.x + r, center.y + r))
        }
        return Outline.Generic(path)
    }
}
