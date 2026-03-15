@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.scrollersdashboard

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.scrollersdashboard.ui.*
import com.example.scrollersdashboard.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : FragmentActivity() {
    
    private val db by lazy {
        AppDatabase.getDatabase(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isDarkMode by remember { mutableStateOf(true) }
            var currentScreen by remember { mutableStateOf("dashboard") }
            var overlayScreen by remember { mutableStateOf("") }
            
            var rippleCenter by remember { mutableStateOf(Offset.Zero) }
            val revealAnim = remember { Animatable(0f) }
            val scope = rememberCoroutineScope()
            
            var refreshKey by remember { mutableIntStateOf(0) }
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        refreshKey++
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            val navigateBack = { center: Offset ->
                rippleCenter = center
                scope.launch {
                    revealAnim.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
                    currentScreen = "dashboard"
                }
            }

            if (currentScreen != "dashboard") {
                BackHandler {
                    navigateBack(Offset.Zero)
                }
            }

            ScrollersDashboardTheme(darkTheme = isDarkMode) {
                val context = LocalContext.current
                val isServiceEnabled = remember(refreshKey) {
                    isAccessibilityServiceEnabled(context, ScrollerAccessibilityService::class.java)
                }
                val isUsageAccessEnabled = remember(refreshKey) {
                    isUsageAccessEnabled(context)
                }
                var hasDismissedPrompt by remember(refreshKey) { mutableStateOf(false) }

                if ((!isServiceEnabled || !isUsageAccessEnabled) && !hasDismissedPrompt) {
                    AlertDialog(
                        onDismissRequest = { hasDismissedPrompt = true },
                        containerColor = Gray800,
                        title = { Text("Permissions Required", color = Color.White, fontWeight = FontWeight.Bold) },
                        text = {
                            Column {
                                Text("To track scrolls and accurate screen time, please enable the following:", color = Gray400)
                                Spacer(modifier = Modifier.height(16.dp))
                                PermissionItem("Accessibility Service", isServiceEnabled, isDarkMode)
                                Spacer(modifier = Modifier.height(8.dp))
                                PermissionItem("Usage Access", isUsageAccessEnabled, isDarkMode)
                            }
                        },
                        confirmButton = {
                            PremiumScalingButton(onClick = {
                                if (!isServiceEnabled) {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                } else {
                                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                }
                            }, isDarkMode = isDarkMode) { 
                                Text(if (!isServiceEnabled) "Enable Accessibility" else "Enable Usage Access", color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontWeight = FontWeight.Bold) 
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { hasDismissedPrompt = true }) { Text("Later", color = Gray500) }
                        },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.border(1.dp, Gray700, RoundedCornerShape(24.dp))
                    )
                }

                Box(modifier = Modifier.fillMaxSize().background(Gray900)) {
                    DashboardScreen(
                        db = db,
                        isDarkMode = isDarkMode,
                        refreshKey = refreshKey,
                        onThemeToggle = { isDarkMode = !isDarkMode },
                        onNavigateToActivity = { center: Offset ->
                            rippleCenter = center
                            authenticateBiometric(this@MainActivity) {
                                scope.launch {
                                    overlayScreen = "activity"
                                    revealAnim.snapTo(0f)
                                    revealAnim.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
                                    currentScreen = "activity"
                                }
                            }
                        },
                        onNavigateToSettings = { center: Offset ->
                            rippleCenter = center
                            scope.launch {
                                overlayScreen = "settings"
                                revealAnim.snapTo(0f)
                                revealAnim.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
                                currentScreen = "settings"
                            }
                        },
                        onNavigateToGoals = { center: Offset ->
                            rippleCenter = center
                            scope.launch {
                                overlayScreen = "goals"
                                revealAnim.snapTo(0f)
                                revealAnim.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
                                currentScreen = "goals"
                            }
                        }
                    )

                    if (currentScreen != "dashboard" || revealAnim.isRunning) {
                        val activeOverlay = if (revealAnim.isRunning && currentScreen == "dashboard") overlayScreen else currentScreen
                        
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .clip(CircularRevealShape(revealAnim.value, rippleCenter))
                            .background(Gray900)
                        ) {
                            when (activeOverlay) {
                                "activity" -> ActivityScreen(
                                    db = db,
                                    isDarkMode = isDarkMode,
                                    refreshKey = refreshKey,
                                    onBack = { center: Offset -> navigateBack(center) }
                                )
                                "settings" -> SettingsScreen(
                                    db = db,
                                    isDarkMode = isDarkMode,
                                    onBack = { center: Offset -> navigateBack(center) }
                                )
                                "goals" -> GoalsScreen(
                                    db = db,
                                    isDarkMode = isDarkMode,
                                    onBack = { center: Offset -> navigateBack(center) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun authenticateBiometric(activity: FragmentActivity, onResult: () -> Unit) {
        val biometricManager = BiometricManager.from(activity)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or 
                             BiometricManager.Authenticators.DEVICE_CREDENTIAL
        
        if (biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS) {
            val executor = ContextCompat.getMainExecutor(activity)
            val biometricPrompt = BiometricPrompt(activity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onResult()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        Toast.makeText(activity, "Authentication failed", Toast.LENGTH_SHORT).show()
                    }
                })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Identity Verification")
                .setSubtitle("Authenticate to view detailed analysis")
                .setNegativeButtonText("Cancel")
                .build()

            biometricPrompt.authenticate(promptInfo)
        } else {
            onResult()
        }
    }
}

fun isAccessibilityServiceEnabled(context: Context, service: Class<*>): Boolean {
    val expectedId = ComponentName(context, service).flattenToString()
    val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
    return enabledServices?.split(':')?.any { it.equals(expectedId, ignoreCase = true) } == true
}

fun isUsageAccessEnabled(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    return mode == AppOpsManager.MODE_ALLOWED
}

@Composable
fun PermissionItem(label: String, isEnabled: Boolean, isDarkMode: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (isEnabled) Icons.Default.CheckCircle else Icons.Default.Error,
            null,
            tint = if (isEnabled) Color(0xFF34C759) else Color(0xFFFF3B30),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun DashboardScreen(db: AppDatabase, isDarkMode: Boolean, refreshKey: Int, onThemeToggle: () -> Unit, onNavigateToActivity: (Offset) -> Unit, onNavigateToSettings: (Offset) -> Unit, onNavigateToGoals: (Offset) -> Unit) {
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()
    
    var todayStr by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    
    LaunchedEffect(refreshKey) {
        todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    val scrollRecordsState = db.scrollDao().getRecentRecords().collectAsState(initial = emptyList())
    val scrollRecords = scrollRecordsState.value
    
    val instagramRecord = scrollRecords.find { it.date == todayStr && it.appType == "Instagram" }
    val youtubeRecord = scrollRecords.find { it.date == todayStr && it.appType == "YouTube" }
    
    val instagramCount = instagramRecord?.scrollCount ?: 0
    val youtubeCount = youtubeRecord?.scrollCount ?: 0
    
    val instagramTime = instagramRecord?.screenTimeMillis ?: 0L
    val youtubeTime = youtubeRecord?.screenTimeMillis ?: 0L
    
    val igLimitState = db.scrollDao().getSettingFlow("limit_ig").collectAsState(initial = "100")
    val ytLimitState = db.scrollDao().getSettingFlow("limit_yt").collectAsState(initial = "100")
    
    val igLimit = igLimitState.value?.toIntOrNull() ?: 100
    val ytLimit = ytLimitState.value?.toIntOrNull() ?: 100
    
    val totalTimeStr = formatTotalTime(instagramTime + youtubeTime)
    
    val todayTime = instagramTime + youtubeTime
    val last3DaysAvgTime = remember(scrollRecords) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var total = 0L
        var count = 0
        for (i in 1..3) {
            val checkCal = Calendar.getInstance()
            checkCal.add(Calendar.DAY_OF_YEAR, -i)
            val dStr = sdf.format(checkCal.time)
            val dayTime = scrollRecords.filter { it.date == dStr }.sumOf { it.screenTimeMillis }
            if (dayTime > 0) {
                total += dayTime
                count++
            }
        }
        if (count > 0) total / count else 0L
    }
    
    val improvement = if (last3DaysAvgTime > 0) {
        ((todayTime - last3DaysAvgTime).toFloat() / last3DaysAvgTime * 100).toInt()
    } else 0

    val streakCount = remember(scrollRecords, igLimit, ytLimit) {
        if (scrollRecords.isEmpty()) return@remember 0
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val recordsByDate = scrollRecords.groupBy { it.date }
        var streak = 0
        val streakCal = Calendar.getInstance()
        
        for (i in 0 until 60) {
            val dStr = sdf.format(streakCal.time)
            val dayRecords = recordsByDate[dStr]
            
            if (dayRecords == null) {
                if (dStr == todayStr) {
                    streak++
                    streakCal.add(Calendar.DAY_OF_YEAR, -1)
                    continue
                } else {
                    break
                }
            }
            
            val igC = dayRecords.find { it.appType == "Instagram" }?.scrollCount ?: 0
            val ytC = dayRecords.find { it.appType == "YouTube" }?.scrollCount ?: 0
            
            if (igC <= igLimit && ytC <= ytLimit) {
                streak++
                streakCal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        streak
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        state = pullRefreshState,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                delay(800)
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Gray900)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Gray800, Gray900)))
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Dashboard", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()), color = Gray400, fontSize = 14.sp)
                    }
                    IconButton(onClick = { onNavigateToSettings(Offset.Zero) }, modifier = Modifier.size(40.dp).clip(CircleShape).background(Gray800)) {
                        Icon(Icons.Default.Settings, null, tint = Gray300, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Gray800.copy(alpha = 0.5f))
                        .border(1.dp, Gray700, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Today", color = Gray400, fontSize = 14.sp)
                            Text(SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date()), color = Gray500, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(totalTimeStr, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        }
                        if (last3DaysAvgTime > 0) {
                            val isImprovement = improvement <= 0
                            val trendColor = if (isImprovement) Green400 else Color(0xFFFF3B30)
                            val trendIcon = if (isImprovement) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp
                            val trendText = if (isImprovement) "${Math.abs(improvement)}% less" else "${Math.abs(improvement)}% more"
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(trendColor.copy(alpha = 0.2f)).padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(trendIcon, null, tint = trendColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(trendText, color = trendColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp)) {
                Text("App Usage", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        UsageCard(
                            platform = "instagram",
                            screenTime = formatMillisToTime(instagramTime),
                            scrollCount = instagramCount,
                            percentage = if (igLimit > 0) ((instagramCount.toFloat() / igLimit.toFloat()) * 100).toInt().coerceAtMost(100) else 0,
                            dailyGoal = igLimit,
                            isDarkMode = true
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        UsageCard(
                            platform = "youtube",
                            screenTime = formatMillisToTime(youtubeTime),
                            scrollCount = youtubeCount,
                            percentage = if (ytLimit > 0) ((youtubeCount.toFloat() / ytLimit.toFloat()) * 100).toInt().coerceAtMost(100) else 0,
                            dailyGoal = ytLimit,
                            isDarkMode = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val totalScrolls = instagramCount + youtubeCount
                    val combinedTimeHours = (instagramTime + youtubeTime).toFloat() / (1000 * 60 * 60)
                    val avgPerHour = if (combinedTimeHours > 0) (totalScrolls / combinedTimeHours).toInt() else 0
                    
                    QuickStatCard("Total Scrolls", totalScrolls.toString(), "Today", modifier = Modifier.weight(1f))
                    QuickStatCard("Avg per Hour", avgPerHour.toString(), "Scrolls", modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (streakCount >= 2) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.horizontalGradient(listOf(Yellow500.copy(alpha = 0.2f), Orange500.copy(alpha = 0.2f))))
                            .border(1.dp, Yellow500.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Yellow500), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.EmojiEvents, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("$streakCount Day Streak!", color = Yellow400, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("You've been below the limit for $streakCount days in a row", color = Gray300, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }

                PremiumScalingButton(onClick = onNavigateToActivity, modifier = Modifier.fillMaxWidth(), isDarkMode = true, cornerRadius = 16.dp) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .background(Blue600.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("View Detailed Analysis", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
        
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            BottomNavigationBar(onNavigateToActivity, onNavigateToGoals, { onNavigateToSettings(Offset.Zero) }, { /* History action here */ })
        }
    }
}

@Composable
fun QuickStatCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Gray800.copy(alpha = 0.5f))
            .border(1.dp, Gray700, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(title, color = Gray400, fontSize = 14.sp)
            Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Gray500, fontSize = 12.sp)
        }
    }
}

@Composable
fun BottomNavigationBar(onStats: (Offset) -> Unit, onGoals: (Offset) -> Unit, onSettings: () -> Unit, onHistory: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF1A1F2B), Color(0xFF0D1117))))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .padding(bottom = 24.dp, top = 8.dp)
    ) {
        Column {
            // Blue indicator at the top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .padding(horizontal = 48.dp)
                    .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                    .background(Color(0xFF007AFF).copy(alpha = 0.8f))
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                BottomNavItem("Home", Icons.Default.Home, true) {}
                BottomNavItem("Activity", Icons.Default.BarChart, false) { onStats(Offset.Zero) }
                var goalsCenter by remember { mutableStateOf(Offset.Zero) }
                BottomNavItem("Goals", Icons.Default.EmojiEvents, false, modifier = Modifier.onGloballyPositioned { goalsCenter = it.positionInWindow() + Offset(it.size.width.toFloat() / 2f, it.size.height.toFloat() / 2f) }) { onGoals(goalsCenter) }
                BottomNavItem("History", Icons.Default.CalendarToday, false) { onHistory() }
            }
        }
    }
}

@Composable
fun BottomNavItem(label: String, icon: ImageVector, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(onClick = onClick).padding(8.dp)
    ) {
        Icon(
            imageVector = icon, 
            contentDescription = null, 
            tint = if (isSelected) Color.White else Color.Gray.copy(alpha = 0.6f), 
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label, 
            color = if (isSelected) Color.White else Color.Gray.copy(alpha = 0.6f), 
            fontSize = 12.sp, 
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun GoalsScreen(db: AppDatabase, isDarkMode: Boolean, onBack: (Offset) -> Unit) {
    var selectedView by remember { mutableStateOf("habits") }
    val scope = rememberCoroutineScope()
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    
    val habitTasks by db.scrollDao().getHabitTasks().collectAsState(initial = emptyList())
    val todoTasks by db.scrollDao().getTodoTasks(today).collectAsState(initial = emptyList())

    val habitsCompleted = habitTasks.count { it.lastCompletedDate == today }
    val todosCompleted = todoTasks.count { it.isCompleted }
    
    val habitProgress = if (habitTasks.isNotEmpty()) (habitsCompleted.toFloat() / habitTasks.size) else 0f
    val todoProgress = if (todoTasks.isNotEmpty()) (todosCompleted.toFloat() / todoTasks.size) else 0f

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var newTaskText by remember { mutableStateOf("") }

    val textColor = if (isDarkMode) Color.White else Color.Black

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Goals", color = textColor, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    var backCenter by remember { mutableStateOf(Offset.Zero) }
                    IconButton(onClick = { onBack(backCenter) }, modifier = Modifier.onGloballyPositioned { backCenter = it.positionInWindow() + Offset(it.size.width.toFloat() / 2f, it.size.height.toFloat() / 2f) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color(0xFF007AFF))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Gray900,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = Color(0xFF007AFF),
                contentColor = Color.White,
                shape = CircleShape
            ) { Icon(Icons.Default.Add, "Add Task") }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 20.dp)) {
            // View Switcher (Habits vs Todos)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(vertical = 4.dp), 
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Habits", "To-Do").forEach { tab ->
                    val isSelected = selectedView.equals(tab, ignoreCase = true) || (selectedView == "todos" && tab == "To-Do")
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) Color(0xFF2C333F) else Color.Transparent)
                            .clickable { selectedView = if (tab == "Habits") "habits" else "todos" }, 
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (tab == "Habits") Icons.Default.Repeat else Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = tab, color = if (isSelected) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Progress Card
            val currentProgress = if (selectedView == "habits") habitProgress else todoProgress
            val countText = if (selectedView == "habits") "$habitsCompleted of ${habitTasks.size} habits" else "$todosCompleted of ${todoTasks.size} tasks"
            val progressColor = if (selectedView == "habits") Color(0xFF34C759) else Color(0xFF007AFF)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(progressColor.copy(alpha = 0.15f))
                    .border(1.dp, progressColor.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Today's Progress", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(countText, color = progressColor.copy(alpha = 0.8f), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        // Progress Bar
                        Box(modifier = Modifier.width(180.dp).height(8.dp).clip(CircleShape).background(Gray800)) {
                            Box(modifier = Modifier.fillMaxWidth(currentProgress).fillMaxHeight().background(progressColor))
                        }
                    }
                    Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Gray800), contentAlignment = Alignment.Center) {
                        Text("${(currentProgress * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Task List
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (selectedView == "habits") {
                    items(habitTasks) { habit ->
                        val isDone = habit.lastCompletedDate == today
                        GoalItemRow(habit.title, isDone, color = Color(0xFF34C759), 
                            onToggle = {
                                scope.launch { db.scrollDao().insertHabit(habit.copy(lastCompletedDate = if (isDone) "" else today)) }
                            }, 
                            onDelete = {
                                scope.launch { db.scrollDao().deleteHabit(habit.id) }
                            }
                        )
                    }
                } else {
                    items(todoTasks) { todo ->
                        GoalItemRow(todo.title, todo.isCompleted, color = Color(0xFF007AFF), 
                            onToggle = {
                                scope.launch { db.scrollDao().insertTodo(todo.copy(isCompleted = !todo.isCompleted)) }
                            }, 
                            onDelete = {
                                scope.launch { db.scrollDao().deleteTodo(todo.id) }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddTaskDialog) {
        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            containerColor = Gray800,
            title = { Text("Add New ${if (selectedView == "habits") "Habit" else "Task"}", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newTaskText,
                    onValueChange = { newTaskText = it },
                    placeholder = { Text("Enter title...") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newTaskText.isNotBlank()) {
                        scope.launch {
                            if (selectedView == "habits") db.scrollDao().insertHabit(HabitTask(title = newTaskText))
                            else db.scrollDao().insertTodo(TodoTask(title = newTaskText, date = today))
                            newTaskText = ""
                            showAddTaskDialog = false
                        }
                    }
                }) { Text("Add", color = Color(0xFF007AFF)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) { Text("Cancel", color = Color.Gray) }
            }
        )
    }
}

@Composable
fun GoalItemRow(text: String, isCompleted: Boolean, color: Color, onToggle: () -> Unit, onDelete: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Gray800.copy(alpha = 0.5f))
            .border(1.dp, if (isCompleted) color.copy(alpha = 0.5f) else Gray700, RoundedCornerShape(16.dp))
            .clickable { onToggle() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isCompleted) color else Color.Transparent)
                    .border(2.dp, if (isCompleted) color else Color.Gray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                color = if (isCompleted) Color.Gray else Color.White,
                modifier = Modifier.weight(1f),
                textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                fontWeight = FontWeight.Medium
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Delete, null, tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun SettingsScreen(db: AppDatabase, isDarkMode: Boolean, onBack: (Offset) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var igLimit by remember { mutableStateOf("100") }
    var ytLimit by remember { mutableStateOf("100") }
    var igGap by remember { mutableStateOf("10") }
    var ytGap by remember { mutableStateOf("10") }
    var trackIG by remember { mutableStateOf(true) }
    var trackYT by remember { mutableStateOf(true) }

    var showResetIGDialog by remember { mutableStateOf(false) }
    var showResetYTDialog by remember { mutableStateOf(false) }
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    LaunchedEffect(Unit) {
        igLimit = db.scrollDao().getSetting("limit_ig") ?: "100"
        ytLimit = db.scrollDao().getSetting("limit_yt") ?: "100"
        igGap = db.scrollDao().getSetting("alert_gap_ig") ?: "10"
        ytGap = db.scrollDao().getSetting("alert_gap_yt") ?: "10"
        trackIG = db.scrollDao().getSetting("track_ig")?.toBoolean() ?: true
        trackYT = db.scrollDao().getSetting("track_yt")?.toBoolean() ?: true
    }

    val textColor = if (isDarkMode) Color.White else Color.Black

    if (showResetIGDialog) {
        AlertDialog(
            onDismissRequest = { showResetIGDialog = false },
            containerColor = Gray800,
            title = { Text("Reset Instagram Count", color = textColor, fontWeight = FontWeight.Bold) },
            text = { Text("This will reset the reels scrolled count to 0 for the day.", color = if (isDarkMode) Color.LightGray else Color.DarkGray) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        db.scrollDao().resetCount(todayStr, "Instagram")
                        db.scrollDao().deleteEventsForToday(todayStr, "Instagram")
                        Toast.makeText(context, "Instagram count reset", Toast.LENGTH_SHORT).show()
                    }
                    showResetIGDialog = false
                }) { Text("Reset", color = Color.Red, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showResetIGDialog = false }) { Text("Cancel", color = Color.Gray) }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showResetYTDialog) {
        AlertDialog(
            onDismissRequest = { showResetYTDialog = false },
            containerColor = Gray800,
            title = { Text("Reset YouTube Count", color = textColor, fontWeight = FontWeight.Bold) },
            text = { Text("This will reset the shorts scrolled count to 0 for the day.", color = if (isDarkMode) Color.LightGray else Color.DarkGray) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        db.scrollDao().resetCount(todayStr, "YouTube")
                        db.scrollDao().deleteEventsForToday(todayStr, "YouTube")
                        Toast.makeText(context, "YouTube count reset", Toast.LENGTH_SHORT).show()
                    }
                    showResetYTDialog = false
                }) { Text("Reset", color = Color.Red, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showResetYTDialog = false }) { Text("Cancel", color = Color.Gray) }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings", color = textColor, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    var backCenter by remember { mutableStateOf(Offset.Zero) }
                    IconButton(onClick = { onBack(backCenter) }, modifier = Modifier.onGloballyPositioned { backCenter = it.positionInWindow() + Offset(it.size.width.toFloat() / 2f, it.size.height.toFloat() / 2f) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color(0xFF007AFF))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Gray900
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(24.dp).fillMaxSize().verticalScroll(rememberScrollState())) {
            SettingsCard("LIMITS", isDarkMode) {
                OutlinedTextField(value = igLimit, onValueChange = { igLimit = it }, label = { Text("Instagram Limit (Reels)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = ytLimit, onValueChange = { ytLimit = it }, label = { Text("YouTube Limit (Shorts)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            SettingsCard("ALERT GAPS", isDarkMode) {
                Text("Show alert after every 'n' scrolls once limit is reached.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(value = igGap, onValueChange = { igGap = it }, label = { Text("Instagram Alert Every 'n' Reels") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = ytGap, onValueChange = { ytGap = it }, label = { Text("YouTube Alert Every 'n' Shorts") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            SettingsCard("TRACKING", isDarkMode) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Instagram", color = textColor, fontWeight = FontWeight.Medium)
                    ScrollerToggle(checked = trackIG, isDarkMode = isDarkMode, onCheckedChange = { trackIG = it })
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("YouTube", color = textColor, fontWeight = FontWeight.Medium)
                    ScrollerToggle(checked = trackYT, isDarkMode = isDarkMode, onCheckedChange = { trackYT = it })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsCard("RESET DATA", isDarkMode) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ClayPillButton(Modifier.weight(1f), "Reset IG", Icons.Default.PhotoCamera, Color(0xFFE1306C), isDarkMode) {
                        showResetIGDialog = true
                    }
                    ClayPillButton(Modifier.weight(1f), "Reset YT", Icons.Default.PlayArrow, Color(0xFFFF0000), isDarkMode) {
                        showResetYTDialog = true
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    scope.launch {
                        db.scrollDao().saveSetting(UserSetting("limit_ig", igLimit))
                        db.scrollDao().saveSetting(UserSetting("limit_yt", ytLimit))
                        db.scrollDao().saveSetting(UserSetting("alert_gap_ig", igGap))
                        db.scrollDao().saveSetting(UserSetting("alert_gap_yt", ytGap))
                        db.scrollDao().saveSetting(UserSetting("track_ig", trackIG.toString()))
                        db.scrollDao().saveSetting(UserSetting("track_yt", trackYT.toString()))
                        Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
            ) { Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 17.sp) }
        }
    }
}

@Composable
fun SettingsCard(title: String, isDarkMode: Boolean, content: @Composable ColumnScope.() -> Unit) {
    // Match Home Screen Design: Solid Gray800 Borders and Background
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Gray800.copy(alpha = 0.5f))
            .border(1.dp, Gray800, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column {
            Text(title, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun ActivityScreen(db: AppDatabase, isDarkMode: Boolean, refreshKey: Int, onBack: (Offset) -> Unit) {
    var selectedTab by remember { mutableStateOf("Day") }
    var viewingDate by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(refreshKey) { viewingDate = Calendar.getInstance() }

    val igLimitState = db.scrollDao().getSettingFlow("limit_ig").collectAsState(initial = "100")
    val ytLimitState = db.scrollDao().getSettingFlow("limit_yt").collectAsState(initial = "100")
    val igLimit = igLimitState.value?.toIntOrNull() ?: 100
    val ytLimit = ytLimitState.value?.toIntOrNull() ?: 100
    val totalLimit = igLimit + ytLimit

    val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(viewingDate.time)
    val eventsState = db.scrollDao().getEventsForDate(todayDateStr).collectAsState(initial = emptyList())
    val historyRecordsState = db.scrollDao().getRecentRecords().collectAsState(initial = emptyList())
    
    val textColor = if (isDarkMode) Color.White else Color.Black

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Activity Analysis", color = textColor, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    var backCenter by remember { mutableStateOf(Offset.Zero) }
                    IconButton(onClick = { onBack(backCenter) }, modifier = Modifier.onGloballyPositioned { backCenter = it.positionInWindow() + Offset(it.size.width.toFloat() / 2f, it.size.height.toFloat() / 2f) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color(0xFF007AFF))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Gray900
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 20.dp)) {
            // Tab Switcher - Match Image: Transparent Outer, High-contrast Selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(vertical = 4.dp), 
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Day", "Week", "Month").forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) Color(0xFF2C333F) else Color.Transparent) // Solid, darker select state as per image
                            .clickable { selectedTab = tab }, 
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab, 
                            color = if (isSelected) Color.White else Color.Gray, 
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, 
                            fontSize = 15.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            AnimatedContent(
                targetState = selectedTab, 
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = ""
            ) { tab ->
                when (tab) {
                    "Day" -> DayView(isDarkMode, viewingDate, eventsState.value, db)
                    "Week" -> WeekView(isDarkMode, historyRecordsState.value, totalLimit)
                    "Month" -> MonthView(isDarkMode, historyRecordsState.value, igLimit, ytLimit) { dateStr ->
                        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
                        if (date != null) { viewingDate = Calendar.getInstance().apply { time = date }; selectedTab = "Day" }
                    }
                }
            }
        }
    }
}

@Composable
fun DayView(isDarkMode: Boolean, date: Calendar, events: List<ScrollEvent>, db: AppDatabase) {
    val sessions = remember(events) {
        val list = mutableListOf<List<ScrollEvent>>()
        if (events.isEmpty()) return@remember list
        var currentSession = mutableListOf<ScrollEvent>()
        currentSession.add(events[0])
        for (i in 1 until events.size) {
            if (events[i].timestamp - events[i-1].timestamp < 5 * 60 * 1000) { currentSession.add(events[i]) }
            else { list.add(currentSession); currentSession = mutableListOf(events[i]) }
        }
        list.add(currentSession); list.reverse(); list
    }

    Column {
        // Activity Graph Panel - Match Home Screen Design
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Gray800.copy(alpha = 0.5f))
                .border(1.dp, Gray800, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            ActivityGraph(events, db, isDarkMode)
        }
        Spacer(modifier = Modifier.height(28.dp))
        Text("SESSIONS", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp, modifier = Modifier.padding(start = 4.dp))
        Spacer(modifier = Modifier.height(16.dp))
        if (sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No activity recorded for today", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(sessions) { session ->
                    SessionItem(session, isDarkMode)
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
fun WeekView(isDarkMode: Boolean, history: List<ScrollRecord>, limit: Int) {
    val cal = Calendar.getInstance(); cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
    val weekDays = mutableListOf<String>()
    val dayNames = mutableListOf<String>()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val daySdf = SimpleDateFormat("E", Locale.getDefault())
    for (i in 0 until 7) { 
        weekDays.add(sdf.format(cal.time))
        dayNames.add(daySdf.format(cal.time).first().toString())
        cal.add(Calendar.DAY_OF_YEAR, 1) 
    }
    
    val weekData = weekDays.map { date ->
        val yt = history.find { it.date == date && it.appType == "YouTube" }?.scrollCount ?: 0
        val ig = history.find { it.date == date && it.appType == "Instagram" }?.scrollCount ?: 0
        Pair(yt, ig)
    }

    Column {
        // Activity Graph Panel - Match Home Screen Design
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Gray800.copy(alpha = 0.5f))
                .border(1.dp, Gray800, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            val textMeasurer = rememberTextMeasurer()
            Canvas(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                val width = size.width
                val labelAreaWidth = 40.dp.toPx()
                val height = size.height - 30.dp.toPx()
                val chartWidth = width - labelAreaWidth
                val barAreaWidth = chartWidth / 7
                val barWidth = 10.dp.toPx()
                val maxVal = (weekData.flatMap { listOf(it.first, it.second) }.maxOrNull() ?: limit).coerceAtLeast(limit + 20).toFloat()
                
                for (i in 0..4) {
                    val value = (maxVal / 4 * i).toInt()
                    val y = height - (value / maxVal * height)
                    drawLine(if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f), Offset(labelAreaWidth, y), Offset(width, y))
                    drawText(textMeasurer, value.toString(), Offset(0f, y - 8.dp.toPx()), style = TextStyle(color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                }
                
                weekData.forEachIndexed { i, data ->
                    val xCenter = labelAreaWidth + i * barAreaWidth + barAreaWidth/2
                    drawText(textMeasurer, dayNames[i], Offset(xCenter - 4.dp.toPx(), height + 10.dp.toPx()), style = TextStyle(color = if (isDarkMode) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Black))
                    
                    if (data.second > 0) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(InstaNeonGradient), 
                            topLeft = Offset(xCenter - barWidth - 3.dp.toPx(), height - (data.second / maxVal * height)), 
                            size = Size(barWidth, data.second / maxVal * height), 
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )
                    }
                    if (data.first > 0) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(YTNeonGradient), 
                            topLeft = Offset(xCenter + 3.dp.toPx(), height - (data.first / maxVal * height)), 
                            size = Size(barWidth, data.first / maxVal * height), 
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            LegendItem(InstaNeonGradient.first(), "Instagram")
            Spacer(modifier = Modifier.width(24.dp))
            LegendItem(YTNeonGradient.first(), "YouTube")
        }
    }
}

@Composable
fun MonthView(isDarkMode: Boolean, history: List<ScrollRecord>, igLimit: Int, ytLimit: Int, onDayClick: (String) -> Unit) {
    val cal = Calendar.getInstance(); cal.set(Calendar.DAY_OF_MONTH, 1)
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val monthDays = mutableListOf<String>()
    val tempCal = cal.clone() as Calendar
    for (i in 1..daysInMonth) { monthDays.add(sdf.format(tempCal.time)); tempCal.add(Calendar.DAY_OF_MONTH, 1) }
    
    val dailyData = monthDays.map { date ->
        val yt = history.find { it.date == date && it.appType == "YouTube" }?.scrollCount ?: 0
        val ig = history.find { it.date == date && it.appType == "Instagram" }?.scrollCount ?: 0
        Triple(ig, yt, ig + yt)
    }

    val totalLimit = igLimit + ytLimit

    Column {
        // Activity Graph Panel - Match Home Screen Design
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Gray800.copy(alpha = 0.5f))
                .border(1.dp, Gray800, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            val textMeasurer = rememberTextMeasurer()
            Canvas(modifier = Modifier.fillMaxWidth().height(250.dp)) {
                val width = size.width
                val labelAreaWidth = 36.dp.toPx()
                val height = size.height - 25.dp.toPx()
                val chartWidth = width - labelAreaWidth
                val barWidth = chartWidth / daysInMonth
                val maxVal = (dailyData.map { it.third }.maxOrNull() ?: totalLimit).coerceAtLeast(totalLimit + 20).toFloat()
                
                for (i in 0..4) {
                    val value = (maxVal / 4 * i).toInt()
                    val y = height - (value / maxVal * height)
                    drawLine(if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f), Offset(labelAreaWidth, y), Offset(width, y))
                    drawText(textMeasurer, value.toString(), Offset(0f, y - 8.dp.toPx()), style = TextStyle(color = Color.Gray, fontSize = 10.sp))
                }
                
                for (i in 0 until daysInMonth) {
                    val x = labelAreaWidth + i * barWidth
                    val barHeight = dailyData[i].third / maxVal * height
                    if (barHeight > 0) {
                        drawRect(
                            brush = Brush.verticalGradient(listOf(Color(0xFF007AFF), Color(0xFF007AFF).copy(alpha = 0.5f))), 
                            topLeft = Offset(x + 1f, height - barHeight), 
                            size = Size(barWidth - 2f, barHeight)
                        )
                    }
                    if ((i + 1) % 5 == 0 || i == 0) {
                        drawText(textMeasurer, "${i + 1}", Offset(x, height + 4.dp.toPx()), style = TextStyle(color = Color.Gray, fontSize = 9.sp))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.Center) {
            LegendItem(Color(0xFF007AFF), "Daily Scrolls")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("CALENDAR VIEW", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
        
        // Calendar Grid Panel - Match Home Screen Design
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Gray800.copy(alpha = 0.5f))
                .border(1.dp, Gray800, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
            Column {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceAround) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                        Text(day, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
                LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.height(220.dp)) {
                    items(firstDayOfWeek) { Box(Modifier.size(32.dp)) }
                    items(daysInMonth) { index ->
                        val dateStr = monthDays[index]
                        val igCount = dailyData[index].first
                        val ytCount = dailyData[index].second
                        val total = dailyData[index].third
                        
                        val bgColor = when {
                            total == 0 -> Color.Transparent
                            igCount > igLimit || ytCount > ytLimit -> Color(0xFFFF3B30).copy(alpha = 0.9f)
                            else -> Color(0xFF34C759).copy(alpha = 0.9f)
                        }
                        
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.aspectRatio(1f).padding(3.dp).clip(CircleShape).background(bgColor).clickable { onDayClick(dateStr) }) {
                            Text("${index + 1}", color = if (total > 0) Color.White else (if (isDarkMode) Color.White else Color.Black), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.Center) {
            LegendItem(Color(0xFF34C759), "Both Under Limit")
            Spacer(modifier = Modifier.width(20.dp))
            LegendItem(Color(0xFFFF3B30), "One or More Exceeded")
        }
    }
}

@Composable
fun ActivityGraph(events: List<ScrollEvent>, db: AppDatabase, isDarkMode: Boolean) {
    val ytBuckets = IntArray(24); val igBuckets = IntArray(24)
    events.forEach { event ->
        val hour = Calendar.getInstance().apply { timeInMillis = event.timestamp }.get(Calendar.HOUR_OF_DAY)
        if (event.appType == "YouTube") ytBuckets[hour]++ else igBuckets[hour]++
    }
    val textMeasurer = rememberTextMeasurer()
    val maxVal = (ytBuckets.maxOrNull() ?: 1).coerceAtLeast(igBuckets.maxOrNull() ?: 1).coerceAtLeast(20).toFloat()
    
    Canvas(modifier = Modifier.fillMaxWidth().height(220.dp)) {
        val width = size.width
        val labelAreaWidth = 36.dp.toPx()
        val height = size.height - 25.dp.toPx()
        val chartWidth = width - labelAreaWidth
        val bucketWidth = chartWidth / 24
        val barWidth = bucketWidth / 2.5f
        
        for (i in 0..4) {
            val value = (i * (maxVal / 4)).toInt()
            val y = height - (i * (maxVal / 4) / maxVal * height)
            drawLine(if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f), Offset(labelAreaWidth, y), Offset(width, y))
            drawText(textMeasurer, value.toString(), Offset(0f, y - 8.dp.toPx()), style = TextStyle(color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold))
        }
        
        val hoursToLabel = listOf(0, 6, 12, 18, 23)
        hoursToLabel.forEach { hour ->
            val xBase = labelAreaWidth + hour * bucketWidth
            drawText(textMeasurer, "${hour}h", Offset(xBase, height + 6.dp.toPx()), style = TextStyle(color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold))
        }

        for (i in 0 until 24) {
            val xBase = labelAreaWidth + i * bucketWidth
            if (ytBuckets[i] > 0) {
                drawRoundRect(
                    brush = Brush.verticalGradient(YTNeonGradient), 
                    topLeft = Offset(xBase + 2f, height - (ytBuckets[i] / maxVal * height)), 
                    size = Size(barWidth, ytBuckets[i] / maxVal * height), 
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
            }
            if (igBuckets[i] > 0) {
                drawRoundRect(
                    brush = Brush.verticalGradient(InstaNeonGradient), 
                    topLeft = Offset(xBase + barWidth + 4f, height - (igBuckets[i] / maxVal * height)), 
                    size = Size(barWidth, igBuckets[i] / maxVal * height), 
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun SessionItem(session: List<ScrollEvent>, isDarkMode: Boolean) {
    val ytCount = session.count { it.appType == "YouTube" }; val igCount = session.count { it.appType == "Instagram" }
    val startTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(session[0].timestamp))
    val endTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(session.last().timestamp))
    
    val durationMillis = session.last().timestamp - session[0].timestamp
    val durationText = if (durationMillis > 60000) {
        val mins = durationMillis / 60000
        "${mins}m"
    } else {
        "${durationMillis / 1000}s"
    }

    // Session Item Panel - Match Home Screen Design
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Gray800.copy(alpha = 0.5f))
            .border(1.dp, Gray800, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            val textColor = if (isDarkMode) Color.White else Color.Black
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (startTime == endTime) startTime else "$startTime - $endTime", color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Text("${igCount + ytCount} scrolls • $durationText duration", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (igCount > 0) SessionTag(Color(0xFFE1306C), "$igCount IG")
                if (ytCount > 0) SessionTag(Color(0xFFFF3B30), "$ytCount YT")
            }
        }
    }
}

@Composable
fun SessionTag(color: Color, text: String) {
    Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.12f)).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
    }
}

fun formatTotalTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return "${hours}h ${minutes}m"
}

fun formatMillisToTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    if (hours > 0) return "${hours}h ${minutes}m"
    return "${minutes}m"
}
