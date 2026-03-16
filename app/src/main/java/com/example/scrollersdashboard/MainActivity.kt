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
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.KeyboardType
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

                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1F2937), Color(0xFF0F172A))))) {
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
                        },
                        onNavigateToHistory = { center: Offset ->
                            rippleCenter = center
                            scope.launch {
                                overlayScreen = "history"
                                revealAnim.snapTo(0f)
                                revealAnim.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
                                currentScreen = "history"
                            }
                        }
                    )

                    if (currentScreen != "dashboard" || revealAnim.isRunning) {
                        val activeOverlay = if (revealAnim.isRunning && currentScreen == "dashboard") overlayScreen else currentScreen
                        
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .clip(CircularRevealShape(revealAnim.value, rippleCenter))
                            .background(Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1F2937), Color(0xFF0F172A))))
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
                                "history" -> HistoryScreen(
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
fun DashboardScreen(db: AppDatabase, isDarkMode: Boolean, refreshKey: Int, onThemeToggle: () -> Unit, onNavigateToActivity: (Offset) -> Unit, onNavigateToSettings: (Offset) -> Unit, onNavigateToGoals: (Offset) -> Unit, onNavigateToHistory: (Offset) -> Unit) {
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
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Color(0xFF1F2937), Color(0xFF111827))))
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
                    IconButton(onClick = { onNavigateToSettings(Offset.Zero) }, modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF1F2937).copy(alpha = 0.5f))) {
                        Icon(Icons.Default.Settings, null, tint = Gray300, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1F2937).copy(alpha = 0.3f))
                        .border(1.dp, Color(0xFF374151), RoundedCornerShape(20.dp))
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
            BottomNavigationBar(onNavigateToActivity, onNavigateToGoals, { onNavigateToSettings(Offset.Zero) }, { onNavigateToHistory(Offset.Zero) })
        }
    }
}

@Composable
fun QuickStatCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1F2937).copy(alpha = 0.3f))
            .border(1.dp, Color(0xFF374151), RoundedCornerShape(16.dp))
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
fun BottomNavigationBar(onStats: (Offset) -> Unit, onGoals: (Offset) -> Unit, onSettings: () -> Unit, onHistory: (Offset) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF1A1F2B), Color(0xFF0D1117))))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .padding(bottom = 24.dp, top = 8.dp)
    ) {
        Column {
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                BottomNavItem("Home", Icons.Default.Home, true) {}
                var activityCenter by remember { mutableStateOf(Offset.Zero) }
                BottomNavItem("Activity", Icons.Default.BarChart, false, modifier = Modifier.onGloballyPositioned { activityCenter = it.positionInWindow() + Offset(it.size.width.toFloat() / 2f, it.size.height.toFloat() / 2f) }) { onStats(activityCenter) }
                var goalsCenter by remember { mutableStateOf(Offset.Zero) }
                BottomNavItem("Goals", Icons.Default.EmojiEvents, false, modifier = Modifier.onGloballyPositioned { goalsCenter = it.positionInWindow() + Offset(it.size.width.toFloat() / 2f, it.size.height.toFloat() / 2f) }) { onGoals(goalsCenter) }
                var historyCenter by remember { mutableStateOf(Offset.Zero) }
                BottomNavItem("History", Icons.Default.CalendarToday, false, modifier = Modifier.onGloballyPositioned { historyCenter = it.positionInWindow() + Offset(it.size.width.toFloat() / 2f, it.size.height.toFloat() / 2f) }) { onHistory(historyCenter) }
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
    
    val refreshDailyTodoState = db.scrollDao().getSettingFlow("refresh_daily_todo").collectAsState(initial = "true")
    val isRefreshDaily = refreshDailyTodoState.value?.toBoolean() ?: true

    // Retrieve tasks: if refresh is ON, only get tasks for today. If OFF, get "permanent_todo" tasks.
    val todoTasks by (if (isRefreshDaily) db.scrollDao().getTodoTasks(today) else db.scrollDao().getTodoTasks("permanent_todo")).collectAsState(initial = emptyList())

    LaunchedEffect(isRefreshDaily) {
        if (isRefreshDaily) {
            db.scrollDao().deleteOldTodos(today)
        }
    }

    val habitsCompleted = habitTasks.count { it.lastCompletedDate == today }
    val todosCompleted = todoTasks.count { it.isCompleted }
    
    val habitProgress = if (habitTasks.isNotEmpty()) (habitsCompleted.toFloat() / habitTasks.size) else 0f
    val todoProgress = if (todoTasks.isNotEmpty()) (todosCompleted.toFloat() / todoTasks.size) else 0f

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
        containerColor = Color.Transparent
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 20.dp)) {
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
                        Text("Today's Progress", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text(countText, color = progressColor.copy(alpha = 0.8f), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(modifier = Modifier.width(180.dp).height(8.dp).clip(CircleShape).background(Color(0xFF1F2937).copy(alpha = 0.5f))) {
                            Box(modifier = Modifier.fillMaxWidth(currentProgress).fillMaxHeight().background(progressColor))
                        }
                    }
                    Text("${(currentProgress * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (selectedView == "habits") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF007AFF).copy(alpha = 0.1f))
                        .border(1.dp, Color(0xFF007AFF).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Daily Habits are permanent tasks that appear every day. Only completion status resets at midnight - the habits themselves remain until you delete them.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                val todoCardColor = Color(0xFF6366F1)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(todoCardColor.copy(alpha = 0.15f))
                        .border(1.dp, todoCardColor.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(todoCardColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CalendarToday, null, tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Refresh Daily", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Tasks reset each day", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                            }
                            Switch(
                                checked = isRefreshDaily,
                                onCheckedChange = { newValue ->
                                    scope.launch {
                                        db.scrollDao().saveSetting(UserSetting("refresh_daily_todo", newValue.toString()))
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = todoCardColor,
                                    checkedTrackColor = todoCardColor.copy(alpha = 0.5f)
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isRefreshDaily) "All tasks (completed or not) will be deleted at midnight for a fresh start." else "Tasks will remain saved and will not be refreshed or deleted automatically.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = newTaskText,
                    onValueChange = { newTaskText = it },
                    placeholder = { Text(if (selectedView == "habits") "Add a new daily habit..." else "Add a new task...", color = Color.Gray) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1F2937).copy(alpha = 0.5f)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = {
                        if (newTaskText.isNotBlank()) {
                            scope.launch {
                                if (selectedView == "habits") {
                                    db.scrollDao().insertHabit(HabitTask(title = newTaskText))
                                } else {
                                    val date = if (isRefreshDaily) today else "permanent_todo"
                                    db.scrollDao().insertTodo(TodoTask(title = newTaskText, date = date))
                                }
                                newTaskText = ""
                            }
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(progressColor)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                }
            }

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
}

@Composable
fun GoalItemRow(text: String, isCompleted: Boolean, color: Color, onToggle: () -> Unit, onDelete: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1F2937).copy(alpha = 0.3f))
            .border(1.dp, if (isCompleted) color.copy(alpha = 0.5f) else Color(0xFF374151), RoundedCornerShape(16.dp))
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

    var dailyIgLimit by remember { mutableStateOf("100") }
    var dailyYtLimit by remember { mutableStateOf("100") }
    var igLimit by remember { mutableStateOf("50") }
    var ytLimit by remember { mutableStateOf("30") }
    var trackIG by remember { mutableStateOf(true) }
    var trackYT by remember { mutableStateOf(true) }
    var alertOnlyAfterLimit by remember { mutableStateOf(true) }
    var alertsEnabled by remember { mutableStateOf(true) }

    var showResetIGDialog by remember { mutableStateOf(false) }
    var showResetYTDialog by remember { mutableStateOf(false) }
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    LaunchedEffect(Unit) {
        dailyIgLimit = db.scrollDao().getSetting("limit_ig") ?: "100"
        dailyYtLimit = db.scrollDao().getSetting("limit_yt") ?: "100"
        igLimit = db.scrollDao().getSetting("alert_gap_ig") ?: "50"
        ytLimit = db.scrollDao().getSetting("alert_gap_yt") ?: "30"
        trackIG = db.scrollDao().getSetting("track_ig")?.toBoolean() ?: true
        trackYT = db.scrollDao().getSetting("track_yt")?.toBoolean() ?: true
        alertOnlyAfterLimit = db.scrollDao().getSetting("alert_only_after_limit")?.toBoolean() ?: true
        alertsEnabled = db.scrollDao().getSetting("alert_screen_enabled")?.toBoolean() ?: true
    }

    if (showResetIGDialog) {
        AlertDialog(
            onDismissRequest = { showResetIGDialog = false },
            containerColor = Gray800,
            title = { Text("Reset Instagram Count", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Reset Instagram scroll count for today?", color = Color.LightGray) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        db.scrollDao().resetCount(todayStr, "Instagram")
                        db.scrollDao().deleteEventsForToday(todayStr, "Instagram")
                        Toast.makeText(context, "Instagram scroll count reset!", Toast.LENGTH_SHORT).show()
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
            title = { Text("Reset YouTube Count", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Reset YouTube scroll count for today?", color = Color.LightGray) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        db.scrollDao().resetCount(todayStr, "YouTube")
                        db.scrollDao().deleteEventsForToday(todayStr, "YouTube")
                        Toast.makeText(context, "YouTube scroll count reset!", Toast.LENGTH_SHORT).show()
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

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1F2937), Color(0xFF0F172A))))) {
        Column(modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())) {
            
            Box(modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Color(0xFF1F2937), Color(0xFF111827))))
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    var backCenter by remember { mutableStateOf(Offset.Zero) }
                    IconButton(
                        onClick = { 
                            scope.launch {
                                db.scrollDao().saveSetting(UserSetting("limit_ig", dailyIgLimit))
                                db.scrollDao().saveSetting(UserSetting("limit_yt", dailyYtLimit))
                                db.scrollDao().saveSetting(UserSetting("alert_gap_ig", igLimit))
                                db.scrollDao().saveSetting(UserSetting("alert_gap_yt", ytLimit))
                                db.scrollDao().saveSetting(UserSetting("track_ig", trackIG.toString()))
                                db.scrollDao().saveSetting(UserSetting("track_yt", trackYT.toString()))
                                db.scrollDao().saveSetting(UserSetting("alert_only_after_limit", alertOnlyAfterLimit.toString()))
                                db.scrollDao().saveSetting(UserSetting("alert_screen_enabled", alertsEnabled.toString()))
                            }
                            onBack(backCenter) 
                        },
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
                    Text("Settings", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column(modifier = Modifier.padding(24.dp)) {
                SectionTitle("Daily Scroll Limits", Icons.Default.EmojiEvents)
                
                AlertLimitCardReplica(
                    title = "Instagram Daily Limit",
                    subtitle = "Maximum reels per day",
                    icon = Icons.Default.PhotoCamera,
                    gradient = Brush.linearGradient(listOf(Color(0xFF9333EA), Color(0xFFDB2777))),
                    borderColor = Color(0xFF9333EA).copy(alpha = 0.3f),
                    value = dailyIgLimit,
                    onValueChange = { dailyIgLimit = it },
                    unit = "reels",
                    instruction = "Maximum daily limit",
                    footer = "Your overall goal is $dailyIgLimit reels per day"
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                AlertLimitCardReplica(
                    title = "YouTube Daily Limit",
                    subtitle = "Maximum shorts per day",
                    icon = Icons.Default.PlayArrow,
                    gradient = SolidColor(Color(0xFFEF4444)),
                    borderColor = Color(0xFFEF4444).copy(alpha = 0.3f),
                    value = dailyYtLimit,
                    onValueChange = { dailyYtLimit = it },
                    unit = "shorts",
                    instruction = "Maximum daily limit",
                    footer = "Your overall goal is $dailyYtLimit shorts per day"
                )

                Spacer(modifier = Modifier.height(24.dp))

                SectionTitle("Alert Limits", Icons.Default.Notifications)
                
                AlertLimitCardReplica(
                    title = "Instagram",
                    subtitle = "Alert frequency",
                    icon = Icons.Default.PhotoCamera,
                    gradient = Brush.linearGradient(listOf(Color(0xFF9333EA), Color(0xFFDB2777))),
                    borderColor = Color(0xFF9333EA).copy(alpha = 0.3f),
                    value = igLimit,
                    onValueChange = { igLimit = it },
                    unit = "reels"
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                AlertLimitCardReplica(
                    title = "YouTube",
                    subtitle = "Alert frequency",
                    icon = Icons.Default.PlayArrow,
                    gradient = SolidColor(Color(0xFFEF4444)),
                    borderColor = Color(0xFFEF4444).copy(alpha = 0.3f),
                    value = ytLimit,
                    onValueChange = { ytLimit = it },
                    unit = "shorts"
                )

                Spacer(modifier = Modifier.height(24.dp))

                SectionTitle("Alert Behavior", Icons.Default.Timer)

                TrackingControlCardReplica(
                    title = "Enable Alerts",
                    isTracking = alertsEnabled,
                    onToggle = { alertsEnabled = !alertsEnabled },
                    gradient = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF059669))),
                    icon = Icons.Default.Notifications
                )

                Spacer(modifier = Modifier.height(12.dp))

                TrackingControlCardReplica(
                    title = "Strict Mode",
                    isTracking = alertOnlyAfterLimit,
                    onToggle = { alertOnlyAfterLimit = !alertOnlyAfterLimit },
                    gradient = Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF2563EB))),
                    icon = Icons.Default.Lock
                )
                Text(
                    text = if (alertOnlyAfterLimit) "Alerts will only appear after daily limit is reached." else "Alerts will appear regularly based on alert frequency, even before the daily limit.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                SectionTitle("Tracking Control", Icons.Default.Dns)

                TrackingControlCardReplica(
                    title = "Instagram Tracking",
                    isTracking = trackIG,
                    onToggle = { trackIG = !trackIG },
                    gradient = Brush.linearGradient(listOf(Color(0xFF9333EA), Color(0xFFDB2777))),
                    icon = Icons.Default.PhotoCamera
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                TrackingControlCardReplica(
                    title = "YouTube Tracking",
                    isTracking = trackYT,
                    onToggle = { trackYT = !trackYT },
                    gradient = SolidColor(Color(0xFFEF4444)),
                    icon = Icons.Default.PlayArrow
                )

                Spacer(modifier = Modifier.height(24.dp))

                SectionTitle("Reset Today's Data", Icons.AutoMirrored.Filled.RotateLeft)

                ResetDataCardReplica(
                    title = "Reset Instagram",
                    description = "Clear today's scroll count",
                    gradient = Brush.linearGradient(listOf(Color(0xFF9333EA), Color(0xFFDB2777))),
                    icon = Icons.Default.PhotoCamera,
                    onReset = { showResetIGDialog = true },
                    accentColor = Color(0xFF9333EA)
                )

                Spacer(modifier = Modifier.height(12.dp))

                ResetDataCardReplica(
                    title = "Reset YouTube",
                    description = "Clear today's scroll count",
                    gradient = SolidColor(Color(0xFFEF4444)),
                    icon = Icons.Default.PlayArrow,
                    onReset = { showResetYTDialog = true },
                    accentColor = Color(0xFFEF4444)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF3B82F6).copy(alpha = 0.1f))
                        .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Note: Reset only clears today's data. Historical data in Activity Analysis and History sections remains unchanged.",
                        color = Color(0xFF93C5FD),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}
