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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrollersdashboard.ui.theme.*
import kotlin.random.Random

// --- Premium Glassmorphism UI Components ---

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
    ) {
        // Separate background layer for blur to keep content sharp
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.White.copy(alpha = 0.1f)) // Translucent background
                .blur(radius = 15.dp) // The "Liquid" blur effect
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(cornerRadius)
                )
        )
        
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()
        }
    }
}

@Composable
fun UsageCard(
    platform: String,
    screenTime: String,
    scrollCount: Int,
    percentage: Int,
    dailyGoal: Int,
    isDarkMode: Boolean
) {
    val gradient = if (platform.lowercase() == "instagram") {
        listOf(Color(0xFFC13584), Color(0xFFE1306C), Color(0xFFFF8C00))
    } else {
        listOf(Color(0xFFFF0000), Color(0xFFFF4D4D))
    }
    
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage.toFloat(),
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "percentage"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF151518))
            .padding(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(130.dp)) {
                // The dark inner circle background
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0A0A0C))
                )
                
                // Progress Ring
                Canvas(modifier = Modifier.size(120.dp)) {
                    val strokeWidth = 14.dp.toPx()
                    val radius = (size.width - strokeWidth) / 2
                    
                    // Track
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = radius,
                        style = Stroke(width = strokeWidth)
                    )
                    
                    // Progress Arc
                    drawArc(
                        brush = if (gradient.size > 1) {
                            Brush.sweepGradient(
                                0.0f to gradient[0],
                                0.5f to gradient[1],
                                1.0f to gradient.last()
                            )
                        } else {
                            SolidColor(gradient[0])
                        },
                        startAngle = -90f,
                        sweepAngle = 360f * (animatedPercentage / 100f),
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                
                // Text inside the ring
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = scrollCount.toString(),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 28.sp
                    )
                    Text(
                        text = "/$dailyGoal",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                platform.replaceFirstChar { it.uppercase() },
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
            
            Text(
                screenTime,
                color = Gray500,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

fun Modifier.liquidGlassCard(
    isDarkMode: Boolean,
    cornerRadius: Dp = 28.dp,
    elevation: Dp = 1.5.dp,
    alpha: Float = 0.08f
): Modifier = this.then(
    Modifier
        .drawBehind {
            val drawSize = this.size
            val shadowColor = if (isDarkMode) Color.Black.copy(alpha = 0.5f) else Color(0xFFA3B1C6).copy(alpha = 0.25f)
            
            // Outer soft glow/shadow for depth
            drawRoundRect(
                color = shadowColor,
                topLeft = Offset(elevation.toPx(), elevation.toPx() * 1.5f),
                size = drawSize,
                cornerRadius = CornerRadius(cornerRadius.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )
        }
        .clip(RoundedCornerShape(cornerRadius))
        .background(
            brush = Brush.verticalGradient(
                colors = if (isDarkMode) {
                    listOf(
                        Color(0xFFFFFFFF).copy(alpha = alpha),
                        Color(0xFFFFFFFF).copy(alpha = alpha * 0.4f),
                        Color(0xFF000000).copy(alpha = 0.05f)
                    )
                } else {
                    listOf(
                        Color.White.copy(alpha = 0.85f),
                        Color(0xFFF0F0F3).copy(alpha = 0.6f)
                    )
                }
            )
        )
        .drawWithCache {
            val borderBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (isDarkMode) 0.25f else 0.9f), // Strong top-left highlight
                    Color.White.copy(alpha = 0.02f),
                    Color.White.copy(alpha = if (isDarkMode) 0.05f else 0.2f)
                ),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            )
            onDrawWithContent {
                drawContent()
                // The "Glass" border
                drawRoundRect(
                    brush = borderBrush,
                    cornerRadius = CornerRadius(cornerRadius.toPx()),
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }
        }
)

fun Modifier.marbleBackground(): Modifier = this.then(
    Modifier.drawBehind {
        val drawSize = this.size
        // Deep obsidian designer background
        drawRect(Color(0xFF08080A))
        
        val random = Random(123)
        // Elegant marble veins - long and flowing
        repeat(12) {
            val startX = random.nextFloat() * drawSize.width
            val startY = random.nextFloat() * drawSize.height
            val endX = startX + (random.nextFloat() - 0.5f) * drawSize.width * 1.2f
            val endY = startY + (random.nextFloat() - 0.5f) * drawSize.height * 0.5f
            
            val veinColor = if (random.nextBoolean()) Color.White else Color(0xFF7B7B7B)
            
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        veinColor.copy(alpha = 0.06f),
                        veinColor.copy(alpha = 0.12f),
                        veinColor.copy(alpha = 0.02f),
                        Color.Transparent
                    )
                ),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = (1.5.dp + (random.nextFloat() * 2).dp).toPx(),
                cap = StrokeCap.Round
            )
        }
        
        // Dynamic "cloudy" nebulas for marble depth
        repeat(5) {
            val center = Offset(random.nextFloat() * drawSize.width, random.nextFloat() * drawSize.height)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1A1A1E).copy(alpha = 0.4f), Color.Transparent),
                    center = center,
                    radius = 300.dp.toPx()
                ),
                radius = 300.dp.toPx(),
                center = center
            )
        }
        
        // Fine grain texture
        repeat(200) {
            drawCircle(
                color = Color.White.copy(alpha = 0.015f),
                radius = 0.8.dp.toPx(),
                center = Offset(random.nextFloat() * drawSize.width, random.nextFloat() * drawSize.height)
            )
        }
    }
)

fun Modifier.neumorphicInset(
    isDarkMode: Boolean,
    cornerRadius: Dp = 16.dp,
    depth: Dp = 2.dp
): Modifier = this.then(
    Modifier
        .drawBehind {
            val drawSize = this.size
            val shadowColor = if (isDarkMode) Color.Black.copy(alpha = 0.8f) else Color(0xFFA3B1C6).copy(alpha = 0.4f)
            
            // Recessed background
            drawRoundRect(
                color = if (isDarkMode) Color(0xFF050506) else Color(0xFFEBEBEF),
                size = drawSize,
                cornerRadius = CornerRadius(cornerRadius.toPx())
            )
            
            // Top/Left inner shadow
            drawRoundRect(
                color = shadowColor,
                size = drawSize,
                cornerRadius = CornerRadius(cornerRadius.toPx()),
                style = Stroke(width = depth.toPx())
            )
        }
        .clip(RoundedCornerShape(cornerRadius))
)

fun Modifier.liquidGlassButton(
    isDarkMode: Boolean,
    isPressed: Boolean,
    cornerRadius: Dp = 16.dp
): Modifier = this.then(
    if (isPressed) {
        this.neumorphicInset(isDarkMode, cornerRadius, depth = 4.dp)
    } else {
        this.liquidGlassCard(isDarkMode, cornerRadius, elevation = 2.5.dp, alpha = 0.15f)
            .drawBehind {
                // Additional shine for buttons
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(size.width * 0.2f, size.height * 0.2f),
                        radius = size.width * 0.6f
                    ),
                    radius = size.width * 0.6f,
                    center = Offset(size.width * 0.2f, size.height * 0.2f)
                )
            }
    }
)

@Composable
fun PremiumScalingButton(
    onClick: (Offset) -> Unit,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = true,
    cornerRadius: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
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
            .liquidGlassButton(isDarkMode, isPressed, cornerRadius = cornerRadius)
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
        if (targetChecked) 38.dp else 4.dp 
    }
    
    val trackColor = if (checked) Color(0xFF34C759).copy(alpha = 0.3f) else (if (isDarkMode) Color(0xFF151518) else Color(0xFFE6E6E9))

    Box(
        modifier = modifier
            .width(80.dp)
            .height(44.dp)
            .neumorphicInset(isDarkMode, cornerRadius = 22.dp, depth = 2.dp)
            .background(trackColor)
            .clickable { onCheckedChange(!checked) }
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset - 4.dp)
                .size(36.dp)
                .liquidGlassCard(isDarkMode, cornerRadius = 18.dp, elevation = 1.dp, alpha = 0.9f)
                .background(
                    brush = Brush.verticalGradient(
                        if (isDarkMode) listOf(Color(0xFFE0E0E0), Color(0xFF9E9E9E)) else listOf(Color.White, Color(0xFFE0E0E0))
                    ),
                    shape = CircleShape
                )
        )
    }
}

@Composable
fun ActivityRingCard(label: String, count: Int, target: Int, gradients: List<Color>, textColor: Color, isDarkMode: Boolean) {
    val subTextColor = textColor.copy(alpha = 0.6f)
    val actualProgress = (count.toFloat() / target).coerceIn(0f, 1.2f)
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .liquidGlassCard(isDarkMode, cornerRadius = 32.dp)
            .padding(24.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .neumorphicInset(isDarkMode, cornerRadius = 60.dp, depth = 2.dp)
            )
            
            AppleStyleRing(actualProgress, gradients, isDarkMode)
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$count", 
                    color = textColor, 
                    fontSize = 42.sp, 
                    fontWeight = FontWeight.Black,
                    lineHeight = 42.sp
                )
                Text(
                    "/${target}", 
                    color = subTextColor, 
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            label, 
            color = textColor, 
            fontSize = 16.sp, 
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun AppleStyleRing(progress: Float, gradients: List<Color>, isDarkMode: Boolean) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 120f),
        label = "RingProgress"
    )

    Canvas(modifier = Modifier.size(130.dp)) {
        val strokeWidth = 16.dp.toPx()
        val radius = (size.width - strokeWidth) / 2
        
        drawCircle(
            color = if (isDarkMode) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.08f),
            radius = radius,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        
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
        
        if (animatedProgress > 0.01f) {
            val angle = -90f + 360f * animatedProgress
            val x = (size.width / 2) + radius * Math.cos(Math.toRadians(angle.toDouble())).toFloat()
            val y = (size.height / 2) + radius * Math.sin(Math.toRadians(angle.toDouble())).toFloat()
            drawCircle(Color.White.copy(alpha = 0.4f), radius = strokeWidth / 3.5f, center = Offset(x, y))
        }
    }
}

@Composable
fun BentoLegendItem(color: Color, label: String, count: Int, textColor: Color, subTextColor: Color, isDarkMode: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassCard(isDarkMode, cornerRadius = 20.dp, elevation = 1.dp)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.7f))))
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.weight(1f))
        Text("$count", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text(" scrolls", color = subTextColor, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun NeumorphicTabSwitcher(
    tabs: List<String>,
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    isDarkMode: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphicInset(isDarkMode, cornerRadius = 24.dp, depth = 2.dp)
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEach { tab ->
            val isSelected = selectedTab == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .then(
                        if (isSelected) {
                            Modifier.liquidGlassCard(isDarkMode, cornerRadius = 20.dp, elevation = 1.dp, alpha = 0.9f)
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onTabSelected(tab) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    tab,
                    color = if (isSelected) (if (isDarkMode) Color.White else Color.Black) else Color.Gray,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }
        }
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
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "")

    Box(
        modifier = modifier
            .height(64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .liquidGlassButton(isDarkMode, isPressed, cornerRadius = 24.dp)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                color = if (isDarkMode) Color.White else Color.Black,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun NeumorphicDivider(isDarkMode: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
    )
}

@Composable
fun PermissionItem(title: String, isGranted: Boolean, isDarkMode: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassCard(isDarkMode, cornerRadius = 16.dp, elevation = 1.dp)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isDarkMode) Color.White else Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = if (isGranted) "Granted" else "Not Granted",
            tint = if (isGranted) Color(0xFF34C759) else Gray500,
            modifier = Modifier.size(24.dp)
        )
    }
}

// Compatibility helpers
fun Modifier.skeuomorphicCard(isDarkMode: Boolean, cornerRadius: Dp = 24.dp, elevation: Dp = 1.5.dp): Modifier = 
    this.liquidGlassCard(isDarkMode, cornerRadius, elevation = elevation)

fun Modifier.skeuomorphicButton(isDarkMode: Boolean, isPressed: Boolean, cornerRadius: Dp = 16.dp): Modifier = 
    this.liquidGlassButton(isDarkMode, isPressed, cornerRadius)
