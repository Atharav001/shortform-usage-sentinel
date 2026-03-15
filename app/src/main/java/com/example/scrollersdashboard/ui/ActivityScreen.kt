package com.example.scrollersdashboard.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrollersdashboard.AppDatabase
import com.example.scrollersdashboard.ui.theme.*
import java.util.*

@Immutable
data class Session(
    val id: String,
    val startTime: String,
    val endTime: String?,
    val scrolls: Int,
    val duration: String,
    val instagram: Int,
    val youtube: Int
)

@Immutable
data class ChartEntry(
    val label: String,
    val value: Int
)

@Immutable
data class CalendarDay(
    val date: Int,
    val underLimit: Boolean?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    db: AppDatabase,
    isDarkMode: Boolean,
    refreshKey: Int,
    onBack: (Offset) -> Unit
) {
    var selectedTab by remember { mutableStateOf("Day") }
    
    // Data from .tsx
    val dayData = remember {
        listOf(
            ChartEntry("0h", 0), ChartEntry("1h", 43), ChartEntry("2h", 52),
            ChartEntry("3h", 0), ChartEntry("4h", 0), ChartEntry("5h", 0),
            ChartEntry("6h", 15), ChartEntry("7h", 8), ChartEntry("8h", 0),
            ChartEntry("9h", 0), ChartEntry("10h", 0), ChartEntry("11h", 0),
            ChartEntry("12h", 38), ChartEntry("13h", 51), ChartEntry("14h", 12),
            ChartEntry("15h", 31), ChartEntry("16h", 35), ChartEntry("17h", 28),
            ChartEntry("18h", 19), ChartEntry("19h", 0), ChartEntry("20h", 0),
            ChartEntry("21h", 0), ChartEntry("22h", 0), ChartEntry("23h", 0)
        )
    }

    val weekData = remember {
        listOf(
            ChartEntry("Mon", 156), ChartEntry("Tue", 234), ChartEntry("Wed", 189),
            ChartEntry("Thu", 278), ChartEntry("Fri", 312), ChartEntry("Sat", 245),
            ChartEntry("Sun", 198)
        )
    }

    val monthData = remember {
        listOf(
            ChartEntry("1", 156), ChartEntry("2", 234), ChartEntry("3", 189),
            ChartEntry("4", 278), ChartEntry("5", 312), ChartEntry("6", 245),
            ChartEntry("7", 198), ChartEntry("8", 167), ChartEntry("9", 223),
            ChartEntry("10", 201), ChartEntry("11", 245), ChartEntry("12", 189),
            ChartEntry("13", 267), ChartEntry("14", 236)
        )
    }

    val calendarDays = remember {
        listOf(
            CalendarDay(1, true), CalendarDay(2, false), CalendarDay(3, true),
            CalendarDay(4, false), CalendarDay(5, false), CalendarDay(6, true),
            CalendarDay(7, true), CalendarDay(8, true), CalendarDay(9, false),
            CalendarDay(10, true), CalendarDay(11, false), CalendarDay(12, true),
            CalendarDay(13, false), CalendarDay(14, true)
        ) + (15..31).map { CalendarDay(it, null) }
    }

    val sessions = remember {
        listOf(
            Session("1", "5:14 pm", "5:16 pm", 11, "1m", 11, 0),
            Session("2", "4:27 pm", null, 1, "0s", 1, 0),
            Session("3", "4:13 pm", "4:20 pm", 47, "7m", 18, 29),
            Session("4", "3:54 pm", "3:57 pm", 36, "3m", 1, 35),
            Session("5", "2:51 pm", "2:52 pm", 12, "51s", 12, 0)
        )
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Color(0xFF1F2937), Color(0xFF111827))))
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    var backCenter by remember { mutableStateOf(Offset.Zero) }
                    IconButton(
                        onClick = { onBack(backCenter) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1F2937).copy(alpha = 0.5f))
                            .onGloballyPositioned {
                                backCenter = it.positionInWindow() + Offset(it.size.width / 2f, it.size.height / 2f)
                            }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Activity Analysis", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Tab Switcher
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1F2937).copy(alpha = 0.5f))
                    .padding(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("Day", "Week", "Month").forEach { tab ->
                        val isSelected = selectedTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFF2C333F) else Color.Transparent)
                                .clickable { selectedTab = tab },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab,
                                color = if (isSelected) Color.White else Gray400,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    val currentData = when (selectedTab) {
                        "Day" -> dayData
                        "Week" -> weekData
                        else -> monthData
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF1F2937).copy(alpha = 0.3f))
                            .border(1.dp, Color(0xFF374151), RoundedCornerShape(24.dp))
                            .padding(16.dp)
                    ) {
                        ActivityBarChart(
                            data = currentData,
                            maxBarSize = if (selectedTab == "Day") 12.dp else 24.dp,
                            interval = if (selectedTab == "Day") 5 else 0
                        )
                    }
                }

                if (selectedTab == "Month") {
                    item {
                        MonthCalendarView(calendarDays)
                    }
                }

                item {
                    Text(
                        "SESSIONS",
                        color = Gray400,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                items(sessions) { session ->
                    SessionItem(session)
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
fun ActivityBarChart(
    data: List<ChartEntry>,
    maxBarSize: androidx.compose.ui.unit.Dp,
    interval: Int
) {
    val textMeasurer = rememberTextMeasurer()
    val maxVal = (data.maxOfOrNull { it.value } ?: 55).coerceAtLeast(55).toFloat()
    
    Canvas(modifier = Modifier.fillMaxWidth().height(280.dp)) {
        val width = size.width
        val height = size.height
        val paddingBottom = 30.dp.toPx()
        val paddingLeft = 35.dp.toPx()
        val chartWidth = width - paddingLeft
        val chartHeight = height - paddingBottom
        
        // Grid lines
        val ticks = listOf(0, 13, 27, 41, 55)
        ticks.forEach { tick ->
            val y = chartHeight - (tick / maxVal) * chartHeight
            drawLine(
                color = Color(0xFF374151),
                start = Offset(paddingLeft, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
            
            val textStyle = TextStyle(color = Gray400, fontSize = 10.sp)
            val textLayout = textMeasurer.measure(tick.toString(), style = textStyle)
            drawText(
                textMeasurer = textMeasurer,
                text = tick.toString(),
                style = textStyle,
                topLeft = Offset(paddingLeft - textLayout.size.width - 8.dp.toPx(), y - textLayout.size.height / 2)
            )
        }

        val barAreaWidth = chartWidth / data.size
        val barWidthPx = maxBarSize.toPx()
        
        data.forEachIndexed { index, entry ->
            val barHeight = (entry.value / maxVal) * chartHeight
            val x = paddingLeft + (index * barAreaWidth) + (barAreaWidth - barWidthPx) / 2
            val y = chartHeight - barHeight
            
            drawRoundRect(
                color = when {
                    entry.value == 0 -> Color(0xFF374151)
                    entry.value < 20 -> Color(0xFFEC4899)
                    entry.value < 40 -> Color(0xFFF97316)
                    else -> Color(0xFFEF4444)
                },
                topLeft = Offset(x, y),
                size = Size(barWidthPx, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            if (interval == 0 || index % interval == 0) {
                val textStyle = TextStyle(color = Gray400, fontSize = 10.sp)
                val textLayout = textMeasurer.measure(entry.label, style = textStyle)
                drawText(
                    textMeasurer = textMeasurer,
                    text = entry.label,
                    style = textStyle,
                    topLeft = Offset(x + (barWidthPx - textLayout.size.width) / 2, height - textLayout.size.height)
                )
            }
        }
    }
}

@Composable
fun MonthCalendarView(days: List<CalendarDay>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1F2937).copy(alpha = 0.3f))
            .border(1.dp, Color(0xFF374151), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column {
            Text("March 2026", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
            
            val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                daysOfWeek.forEach {
                    Text(it, color = Gray500, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val chunks = days.chunked(7)
            chunks.forEach { week ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    week.forEach { day ->
                        Box(
                            modifier = Modifier.weight(1f).aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(day.date.toString(), color = Gray300, fontSize = 14.sp)
                            if (day.underLimit != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 4.dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (day.underLimit) Color(0xFF22C55E) else Color(0xFFEF4444))
                                )
                            }
                        }
                    }
                    if (week.size < 7) {
                        repeat(7 - week.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFF374151))
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF22C55E)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Under limit", color = Gray400, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Over limit", color = Gray400, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SessionItem(session: Session) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1F2937).copy(alpha = 0.3f))
            .border(1.dp, Color(0xFF374151), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, null, tint = Gray400, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${session.startTime}${if (session.endTime != null) " - ${session.endTime}" else ""}",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${session.scrolls} scrolls • ${session.duration} duration",
                    color = Gray400,
                    fontSize = 13.sp
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (session.instagram > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF9333EA), Color(0xFFDB2777), Color(0xFFF97316))))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("${session.instagram} IG", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
                if (session.youtube > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEF4444))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("${session.youtube} YT", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
