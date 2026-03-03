package com.example.scrollersdashboard.ui

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PremiumScalingButton(
    onClick: (Offset) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "ButtonScale"
    )
    var center by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .onGloballyPositioned {
                val windowPos = it.positionInWindow()
                center = windowPos + Offset(it.size.width.toFloat() / 2f, it.size.height.toFloat() / 2f)
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(interactionSource = interactionSource, indication = null) { onClick(center) },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun ScrollerToggle(
    checked: Boolean,
    isDarkMode: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = updateTransition(checked, label = "ToggleTransition")
    val thumbOffset by transition.animateDp(label = "ThumbOffset") { targetChecked ->
        if (targetChecked) 36.dp else 4.dp 
    }
    val backgroundColor by transition.animateColor(label = "BackgroundColor") { targetChecked ->
        if (targetChecked) ToggleTrackOn else ToggleTrackOff
    }

    Box(
        modifier = modifier
            .width(72.dp)
            .height(40.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onCheckedChange(!checked) }
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset - 4.dp)
                .size(32.dp)
                .shadow(4.dp, CircleShape)
                .background(Color.White, CircleShape)
        )
    }
}

@Composable
fun ClayPillButton(
    modifier: Modifier,
    text: String,
    icon: ImageVector,
    color: Color,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "")

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .height(64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .border(
                brush = Brush.verticalGradient(
                    colors = listOf(color.copy(alpha = 0.4f), color.copy(alpha = 0.05f))
                ),
                width = 1.dp,
                shape = RoundedCornerShape(20.dp)
            ),
        color = color.copy(alpha = if (isDarkMode) 0.12f else 0.05f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ActivityRingCard(label: String, count: Int, target: Int, gradients: List<Color>, textColor: Color, isDarkMode: Boolean) {
    val subTextColor = textColor.copy(alpha = 0.5f)
    val progress = (count.toFloat() / target).coerceIn(0f, 1.2f)
    
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
            // Glow effect
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .blur(40.dp)
                    .background(gradients.first().copy(alpha = 0.15f), CircleShape)
            )
            
            AppleStyleRing(progress, gradients, isDarkMode)
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$count", 
                    color = textColor, 
                    fontSize = 38.sp, 
                    fontWeight = FontWeight.Black,
                    lineHeight = 38.sp
                )
                Text(
                    "/${target}", 
                    color = subTextColor, 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            label, 
            color = textColor, 
            fontSize = 15.sp, 
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AppleStyleRing(progress: Float, gradients: List<Color>, isDarkMode: Boolean) {
    val baseColor = gradients.first()
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 150f),
        label = "RingProgress"
    )

    Canvas(modifier = Modifier.size(120.dp)) {
        val strokeWidth = 14.dp.toPx()
        val radius = (size.width - strokeWidth) / 2
        
        // Background track
        drawCircle(
            color = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f),
            radius = radius,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        
        // Foreground progress
        drawArc(
            brush = Brush.sweepGradient(
                0.0f to gradients[0],
                0.5f to gradients[1],
                1.0f to gradients.last(),
            ), 
            startAngle = -90f, 
            sweepAngle = 360f * animatedProgress, 
            useCenter = false, 
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        
        // End cap shadow/glow for better depth
        if (animatedProgress > 0.01f) {
            val angle = -90f + 360f * animatedProgress
            val x = (size.width / 2) + radius * Math.cos(Math.toRadians(angle.toDouble())).toFloat()
            val y = (size.height / 2) + radius * Math.sin(Math.toRadians(angle.toDouble())).toFloat()
            drawCircle(gradients.last(), radius = strokeWidth / 2.2f, center = Offset(x, y))
        }
    }
}

@Composable
fun BentoLegendItem(color: Color, label: String, count: Int, textColor: Color, subTextColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (textColor == Color.White) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.02f))
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(color, color.copy(alpha = 0.7f)))
                )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.weight(1f))
        Text("$count", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Black)
        Text(" scrolls", color = subTextColor, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
