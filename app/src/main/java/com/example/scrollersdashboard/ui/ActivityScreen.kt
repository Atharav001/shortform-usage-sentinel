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
import com.example.scrollersdashboard.ScrollEvent
import com.example.scrollersdashboard.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Immutable
data class Session(
    val startTime: String,
    val endTime: String,
    val scrolls: Int,
    val appType: String,
    val startTimestamp: Long
)

@Immutable
data class HourlyData(
    val label: String,
    val instagramCount: Int,
    val youtubeCount: Int
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
    val today = remember(refreshKey) { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    // Real data for analysis
    val allRecords by db.scrollDao().getAllRecords().collectAsState(initial = emptyList())
    val dayEvents by db.scrollDao().getEventsForDate(today).collectAsState(initial = emptyList())
    
    val igLimitStr by db.scrollDao().getSettingFlow("limit_ig").collectAsState(initial = "100")
    val ytLimitStr by db.scrollDao().getSettingFlow("limit_yt").collectAsState(initial = "100")
    val igLimit = igLimitStr?.toIntOrNull() ?: 100
    val ytLimit = ytLimitStr?.toIntOrNull() ?: 100

    // Processing Hourly Data (Day Tab)
    val hourlyDataList = remember(dayEvents) {
        val list = MutableList(24) { hour -> HourlyData("${hour}h", 0, 0) }
        val cal = Calendar.getInstance()
        dayEvents.forEach { event ->
            cal.timeInMillis = event.timestamp
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            if (hour in 0..23) {
                if (event.appType == "Instagram") {
                    list[hour] = list[hour].copy(instagramCount = list[hour].instagramCount + 1)
                } else if (event.appType == "YouTube") {
                    list[hour] = list[hour].copy(youtubeCount = list[hour].youtubeCount + 1)
                }
            }
        }
        list
    }

    // Processing Weekly Data (Week Tab)
    val weeklyDataList = remember(allRecords) {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        
        List(7) { i ->
            val dateStr = sdf.format(cal.time)
            val ig = allRecords.find { it.date == dateStr && it.appType == "Instagram" }?.scrollCount ?: 0
            val yt = allRecords.find { it.date == dateStr && it.appType == "YouTube" }?.scrollCount ?: 0
            val label = dayNames[i]
            cal.add(Calendar.DAY_OF_YEAR, 1)
            HourlyData(label, ig, yt)
        }
    }

    // Processing Monthly Data (Month Tab)
    val monthlyDataList = remember(allRecords) {
        val cal = Calendar.getInstance()
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        List(daysInMonth) { i ->
            val dateStr = sdf.format(cal.time)
            val ig = allRecords.find { it.date == dateStr && it.appType == "Instagram" }?.scrollCount ?: 0
            val yt = allRecords.find { it.date == dateStr && it.appType == "YouTube" }?.scrollCount ?: 0
            cal.add(Calendar.DAY_OF_YEAR, 1)
            ChartEntry((i + 1).toString(), ig + yt)
        }
    }

    // Processing Calendar Data (Current Month)
    val calendarDays = remember(allRecords, igLimit, ytLimit) {
        val cal = Calendar.getInstance()
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val monthStartDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
        
        val paddingCount = if (monthStartDayOfWeek == Calendar.SUNDAY) 6 else monthStartDayOfWeek - Calendar.MONDAY
        val padding = List(paddingCount) { CalendarDay(0, null) }
        
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val days = List(daysInMonth) { i ->
            val dateStr = sdf.format(cal.time)
            val ig = allRecords.find { it.date == dateStr && it.appType == "Instagram" }?.scrollCount ?: 0
            val yt = allRecords.find { it.date == dateStr && it.appType == "YouTube" }?.scrollCount ?: 0
            
            val totalScrolls = ig + yt
            val hasActivity = totalScrolls > 0
            val isFuture = cal.timeInMillis > System.currentTimeMillis()
            
            // Marks green when total scrolls are below combined limit, red otherwise
            val underLimit = when {
                isFuture -> null
                !hasActivity -> null
                else -> totalScrolls <= (igLimit + ytLimit)
            }
            
            val day = CalendarDay(i + 1, underLimit)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            day
        }
        padding + days
    }

    val sessions = remember(dayEvents) {
        val result = mutableListOf<Session>()
        if (dayEvents.isEmpty()) return@remember result

        var currentSessionEvents = mutableListOf<ScrollEvent>()
        val SESSION_THRESHOLD_MS = 2 * 60 * 1000L

        dayEvents.forEach { event ->
            if (currentSessionEvents.isEmpty()) {
                currentSessionEvents.add(event)
            } else {
                val lastEvent = currentSessionEvents.last()
                if (event.appType == lastEvent.appType && (event.timestamp - lastEvent.timestamp) < SESSION_THRESHOLD_MS) {
                    currentSessionEvents.add(event)
                } else {
                    result.add(createSessionFromEvents(currentSessionEvents))
                    currentSessionEvents = mutableListOf(event)
                }
            }
        }
        if (currentSessionEvents.isNotEmpty()) {
            result.add(createSessionFromEvents(currentSessionEvents))
        }
        result.reversed()
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF1F2937).copy(alpha = 0.3f))
                            .border(1.dp, Color(0xFF374151), RoundedCornerShape(24.dp))
                            .padding(16.dp)
                    ) {
                        when (selectedTab) {
                            "Day" -> DualBarActivityChart(data = hourlyDataList, labelInterval = 4)
                            "Week" -> DualBarActivityChart(data = weeklyDataList, labelInterval = 1)
                            "Month" -> MonthlyCombinedChart(data = monthlyDataList)
                        }
                    }
                }

                if (selectedTab == "Month") {
                    item {
                        MonthCalendarView(calendarDays)
                    }
                }

                if (selectedTab == "Day") {
                    item {
                        Text(
                            "SESSIONS",
                            color = Gray400,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    if (sessions.isEmpty()) {
                        item {
                            Text(
                                "No scrolling activity recorded for today.",
                                color = Gray500,
                                fontSize = 14.sp,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        items(sessions) { session ->
                            SessionItem(session)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

private fun createSessionFromEvents(events: List<ScrollEvent>): Session {
    val first = events.first()
    val last = events.last()
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    return Session(
        startTime = timeFormat.format(Date(first.timestamp)),
        endTime = timeFormat.format(Date(last.timestamp)),
        scrolls = events.size,
        appType = first.appType,
        startTimestamp = first.timestamp
    )
}

@Composable
fun DualBarActivityChart(data: List<HourlyData>, labelInterval: Int) {
    val textMeasurer = rememberTextMeasurer()
    val maxVal = remember(data) {
        val peak = data.maxOfOrNull { maxOf(it.instagramCount, it.youtubeCount) } ?: 0
        maxOf(peak.toFloat(), 20f) * 1.2f
    }
    
    Canvas(modifier = Modifier.fillMaxWidth().height(280.dp)) {
        val width = size.width
        val height = size.height
        val paddingBottom = 30.dp.toPx()
        val paddingLeft = 35.dp.toPx()
        val chartWidth = width - paddingLeft
        val chartHeight = height - paddingBottom
        
        // Y-Axis Ticks
        val tickCount = 5
        for (i in 0 until tickCount) {
            val tickVal = (maxVal / (tickCount - 1) * i).toInt()
            val y = chartHeight - (tickVal / maxVal) * chartHeight
            drawLine(
                color = Color(0xFF374151),
                start = Offset(paddingLeft, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
            
            val textStyle = TextStyle(color = Gray400, fontSize = 10.sp)
            val textLayout = textMeasurer.measure(tickVal.toString(), style = textStyle)
            drawText(
                textMeasurer = textMeasurer,
                text = tickVal.toString(),
                style = textStyle,
                topLeft = Offset(paddingLeft - textLayout.size.width - 8.dp.toPx(), y - textLayout.size.height / 2)
            )
        }

        val areaWidth = chartWidth / data.size
        val barGap = 2.dp.toPx()
        val barWidth = (areaWidth - barGap * 3) / 2
        
        data.forEachIndexed { index, entry ->
            // Instagram Bar
            if (entry.instagramCount > 0) {
                val igBarHeight = (entry.instagramCount / maxVal) * chartHeight
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFF9333EA), Color(0xFFDB2777))),
                    topLeft = Offset(paddingLeft + index * areaWidth + barGap, chartHeight - igBarHeight),
                    size = Size(barWidth, igBarHeight),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }

            // YouTube Bar
            if (entry.youtubeCount > 0) {
                val ytBarHeight = (entry.youtubeCount / maxVal) * chartHeight
                drawRoundRect(
                    color = Color(0xFFEF4444),
                    topLeft = Offset(paddingLeft + index * areaWidth + barWidth + barGap * 2, chartHeight - ytBarHeight),
                    size = Size(barWidth, ytBarHeight),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }

            // X-Axis Labels
            if (index % labelInterval == 0) {
                val textStyle = TextStyle(color = Gray400, fontSize = 10.sp)
                val textLayout = textMeasurer.measure(entry.label, style = textStyle)
                drawText(
                    textMeasurer = textMeasurer,
                    text = entry.label,
                    style = textStyle,
                    topLeft = Offset(paddingLeft + index * areaWidth + (areaWidth - textLayout.size.width) / 2, height - textLayout.size.height)
                )
            }
        }
    }
}

@Composable
fun MonthlyCombinedChart(data: List<ChartEntry>) {
    val textMeasurer = rememberTextMeasurer()
    val maxVal = remember(data) {
        val peak = data.maxOfOrNull { it.value } ?: 0
        maxOf(peak.toFloat(), 50f) * 1.2f
    }
    
    Canvas(modifier = Modifier.fillMaxWidth().height(280.dp)) {
        val width = size.width
        val height = size.height
        val paddingBottom = 30.dp.toPx()
        val paddingLeft = 35.dp.toPx()
        val chartWidth = width - paddingLeft
        val chartHeight = height - paddingBottom
        
        // Y-Axis Ticks
        val tickCount = 5
        for (i in 0 until tickCount) {
            val tickVal = (maxVal / (tickCount - 1) * i).toInt()
            val y = chartHeight - (tickVal / maxVal) * chartHeight
            drawLine(
                color = Color(0xFF374151),
                start = Offset(paddingLeft, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
            
            val textStyle = TextStyle(color = Gray400, fontSize = 10.sp)
            val textLayout = textMeasurer.measure(tickVal.toString(), style = textStyle)
            drawText(
                textMeasurer = textMeasurer,
                text = tickVal.toString(),
                style = textStyle,
                topLeft = Offset(paddingLeft - textLayout.size.width - 8.dp.toPx(), y - textLayout.size.height / 2)
            )
        }

        val areaWidth = chartWidth / data.size
        val barWidth = areaWidth * 0.8f
        
        data.forEachIndexed { index, entry ->
            if (entry.value > 0) {
                val barHeight = (entry.value / maxVal) * chartHeight
                drawRoundRect(
                    color = Color(0xFF3B82F6),
                    topLeft = Offset(paddingLeft + index * areaWidth + (areaWidth - barWidth) / 2, chartHeight - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }

            // X-Axis Labels (Scale of 5)
            val day = entry.label.toInt()
            if (day == 1 || day % 5 == 0) {
                val textStyle = TextStyle(color = Gray400, fontSize = 10.sp)
                val textLayout = textMeasurer.measure(entry.label, style = textStyle)
                drawText(
                    textMeasurer = textMeasurer,
                    text = entry.label,
                    style = textStyle,
                    topLeft = Offset(paddingLeft + index * areaWidth + (areaWidth - textLayout.size.width) / 2, height - textLayout.size.height)
                )
            }
        }
    }
}

@Composable
fun MonthCalendarView(days: List<CalendarDay>) {
    val monthName = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1F2937).copy(alpha = 0.3f))
            .border(1.dp, Color(0xFF374151), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(monthName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
            
            val daysOfWeek = listOf("M", "T", "W", "T", "F", "S", "S")
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
                            if (day.date != 0) {
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
                        "${session.startTime} - ${session.endTime}",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${session.scrolls} scrolls",
                    color = Gray400,
                    fontSize = 13.sp
                )
            }
            
            if (session.appType == "Instagram") {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF9333EA), Color(0xFFDB2777), Color(0xFFF97316))))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("Instagram", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            } else if (session.appType == "YouTube") {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEF4444))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("YouTube", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
