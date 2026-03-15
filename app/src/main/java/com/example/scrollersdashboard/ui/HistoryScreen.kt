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

@Immutable
data class HistoryMonthlySummary(
    val month: String,
    val totalScrolls: Int,
    val totalScreenTime: String,
    val daysUnderGoal: Int,
    val totalDays: Int,
    val avgScrollsPerDay: Int,
    val improvement: Int
)

@Immutable
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

    val allRecords by db.scrollDao().getAllRecords().collectAsState(initial = emptyList())
    val igLimitState = db.scrollDao().getSettingFlow("limit_ig").collectAsState(initial = "100")
    val ytLimitState = db.scrollDao().getSettingFlow("limit_yt").collectAsState(initial = "100")
    
    val igLimit = igLimitState.value?.toIntOrNull() ?: 100
    val ytLimit = ytLimitState.value?.toIntOrNull() ?: 100

    val todayCal = Calendar.getInstance()
    val startTime = todayCal.timeInMillis - (14 * 24 * 60 * 60 * 1000L)
    val allEvents by db.scrollDao().getEventsInRange(startTime, todayCal.timeInMillis).collectAsState(initial = emptyList())

    val achievements = remember {
        listOf(
            Achievement("1", "First Steps", "Started tracking your screen time", "Jan 1, 2026", "🎯", true),
            Achievement("2", "Week Warrior", "Stayed under goal for 7 days straight", "Jan 15, 2026", "⚡", true),
            Achievement("3", "3 Day Streak", "Reduced usage 3 days in a row", "Mar 12, 2026", "🔥", true),
            Achievement("4", "Early Bird", "No scrolling before 9 AM for a week", "Feb 20, 2026", "🌅", true),
            Achievement("5", "Month Master", "Complete 30 days of tracking", "Jan 31, 2026", "🏆", true),
            Achievement("6", "Night Owl Tamer", "No scrolling after 10 PM for 5 days", "Locked", "🌙", false)
        )
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1F2937), Color(0xFF0F172A))))) {
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
                        Text("History", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 20.dp)) {
                Spacer(modifier = Modifier.height(24.dp))
                
                // View Toggle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1F2937).copy(alpha = 0.5f))
                        .padding(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("overview", "achievements").forEach { view ->
                            val isSelected = selectedView == view
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFF2C333F) else Color.Transparent)
                                    .clickable { selectedView = view },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = view.replaceFirstChar { it.uppercase() },
                                    color = if (isSelected) Color.White else Color(0xFF9CA3AF),
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
                    label = "ViewTransition"
                ) { view ->
                    if (view == "overview") {
                        HistoryOverviewView(allRecords, allEvents, igLimit, ytLimit)
                    } else {
                        AchievementsView(achievements)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryOverviewView(allRecords: List<ScrollRecord>, allEvents: List<ScrollEvent>, igLimit: Int, ytLimit: Int) {
    val scrollState = rememberScrollState()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Calculations
    val stats = remember(allRecords, allEvents, igLimit, ytLimit) {
        val today = Calendar.getInstance()
        val calendar = Calendar.getInstance()
        
        // 1. This Week vs Last Week (Exact combined scroll counts)
        val thisWeekRecords = mutableListOf<ScrollRecord>()
        var thisWeekDaysUnderGoalCount = 0
        for (i in 0 until 7) {
            val d = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dStr = sdf.format(d.time)
            val dayRecs = allRecords.filter { it.date == dStr }
            thisWeekRecords.addAll(dayRecs)
            
            // 3. Days Under Goal where both apps stayed within limits
            val igCount = dayRecs.find { it.appType == "Instagram" }?.scrollCount ?: 0
            val ytCount = dayRecs.find { it.appType == "YouTube" }?.scrollCount ?: 0
            if (dayRecs.isNotEmpty() && igCount <= igLimit && ytCount <= ytLimit) {
                thisWeekDaysUnderGoalCount++
            }
        }
        
        val lastWeekRecords = mutableListOf<ScrollRecord>()
        for (i in 7 until 14) {
            val d = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dStr = sdf.format(d.time)
            lastWeekRecords.addAll(allRecords.filter { it.date == dStr })
        }

        val thisWeekScrolls = thisWeekRecords.sumOf { it.scrollCount }
        val lastWeekScrolls = lastWeekRecords.sumOf { it.scrollCount }
        
        // 2. Trend Bar Dynamic Calculation
        val scrollTrend = if (lastWeekScrolls > 0) {
            ((thisWeekScrolls - lastWeekScrolls).toFloat() / lastWeekScrolls * 100).toInt()
        } else 0

        // 3. Current Week Metrics (Combined Screen Time)
        val thisWeekTimeMillis = thisWeekRecords.sumOf { it.screenTimeMillis }
        val lastWeekTimeMillis = lastWeekRecords.sumOf { it.screenTimeMillis }
        
        // 7. Time Saved / Wasted
        val timeDifferenceMillis = lastWeekTimeMillis - thisWeekTimeMillis
        val timeTrend = if (lastWeekTimeMillis > 0) {
            ((thisWeekTimeMillis - lastWeekTimeMillis).toFloat() / lastWeekTimeMillis * 100).toInt()
        } else 0

        // 4. Peak Hours (Average over last 7 days, 3-hour window)
        val hourCounts = IntArray(24)
        val sevenDaysAgo = today.timeInMillis - (7 * 24 * 60 * 60 * 1000L)
        allEvents.filter { it.timestamp >= sevenDaysAgo }.forEach { event ->
            calendar.timeInMillis = event.timestamp
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            hourCounts[hour]++
        }
        
        var maxWindowSum = -1
        var peakStartHour = 0
        for (h in 0 until 24) {
            val windowSum = hourCounts[h] + hourCounts[(h + 1) % 24] + hourCounts[(h + 2) % 24]
            if (windowSum > maxWindowSum) {
                maxWindowSum = windowSum
                peakStartHour = h
            }
        }
        val peakHoursStr = if (maxWindowSum <= 0) "No data" else {
            val startStr = formatHourLocal(peakStartHour)
            val endStr = formatHourLocal((peakStartHour + 3) % 24)
            "$startStr - $endStr"
        }

        // 5. Best Day (Lowest total usage from last week)
        var minDayUsage = Long.MAX_VALUE
        var bestDayStr = "No data"
        for (i in 7 until 14) {
            val d = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dStr = sdf.format(d.time)
            val dayRecs = allRecords.filter { it.date == dStr }
            val dayUsage = dayRecs.sumOf { it.scrollCount }.toLong()
            if (dayRecs.isNotEmpty() && dayUsage < minDayUsage) {
                minDayUsage = dayUsage
                bestDayStr = SimpleDateFormat("EEEE", Locale.getDefault()).format(d.time)
            }
        }

        // 6. Most Used App (Compare Instagram vs YouTube from last week)
        val igLastWeek = lastWeekRecords.filter { it.appType == "Instagram" }.sumOf { it.scrollCount }
        val ytLastWeek = lastWeekRecords.filter { it.appType == "YouTube" }.sumOf { it.scrollCount }
        val mostUsedApp = when {
            igLastWeek == 0 && ytLastWeek == 0 -> "No data"
            igLastWeek >= ytLastWeek -> "Instagram"
            else -> "YouTube"
        }

        // 8. Monthly Summaries
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val combinedLimit = igLimit + ytLimit
        val monthlyData = allRecords.groupBy { 
            val date = sdf.parse(it.date)
            monthFormat.format(date!!)
        }.map { (month, records) ->
            val totalScrolls = records.sumOf { it.scrollCount }
            val totalMillis = records.sumOf { it.screenTimeMillis }
            val recordsByDate = records.groupBy { it.date }
            // Goal Progress: Combined scrolls under combined limit
            val daysUnder = recordsByDate.count { (_, dayRecs) ->
                val combinedScrolls = dayRecs.sumOf { it.scrollCount }
                combinedScrolls <= combinedLimit
            }
            val totalDaysInMonth = recordsByDate.size
            
            HistoryMonthlySummary(
                month = month,
                totalScrolls = totalScrolls,
                totalScreenTime = formatMillisToTime(totalMillis),
                daysUnderGoal = daysUnder,
                totalDays = totalDaysInMonth,
                avgScrollsPerDay = if (totalDaysInMonth > 0) totalScrolls / totalDaysInMonth else 0,
                improvement = 0 
            )
        }.sortedByDescending { 
            val date = monthFormat.parse(it.month)
            date?.time ?: 0L
        }

        object {
            val thisWeekScrolls = thisWeekScrolls
            val lastWeekScrolls = lastWeekScrolls
            val scrollTrend = scrollTrend
            val thisWeekTime = formatMillisToTime(thisWeekTimeMillis)
            val timeTrend = timeTrend
            val daysUnderGoal = thisWeekDaysUnderGoalCount
            val peakHours = peakHoursStr
            val bestDay = bestDayStr
            val mostUsedApp = mostUsedApp
            val timeDifferenceStr = formatMillisToTime(Math.abs(timeDifferenceMillis))
            val isTimeSaved = timeDifferenceMillis >= 0
            val monthlySummaries = monthlyData
        }
    }

    Column(modifier = Modifier.verticalScroll(scrollState)) {
        // 1. This Week vs Last Week
        SectionHeader("THIS WEEK VS LAST WEEK")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1F2937).copy(alpha = 0.3f))
                .border(1.dp, Color(0xFF374151), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("This Week", color = Color(0xFF6B7280), fontSize = 12.sp)
                        Text(String.format(Locale.getDefault(), "%,d", stats.thisWeekScrolls), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text("scrolls", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Last Week", color = Color(0xFF6B7280), fontSize = 12.sp)
                        Text(String.format(Locale.getDefault(), "%,d", stats.lastWeekScrolls), color = Color(0xFF9CA3AF), fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text("scrolls", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 2. Trend Bar
                val isImprovement = stats.scrollTrend <= 0
                val trendColor = if (isImprovement) Color(0xFF4ADE80) else Color(0xFFEF4444)
                val trendIcon = if (isImprovement) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp
                val trendText = if (isImprovement) "${Math.abs(stats.scrollTrend)}% fewer scrolls" else "${Math.abs(stats.scrollTrend)}% more scrolls"

                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(trendColor.copy(alpha = 0.15f)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(trendIcon, null, tint = trendColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(trendText, color = trendColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 3. Current Week Metrics
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Color(0xFF111827).copy(alpha = 0.5f)).padding(12.dp)) {
                        Column {
                            Text("Screen Time", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                            Text(stats.thisWeekTime, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            val timeTrendColor = if (stats.timeTrend <= 0) Color(0xFF4ADE80) else Color(0xFFEF4444)
                            Text(if (stats.timeTrend <= 0) "↓ ${Math.abs(stats.timeTrend)}%" else "↑ ${stats.timeTrend}%", color = timeTrendColor, fontSize = 11.sp)
                        }
                    }
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Color(0xFF111827).copy(alpha = 0.5f)).padding(12.dp)) {
                        Column {
                            Text("Days Under Goal", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                            Text("${stats.daysUnderGoal}/7", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Both IG + YT", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 4, 5, 6, 7. Insights
        SectionHeader("INSIGHTS")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GradientInsightCard(modifier = Modifier.weight(1f), icon = Icons.Default.AccessTime, color = Color(0xFF9333EA), title = "Peak Hours", value = stats.peakHours)
            GradientInsightCard(modifier = Modifier.weight(1f), icon = Icons.Default.CalendarToday, color = Color(0xFF2563EB), title = "Best Day", value = stats.bestDay)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GradientInsightCard(modifier = Modifier.weight(1f), icon = Icons.Default.Smartphone, color = Color(0xFFF97316), title = "Most Used", value = stats.mostUsedApp)
            GradientInsightCard(modifier = Modifier.weight(1f), icon = if (stats.isTimeSaved) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp, color = if (stats.isTimeSaved) Color(0xFF22C55E) else Color(0xFFEF4444), title = if (stats.isTimeSaved) "Time Saved" else "Time Wasted", value = stats.timeDifferenceStr)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 8. Monthly Summaries
        SectionHeader("MONTHLY SUMMARIES")
        if (stats.monthlySummaries.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No history data available yet.", color = Color.Gray, fontSize = 14.sp)
            }
        }
        stats.monthlySummaries.forEach { summary ->
            MonthlyHistorySummaryCard(summary)
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun GradientInsightCard(modifier: Modifier, icon: ImageVector, color: Color, title: String, value: String) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(color.copy(alpha = 0.15f), color.copy(alpha = 0.25f))))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun MonthlyHistorySummaryCard(summary: HistoryMonthlySummary) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1F2937).copy(alpha = 0.3f))
            .border(1.dp, Color(0xFF374151), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(summary.month, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryStatItem("Total Scrolls", String.format(Locale.getDefault(), "%,d", summary.totalScrolls))
                SummaryStatItem("Screen Time", summary.totalScreenTime)
                SummaryStatItem("Daily Avg", summary.avgScrollsPerDay.toString())
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFF374151))
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Goal Progress Bar", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                Text("${summary.daysUnderGoal}/${summary.totalDays} days", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            val progress = if (summary.totalDays > 0) (summary.daysUnderGoal.toFloat() / summary.totalDays.toFloat()).coerceIn(0f, 1f) else 0f
            Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color(0xFF111827).copy(alpha = 0.5f))) {
                Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().clip(CircleShape).background(Brush.horizontalGradient(listOf(Color(0xFF22C55E), Color(0xFF34C759)))))
            }
        }
    }
}

@Composable
fun SummaryStatItem(label: String, value: String) {
    Column {
        Text(label, color = Color(0xFF9CA3AF), fontSize = 11.sp)
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun AchievementsView(achievements: List<Achievement>) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFFEAB308).copy(alpha = 0.15f), Color(0xFFF97316).copy(alpha = 0.15f))))
                .border(1.dp, Color(0xFFEAB308).copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFEAB308)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.EmojiEvents, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    val unlockedCount = achievements.count { it.unlocked }
                    Text("$unlockedCount of ${achievements.size} Unlocked", color = Color(0xFFFACC15), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Keep going to unlock more!", color = Color(0xFFD1D5DB), fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(achievements) { achievement ->
                AchievementItem(achievement)
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
fun AchievementItem(achievement: Achievement) {
    val itemAlpha = if (achievement.unlocked) 1f else 0.5f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1F2937).copy(alpha = if (achievement.unlocked) 0.3f else 0.1f))
            .border(1.dp, if (achievement.unlocked) Color(0xFF374151) else Color(0xFF1F2937), RoundedCornerShape(20.dp))
            .padding(16.dp)
            .graphicsLayer { alpha = itemAlpha }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(if (achievement.unlocked) Color(0xFF374151) else Color(0xFF111827).copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (achievement.unlocked) achievement.icon else "🔒", fontSize = 28.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(achievement.title, color = if (achievement.unlocked) Color.White else Color(0xFF6B7280), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(achievement.description, color = if (achievement.unlocked) Color(0xFF9CA3AF) else Color(0xFF4B5563), fontSize = 13.sp)
                Text(achievement.date, color = if (achievement.unlocked) Color(0xFF6B7280) else Color(0xFF374151), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(title, color = Color(0xFF6B7280), fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 16.dp, top = 8.dp))
}

private fun formatHourLocal(hour: Int): String {
    val h = if (hour == 0 || hour == 12) 12 else hour % 12
    val ampm = if (hour < 12) "AM" else "PM"
    return "$h $ampm"
}
