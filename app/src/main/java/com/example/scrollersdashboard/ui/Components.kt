package com.example.scrollersdashboard.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrollersdashboard.ui.theme.*
import kotlin.random.Random

@Composable
fun SectionTitle(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CobaltPremium.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CobaltPremium,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title.uppercase(),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun AlertLimitCardReplica(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: Brush,
    borderColor: Color,
    value: String,
    onValueChange: (String) -> Unit,
    unit: String,
    instruction: String,
    footer: String,
    onDone: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CardBackground)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(gradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text(subtitle, color = Gray400, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.2f))
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(instruction.uppercase(), color = Gray500, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            fontFamily = FontFamily.SansSerif
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { onDone() }
                        ),
                        cursorBrush = Brush.verticalGradient(listOf(Color.White, Color.White))
                    )
                }
                Text(unit.uppercase(), color = Gray400, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(footer, color = Gray500, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldPremium.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp).clickable { onDone() }
                )
            }
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    depth: GlassDepthLevel = GlassDepthLevel.Mid,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassmorphicCard(
        modifier = modifier.fillMaxWidth(),
        depth = depth,
        style = GlassStyle.Plain,
        cornerRadius = cornerRadius,
        padding = PaddingValues(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
    isDarkMode: Boolean,
    totalScrollsForGlow: Int = scrollCount
) {
    val platformLabel = platform.replaceFirstChar { it.uppercase() }

    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        depth = GlassDepthLevel.Mid,
        style = GlassStyle.Plain,
        cornerRadius = 24.dp,
        padding = PaddingValues(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            UsageProgressRing(
                platform = platform,
                scrollCount = scrollCount,
                dailyGoal = dailyGoal,
                screenTime = screenTime
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                platformLabel,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                screenTime,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun MetricCard(
    platform: String,
    scrollCount: Int,
    dailyGoal: Int,
    screenTime: String,
    gradient: List<Color>,
    isDarkMode: Boolean
) {
    val percentage = (scrollCount.toFloat() / dailyGoal * 100).coerceAtMost(100f)
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 150f),
        label = "MetricProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassCard(isDarkMode)
            .padding(20.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 10.dp.toPx()
                    val radius = (size.width - strokeWidth) / 2
                    
                    // Background Track
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = radius,
                        style = Stroke(width = strokeWidth)
                    )

                    // Progress
                    drawArc(
                        brush = Brush.linearGradient(gradient),
                        startAngle = -90f,
                        sweepAngle = 360f * (animatedPercentage / 100f),
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = scrollCount.toString(),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "/$dailyGoal",
                        color = Gray500,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                platform.replaceFirstChar { it.uppercase() },
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )
            
            Text(
                screenTime,
                color = Gray400,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

fun Modifier.liquidGlassCard(
    isDarkMode: Boolean,
    cornerRadius: Dp = 28.dp,
    elevation: Dp = 1.5.dp,
    alpha: Float = 0.15f
): Modifier = this.then(
    Modifier
        .clip(RoundedCornerShape(cornerRadius))
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = alpha),
                    Color.White.copy(alpha = alpha * 0.4f),
                    Color.Black.copy(alpha = 0.1f)
                )
            )
        )
        .drawWithCache {
            val borderBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.15f),
                    Color.White.copy(alpha = 0.02f),
                    Color.White.copy(alpha = 0.08f)
                ),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            )
            onDrawWithContent {
                drawContent()
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
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    com.example.scrollersdashboard.ui.theme.WarmBaseTop,
                    com.example.scrollersdashboard.ui.theme.WarmBaseMid,
                    ObsidianBlack
                )
            )
        )

        val random = Random(42)
        repeat(6) {
            val startX = random.nextFloat() * size.width
            val startY = random.nextFloat() * size.height
            val endX = startX + (random.nextFloat() - 0.5f) * size.width * 0.6f
            val endY = startY + (random.nextFloat() - 0.5f) * size.height * 0.6f

            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFF59E0B).copy(alpha = 0.04f),
                        Color.Transparent
                    )
                ),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        repeat(3) {
            val center = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFEA580C).copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = 380.dp.toPx()
                ),
                radius = 380.dp.toPx(),
                center = center
            )
        }
    }
)

fun Modifier.liquidGlassButton(
    isDarkMode: Boolean,
    isPressed: Boolean,
    cornerRadius: Dp = 16.dp
): Modifier = this.then(
    if (isPressed) {
        Modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.Black.copy(alpha = 0.4f))
            .border(1.dp, GlassBorder, RoundedCornerShape(cornerRadius))
    } else {
        Modifier.liquidGlassCard(isDarkMode, cornerRadius, alpha = 0.12f)
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
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(120),
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
fun OverlayQuickSummaryCard(
    igCount: Int,
    ytCount: Int,
    igTime: String,
    ytTime: String,
    igLimit: Int,
    ytLimit: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            UsageProgressRing(
                platform = "instagram",
                scrollCount = igCount,
                dailyGoal = igLimit,
                screenTime = igTime,
                modifier = Modifier.size(88.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            UsageProgressRing(
                platform = "youtube",
                scrollCount = ytCount,
                dailyGoal = ytLimit,
                screenTime = ytTime,
                modifier = Modifier.size(88.dp)
            )
        }
    }
}

@Composable
fun ActivityRingCard(label: String, count: Int, target: Int, gradients: List<Color>, textColor: Color, isDarkMode: Boolean) {
    val subTextColor = Color.White.copy(alpha = 0.5f)
    val progress = if (target > 0) (count.toFloat() / target).coerceIn(0f, 1.2f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 80f),
        label = "RingProgress"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, GlassBorder, RoundedCornerShape(32.dp))
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(110.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 12.dp.toPx()
                    val radius = (size.width - strokeWidth) / 2
                    
                    // Track
                    drawCircle(
                        color = Color.White.copy(alpha = 0.06f),
                        radius = radius,
                        style = Stroke(width = strokeWidth)
                    )
                    
                    // Progress
                    drawArc(
                        brush = Brush.linearGradient(gradients),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$count", 
                        color = Color.White, 
                        fontSize = 28.sp, 
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        "/$target", 
                        color = subTextColor, 
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                label.uppercase(), 
                color = Color.White, 
                fontSize = 11.sp, 
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp
            )
        }
    }
}

@Composable
fun AppleStyleRing(progress: Float, gradients: List<Color>, isDarkMode: Boolean) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 100f),
        label = "RingProgress"
    )

    Canvas(modifier = Modifier.size(140.dp).aspectRatio(1f)) {
        val strokeWidth = 16.dp.toPx()
        val radius = (size.width - strokeWidth) / 2
        
        // Background Track
        drawCircle(
            color = RingTrack,
            radius = radius,
            style = Stroke(width = strokeWidth)
        )
        
        // Progress Arc with Gradient
        drawArc(
            brush = Brush.linearGradient(gradients), 
            startAngle = -90f, 
            sweepAngle = 360f * animatedProgress, 
            useCenter = false, 
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun BentoLegendItem(color: Color, label: String, count: Int, textColor: Color, subTextColor: Color, isDarkMode: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassCard(isDarkMode, cornerRadius = 24.dp)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.6f))))
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.weight(1f))
        Text("$count", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(" scrolls", color = Gray500, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 4.dp))
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
            .clip(RoundedCornerShape(24.dp))
            .background(CardBackground)
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEach { tab ->
            val isSelected = selectedTab == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .then(
                        if (isSelected) Modifier.background(Color.White.copy(alpha = 0.08f)) else Modifier
                    )
                    .clickable { onTabSelected(tab) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    tab,
                    color = if (isSelected) Color.White else Gray500,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
        }
    }
}

fun formatTotalTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

fun formatMillisToTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
