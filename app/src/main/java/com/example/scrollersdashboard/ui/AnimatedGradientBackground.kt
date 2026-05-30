package com.example.scrollersdashboard.ui

import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.scrollersdashboard.ui.theme.CobaltPremium
import com.example.scrollersdashboard.ui.theme.IndigoPremium
import com.example.scrollersdashboard.ui.theme.SlateBlue

private val PremiumBase = Color(0xFF0A0A14)
private val PremiumMid = Color(0xFF0E0E18)

@Composable
fun AnimatedGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(PremiumBase, PremiumMid, PremiumBase)
                    )
                )
        )

        GradientOrb(
            alignment = Alignment.TopEnd,
            offsetX = 40.dp,
            offsetY = (-80).dp,
            color = IndigoPremium,
            opacity = 0.14f,
            diameter = 480.dp,
            animationDelayMillis = 0
        )

        GradientOrb(
            alignment = Alignment.BottomStart,
            offsetX = (-60).dp,
            offsetY = 60.dp,
            color = CobaltPremium,
            opacity = 0.10f,
            diameter = 420.dp,
            animationDelayMillis = 2200
        )

        GradientOrb(
            alignment = Alignment.Center,
            offsetX = 0.dp,
            offsetY = 0.dp,
            color = SlateBlue,
            opacity = 0.06f,
            diameter = 360.dp,
            animationDelayMillis = 1100
        )

        content()
    }
}

@Composable
fun WarmScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    AnimatedGradientBackground(modifier = modifier, content = content)
}

@Composable
private fun GradientOrb(
    alignment: Alignment,
    offsetX: Dp,
    offsetY: Dp,
    color: Color,
    opacity: Float,
    diameter: Dp,
    animationDelayMillis: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "premiumOrbPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = FastOutSlowInEasing, delayMillis = animationDelayMillis),
            repeatMode = RepeatMode.Reverse
        ),
        label = "premiumOrbScale"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = alignment
    ) {
        val orbModifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .size(diameter * pulseScale)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to color.copy(alpha = opacity),
                        0.55f to color.copy(alpha = opacity * 0.4f),
                        1.0f to Color.Transparent
                    )
                )
            )
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.blur(72.dp)
                } else {
                    Modifier
                }
            )

        Box(modifier = orbModifier)
    }
}
