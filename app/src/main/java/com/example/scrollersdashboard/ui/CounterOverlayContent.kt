package com.example.scrollersdashboard.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrollersdashboard.ui.theme.InstagramGradient
import com.example.scrollersdashboard.ui.theme.YouTubeGradient
import kotlinx.coroutines.delay

data class OverlayCountStyle(
    val accentColor: Color,
    val glowAlpha: Float
)

fun overlayCountStyleForCount(count: Int): OverlayCountStyle = when {
    count <= 20 -> OverlayCountStyle(Color(0xFF34D399), 0.35f)
    count <= 50 -> OverlayCountStyle(Color(0xFFFBBF24), 0.4f)
    count <= 100 -> OverlayCountStyle(Color(0xFFFB923C), 0.5f)
    else -> OverlayCountStyle(Color(0xFFEF4444), 0.6f)
}

@Composable
fun CounterOverlayContent(
    count: Int,
    app: String,
    limit: Int = 100,
    showBrain: Boolean = true,
    overlayAlpha: Float = 0.85f,
    modifier: Modifier = Modifier
) {
    val style = remember(count) { overlayCountStyleForCount(count) }
    val brainState = remember(count) { brainStateForScrollCount(count) }
    val progress = if (limit > 0) count.toFloat() / limit else 0f
    val isNearLimit = progress >= 0.8f

    val animatedCount by animateFloatAsState(
        targetValue = count.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "overlayCount"
    )

    var previousCount by remember { mutableIntStateOf(count) }
    val pulseScale = remember { Animatable(1f) }
    val shakeOffset = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "limitShake")
    val shakeWave by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(100), RepeatMode.Reverse),
        label = "shake"
    )

    LaunchedEffect(count) {
        if (count != previousCount) {
            previousCount = count
            pulseScale.snapTo(1f)
            pulseScale.animateTo(1.15f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh))
            pulseScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
    }

    LaunchedEffect(isNearLimit) {
        if (isNearLimit) {
            repeat(2) {
                shakeOffset.animateTo(4f, tween(50))
                shakeOffset.animateTo(-4f, tween(50))
            }
            shakeOffset.animateTo(0f, tween(80))
        }
    }

    val isInstagram = app.contains("Instagram", ignoreCase = true)
    val pillShape = RoundedCornerShape(20.dp)
    val borderColor = if (isNearLimit) Color(0xFFEF4444) else Color.White.copy(alpha = 0.2f)

    Box(
        modifier = modifier
            .graphicsLayer { translationX = if (isNearLimit) shakeWave else shakeOffset.value }
            .scale(pulseScale.value)
            .widthIn(min = 100.dp, max = 200.dp)
            .height(40.dp)
            .shadow(12.dp, pillShape, spotColor = Color.Black.copy(alpha = 0.6f))
            .clip(pillShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = overlayAlpha.coerceIn(0.5f, 1f)))
        )

        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        style.accentColor.copy(alpha = style.glowAlpha),
                        Color.Transparent
                    ),
                    radius = size.minDimension * 1.2f
                ),
                radius = size.minDimension,
                center = center
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, borderColor, pillShape)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        if (isInstagram) Brush.linearGradient(InstagramGradient)
                        else Brush.linearGradient(YouTubeGradient)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isInstagram) Icons.Rounded.CameraAlt else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = animatedCount.toInt().toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = style.accentColor
            )

            if (showBrain) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.size(26.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(
                                    (brainState.glowColor ?: brainState.fallbackColor).copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            ),
                            radius = size.minDimension * 0.55f,
                            center = center
                        )
                    }
                    BrainLottieLayer(
                        state = brainState,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
