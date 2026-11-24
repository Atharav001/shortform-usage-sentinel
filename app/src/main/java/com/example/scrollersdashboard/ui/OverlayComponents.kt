package com.example.scrollersdashboard.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrollersdashboard.OverlayDaySummary
import com.example.scrollersdashboard.R
import com.example.scrollersdashboard.ui.theme.CobaltPremium
import com.example.scrollersdashboard.ui.theme.Gray400
import com.example.scrollersdashboard.ui.theme.Gray500
import com.example.scrollersdashboard.ui.theme.IndigoPremium
import kotlinx.coroutines.delay

/** Glow tier from combined daily scrolls across both apps. */
fun overlayGlowStyleForDailyTotal(totalDailyScrolls: Int): OverlayCountStyle =
    overlayCountStyleForCount(totalDailyScrolls)

@Composable
fun PremiumCounterCapsule(
    appCount: Int,
    totalDailyScrolls: Int,
    app: String,
    showBrain: Boolean,
    overlayAlpha: Float,
    modifier: Modifier = Modifier
) {
    val dailyStyle = remember(totalDailyScrolls) { overlayGlowStyleForDailyTotal(totalDailyScrolls) }
    val brainState = remember(totalDailyScrolls) { brainStateForScrollCount(totalDailyScrolls) }
    val isInstagram = app.contains("Instagram", ignoreCase = true)

    var displayCount by remember { mutableIntStateOf(appCount) }
    val animatedDisplay by animateFloatAsState(
        targetValue = displayCount.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessHigh, dampingRatio = 0.72f),
        label = "capsuleCount"
    )

    var previousCount by remember { mutableIntStateOf(appCount) }
    val pulseScale = remember { Animatable(1f) }
    var milestoneFlash by remember { androidx.compose.runtime.mutableStateOf(false) }

    LaunchedEffect(appCount) {
        if (appCount != previousCount) {
            previousCount = appCount
            displayCount = appCount
            pulseScale.snapTo(1f)
            pulseScale.animateTo(1.18f, tween(140, easing = androidx.compose.animation.core.FastOutSlowInEasing))
            pulseScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
            if (appCount > 0 && appCount % 10 == 0) {
                milestoneFlash = true
                delay(520)
                milestoneFlash = false
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "capsuleAmbient")
    val breathe by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "breathe"
    )
    val ringPulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = if (milestoneFlash) 1f else 0.65f,
        animationSpec = infiniteRepeatable(
            tween(if (milestoneFlash) 280 else 1400),
            RepeatMode.Reverse
        ),
        label = "ring"
    )

    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .scale(pulseScale.value * breathe)
            .shadow(
                elevation = 10.dp,
                shape = shape,
                spotColor = dailyStyle.accentColor.copy(alpha = 0.45f),
                ambientColor = Color.Black.copy(alpha = 0.5f)
            )
            .clip(shape)
    ) {
        Box(
            modifier = Modifier
                .background(Color(0xFF0C0C12).copy(alpha = overlayAlpha.coerceIn(0.72f, 0.94f)))
        )

        Canvas(modifier = Modifier.matchParentSize()) {
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        dailyStyle.accentColor.copy(alpha = dailyStyle.glowAlpha * 0.9f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.55f),
                    radius = size.maxDimension * 0.85f
                ),
                cornerRadius = CornerRadius(14.dp.toPx())
            )
        }

        Canvas(modifier = Modifier.matchParentSize()) {
            val strokeW = 2.dp.toPx()
            drawRoundRect(
                color = dailyStyle.accentColor.copy(alpha = ringPulse * 0.55f),
                cornerRadius = CornerRadius(14.dp.toPx()),
                style = Stroke(width = strokeW * 1.6f)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.14f),
                cornerRadius = CornerRadius(14.dp.toPx()),
                style = Stroke(width = strokeW)
            )
        }

        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            AppBrandIcon(isInstagram = isInstagram, modifier = Modifier.size(28.dp))

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = animatedDisplay.toInt().toString(),
                color = dailyStyle.accentColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.graphicsLayer {
                    if (milestoneFlash) {
                        alpha = 0.85f + ringPulse * 0.15f
                    }
                }
            )

            if (showBrain) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.size(24.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(
                                    (brainState.glowColor ?: brainState.fallbackColor).copy(alpha = 0.45f),
                                    Color.Transparent
                                )
                            ),
                            radius = size.minDimension * 0.55f,
                            center = center
                        )
                    }
                    BrainLottieLayer(state = brainState, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
fun AppBrandIcon(isInstagram: Boolean, modifier: Modifier = Modifier) {
    val squircle = RoundedCornerShape(8.dp)
    Image(
        painter = painterResource(
            if (isInstagram) R.drawable.instagram_logo else R.drawable.youtube_logo
        ),
        contentDescription = null,
        modifier = modifier
            .clip(squircle)
            .background(Color.Black.copy(alpha = 0.25f))
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), squircle)
            .padding(2.dp)
    )
}

@Composable
fun OverlayDashboardPopup(
    summary: OverlayDaySummary,
    onDismiss: () -> Unit,
    onOpenDeepAnalysis: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalScrolls = summary.igCount + summary.ytCount
    val totalTimeMillis = summary.igTimeMillis + summary.ytTimeMillis

    GlassmorphicCard(
        modifier = modifier.fillMaxWidth(),
        depth = GlassDepthLevel.Front,
        style = GlassStyle.Elevated,
        cornerRadius = 24.dp,
        padding = androidx.compose.foundation.layout.PaddingValues(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "TODAY'S FOCUS",
                color = Gray500,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            BrainMascot(scrollCount = totalScrolls, sizeDp = 88.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatPill("Scrolls", totalScrolls.toString())
                StatPill("Screen", formatTotalTime(totalTimeMillis))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    UsageProgressRing(
                        platform = "instagram",
                        scrollCount = summary.igCount,
                        dailyGoal = summary.igLimit,
                        screenTime = formatMillisToTime(summary.igTimeMillis),
                        modifier = Modifier.size(92.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    UsageProgressRing(
                        platform = "youtube",
                        scrollCount = summary.ytCount,
                        dailyGoal = summary.ytLimit,
                        screenTime = formatMillisToTime(summary.ytTimeMillis),
                        modifier = Modifier.size(92.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(CobaltPremium, IndigoPremium)
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenDeepAnalysis
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Deep Analysis",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Tap outside to resume",
                color = Gray400,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(label.uppercase(), color = Gray500, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
    }
}
