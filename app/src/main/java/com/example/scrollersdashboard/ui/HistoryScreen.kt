package com.example.scrollersdashboard.ui

import android.os.Build
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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

enum class AchievementRarity { COMMON, RARE, EPIC }

@Immutable
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val date: String,
    val icon: String,
    val rarity: AchievementRarity = AchievementRarity.COMMON,
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
            Achievement("1", "Digital Pioneer", "Started your journey to digital wellness", "Jan 1, 2026", "✨", AchievementRarity.COMMON, true),
            Achievement("2", "Consistency King", "Stayed under goal for 7 days straight", "Jan 15, 2026", "👑", AchievementRarity.EPIC, true),
            Achievement("3", "Focus Fire", "Reduced usage for 3 consecutive days", "Mar 12, 2026", "🔥", AchievementRarity.RARE, true),
            Achievement("4", "Dawn Guardian", "No morning scrolling for a full week", "Feb 20, 2026", "🌅", AchievementRarity.RARE, true),
            Achievement("5", "Obsidian Master", "Complete 30 days of mindful scrolling", "Jan 31, 2026", "💎", AchievementRarity.EPIC, true),
            Achievement("6", "Midnight Sentinel", "Zero activity after 11 PM for 5 days", "Locked", "🌌", AchievementRarity.RARE, false)
        )
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
                        Text("Usage History", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    }
                }
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 20.dp)) {
                NeumorphicTabSwitcher(
                    tabs = listOf("Overview", "Achievements"),
                    selectedTab = if (selectedView == "overview") "Overview" else "Achievements",
                    onTabSelected = { selectedView = it.lowercase() },
                    isDarkMode = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                AnimatedContent(
                    targetState = selectedView,
                    transitionSpec = {
                        (fadeIn(tween(400)) + scaleIn(initialScale = 0.92f, animationSpec = tween(400)))
                            .togetherWith(fadeOut(tween(200)))
                    },
                    label = "HistoryViewTransition"
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

    val stats = remember(allRecords, allEvents, igLimit, ytLimit) {
        val today = Calendar.getInstance()
        val calendar = Calendar.getInstance()
        
        val thisWeekRecords = mutableListOf<ScrollRecord>()
        var thisWeekDaysUnderGoalCount = 0
        for (i in 0 until 7) {
            val d = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dStr = sdf.format(d.time)
            val dayRecs = allRecords.filter { it.date == dStr }
            thisWeekRecords.addAll(dayRecs)
            
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
        
        val scrollTrend = if (lastWeekScrolls > 0) {
            ((thisWeekScrolls - lastWeekScrolls).toFloat() / lastWeekScrolls * 100).toInt()
        } else 0

        val thisWeekTimeMillis = thisWeekRecords.sumOf { it.screenTimeMillis }
        val lastWeekTimeMillis = lastWeekRecords.sumOf { it.screenTimeMillis }
        
        val timeDifferenceMillis = lastWeekTimeMillis - thisWeekTimeMillis
        val timeTrend = if (lastWeekTimeMillis > 0) {
            ((thisWeekTimeMillis - lastWeekTimeMillis).toFloat() / lastWeekTimeMillis * 100).toInt()
        } else 0

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

        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val combinedLimit = igLimit + ytLimit
        val monthlyData = allRecords.groupBy { 
            val date = sdf.parse(it.date)
            monthFormat.format(date!!)
        }.map { (month, records) ->
            val totalScrolls = records.sumOf { it.scrollCount }
            val totalMillis = records.sumOf { it.screenTimeMillis }
            val recordsByDate = records.groupBy { it.date }
            val daysUnder = recordsByDate.count { (_, dayRecs) ->
                val combinedScrolls = dayRecs.sumOf { it.scrollCount }
                combinedScrolls <= combinedLimit
            }
            
            val calForMonth = Calendar.getInstance()
            calForMonth.time = monthFormat.parse(month)!!
            val totalDaysInMonth = calForMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
            
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
            val timeDifferenceStr = formatMillisToTime(Math.abs(timeDifferenceMillis))
            val isTimeSaved = timeDifferenceMillis >= 0
            val monthlySummaries = monthlyData
        }
    }

    Column(modifier = Modifier.verticalScroll(scrollState)) {
        SectionTitle("Weekly Comparison", Icons.Default.BarChart)
        WeeklyComparisonHeroCard(
            thisWeekScrolls = stats.thisWeekScrolls,
            lastWeekScrolls = stats.lastWeekScrolls,
            scrollTrend = stats.scrollTrend,
            screenTime = stats.thisWeekTime,
            goalDays = stats.daysUnderGoal
        )

        Spacer(modifier = Modifier.height(32.dp))

        SectionTitle("Deep Insights", Icons.Default.AutoGraph)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModernInsightCard(modifier = Modifier.weight(1f), icon = Icons.Default.AccessTime, color = CobaltPremium, title = "Peak Usage", value = stats.peakHours)
            ModernInsightCard(modifier = Modifier.weight(1f), icon = if (stats.isTimeSaved) Icons.Default.Timer else Icons.Default.TimerOff, color = if (stats.isTimeSaved) EmeraldPremium else RosePremium, title = if (stats.isTimeSaved) "Time Saved" else "Time Added", value = stats.timeDifferenceStr)
        }

        Spacer(modifier = Modifier.height(32.dp))

        SectionTitle("Monthly Archives", Icons.Default.Folder)
        if (stats.monthlySummaries.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Text("Your digital footprints will appear here.", color = Gray500, fontSize = 14.sp)
            }
        }
        stats.monthlySummaries.forEach { summary ->
            ModernMonthlySummaryCard(summary)
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun ModernInsightCard(modifier: Modifier, icon: ImageVector, color: Color, title: String, value: String) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(CardBackground)
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column {
            Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title.uppercase(), color = Gray500, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
            Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun ModernMonthlySummaryCard(summary: HistoryMonthlySummary) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CardBackground)
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(summary.month.uppercase(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Box(modifier = Modifier.clip(CircleShape).background(EmeraldPremium.copy(alpha = 0.1f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("${summary.daysUnderGoal} goal days", color = EmeraldPremium, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("TOTAL SCROLLS", color = Gray500, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Text(String.format(Locale.getDefault(), "%,d", summary.totalScrolls), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("SCREEN TIME", color = Gray500, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Text(summary.totalScreenTime, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            val progress = if (summary.totalDays > 0) (summary.daysUnderGoal.toFloat() / summary.totalDays.toFloat()).coerceIn(0f, 1f) else 0f
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(RingTrack)) {
                Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().clip(CircleShape).background(EmeraldPremium))
            }
        }
    }
}

@Composable
fun AchievementsView(achievements: List<Achievement>) {
    val unlockedCount = achievements.count { it.unlocked }

    Column {
        HallOfFameHeader(unlockedCount = unlockedCount, totalCount = achievements.size)

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(achievements, key = { it.id }) { achievement ->
                Box(modifier = Modifier.animateItem()) {
                    PremiumAchievementCard(achievement)
                }
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
fun WeeklyComparisonHeroCard(
    thisWeekScrolls: Int,
    lastWeekScrolls: Int,
    scrollTrend: Int,
    screenTime: String,
    goalDays: Int
) {
    val isImprovement = scrollTrend <= 0
    val trendColor = if (isImprovement) Color(0xFF34D399) else RosePremium
    val trendIcon = if (isImprovement) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp
    val trendText = if (isImprovement) {
        "${kotlin.math.abs(scrollTrend)}% scroll reduction"
    } else {
        "${kotlin.math.abs(scrollTrend)}% scroll increase"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.4f))
            .clip(RoundedCornerShape(24.dp))
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(28.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.04f))
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(CardBackground)
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
        )
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("THIS WEEK", color = Gray500, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text(
                        String.format(Locale.getDefault(), "%,d", thisWeekScrolls),
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(80.dp)
                        .drawBehind {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0f),
                                        Color.White.copy(alpha = 0.2f),
                                        Color.White.copy(alpha = 0f)
                                    )
                                ),
                                size = size
                            )
                        }
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LAST WEEK", color = Gray500, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text(
                        String.format(Locale.getDefault(), "%,d", lastWeekScrolls),
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(trendColor.copy(alpha = 0.25f), trendColor.copy(alpha = 0.12f))
                        )
                    )
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.drawBehind {
                                drawCircle(
                                    color = trendColor.copy(alpha = 0.2f),
                                    radius = 40.dp.toPx(),
                                    center = Offset(size.width / 2f, size.height / 2f)
                                )
                            }
                        } else Modifier
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(trendIcon, null, tint = trendColor, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(trendText, color = trendColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                if (!isImprovement) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.Warning, null, tint = trendColor, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WeeklyStatPill(
                    icon = Icons.Default.AccessTime,
                    label = "Screen Time",
                    value = screenTime,
                    modifier = Modifier.weight(1f)
                )
                WeeklyStatPill(
                    icon = Icons.Default.EmojiEvents,
                    label = "Goal Days",
                    value = "$goalDays/7",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WeeklyStatPill(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = CobaltPremium, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(label.uppercase(), color = Gray500, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun HallOfFameHeader(unlockedCount: Int, totalCount: Int) {
    val goldStart = Color(0xFFF59E0B)
    val goldEnd = Color(0xFFD97706)
    val infiniteTransition = rememberInfiniteTransition(label = "trophySpin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "trophyRotation"
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(12.dp)
                    .background(goldStart.copy(alpha = 0.35f), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(Brush.linearGradient(listOf(goldStart, goldEnd)))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(40.dp)
                        .graphicsLayer { rotationZ = rotation }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "HALL OF FAME",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        "$unlockedCount of $totalCount Unlocked",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumAchievementCard(achievement: Achievement) {
    val glowColor = when (achievement.rarity) {
        AchievementRarity.COMMON -> Color(0xFF3B82F6)
        AchievementRarity.RARE -> Color(0xFFA855F7)
        AchievementRarity.EPIC -> Color(0xFFF59E0B)
    }
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .shadow(if (achievement.unlocked) 6.dp else 0.dp, shape, spotColor = Color.Black.copy(alpha = 0.3f))
            .clip(shape)
    ) {
        if (achievement.unlocked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(24.dp)
                    .background(Color.White.copy(alpha = 0.06f))
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.White.copy(alpha = if (achievement.unlocked) 0.06f else 0.02f))
            )
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (achievement.unlocked) {
                        Modifier.border(1.dp, Color.White.copy(alpha = 0.10f), shape)
                    } else {
                        Modifier.drawBehind {
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.15f),
                                cornerRadius = CornerRadius(20.dp.toPx()),
                                style = Stroke(
                                    width = 1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                                )
                            )
                        }
                    }
                )
        )

        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (achievement.unlocked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .blur(8.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(glowColor.copy(alpha = 0.5f), Color.Transparent)
                                ),
                                RoundedCornerShape(16.dp)
                            )
                    )
                }
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (achievement.unlocked) achievement.icon else "🔒",
                        fontSize = 32.sp,
                        modifier = if (!achievement.unlocked) Modifier.graphicsLayer { alpha = 0.35f } else Modifier
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    achievement.title,
                    color = if (achievement.unlocked) Color.White else Color.White.copy(alpha = 0.3f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    achievement.description,
                    color = Color.White.copy(alpha = if (achievement.unlocked) 0.7f else 0.35f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                if (achievement.unlocked) {
                    Text(
                        achievement.date.uppercase(),
                        color = Color(0xFFF59E0B).copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

private fun formatHourLocal(hour: Int): String {
    val h = if (hour == 0 || hour == 12) 12 else hour % 12
    val ampm = if (hour < 12) "AM" else "PM"
    return "$h $ampm"
}
