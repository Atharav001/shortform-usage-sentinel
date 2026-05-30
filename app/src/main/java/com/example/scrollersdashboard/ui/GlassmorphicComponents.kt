package com.example.scrollersdashboard.ui

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val CounterOvershootEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
private val GlassPressEasing = CubicBezierEasing(0.4f, 0f, 1f, 1f)
private val GlassHoverEasing = CubicBezierEasing(0f, 0f, 0.2f, 1f)

enum class GlassDepthLevel(
    val shadowDp: Dp,
    val hoverShadowDp: Dp,
    val pressedShadowDp: Dp
) {
    Back(4.dp, 8.dp, 4.dp),
    Mid(8.dp, 16.dp, 4.dp),
    Front(12.dp, 16.dp, 4.dp)
}

enum class GlassStyle {
    /** Flat glass panel — no shadow or center glow */
    Plain,
    /** Hero / interactive card with elevation */
    Elevated
}

fun brainGlowColorForScrollCount(count: Int): Color = when (brainStateForScrollCount(count)) {
    BrainState.HEALTHY -> com.example.scrollersdashboard.ui.theme.BrainHealthyPink
    BrainState.CONCERNED -> com.example.scrollersdashboard.ui.theme.BrainConcernedOrange
    BrainState.TIRED -> com.example.scrollersdashboard.ui.theme.BrainTiredYellow
    BrainState.MELTING -> com.example.scrollersdashboard.ui.theme.BrainMeltingRed
    BrainState.EXPLODING -> com.example.scrollersdashboard.ui.theme.BrainExplodingGray
}

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    depth: GlassDepthLevel = GlassDepthLevel.Mid,
    style: GlassStyle = GlassStyle.Elevated,
    cornerRadius: Dp = 24.dp,
    padding: PaddingValues = PaddingValues(24.dp),
    glowColor: Color? = null,
    celebrationKey: Int = 0,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    var showParticles by remember { mutableStateOf(false) }
    val pulseScale = remember { Animatable(1f) }

    LaunchedEffect(celebrationKey) {
        if (celebrationKey <= 0) return@LaunchedEffect
        pulseScale.snapTo(1f)
        pulseScale.animateTo(1.05f, tween(200, easing = CounterOvershootEasing))
        pulseScale.animateTo(1f, tween(200, easing = GlassPressEasing))
        showParticles = true
        delay(800)
        showParticles = false
    }

    val targetShadow = when (style) {
        GlassStyle.Plain -> 0.dp
        GlassStyle.Elevated -> when {
            isPressed -> depth.pressedShadowDp
            isHovered -> depth.hoverShadowDp
            else -> depth.shadowDp
        }
    }
    val animatedShadow by animateFloatAsState(
        targetValue = targetShadow.value,
        animationSpec = tween(
            durationMillis = if (isPressed) 150 else 200,
            easing = if (isPressed) GlassPressEasing else GlassHoverEasing
        ),
        label = "glassShadow"
    )

    val targetScale = when {
        isPressed -> 0.98f
        isHovered -> 1.02f
        else -> 1f
    }
    val animatedScale by animateFloatAsState(
        targetValue = targetScale * pulseScale.value,
        animationSpec = tween(
            durationMillis = if (isPressed) 150 else 200,
            easing = if (isPressed) GlassPressEasing else GlassHoverEasing
        ),
        label = "glassScale"
    )

    val borderAlpha = if (isHovered) 0.15f else 0.10f

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .then(
                if (style == GlassStyle.Elevated && animatedShadow > 0f) {
                    Modifier.shadow(
                        elevation = animatedShadow.dp,
                        shape = shape,
                        spotColor = Color.Black.copy(alpha = 0.36f),
                        ambientColor = Color.Black.copy(alpha = 0.20f)
                    )
                } else Modifier
            )
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .hoverable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onClick?.invoke() }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
        ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (style == GlassStyle.Elevated && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(28.dp)
                    } else {
                        Modifier
                    }
                )
                .background(
                    Brush.verticalGradient(
                        colors = if (style == GlassStyle.Plain) {
                            listOf(
                                Color.White.copy(alpha = 0.06f),
                                Color.White.copy(alpha = 0.03f)
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.04f)
                            )
                        }
                    )
                )
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .border(1.dp, Color.White.copy(alpha = borderAlpha), RoundedCornerShape(cornerRadius))
                .drawBehind {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = 2.dp.toPx().coerceAtLeast(1f)
                        ),
                        cornerRadius = CornerRadius(cornerRadius.toPx()),
                        size = size.copy(height = 2.dp.toPx().coerceAtLeast(1f))
                    )
                    if (style == GlassStyle.Elevated) {
                        glowColor?.let { glow ->
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(glow.copy(alpha = 0.12f), Color.Transparent),
                                    center = Offset(size.width / 2f, size.height * 0.35f),
                                    radius = size.minDimension * 0.45f
                                ),
                                radius = size.minDimension * 0.45f,
                                center = Offset(size.width / 2f, size.height * 0.35f)
                            )
                        }
                    }
                }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            content = content
        )

        if (showParticles) {
            MilestoneParticleBurst(
                modifier = Modifier.matchParentSize(),
                color = glowColor ?: Color.White
            )
        }
        }
    }
}

@Composable
fun AnimatedGlowNumberText(
    text: String,
    glowColor: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle(
        color = Color.White,
        fontSize = 24.sp,
        fontWeight = FontWeight.Black
    )
) {
    AnimatedContent(
        targetState = text,
        modifier = modifier,
        transitionSpec = {
            (fadeIn(tween(600, easing = CounterOvershootEasing)) +
                scaleIn(
                    initialScale = 0.85f,
                    animationSpec = tween(600, easing = CounterOvershootEasing)
                )) togetherWith
                (fadeOut(tween(300)) +
                    scaleOut(
                        targetScale = 0.9f,
                        animationSpec = tween(300)
                    )) using SizeTransform(clip = false)
        },
        label = "glowNumber"
    ) { target ->
        Box(contentAlignment = Alignment.Center) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Text(
                    text = target,
                    style = style,
                    color = glowColor.copy(alpha = 0.4f),
                    modifier = Modifier
                        .blur(16.dp)
                        .graphicsLayer { alpha = 0.85f }
                )
            }
            Text(text = target, style = style)
        }
    }
}

@Composable
fun AnimatedScrollCountDisplay(
    count: Int,
    dailyGoal: Int,
    glowColor: Color,
    countFontSize: androidx.compose.ui.unit.TextUnit = 22.sp,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        AnimatedGlowNumberText(
            text = count.toString(),
            glowColor = glowColor,
            style = TextStyle(
                color = Color.White,
                fontSize = countFontSize,
                fontWeight = FontWeight.Black
            )
        )
        Text(
            text = "/$dailyGoal",
            color = com.example.scrollersdashboard.ui.theme.Gray500,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AnimatedUsageTimeText(
    millis: Long,
    glowColor: Color,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 36.sp
) {
    val label = formatTotalTime(millis)
    AnimatedGlowNumberText(
        text = label,
        glowColor = glowColor,
        modifier = modifier,
        style = TextStyle(
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.Black
        )
    )
}

@Composable
private fun MilestoneParticleBurst(
    modifier: Modifier = Modifier,
    color: Color
) {
    val particleCount = remember { 10 }
    val angles = remember {
        List(particleCount) { i -> (360f / particleCount) * i + Random.nextFloat() * 12f }
    }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(800, easing = GlassHoverEasing))
    }

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxRadius = size.minDimension * 0.42f
        val t = progress.value

        angles.forEachIndexed { index, angleDeg ->
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val radius = maxRadius * t
            val x = cx + (cos(angleRad) * radius).toFloat()
            val y = cy + (sin(angleRad) * radius).toFloat()
            val alpha = (1f - t).coerceIn(0f, 1f)
            val dotRadius = (3f + (index % 3)) * (1f - t * 0.4f)

            drawCircle(
                color = color.copy(alpha = alpha * 0.85f),
                radius = dotRadius,
                center = Offset(x, y)
            )
        }
    }
}
