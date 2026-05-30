package com.example.scrollersdashboard.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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

    val allRecords by db.scrollDao().getAllRecords().collectAsState(initial = emptyList())
    val dayEvents by db.scrollDao().getEventsForDate(today).collectAsState(initial = emptyList())
    
    val igLimitStr by db.scrollDao().getSettingFlow("limit_ig").collectAsState(initial = "100")
    val ytLimitStr by db.scrollDao().getSettingFlow("limit_yt").collectAsState(initial = "100")
    val igLimit = igLimitStr?.toIntOrNull() ?: 100
    val ytLimit = ytLimitStr?.toIntOrNull() ?: 100

    // Processing Logic
    val currentHourIndex = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val weekTodayIndex = remember {
        when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            else -> 6
        }
    }
    val monthTodayIndex = remember { Calendar.getInstance().get(Calendar.DAY_OF_MONTH) - 1 }

    val hourlyDataList = remember(dayEvents) {
        val list = MutableList(24) { hour -> HourlyData(formatChartHour(hour), 0, 0) }
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

    var calendarMonthOffset by remember { mutableIntStateOf(0) }

    val calendarDays = remember(allRecords, igLimit, ytLimit, calendarMonthOffset) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, calendarMonthOffset)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val monthStartDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

        val paddingCount = if (monthStartDayOfWeek == Calendar.SUNDAY) 6 else monthStartDayOfWeek - Calendar.MONDAY
        val padding = List(paddingCount) { CalendarDay(0, null, isPadding = true) }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayCal = Calendar.getInstance()
        val days = List(daysInMonth) { i ->
            val dateStr = sdf.format(cal.time)
            val ig = allRecords.find { it.date == dateStr && it.appType == "Instagram" }?.scrollCount ?: 0
            val yt = allRecords.find { it.date == dateStr && it.appType == "YouTube" }?.scrollCount ?: 0

            val totalScrolls = ig + yt
            val hasActivity = totalScrolls > 0
            val isFuture = cal.get(Calendar.YEAR) > todayCal.get(Calendar.YEAR) ||
                (cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) > todayCal.get(Calendar.DAY_OF_YEAR))

            val underLimit = when {
                isFuture -> null
                !hasActivity -> null
                else -> totalScrolls <= (igLimit + ytLimit)
            }

            val day = CalendarDay(
                date = i + 1,
                underLimit = underLimit,
                totalScrolls = totalScrolls,
                hasActivity = hasActivity,
                isFuture = isFuture
            )
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

    Box(modifier = Modifier.fillMaxSize().marbleBackground()) {
        Scaffold(
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        var backCenter by remember { mutableStateOf(Offset.Zero) }
                        IconButton(
                            onClick = { onBack(backCenter) },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(CardBackground)
                                .border(1.dp, GlassBorder, CircleShape)
                                .onGloballyPositioned {
                                    backCenter = it.positionInWindow() + Offset(it.size.width / 2f, it.size.height / 2f)
                                }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Usage Analytics", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
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
                NeumorphicTabSwitcher(
                    tabs = listOf("Day", "Week", "Month"),
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    isDarkMode = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(28.dp))
                                .background(CardBackground)
                                .border(1.dp, GlassBorder, RoundedCornerShape(28.dp))
                                .padding(20.dp)
                        ) {
                            AnimatedContent(
                                targetState = selectedTab,
                                transitionSpec = {
                                    (fadeIn(tween(400)) + scaleIn(initialScale = 0.95f)).togetherWith(fadeOut(tween(200)))
                                },
                                label = "ChartSwitch"
                            ) { tab ->
                                when (tab) {
                                    "Day" -> AnimatedBarChart(
                                        data = hourlyDataList.map {
                                            ChartEntry(it.label, it.instagramCount + it.youtubeCount)
                                        },
                                        labelInterval = 4,
                                        highlightedIndices = setOf(currentHourIndex),
                                        showEmojis = false
                                    )
                                    "Week" -> AnimatedBarChart(
                                        data = weeklyDataList.map {
                                            ChartEntry(it.label, it.instagramCount + it.youtubeCount)
                                        },
                                        labelInterval = 1,
                                        highlightedIndices = setOf(weekTodayIndex),
                                        showEmojis = true
                                    )
                                    "Month" -> AnimatedBarChart(
                                        data = monthlyDataList,
                                        labelInterval = if (monthlyDataList.size > 28) 5 else 4,
                                        highlightedIndices = setOf(monthTodayIndex),
                                        showEmojis = false
                                    )
                                }
                            }
                        }
                    }

                    item {
                        AnimatedVisibility(
                            visible = selectedTab == "Month",
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            HeatmapCalendarView(
                                days = calendarDays,
                                monthOffset = calendarMonthOffset,
                                onMonthChange = { calendarMonthOffset = it }
                            )
                        }
                    }

                    if (selectedTab == "Day") {
                        item {
                            SectionTitle("Recent Sessions", Icons.Default.AccessTime)
                        }

                        if (sessions.isEmpty()) {
                            item {
                                Text(
                                    "No scrolling activity recorded today.",
                                    color = Gray500,
                                    fontSize = 14.sp,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            items(sessions, key = { it.startTimestamp }) { session ->
                                Box(modifier = Modifier.animateItem()) {
                                    SessionItem(session)
                                }
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
}

private fun formatChartHour(hour: Int): String {
    val h = hour % 24
    return when {
        h == 0 -> "12a"
        h < 12 -> "${h}a"
        h == 12 -> "12p"
        else -> "${h - 12}p"
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
fun SessionItem(session: Session) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground)
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, null, tint = Gray500, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${session.startTime} - ${session.endTime}",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "${session.scrolls} scrolls",
                    color = Gray400,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 22.dp)
                )
            }
            
            val appGradient = if (session.appType == "Instagram") InstagramGradient else YouTubeGradient
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(appGradient))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(session.appType.uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
            }
        }
    }
}
