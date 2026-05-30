package com.example.scrollersdashboard.ui

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.scrollersdashboard.R
import com.example.scrollersdashboard.ui.theme.BrainConcernedOrange
import com.example.scrollersdashboard.ui.theme.BrainHealthyPink
import com.example.scrollersdashboard.ui.theme.BrainMeltingRed
import com.example.scrollersdashboard.ui.theme.BrainTiredYellow
import kotlinx.coroutines.delay

enum class BrainState(
    val animationRes: Int,
    val message: String,
    val glowColor: Color?,
    val fallbackColor: Color
) {
    HEALTHY(
        R.raw.brain_healthy,
        "Feeling fresh!",
        BrainHealthyPink,
        BrainHealthyPink
    ),
    CONCERNED(
        R.raw.brain_concerned,
        "Getting tired...",
        BrainConcernedOrange,
        BrainConcernedOrange
    ),
    TIRED(
        R.raw.brain_tired,
        "Need a break!",
        BrainTiredYellow,
        BrainTiredYellow
    ),
    MELTING(
        R.raw.brain_melting,
        "Brain melting!",
        BrainMeltingRed,
        BrainMeltingRed
    ),
    EXPLODING(
        R.raw.brain_exploding,
        "STOP SCROLLING!",
        null,
        Color(0xFF71717A)
    );

    val isHealthy: Boolean get() = this == HEALTHY
}

fun brainStateForScrollCount(count: Int): BrainState = when {
    count <= 20 -> BrainState.HEALTHY
    count <= 50 -> BrainState.CONCERNED
    count <= 100 -> BrainState.TIRED
    count <= 200 -> BrainState.MELTING
    else -> BrainState.EXPLODING
}

@Composable
fun BrainMascot(
    scrollCount: Int,
    modifier: Modifier = Modifier,
    sizeDp: androidx.compose.ui.unit.Dp = 180.dp
) {
    val brainState = remember(scrollCount) { brainStateForScrollCount(scrollCount) }

    val infiniteTransition = rememberInfiniteTransition(label = "brainFloat")
    val floatY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )
    val breathe by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    val stateScale by animateFloatAsState(
        targetValue = if (brainState.isHealthy) 1f else 0.96f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "brainScale"
    )

    var showBubble by remember { mutableStateOf(true) }

    LaunchedEffect(brainState) {
        showBubble = true
        delay(3000)
        showBubble = false
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedSpeechBubble(
            text = brainState.message,
            visible = showBubble,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .size(sizeDp)
                .graphicsLayer {
                    translationY = floatY
                    scaleX = stateScale * breathe
                    scaleY = stateScale * breathe
                },
            contentAlignment = Alignment.Center
        ) {
            val glow = brainState.glowColor ?: brainState.fallbackColor
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glow.copy(alpha = 0.4f), glow.copy(alpha = 0.12f), Color.Transparent),
                        radius = size.minDimension / 1.8f
                    ),
                    radius = size.minDimension / 1.8f,
                    center = center
                )
            }

            AnimatedContent(
                targetState = brainState,
                transitionSpec = {
                    (fadeIn(tween(450, easing = FastOutSlowInEasing)) +
                        scaleIn(initialScale = 0.88f, animationSpec = tween(450, easing = FastOutSlowInEasing)))
                        .togetherWith(
                            fadeOut(tween(280)) +
                                scaleOut(targetScale = 0.92f, animationSpec = tween(280))
                        )
                },
                label = "brainStateCrossfade"
            ) { state ->
                BrainLottieLayer(
                    state = state,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun BrainLottieLayer(
    state: BrainState,
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(state.animationRes)
    )

    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = composition != null,
        speed = 0.92f
    )

    if (composition != null) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = modifier
        )
    } else {
        BrainMascotCanvasFallback(
            color = state.fallbackColor,
            modifier = modifier
        )
    }
}

@Composable
private fun BrainMascotCanvasFallback(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val s = size.minDimension / 180f

        val path = Path().apply {
            moveTo(cx, cy - 70f * s)
            cubicTo(cx + 55f * s, cy - 55f * s, cx + 68f * s, cy + 15f * s, cx + 44f * s, cy + 58f * s)
            cubicTo(cx + 24f * s, cy + 68f * s, cx - 24f * s, cy + 68f * s, cx - 44f * s, cy + 58f * s)
            cubicTo(cx - 68f * s, cy + 15f * s, cx - 55f * s, cy - 55f * s, cx, cy - 70f * s)
            close()
        }
        drawPath(
            path = path,
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.95f), color.copy(alpha = 0.7f)),
                center = Offset(cx, cy - 10f * s),
                radius = 70f * s
            )
        )
        drawPath(path, color.copy(alpha = 0.5f), style = Stroke(width = 2f * s))

        val eyeY = cy + 4f * s
        val eyeSpacing = 22f * s
        drawCircle(Color(0xFF1E1E2E), 5f * s, Offset(cx - eyeSpacing, eyeY))
        drawCircle(Color(0xFF1E1E2E), 5f * s, Offset(cx + eyeSpacing, eyeY))

        val smile = Path().apply {
            moveTo(cx - 16f * s, cy + 22f * s)
            quadraticTo(cx, cy + 32f * s, cx + 16f * s, cy + 22f * s)
        }
        drawPath(smile, Color(0xFF1E1E2E), style = Stroke(width = 2.5f * s, cap = StrokeCap.Round))
    }
}

@Composable
fun AnimatedSpeechBubble(
    text: String,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(350)) + scaleIn(initialScale = 0.9f, animationSpec = spring(stiffness = Spring.StiffnessMedium)),
        exit = fadeOut(tween(280)) + scaleOut(targetScale = 0.9f),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.blur(24.dp)
                        } else {
                            Modifier
                        }
                    )
                    .background(
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
            )
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}
