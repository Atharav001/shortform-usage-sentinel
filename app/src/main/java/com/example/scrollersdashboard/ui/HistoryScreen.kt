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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrollersdashboard.AppDatabase
import com.example.scrollersdashboard.ScrollRecord
import com.example.scrollersdashboard.ScrollEvent
import com.example.scrollersdashboard.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

data class MonthlySummary(
    val month: String,
    val totalScrolls: Int,
    val totalScreenTime: String,
    val daysUnderGoal: Int,
    val totalDays: Int,
    val avgScrollsPerDay: Int,
    val improvement: Int
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val date: String,
    val icon: String,
    val unlocked: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    db: AppDatabase,
    isDarkMode: Boolean,
    onBack: (Offset) -> Unit
) {
    var selectedView by remember { mutableStateOf("overview") }
    val textColor = if (isDarkMode) Color.White else Color.Black

    val recordsState = db.scrollDao().getRecentRecords().collectAsState(initial = emptyList())
    val igLimitState = db.scrollDao().getSettingFlow("limit_ig").collectAsState(initial = "100")
    val ytLimitState = db.scrollDao().getSettingFlow("limit_yt").collectAsState(initial = "100")

    val igLimit = igLimitState.value?.toIntOrNull() ?: 100
    val ytLimit = ytLimitState.value?.toIntOrNull() ?: 100

    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val endTime = calendar.timeInMillis + (24 * 60 * 60 * 1000)
    calendar.add(Calendar.DAY_OF_YEAR, -7) // We only need current week for peak hours average
    val startTime = calendar.timeInMillis
    
    val eventsState = db.scrollDao().getEventsInRange(startTime, endTime).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History", color = textColor, fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                navigationIcon = {
                    var backCenter by remember { mutableStateOf(Offset.Zero) }
                    IconButton(
                        onClick = { onBack(backCenter) },
                        modifier = Modifier.onGloballyPositioned { backCenter = it.positionInWindow() + Offset(it.size.width / 2f, it.size.height / 2f) }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Gray900
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 20.dp)) {
            // View Toggle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1A1C22))
                    .padding(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    listOf("overview", "achievements").forEach { view ->
                        val isSelected = selectedView == view
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFF2D3139) else Color.Transparent)
                                .clickable { selectedView = view },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                view.replaceFirstChar { it.uppercase() },
                                color = if (isSelected) Color.White else Color.Gray,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedContent(
                targetState = selectedView,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = ""
            ) { view ->
                if (view == "overview") {
                    OverviewView(isDarkMode, recordsState.value, eventsState.value, igLimit, ytLimit)
                } else {
                    AchievementsView(isDarkMode)
                }
            }
        }
    }
}

@Composable
fun OverviewView(isDarkMode: Boolean, records: List<ScrollRecord>, events: List<ScrollEvent>, igLimit: Int, ytLimit: Int) {
    val scrollState = rememberScrollState()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val recordsByDate = records.groupBy { it.date }
    
    // CURRENT WEEK calculation (Last 7 days)
    var thisWeekScrolls = 0
    var thisWeekTime = 0L
    var daysUnderGoal = 0
    val currentWeekCalendar = Calendar.getInstance()
    for (i in 0 until 7) {
        val dateStr = sdf.format(currentWeekCalendar.time)
        val dayRecords = recordsByDate[dateStr] ?: emptyList()
        thisWeekScrolls += dayRecords.sumOf { it.scrollCount }
        thisWeekTime += dayRecords.sumOf { it.screenTimeMillis }
        
        val igCount = dayRecords.find { it.appType == "Instagram" }?.scrollCount ?: 0
        val ytCount = dayRecords.find { it.appType == "YouTube" }?.scrollCount ?: 0
        if (igCount <= igLimit && ytCount <= ytLimit && dayRecords.isNotEmpty()) {
            daysUnderGoal++
        }
        currentWeekCalendar.add(Calendar.DAY_OF_YEAR, -1)
    }
    
    // LAST WEEK calculation (7 days before)
    var lastWeekScrolls = 0
    var lastWeekTime = 0L
    var lastWeekLeastDayCount = Int.MAX_VALUE
    var lastWeekBestDayName = "N/A"
    val dayNameSdf = SimpleDateFormat("EEEE", Locale.getDefault())
    var igLastWeekTotal = 0
    var ytLastWeekTotal = 0
    
    for (i in 0 until 7) {
        val dateStr = sdf.format(currentWeekCalendar.time)
        val dayRecords = recordsByDate[dateStr] ?: emptyList()
        val dayScrolls = dayRecords.sumOf { it.scrollCount }
        lastWeekScrolls += dayScrolls
        lastWeekTime += dayRecords.sumOf { it.screenTimeMillis }
        
        if (dayRecords.isNotEmpty() && dayScrolls < lastWeekLeastDayCount) {
            lastWeekLeastDayCount = dayScrolls
            lastWeekBestDayName = dayNameSdf.format(currentWeekCalendar.time)
        }
        
        igLastWeekTotal += dayRecords.find { it.appType == "Instagram" }?.scrollCount ?: 0
        ytLastWeekTotal += dayRecords.find { it.appType == "YouTube" }?.scrollCount ?: 0
        currentWeekCalendar.add(Calendar.DAY_OF_YEAR, -1)
    }

    val scrollsChange = if (lastWeekScrolls > 0) {
        ((thisWeekScrolls - lastWeekScrolls).toFloat() / lastWeekScrolls * 100)
    } else 0f

    val timeDifference = thisWeekTime - lastWeekTime
    val isTimeSaved = timeDifference <= 0

    // Average Peak Hours Calculation for current week
    val peakHourRange = remember(events) {
        if (events.isEmpty()) return@remember "N/A"
        val dailyPeakStarts = mutableListOf<Int>()
        val weekCal = Calendar.getInstance()
        for (i in 0 until 7) {
            val dateStr = sdf.format(weekCal.time)
            val dayEvents = events.filter { it.date == dateStr }
            if (dayEvents.isNotEmpty()) {
                val hourCounts = IntArray(24)
                dayEvents.forEach { 
                    val h = Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.HOUR_OF_DAY)
                    hourCounts[h]++
                }
                var maxIn3h = -1
                var bestStart = 0
                for (h in 0..21) {
                    val count = hourCounts[h] + hourCounts[h+1] + hourCounts[h+2]
                    if (count > maxIn3h) {
                        maxIn3h = count
                        bestStart = h
                    }
                }
                dailyPeakStarts.add(bestStart)
            }
            weekCal.add(Calendar.DAY_OF_YEAR, -1)
        }
        if (dailyPeakStarts.isEmpty()) return@remember "N/A"
        val avgStart = dailyPeakStarts.average().toInt()
        "${formatHour(avgStart)} - ${formatHour((avgStart + 3) % 24)}"
    }

    Column(modifier = Modifier.verticalScroll(scrollState)) {
        SectionHeader("THIS WEEK VS LAST WEEK")
        Box(modifier = Modifier.fillMaxWidth().liquidGlassCard(isDarkMode).padding(20.dp)) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("This Week", color = Color.Gray, fontSize = 12.sp)
                        Text(thisWeekScrolls.toString(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text("scrolls", color = Color.Gray, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Last Week", color = Color.Gray, fontSize = 12.sp)
                        Text(lastWeekScrolls.toString(), color = Color.Gray, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text("scrolls", color = Color.Gray, fontSize = 12.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val trendColor = if (scrollsChange <= 0) Color(0xFF34C759) else Color(0xFFFF3B30)
                val trendIcon = if (scrollsChange <= 0) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp
                val trendText = if (scrollsChange <= 0) "${Math.abs(scrollsChange).toInt()}% fewer scrolls" else "${scrollsChange.toInt()}% more scrolls"
                
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(trendColor.copy(alpha = 0.15f)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(trendIcon, null, tint = trendColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(trendText, color = trendColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.05f)).padding(12.dp)) {
                        Column {
                            Text("Screen Time", color = Color.Gray, fontSize = 11.sp)
                            Text(formatMillisToTime(thisWeekTime), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Instagram + YouTube", color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.05f)).padding(12.dp)) {
                        Column {
                            Text("Days Under Goal", color = Color.Gray, fontSize = 11.sp)
                            Text("$daysUnderGoal/7", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("IG < limit & YT < limit", color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        SectionHeader("INSIGHTS")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InsightCard(modifier = Modifier.weight(1f), icon = Icons.Default.Schedule, color = Color(0xFF9C27B0), title = "Peak Hours", value = peakHourRange)
            InsightCard(modifier = Modifier.weight(1f), icon = Icons.Default.CalendarToday, color = Color(0xFF2196F3), title = "Best Day", value = lastWeekBestDayName)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val mostUsed = if (igLastWeekTotal >= ytLastWeekTotal) "Instagram" else "YouTube"
            InsightCard(modifier = Modifier.weight(1f), icon = Icons.Default.Smartphone, color = Color(0xFFFF9800), title = "Most Used", value = mostUsed)
            
            InsightCard(
                modifier = Modifier.weight(1f), 
                icon = if (isTimeSaved) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp, 
                color = if (isTimeSaved) Color(0xFF4CAF50) else Color(0xFFFF3B30), 
                title = if (isTimeSaved) "Time Saved" else "Time Wasted", 
                value = formatMillisToTime(Math.abs(timeDifference))
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        SectionHeader("MONTHLY SUMMARIES")
        val monthSdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val recordsByMonth = records.groupBy { 
            try {
                val date = sdf.parse(it.date)
                if (date != null) monthSdf.format(date) else "Unknown"
            } catch (e: Exception) { "Unknown" }
        }.filterKeys { it != "Unknown" }

        recordsByMonth.forEach { (monthName, monthRecords) ->
            val totalMonthScrolls = monthRecords.sumOf { it.scrollCount }
            val totalMonthTime = monthRecords.sumOf { it.screenTimeMillis }
            val uniqueDays = monthRecords.map { it.date }.distinct().size
            val dailyAvg = if (uniqueDays > 0) totalMonthScrolls / uniqueDays else 0
            
            val combinedLimit = igLimit + ytLimit
            val daysUnderGoalCombined = monthRecords.groupBy { it.date }.filter { (_, dayRecs) ->
                val dayTotal = dayRecs.sumOf { it.scrollCount }
                dayTotal <= combinedLimit && dayRecs.isNotEmpty()
            }.size
            
            val monthCal = Calendar.getInstance().apply { 
                try {
                    val date = monthSdf.parse(monthName)
                    if (date != null) time = date
                } catch (e: Exception) {}
            }
            val totalDaysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)

            MonthlySummaryCard(
                MonthlySummary(
                    month = monthName,
                    totalScrolls = totalMonthScrolls,
                    totalScreenTime = formatMillisToTime(totalMonthTime),
                    daysUnderGoal = daysUnderGoalCombined,
                    totalDays = totalDaysInMonth,
                    avgScrollsPerDay = dailyAvg,
                    improvement = 0
                ), 
                isDarkMode
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

fun formatHour(hour: Int): String {
    val h = if (hour == 0 || hour == 12) 12 else hour % 12
    val ampm = if (hour < 12) "AM" else "PM"
    return "$h:00 $ampm"
}

@Composable
fun SectionHeader(title: String) {
    Text(title, color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 16.dp))
}

@Composable
fun InsightCard(modifier: Modifier, icon: ImageVector, color: Color, title: String, value: String) {
    Box(modifier = modifier.clip(RoundedCornerShape(24.dp)).background(color.copy(alpha = 0.1f)).border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(24.dp)).padding(16.dp)) {
        Column {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, color = color.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun MonthlySummaryCard(summary: MonthlySummary, isDarkMode: Boolean) {
    Box(modifier = Modifier.fillMaxWidth().liquidGlassCard(isDarkMode).padding(20.dp)) {
        Column {
            Text(summary.month, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryStat("Total Scrolls", summary.totalScrolls.toString())
                SummaryStat("Screen Time", summary.totalScreenTime)
                SummaryStat("Daily Avg", summary.avgScrollsPerDay.toString())
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Days Under Goal", color = Color.Gray, fontSize = 12.sp)
                Text("${summary.daysUnderGoal}/${summary.totalDays}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            val progress = (summary.daysUnderGoal.toFloat() / summary.totalDays.toFloat()).coerceIn(0f, 1f)
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f))) {
                Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().clip(CircleShape).background(Brush.horizontalGradient(listOf(Color(0xFF34C759), Color(0xFF30D158)))))
            }
        }
    }
}

@Composable
fun SummaryStat(label: String, value: String) {
    Column {
        Text(label, color = Color.Gray, fontSize = 11.sp)
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun AchievementsView(isDarkMode: Boolean) {
    val achievements = listOf(
        Achievement("1", "First Steps", "Started tracking your screen time", "Jan 1, 2026", "🎯", true),
        Achievement("2", "Week Warrior", "Stayed under goal for 7 days straight", "Jan 15, 2026", "⚡", true),
        Achievement("3", "3 Day Streak", "Reduced usage 3 days in a row", "Mar 12, 2026", "🔥", true),
        Achievement("4", "Early Bird", "No scrolling before 9 AM for a week", "Feb 20, 2026", "🌅", true),
        Achievement("5", "Month Master", "Complete 30 days of tracking", "Jan 31, 2026", "🏆", true),
        Achievement("6", "Night Owl Tamer", "No scrolling after 10 PM for 5 days", "Locked", "🌙", false)
    )

    Column {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.horizontalGradient(listOf(Color(0xFFFFD600).copy(alpha = 0.1f), Color(0xFFFF9500).copy(alpha = 0.1f)))).border(1.dp, Color(0xFFFFD600).copy(alpha = 0.2f), RoundedCornerShape(24.dp)).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFFFD600)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.EmojiEvents, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("5 of 6 Unlocked", color = Color(0xFFFFD600), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Keep going to unlock more!", color = Color.LightGray, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(achievements) { achievement ->
                AchievementItem(achievement, isDarkMode)
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
fun AchievementItem(achievement: Achievement, isDarkMode: Boolean) {
    val alpha = if (achievement.unlocked) 1f else 0.5f
    Box(modifier = Modifier.fillMaxWidth().liquidGlassCard(isDarkMode).padding(16.dp).graphicsLayer { this.alpha = alpha }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(if (achievement.unlocked) Color.White.copy(alpha = 0.1f) else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Text(if (achievement.unlocked) achievement.icon else "🔒", fontSize = 28.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(achievement.title, color = if (achievement.unlocked) Color.White else Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(achievement.description, color = Color.Gray, fontSize = 13.sp)
                Text(achievement.date, color = Color.Gray.copy(alpha = 0.7f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

fun formatMillisToTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    if (hours > 0) return "${hours}h ${minutes}m"
    return "${minutes}m"
}
