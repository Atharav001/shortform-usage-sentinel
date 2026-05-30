package com.example.scrollersdashboard.ui

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrollersdashboard.ui.theme.GlassBorder
import com.example.scrollersdashboard.ui.theme.Gray400
import com.example.scrollersdashboard.ui.theme.Gray500
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CalendarDay(
    val date: Int,
    val underLimit: Boolean?,
    val totalScrolls: Int = 0,
    val hasActivity: Boolean = false,
    val isPadding: Boolean = false,
    val isFuture: Boolean = false
)

fun scrollCountTier(count: Int): Int = when {
    count <= 30 -> 0
    count <= 70 -> 1
    count <= 150 -> 2
    else -> 3
}

fun tierGradient(tier: Int): List<Color> = when (tier) {
    0 -> listOf(Color(0xFF34D399), Color(0xFF14B8A6))
    1 -> listOf(Color(0xFFFBBF24), Color(0xFFFB923C))
    2 -> listOf(Color(0xFFFB923C), Color(0xFFEF4444))
    else -> listOf(Color(0xFFEF4444), Color(0xFF991B1B))
}

fun tierBrainEmoji(tier: Int): String = when (tier) {
    0 -> "😊"
    1 -> "😐"
    2 -> "😰"
    else -> "🤯"
}

fun tierBrainTint(tier: Int): Color = when (tier) {
    0 -> Color(0xFF34D399)
    1 -> Color(0xFFFBBF24)
    2 -> Color(0xFFFB923C)
    else -> Color(0xFFEF4444)
}

fun brainMessageForCount(count: Int): String = when (scrollCountTier(count)) {
    0 -> "Healthy scrolling pace"
    1 -> "Moderate activity"
    2 -> "Heavy scrolling day"
    else -> "Extreme scroll overload!"
}

private fun computeYAxisTicks(maxValue: Int): List<Int> {
    if (maxValue <= 0) return listOf(10, 5, 0)
    val step = when {
        maxValue <= 20 -> 5
        maxValue <= 50 -> 10
        maxValue <= 200 -> 50
        else -> ((maxValue / 4) / 50 + 1) * 50
    }
    val top = ((maxValue + step - 1) / step) * step
    return (0..top step step).toList().reversed()
}

@Composable
fun EnhancedActivityBarChart(
    data: List<ChartEntry>,
    labelInterval: Int = 1,
    modifier: Modifier = Modifier,
    chartHeight: androidx.compose.ui.unit.Dp = 180.dp,
    showEmojis: Boolean = false,
    showYAxis: Boolean = true,
    highlightedIndices: Set<Int> = emptySet(),
    staggerBars: Boolean = false
) {
    val peak = data.maxOfOrNull { it.value } ?: 0
    val maxVal = remember(data) {
        maxOf(peak.toFloat(), 10f) * 1.12f
    }
    val yTicks = remember(peak) { computeYAxisTicks(peak) }
    val yAxisMax = yTicks.firstOrNull()?.toFloat()?.coerceAtLeast(1f) ?: maxVal
    var selectedIndex by remember { mutableIntStateOf(-1) }

    Column(modifier = modifier.fillMaxWidth()) {
        if (selectedIndex >= 0 && selectedIndex < data.size) {
            val entry = data[selectedIndex]
            GlassBarTooltip(
                count = entry.value,
                message = brainMessageForCount(entry.value),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            if (showYAxis) {
                Column(
                    modifier = Modifier
                        .width(36.dp)
                        .height(chartHeight + 28.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    yTicks.forEach { tick ->
                        Text(
                            text = tick.toString(),
                            color = Gray500,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(chartHeight + 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEachIndexed { index, entry ->
                    val tier = scrollCountTier(entry.value)
                    val barGradient = tierGradient(tier)
                    val visible = index % labelInterval == 0 || labelInterval == 1
                    val isHighlighted = index in highlightedIndices
                    val barAnim = remember { Animatable(0f) }

                    LaunchedEffect(entry.value, yAxisMax, staggerBars) {
                        if (staggerBars) {
                            barAnim.snapTo(0f)
                            delay(index * 80L + 200L)
                        }
                        barAnim.animateTo(
                            targetValue = if (entry.value > 0) entry.value / yAxisMax else 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedIndex = if (selectedIndex == index) -1 else index },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(chartHeight),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            if (entry.value > 0) {
                                val barHeight = chartHeight * barAnim.value
                                val barShape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                val barWidth = if (data.size > 20) 6.dp else 10.dp
                                val barModifier = Modifier
                                    .width(barWidth)
                                    .height(maxOf(barHeight, 3.dp))

                                Box(
                                    modifier = barModifier
                                        .then(
                                            if (isHighlighted) {
                                                Modifier.border(
                                                    1.dp,
                                                    Color.White.copy(alpha = 0.5f),
                                                    barShape
                                                )
                                            } else Modifier
                                        )
                                        .clip(barShape)
                                        .background(Brush.verticalGradient(barGradient))
                                )
                            }
                        }

                        if (showEmojis) {
                            ChartBrainMascot(
                                scrollCount = entry.value,
                                delayMs = if (staggerBars) index * 100L + 400L else 0L,
                                modifier = Modifier.height(36.dp)
                            )
                        }

                        if (visible) {
                            Text(
                                text = entry.label,
                                color = if (isHighlighted) Color.White else Gray400,
                                fontSize = if (data.size > 20) 8.sp else 9.sp,
                                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                modifier = if (isHighlighted) {
                                    Modifier
                                        .padding(top = 2.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                } else Modifier
                            )
                        } else {
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassBarTooltip(count: Int, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("$count scrolls", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(message, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
    }
}

@Composable
fun HeatmapCalendarView(
    days: List<CalendarDay>,
    monthOffset: Int,
    onMonthChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayCal = remember(monthOffset) {
        Calendar.getInstance().apply { add(Calendar.MONTH, monthOffset) }
    }
    val monthTitle = remember(monthOffset) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(displayCal.time).uppercase()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(24.dp)
                    } else Modifier
                )
                .background(Color.White.copy(alpha = 0.04f))
        )

        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onMonthChange(monthOffset - 1) },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = Color.White)
                }
                Text(
                    monthTitle,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { onMonthChange(monthOffset + 1) },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val daysOfWeek = listOf("M", "T", "W", "T", "F", "S", "S")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                daysOfWeek.forEach {
                    Text(
                        it,
                        color = Gray500,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            days.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    week.forEach { day ->
                        HeatmapDayCell(day = day, modifier = Modifier.weight(1f))
                    }
                    if (week.size < 7) {
                        repeat(7 - week.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HeatmapLegendPill("Consistent", Color(0xFF10B981))
                HeatmapLegendPill("Over Limit", Color(0xFFF43F5E))
                HeatmapLegendPill("No Activity", Color(0xFF6B7280), dimmed = true)
            }
        }
    }
}

@Composable
private fun HeatmapDayCell(day: CalendarDay, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var showTooltip by remember { mutableStateOf(false) }

    val showCircle = day.date != 0 && !day.isPadding && !day.isFuture

    val circleColor = when {
        !showCircle -> Color.Transparent
        !day.hasActivity -> Color(0xFF6B7280).copy(alpha = 0.3f)
        day.underLimit == true -> Color(0xFF10B981)
        day.underLimit == false -> Color(0xFFF43F5E)
        else -> Color(0xFF6B7280).copy(alpha = 0.3f)
    }

    val glowColor = when {
        !showCircle -> Color.Transparent
        day.underLimit == true -> Color(0xFF10B981).copy(alpha = 0.4f)
        day.underLimit == false -> Color(0xFFF43F5E).copy(alpha = 0.4f)
        else -> Color.Transparent
    }

    val scale = if (isHovered && day.date != 0) 1.3f else 1f

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                if (day.date != 0) showTooltip = !showTooltip
            },
        contentAlignment = Alignment.Center
    ) {
        if (showTooltip && day.date != 0) {
            Text(
                text = if (day.hasActivity) "${day.totalScrolls} scrolls" else "No activity",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-8).dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        if (day.date != 0) {
            Text(
                day.date.toString(),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
        if (showCircle) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            ) {
                if (glowColor.alpha > 0f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .blur(8.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(glowColor, Color.Transparent)
                                ),
                                CircleShape
                            )
                    )
                }
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(circleColor)
                )
            }
        }
    }
}

@Composable
private fun HeatmapLegendPill(label: String, color: Color, dimmed: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (!dimmed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .blur(6.dp)
                        .background(color.copy(alpha = 0.4f), CircleShape)
                )
            }
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = if (dimmed) 0.5f else 1f))
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = Gray400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
