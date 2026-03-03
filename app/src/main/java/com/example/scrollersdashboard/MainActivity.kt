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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.scrollersdashboard.ui.*
import com.example.scrollersdashboard.ui.theme.ScrollersDashboardTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : FragmentActivity() {
    
    private val db by lazy {
        androidx.room.Room.databaseBuilder(applicationContext, AppDatabase::class.java, "scroller-db")
            .fallbackToDestructiveMigration(true).build()
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

            // Function to handle global back navigation
            val navigateBack = { center: Offset ->
                rippleCenter = center
                scope.launch {
                    revealAnim.animateTo(0f, tween(600, easing = FastOutSlowInEasing))
                    currentScreen = "dashboard"
                }
            }

            // Android Hardware Back Button Handling
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
                        containerColor = if (isDarkMode) GlassColor else Color.White,
                        title = { Text("Permissions Required", color = if (isDarkMode) Color.White else Color.Black, fontWeight = FontWeight.Bold) },
                        text = {
                            Column {
                                Text("To track scrolls and accurate screen time, please enable the following:", color = if (isDarkMode) Color.LightGray else Color.DarkGray)
                                Spacer(modifier = Modifier.height(16.dp))
                                PermissionItem("Accessibility Service", isServiceEnabled, isDarkMode)
                                Spacer(modifier = Modifier.height(8.dp))
                                PermissionItem("Usage Access", isUsageAccessEnabled, isDarkMode)
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                if (!isServiceEnabled) {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                } else if (!isUsageAccessEnabled) {
                                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                }
                            }, shape = RoundedCornerShape(12.dp)) { 
                                Text(if (!isServiceEnabled) "Enable Accessibility" else "Enable Usage Access") 
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { hasDismissedPrompt = true }) { Text("Later") }
                        },
                        shape = RoundedCornerShape(24.dp)
                    )
                }

                Box(modifier = Modifier.fillMaxSize().background(if (isDarkMode) DeepCharcoal else LightGrey)) {
                    DashboardScreen(
                        db = db,
                        isDarkMode = isDarkMode,
                        refreshKey = refreshKey,
                        onThemeToggle = { isDarkMode = !isDarkMode },
                        onNavigateToActivity = { center ->
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
                        onNavigateToSettings = { center ->
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
                            .background(if (isDarkMode) DeepCharcoal else LightGrey)
                        ) {
                            when (activeOverlay) {
                                "activity" -> ActivityScreen(
                                    db = db,
                                    isDarkMode = isDarkMode,
                                    refreshKey = refreshKey,
                                    onBack = { center -> navigateBack(center) }
                                )
                                "settings" -> SettingsScreen(
                                    db = db,
                                    isDarkMode = isDarkMode,
                                    onBack = { center -> navigateBack(center) }
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
                    onResult() 
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
        Text(label, color = if (isDarkMode) Color.White else Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
    
    val textColor = if (isDarkMode) Color.White else Color(0xFF1C1C1E)
    val subTextColor = if (isDarkMode) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.4f)

    fun formatMillis(millis: Long): String {
        val seconds = millis / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    fun formatMillisShort(millis: Long): String {
        val seconds = millis / 1000
        val mins = (seconds / 60).toInt()
        return "$mins min"
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
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Dashboard", color = textColor, fontSize = 34.sp, fontWeight = FontWeight.Black)
                    Text(SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()), color = subTextColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onThemeToggle, modifier = Modifier.size(44.dp).clip(CircleShape).background(if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f))) {
                        Icon(if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, null, tint = if (isDarkMode) Color(0xFFFFD600) else Color.DarkGray, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    PremiumScalingButton(onClick = onNavigateToSettings) {
                        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Tune, null, tint = textColor, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f).modernGlassy(isDarkMode), contentAlignment = Alignment.Center) {
                    ActivityRingCard("Instagram", instagramCount, igLimit, InstaNeonGradient, textColor, isDarkMode)
                }
                Box(modifier = Modifier.weight(1f).modernGlassy(isDarkMode), contentAlignment = Alignment.Center) {
                    ActivityRingCard("YouTube", youtubeCount, ytLimit, YTNeonGradient, textColor, isDarkMode)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // REFINED LIVE ACTIVITY PANEL (Matching Image)
            Box(modifier = Modifier.fillMaxWidth().modernGlassy(isDarkMode)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Insights, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Live Activity", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    
                    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        // Instagram Section
                        Column(modifier = Modifier.weight(1f).padding(vertical = 16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PhotoCamera, null, tint = Color(0xFFE1306C), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Instagram", color = Color.Gray, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("$instagramCount", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                            Text("REELS SCROLLED", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(formatMillisShort(instagramTime), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        // Vertical Divider
                        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.1f)))
                        
                        // YouTube Section
                        Column(modifier = Modifier.weight(1f).padding(vertical = 16.dp), horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("YouTube", color = Color.Gray, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.PlayArrow, null, tint = Color.Red, modifier = Modifier.size(20.dp))
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Red)) // Red dot
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("$youtubeCount", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                            Text("SHORTS SCROLLED", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(formatMillisShort(youtubeTime), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Footer Row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("TOTAL SCROLLS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                            Text("${instagramCount + youtubeCount}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("TOTAL TIME", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                            Text(formatMillisShort(instagramTime + youtubeTime), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Detailed Analysis Button (Matching image)
            PremiumScalingButton(onClick = onNavigateToActivity, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Brush.linearGradient(AnalysisButtonGradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("View Detailed Analysis", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
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
            containerColor = if (isDarkMode) GlassColor else Color.White,
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
            containerColor = if (isDarkMode) GlassColor else Color.White,
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
        containerColor = if (isDarkMode) DeepCharcoal else LightGrey
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
                    ClayPillButton(Modifier.weight(1f), "Reset YT", Icons.Default.SmartDisplay, Color(0xFFFF0000), isDarkMode) {
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
    Column(modifier = Modifier.fillMaxWidth().modernGlassy(isDarkMode).padding(20.dp)) {
        Text(title, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(16.dp))
        content()
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
        containerColor = if (isDarkMode) DeepCharcoal else LightGrey
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 20.dp)) {
            // Tab Switcher
            Row(modifier = Modifier.fillMaxWidth().modernGlassy(isDarkMode).padding(6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("Day", "Week", "Month").forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(modifier = Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) (if (isDarkMode) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)) else Color.Transparent)
                        .clickable { selectedTab = tab }, contentAlignment = Alignment.Center) {
                        Text(tab, color = if (isSelected) textColor else Color.Gray, fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold, fontSize = 14.sp)
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
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
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
        Box(modifier = Modifier.fillMaxWidth().height(300.dp).padding(horizontal = 4.dp)) {
            val textMeasurer = rememberTextMeasurer()
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width; 
                val labelAreaWidth = 40.dp.toPx()
                val height = size.height - 30.dp.toPx()
                val chartWidth = width - labelAreaWidth
                val barAreaWidth = chartWidth / 7; val barWidth = 10.dp.toPx()
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
        Box(modifier = Modifier.fillMaxWidth().height(250.dp).padding(horizontal = 4.dp)) {
            val textMeasurer = rememberTextMeasurer()
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width; 
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
        Box(modifier = Modifier.modernGlassy(isDarkMode).padding(16.dp)) {
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
        val width = size.width; 
        val labelAreaWidth = 36.dp.toPx()
        val height = size.height - 25.dp.toPx(); 
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
    
    // Calculate duration
    val durationMillis = session.last().timestamp - session[0].timestamp
    val durationText = if (durationMillis > 60000) {
        val mins = durationMillis / 60000
        "${mins}m"
    } else {
        "${durationMillis / 1000}s"
    }

    Box(modifier = Modifier.fillMaxWidth().modernGlassy(isDarkMode).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            val textColor = if (isDarkMode) Color.White else Color.Black
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (startTime == endTime) startTime else "$startTime - $endTime", color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                // Merged scroll count and duration
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

fun isAccessibilityServiceEnabled(context: Context, service: Class<out android.accessibilityservice.AccessibilityService>): Boolean {
    val expectedId = android.content.ComponentName(context, service).flattenToString()
    val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
    val colonSplitter = TextUtils.SimpleStringSplitter(':'); colonSplitter.setString(enabledServices)
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
