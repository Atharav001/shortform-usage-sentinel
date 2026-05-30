package com.example.scrollersdashboard.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class DayData(
    val day: String,
    val count: Int
)

fun getBarColors(count: Int) = tierGradient(scrollCountTier(count))

fun getBrainEmojiForCount(count: Int): String = tierBrainEmoji(scrollCountTier(count))

@Composable
fun AnimatedBarChart(
    data: List<DayData>,
    modifier: Modifier = Modifier,
    labelInterval: Int = 1,
    highlightedIndices: Set<Int> = emptySet()
) {
    AnimatedBarChart(
        data = data.map { ChartEntry(it.day, it.count) },
        modifier = modifier,
        labelInterval = labelInterval,
        highlightedIndices = highlightedIndices,
        showEmojis = true
    )
}

@Composable
fun AnimatedBarChart(
    data: List<ChartEntry>,
    modifier: Modifier = Modifier,
    labelInterval: Int = 1,
    highlightedIndices: Set<Int> = emptySet(),
    showEmojis: Boolean = true
) {
    EnhancedActivityBarChart(
        data = data,
        modifier = modifier,
        labelInterval = labelInterval,
        highlightedIndices = highlightedIndices,
        showEmojis = showEmojis,
        staggerBars = true
    )
}

/** Small brain mascot for week chart bars — replaces emoji clutter. */
@Composable
fun ChartBrainMascot(
    scrollCount: Int,
    delayMs: Long,
    modifier: Modifier = Modifier
) {
    val state = remember(scrollCount) { brainStateForScrollCount(scrollCount) }
    val visible = remember { Animatable(0f) }

    LaunchedEffect(scrollCount, delayMs) {
        visible.snapTo(0f)
        delay(delayMs)
        visible.animateTo(
            targetValue = 1.2f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessHigh
            )
        )
        visible.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.55f,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    BoxWithScale(
        scale = visible.value.coerceIn(0f, 1.25f),
        modifier = modifier.size(36.dp)
    ) {
        val glow = state.glowColor ?: state.fallbackColor
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(glow.copy(alpha = 0.35f), Color.Transparent),
                    radius = size.minDimension * 0.55f
                ),
                radius = size.minDimension * 0.55f,
                center = center
            )
        }
        BrainLottieLayer(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
        )
    }
}

@Composable
private fun BoxWithScale(
    scale: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.scale(scale),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        content()
    }
}
