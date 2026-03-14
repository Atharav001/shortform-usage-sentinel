@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.scrollersdashboard

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
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
                    revealAnim.animateTo(0f, tween(600, easing = FastOutSlowInEasing))
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
                                    revealAnim.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
                                    currentScreen = "activity"
                                }
                            }
                        },
                        onNavigateToSettings = { center: Offset ->
                            rippleCenter = center
                            scope.launch {
                                overlayScreen = "settings"
                                revealAnim.snapTo(0f)
                                revealAnim.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
                                currentScreen = "settings"
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
                            }
                        }
                    }
                }
            }
        }
    }

    private fun authenticateBiometric(activity: FragmentActivity, onResult: () -> Unit) {
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
    }
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
fun DashboardScreen(db: AppDatabase, isDarkMode: Boolean, refreshKey: Int, onThemeToggle: () -> Unit, onNavigateToActivity: (Offset) -> Unit, onNavigateToSettings: (Offset) -> Unit) {
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
    val improvement = -12 // Simulated improvement

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
            // Header Section
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
                        Text("Screen Time", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()), color = Gray400, fontSize = 14.sp)
                    }
                    IconButton(onClick = {}, modifier = Modifier.size(40.dp).clip(CircleShape).background(Gray800)) {
                        Icon(Icons.Default.CalendarToday, null, tint = Gray300, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Daily Summary Card
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
                            Text(totalTimeStr, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Green400.copy(alpha = 0.2f)).padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.TrendingDown, null, tint = Green400, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${Math.abs(improvement)}% less", color = Green400, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Main Content
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp)) {
                Text("App Usage", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        UsageCard(
                            platform = "instagram",
                            screenTime = formatMillisToTime(instagramTime),
                            scrollCount = instagramCount,
                            percentage = ((instagramCount.toFloat() / igLimit.toFloat()) * 100).toInt().coerceAtMost(100),
                            dailyGoal = igLimit,
                            isDarkMode = true
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        UsageCard(
                            platform = "youtube",
                            screenTime = formatMillisToTime(youtubeTime),
                            scrollCount = youtubeCount,
                            percentage = ((youtubeCount.toFloat() / ytLimit.toFloat()) * 100).toInt().coerceAtMost(100),
                            dailyGoal = ytLimit,
                            isDarkMode = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Quick Stats
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    QuickStatCard("Total Scrolls", (instagramCount + youtubeCount).toString(), "Today", modifier = Modifier.weight(1f))
                    QuickStatCard("Avg per Hour", "24", "Scrolls", modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Achievement Badge
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
                            Text("3 Day Streak!", color = Yellow400, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("You've reduced usage 3 days in a row", color = Gray300, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // View Detailed Analysis Button
                PremiumScalingButton(onClick = onNavigateToActivity, modifier = Modifier.fillMaxWidth(), isDarkMode = true, cornerRadius = 16.dp) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .background(Brush.horizontalGradient(AnalysisButtonGradient)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("View Detailed Analysis", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Tips Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Gray800.copy(alpha = 0.3f))
                        .border(1.dp, Gray700, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text("💡 Daily Tip", color = Gray300, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Try setting app timers to limit your scroll time. Studies show reducing social media by 30 minutes improves well-being.",
                            color = Gray400,
                            fontSize = 12.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(100.dp)) // Bottom padding for navigation
            }
        }
        
        // Bottom Navigation Overlay
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            BottomNavigationBar(onNavigateToActivity, { onNavigateToSettings(Offset.Zero) }, { /* Goals */ }, { /* History */ })
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
fun BottomNavigationBar(onStats: (Offset) -> Unit, onSettings: () -> Unit, onGoals: () -> Unit, onHistory: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Gray900.copy(alpha = 0.95f))
            .border(width = 1.dp, color = Gray800, shape = RectangleShape)
            .padding(vertical = 12.dp)
    ) {
        // We can't easily blur the background of a Box in Compose like backdrop-filter: blur()
        // But we can blur the content behind it if we had access to it.
        // For now, a solid dark semi-transparent background is used.
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            BottomNavItem("Home", Icons.Default.Home, true) {}
            BottomNavItem("Stats", Icons.AutoMirrored.Filled.TrendingDown, false) { onStats(Offset.Zero) }
            BottomNavItem("Goals", Icons.Default.EmojiEvents, false) { onGoals() }
            BottomNavItem("History", Icons.Default.CalendarToday, false) { onHistory() }
        }
    }
}

@Composable
fun BottomNavItem(label: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp)
    ) {
        Icon(icon, null, tint = if (isSelected) Blue500 else Gray500, modifier = Modifier.size(24.dp))
        Text(label, color = if (isSelected) Blue500 else Gray500, fontSize = 10.sp, fontWeight = FontWeight.Medium)
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

fun isAccessibilityServiceEnabled(context: Context, service: Class<out android.accessibilityservice.AccessibilityService>): Boolean {
    val expectedId = android.content.ComponentName(context, service).flattenToString()
    val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
    val colonSplitter = TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServices)
    while (colonSplitter.hasNext()) { if (colonSplitter.next().equals(expectedId, ignoreCase = true)) return true }
    return false
}

fun isUsageAccessEnabled(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    }
    return mode == AppOpsManager.MODE_ALLOWED
}
