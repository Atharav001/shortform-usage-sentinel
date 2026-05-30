@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.scrollersdashboard

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.scrollersdashboard.ui.*
import com.example.scrollersdashboard.ui.onboarding.OnboardingFlow
import com.example.scrollersdashboard.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class MainActivity : FragmentActivity() {

    var deepAnalysisRequest = mutableIntStateOf(0)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getStringExtra(EXTRA_OPEN_SCREEN) == SCREEN_ACTIVITY) {
            deepAnalysisRequest.intValue++
        }
    }

    companion object {
        private const val PREFS_NAME = "scrollers_prefs"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        const val EXTRA_OPEN_SCREEN = "open_screen"
        const val SCREEN_ACTIVITY = "activity"
    }

    private val db by lazy {
        AppDatabase.getDatabase(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isDarkMode by remember { mutableStateOf(true) }
            val initialScreen = remember {
                when (intent?.getStringExtra(EXTRA_OPEN_SCREEN)) {
                    SCREEN_ACTIVITY -> "activity"
                    else -> "dashboard"
                }
            }
            var currentScreen by remember { mutableStateOf(initialScreen) }
            
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

            val navigateBack = {
                currentScreen = "dashboard"
            }

            if (currentScreen != "dashboard") {
                BackHandler {
                    navigateBack()
                }
            }

            ScrollersDashboardTheme(darkTheme = isDarkMode) {
                val context = LocalContext.current
                val activity = context as? MainActivity
                val deepAnalysisReq by remember(activity) {
                    activity?.deepAnalysisRequest ?: mutableIntStateOf(0)
                }
                LaunchedEffect(deepAnalysisReq) {
                    if (deepAnalysisReq > 0) {
                        currentScreen = "activity"
                    }
                }
                val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
                var showOnboarding by remember {
                    mutableStateOf(!prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false))
                }

                if (showOnboarding) {
                    OnboardingFlow(
                        onComplete = {
                            prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, true).apply()
                            showOnboarding = false
                        }
                    )
                    return@ScrollersDashboardTheme
                }

                // Permission states
                val isUsageAccessEnabled = remember(refreshKey) { isUsageAccessEnabled(context) }
                val isServiceEnabled = remember(refreshKey) { isAccessibilityServiceEnabled(context, ScrollerAccessibilityService::class.java) }
                val isOverlayEnabled = remember(refreshKey) { isOverlayPermissionEnabled(context) }

                var hasDismissedPrompt by remember(refreshKey) { mutableStateOf(false) }

                val anyDisabled = !isUsageAccessEnabled || !isServiceEnabled || !isOverlayEnabled
                val vitalMissing = !isUsageAccessEnabled || !isServiceEnabled

                if (anyDisabled && !hasDismissedPrompt) {
                    Dialog(
                        onDismissRequest = { if (!vitalMissing) hasDismissedPrompt = true },
                        properties = DialogProperties(
                            usePlatformDefaultWidth = false,
                            dismissOnBackPress = !vitalMissing,
                            dismissOnClickOutside = !vitalMissing
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .clip(RoundedCornerShape(32.dp))
                                .background(OnyxSurface)
                                .border(1.dp, GlassBorder, RoundedCornerShape(32.dp))
                                .padding(28.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(CobaltPremium.copy(alpha = 0.12f))
                                        .border(1.dp, CobaltPremium.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Security, 
                                        null, 
                                        tint = CobaltPremium, 
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Text(
                                    "Permissions Required", 
                                    color = Color.White, 
                                    fontSize = 22.sp, 
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                val message = when {
                                    vitalMissing -> "Vital permissions are missing. Scrollers Dashboard needs usage access and accessibility service to track your habits."
                                    else -> "Overlay permission is optional but required for the floating counter feature."
                                }
                                
                                Text(
                                    message,
                                    color = Gray400,
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp
                                )
                                
                                Spacer(modifier = Modifier.height(28.dp))
                                
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(ObsidianBlack)
                                        .border(1.dp, GlassBorder.copy(alpha = 0.04f), RoundedCornerShape(24.dp))
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    PermissionItemRow("User Usage Access (Vital)", isUsageAccessEnabled)
                                    PermissionItemRow("Accessibility Service (Vital)", isServiceEnabled)
                                    PermissionItemRow("Overlay Permission", isOverlayEnabled)
                                }
                                
                                Spacer(modifier = Modifier.height(32.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!vitalMissing) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(54.dp)
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(Color.White.copy(alpha = 0.05f))
                                                .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
                                                .clickable { hasDismissedPrompt = true },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Later", color = Gray300, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                    }
                                    
                                    PremiumScalingButton(
                                        onClick = {
                                            when {
                                                !isUsageAccessEnabled -> {
                                                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                                }
                                                !isServiceEnabled -> {
                                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                                }
                                                !isOverlayEnabled -> {
                                                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                                    context.startActivity(intent)
                                                }
                                            }
                                        }, 
                                        isDarkMode = isDarkMode,
                                        modifier = Modifier.weight(if (vitalMissing) 1f else 1.8f).height(54.dp),
                                        cornerRadius = 18.dp
                                    ) { 
                                        val btnText = when {
                                            !isUsageAccessEnabled -> "Enable User Access"
                                            !isServiceEnabled -> "Enable Accessibility"
                                            else -> "Enable Overlay"
                                        }
                                        Box(
                                            modifier = Modifier.fillMaxSize().background(Brush.linearGradient(TealGradient)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                btnText.uppercase(), 
                                                color = Color.White, 
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.sp,
                                                letterSpacing = 1.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    DashboardScreen(
                        db = db,
                        refreshKey = refreshKey,
                        onNavigateToActivity = { _ ->
                            authenticateBiometric(this@MainActivity) {
                                currentScreen = "activity"
                            }
                        },
                        onNavigateToSettings = { _ ->
                            currentScreen = "settings"
                        },
                        onNavigateToGoals = { _ ->
                            currentScreen = "goals"
                        },
                        onNavigateToHistory = { _ ->
                            currentScreen = "history"
                        }
                    )

                    AnimatedContent(
                        targetState = currentScreen,
                        modifier = Modifier.fillMaxSize(),
                        transitionSpec = {
                            (fadeIn(tween(260, easing = FastOutSlowInEasing)) +
                                slideInVertically(
                                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                                    initialOffsetY = { it / 14 }
                                )) togetherWith fadeOut(tween(200))
                        },
                        label = "screenTransition"
                    ) { screen ->
                        if (screen != "dashboard") {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(ObsidianBlack)
                            ) {
                                when (screen) {
                                    "activity" -> ActivityScreen(
                                        db = db,
                                        isDarkMode = isDarkMode,
                                        refreshKey = refreshKey,
                                        onBack = { _ -> navigateBack() }
                                    )
                                    "settings" -> SettingsScreen(
                                        db = db,
                                        isDarkMode = isDarkMode,
                                        onBack = { _ -> navigateBack() }
                                    )
                                    "goals" -> GoalsScreen(
                                        db = db,
                                        isDarkMode = isDarkMode,
                                        onBack = { _ -> navigateBack() }
                                    )
                                    "history" -> HistoryScreen(
                                        db = db,
                                        isDarkMode = isDarkMode,
                                        onBack = { _ -> navigateBack() }
                                    )
                                }
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
    @Suppress("DEPRECATION")
    val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    return mode == AppOpsManager.MODE_ALLOWED
}

fun isOverlayPermissionEnabled(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true
    }
}

@Composable
fun PermissionItemRow(label: String, isEnabled: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isEnabled) EmeraldPremium.copy(alpha = 0.15f) else RosePremium.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isEnabled) Icons.Default.Check else Icons.Default.Close,
                null,
                tint = if (isEnabled) EmeraldPremium else RosePremium,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun DashboardScreen(db: AppDatabase, refreshKey: Int, onNavigateToActivity: (Offset) -> Unit, onNavigateToSettings: (Offset) -> Unit, onNavigateToGoals: (Offset) -> Unit, onNavigateToHistory: (Offset) -> Unit) {
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
    
    val todayTime = instagramTime + youtubeTime
    val yesterdayTime = remember(scrollRecords) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdf.format(cal.time)
        scrollRecords.filter { it.date == yesterdayStr }.sumOf { it.screenTimeMillis }
    }
    
    val improvement = if (yesterdayTime > 0) {
        ((todayTime - yesterdayTime).toFloat() / yesterdayTime * 100).toInt()
    } else 0

    val totalScrolls = instagramCount + youtubeCount
    val brainGlow = brainGlowColorForScrollCount(totalScrolls)
    val view = LocalView.current
    var prevTotalScrolls by remember { mutableIntStateOf(totalScrolls) }
    var milestoneCelebrationKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(totalScrolls) {
        if (totalScrolls > prevTotalScrolls) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            val hitMilestone = listOf(10, 50, 100).any { m ->
                prevTotalScrolls < m && totalScrolls >= m
            }
            if (hitMilestone) {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                milestoneCelebrationKey++
            }
        }
        prevTotalScrolls = totalScrolls
    }

    val streakCount = remember(scrollRecords, igLimit, ytLimit) {
        if (scrollRecords.isEmpty()) return@remember 0
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val recordsByDate = scrollRecords.groupBy { it.date }
        var streak = 0
        val streakCal = Calendar.getInstance()
        
        for (i in 0 until 60) {
            val dStr = sdf.format(streakCal.time)
            val dayRecords = recordsByDate[dStr] ?: if (dStr == todayStr) {
                streak++
                streakCal.add(Calendar.DAY_OF_YEAR, -1)
                continue
            } else break
            
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
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedGradientBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
            // Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(54.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Dashboard", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text(SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()), color = Gray400, fontSize = 14.sp)
                    }
                    IconButton(onClick = { onNavigateToSettings(Offset.Zero) }, modifier = Modifier.size(44.dp).clip(CircleShape).background(CardBackground).border(1.dp, GlassBorder, CircleShape)) {
                        Icon(Icons.Default.Settings, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    BrainMascot(
                        scrollCount = instagramCount + youtubeCount,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(800)) + slideInVertically(tween(800)) { it / 3 }
                ) {
                    GlassmorphicCard(
                        modifier = Modifier.fillMaxWidth(),
                        depth = GlassDepthLevel.Front,
                        style = GlassStyle.Plain,
                        cornerRadius = 24.dp,
                        celebrationKey = milestoneCelebrationKey
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("TOTAL USAGE", color = Gray500, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                AnimatedUsageTimeText(
                                    millis = instagramTime + youtubeTime,
                                    glowColor = brainGlow,
                                    fontSize = 36.sp
                                )
                            }
                            if (yesterdayTime > 0) {
                                val isImprovement = improvement <= 0
                                val trendColor = if (isImprovement) EmeraldPremium else RosePremium
                                val trendIcon = if (isImprovement) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp
                                val trendText = "${abs(improvement)}%"
                                
                                Column(horizontalAlignment = Alignment.End) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(trendColor.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(trendIcon, null, tint = trendColor, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(trendText, color = trendColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text("vs yesterday", color = Gray500, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                val combinedTimeHours = (instagramTime + youtubeTime).toFloat() / (1000 * 60 * 60)
                val avgPerHour = if (combinedTimeHours > 0) (totalScrolls / combinedTimeHours).toInt() else 0

                SectionTitle("Today's Focus", Icons.Default.ElectricBolt)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        UsageCard(
                            platform = "instagram",
                            screenTime = formatMillisToTime(instagramTime),
                            scrollCount = instagramCount,
                            percentage = if (igLimit > 0) ((instagramCount.divideSafely(igLimit)) * 100).toInt().coerceAtMost(100) else 0,
                            dailyGoal = igLimit,
                            isDarkMode = true,
                            totalScrollsForGlow = totalScrolls
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        UsageCard(
                            platform = "youtube",
                            screenTime = formatMillisToTime(youtubeTime),
                            scrollCount = youtubeCount,
                            percentage = if (ytLimit > 0) ((youtubeCount.divideSafely(ytLimit)) * 100).toInt().coerceAtMost(100) else 0,
                            dailyGoal = ytLimit,
                            isDarkMode = true,
                            totalScrollsForGlow = totalScrolls
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    QuickStatCard(
                        title = "Total Scrolls",
                        value = totalScrolls.toString(),
                        subtitle = "Today",
                        modifier = Modifier.weight(1f),
                        animateValue = true
                    )
                    QuickStatCard(
                        title = "Scroll Velocity",
                        value = "$avgPerHour/h",
                        subtitle = "Average",
                        modifier = Modifier.weight(1f),
                        animateValue = true
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (streakCount >= 2) {
                    GlassmorphicCard(
                        modifier = Modifier.fillMaxWidth(),
                        depth = GlassDepthLevel.Mid,
                        style = GlassStyle.Plain,
                        cornerRadius = 24.dp,
                        padding = PaddingValues(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(AmberPremium.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.EmojiEvents, null, tint = AmberPremium, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("$streakCount DAY STREAK", color = AmberPremium, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                Text("Consistency is key! Keep it up.", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                var analysisCenter by remember { mutableStateOf(Offset.Zero) }
                val analysisPressed = remember { MutableInteractionSource() }
                val isAnalysisPressed by analysisPressed.collectIsPressedAsState()
                val analysisScale by animateFloatAsState(
                    targetValue = if (isAnalysisPressed) 0.97f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessHigh),
                    label = "analysisScale"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(analysisScale)
                        .onGloballyPositioned {
                            analysisCenter = it.positionInWindow() + Offset(
                                it.size.width / 2f,
                                it.size.height / 2f
                            )
                        }
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(18.dp),
                            spotColor = CobaltPremium.copy(alpha = 0.5f)
                        )
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(CobaltPremium, IndigoPremium, VioletPremium)
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
                        .clickable(
                            interactionSource = analysisPressed,
                            indication = null,
                            onClick = { onNavigateToActivity(analysisCenter) }
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Analytics, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Deep Analysis",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            null,
                            tint = Color.White.copy(alpha = 0.95f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
            }
        }
        
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            BottomNavigationBar(onNavigateToActivity, onNavigateToGoals) { onNavigateToHistory(Offset.Zero) }
        }
    }
}

private fun Int.divideSafely(total: Int): Float = if (total > 0) this.toFloat() / total else 0f

@Composable
fun QuickStatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    animateValue: Boolean = false,
) {
    GlassmorphicCard(
        modifier = modifier,
        depth = GlassDepthLevel.Back,
        style = GlassStyle.Plain,
        cornerRadius = 24.dp,
        padding = PaddingValues(16.dp)
    ) {
        Column {
            Text(title.uppercase(), color = Gray500, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(4.dp))
            if (animateValue) {
                Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            } else {
                Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
            Text(subtitle, color = Gray400, fontSize = 11.sp)
        }
    }
}

@Composable
fun BottomNavigationBar(onStats: (Offset) -> Unit, onGoals: (Offset) -> Unit, onHistory: (Offset) -> Unit) {
    val navShape = RoundedCornerShape(22.dp)
    val navScrim = Color(0xFF08080F).copy(alpha = 0.94f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .height(74.dp)
            .clip(navShape)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(navScrim)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(36.dp)
                    .background(Color.Black.copy(alpha = 0.35f))
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(1.dp, Color.White.copy(alpha = 0.08f), navShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = tween(140),
        label = "navScale"
    )
    val tint by animateColorAsState(
        targetValue = if (isSelected) CobaltPremium else if (isPressed) Color.White.copy(alpha = 0.7f) else Gray500,
        animationSpec = tween(140),
        label = "navTint"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun GoalsScreen(db: AppDatabase, isDarkMode: Boolean, onBack: (Offset) -> Unit) {
    var selectedView by remember { mutableStateOf("habits") }
    val scope = rememberCoroutineScope()
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    val habitTasks by db.scrollDao().getHabitTasks().collectAsState(initial = emptyList())
    
    val refreshDailyTodoState = db.scrollDao().getSettingFlow("refresh_daily_todo").collectAsState(initial = "true")
    val isRefreshDaily = refreshDailyTodoState.value?.toBoolean() ?: true

    val todoTasks by (if (isRefreshDaily) db.scrollDao().getTodoTasks(today) else db.scrollDao().getTodoTasks("permanent_todo")).collectAsState(initial = emptyList())

    LaunchedEffect(isRefreshDaily) {
        if (isRefreshDaily) {
            db.scrollDao().deleteOldTodos(today)
        }
    }

    val habitsCompleted = habitTasks.count { it.lastCompletedDate == today }
    val todosCompleted = todoTasks.count { it.isCompleted }
    
    val habitProgress = if (habitTasks.isNotEmpty()) (habitsCompleted.divideSafely(habitTasks.size)) else 0f
    val todoProgress = if (todoTasks.isNotEmpty()) (todosCompleted.divideSafely(todoTasks.size)) else 0f

    var newTaskText by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().marbleBackground()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Daily Goals", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp) },
                    navigationIcon = {
                        var backCenter by remember { mutableStateOf(Offset.Zero) }
                        IconButton(onClick = { onBack(backCenter) }, modifier = Modifier.onGloballyPositioned { backCenter = it.positionInWindow() + Offset(it.size.width.toFloat() / 2f, it.size.height.toFloat() / 2f) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 20.dp)) {
                NeumorphicTabSwitcher(
                    tabs = listOf("Habits", "Tasks"), 
                    selectedTab = if (selectedView == "habits") "Habits" else "Tasks", 
                    onTabSelected = { selectedView = if (it == "Habits") "habits" else "todos" }, 
                    isDarkMode = isDarkMode
                )

                Spacer(modifier = Modifier.height(24.dp))

                AnimatedContent(
                    targetState = selectedView,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(400, delayMillis = 90)) + scaleIn(initialScale = 0.92f, animationSpec = tween(400, delayMillis = 90)))
                            .togetherWith(fadeOut(animationSpec = tween(100)))
                    },
                    label = "GoalsTabSwitch"
                ) { view ->
                    Column {
                        val currentProgress = if (view == "habits") habitProgress else todoProgress
                        val countText = if (view == "habits") "$habitsCompleted of ${habitTasks.size} habits" else "$todosCompleted of ${todoTasks.size} tasks"
                        val itemGradient = if (view == "habits") {
                            AppGradients.HabitsAccent
                        } else {
                            AppGradients.TasksAccent
                        }

                        GoalsProgressCard(
                            progress = currentProgress,
                            countLabel = countText
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        val onAddTask = {
                            if (newTaskText.isNotBlank()) {
                                scope.launch {
                                    if (selectedView == "habits") {
                                        db.scrollDao().insertHabit(HabitTask(title = newTaskText))
                                    } else {
                                        val date = if (isRefreshDaily) today else "permanent_todo"
                                        db.scrollDao().insertTodo(TodoTask(title = newTaskText, date = date))
                                    }
                                    newTaskText = ""
                                    keyboardController?.hide()
                                }
                            }
                        }

                        GlassGoalsInputRow(
                            value = newTaskText,
                            onValueChange = { newTaskText = it },
                            placeholder = if (selectedView == "habits") "New daily habit..." else "New task...",
                            onAdd = onAddTask,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        LazyColumn {
                            if (selectedView == "habits") {
                                items(habitTasks, key = { it.id }) { habit ->
                                    val isDone = habit.lastCompletedDate == today
                                    SwipeableGoalListItem(
                                        title = habit.title,
                                        isCompleted = isDone,
                                        accentGradient = itemGradient,
                                        onToggle = {
                                            scope.launch {
                                                db.scrollDao().insertHabit(
                                                    habit.copy(lastCompletedDate = if (isDone) "" else today)
                                                )
                                            }
                                        },
                                        onDelete = {
                                            scope.launch { db.scrollDao().deleteHabit(habit.id) }
                                        }
                                    )
                                }
                            } else {
                                items(todoTasks, key = { it.id }) { todo ->
                                    SwipeableGoalListItem(
                                        title = todo.title,
                                        isCompleted = todo.isCompleted,
                                        accentGradient = itemGradient,
                                        onToggle = {
                                            scope.launch {
                                                db.scrollDao().insertTodo(todo.copy(isCompleted = !todo.isCompleted))
                                            }
                                        },
                                        onDelete = {
                                            scope.launch { db.scrollDao().deleteTodo(todo.id) }
                                        }
                                    )
                                }
                            }
                            item { Spacer(modifier = Modifier.height(100.dp)) }
                        }
                    }
                }
            }
        }
    }
}

