package com.example.scrollersdashboard.ui

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrollersdashboard.ui.theme.AppGradients
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private val RingBounceEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

val InstagramRingGradient = listOf(Color(0xFFF56040), Color(0xFFFCAF45))
val YouTubeRingGradient = listOf(Color(0xFFFF0000), Color(0xFFCC0000))

@Composable
fun UsageProgressRing(
    platform: String,
    scrollCount: Int,
    dailyGoal: Int,
    screenTime: String,
    modifier: Modifier = Modifier,
    onTapExpand: (() -> Unit)? = null
) {
    val isInstagram = platform.lowercase() == "instagram"
    val progressColors = if (isInstagram) InstagramRingGradient else YouTubeRingGradient
    val targetProgress = (scrollCount.toFloat() / dailyGoal.coerceAtLeast(1)).coerceIn(0f, 1f)
    val isGoalComplete = scrollCount >= dailyGoal && dailyGoal > 0

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "ringProgress"
    )

    var showDetails by remember { mutableStateOf(false) }
    var previousCount by remember { mutableIntStateOf(scrollCount) }
    var showSparkle by remember { mutableStateOf(false) }
    val goalPulse = remember { Animatable(1f) }

    LaunchedEffect(scrollCount) {
        if (scrollCount != previousCount) {
            previousCount = scrollCount
        }
    }

    LaunchedEffect(isGoalComplete) {
        if (isGoalComplete) {
            goalPulse.snapTo(1f)
            goalPulse.animateTo(1.08f, androidx.compose.animation.core.tween(200, easing = RingBounceEasing))
            goalPulse.animateTo(1f, androidx.compose.animation.core.tween(200))
            showSparkle = true
            delay(900)
            showSparkle = false
        }
    }

    val platformLabel = platform.replaceFirstChar { it.uppercase() }

    Box(
        modifier = modifier
            .size(140.dp)
            .graphicsLayer {
                scaleX = goalPulse.value
                scaleY = goalPulse.value
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onTapExpand?.invoke() ?: run { showDetails = true }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f
            )
            val arcSize = Size(diameter, diameter)
            val startAngle = -90f
            val sweep = 360f * animatedProgress

            drawArc(
                color = Color.White.copy(alpha = 0.08f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            if (animatedProgress > 0.01f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = progressColors + progressColors.first(),
                        center = Offset(size.width / 2f, size.height / 2f)
                    ),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth + 6.dp.toPx(), cap = StrokeCap.Round),
                    alpha = 0.35f
                )

                drawArc(
                    brush = Brush.sweepGradient(
                        colors = progressColors + progressColors.first(),
                        center = Offset(size.width / 2f, size.height / 2f)
                    ),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && animatedProgress > 0.01f) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(8.dp)
                    .graphicsLayer { alpha = 0.7f }
            ) {
                val strokeWidth = 8.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset(
                    (size.width - diameter) / 2f,
                    (size.height - diameter) / 2f
                )
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = progressColors + progressColors.first(),
                        center = Offset(size.width / 2f, size.height / 2f)
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    alpha = 0.55f
                )
            }
        }

        if (showSparkle) {
            GoalSparkleOverlay(
                modifier = Modifier.fillMaxSize(),
                color = progressColors.first()
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedGlowNumberText(
                text = scrollCount.toString(),
                glowColor = progressColors.first(),
                modifier = Modifier,
                style = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black
                )
            )
            Text(
                text = "/$dailyGoal",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (showDetails) {
        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = { Text("$platformLabel Breakdown", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Scrolls: $scrollCount / $dailyGoal\nScreen time: $screenTime\n\nDetailed analytics coming soon.",
                    color = Color.White.copy(alpha = 0.85f)
                )
            },
            confirmButton = {
                TextButton(onClick = { showDetails = false }) {
                    Text("OK")
                }
            },
            containerColor = Color(0xFF1B1B22)
        )
    }
}

@Composable
private fun GoalSparkleOverlay(modifier: Modifier, color: Color) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, androidx.compose.animation.core.tween(900))
    }
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.minDimension * 0.42f
        repeat(12) { i ->
            val angle = Math.toRadians((i * 30f).toDouble())
            val t = progress.value
            val x = cx + (cos(angle) * radius * t).toFloat()
            val y = cy + (sin(angle) * radius * t).toFloat()
            drawCircle(
                color = color.copy(alpha = (1f - t) * 0.9f),
                radius = 3f * (1f - t * 0.5f),
                center = Offset(x, y)
            )
        }
    }
}
