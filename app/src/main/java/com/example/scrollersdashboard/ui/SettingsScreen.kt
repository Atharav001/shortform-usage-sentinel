package com.example.scrollersdashboard.ui

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrollersdashboard.AppDatabase
import com.example.scrollersdashboard.UserSetting
import com.example.scrollersdashboard.ui.theme.Gray800
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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
            text = { Text("It will reset today's scroll count only.", color = Color.LightGray) },
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
            text = { Text("It will reset today's scroll count only.", color = Color.LightGray) },
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
                                Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
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
                    footer = "Your overall goal is $dailyIgLimit reels per day",
                    onDone = { 
                        scope.launch { 
                            db.scrollDao().saveSetting(UserSetting("limit_ig", dailyIgLimit))
                            Toast.makeText(context, "Instagram limit saved", Toast.LENGTH_SHORT).show()
                        } 
                    }
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
                    footer = "Your overall goal is $dailyYtLimit shorts per day",
                    onDone = { 
                        scope.launch { 
                            db.scrollDao().saveSetting(UserSetting("limit_yt", dailyYtLimit))
                            Toast.makeText(context, "YouTube limit saved", Toast.LENGTH_SHORT).show()
                        } 
                    }
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
                    unit = "reels",
                    instruction = "Alert after every",
                    footer = "An alert screen will appear after scrolling $igLimit reels",
                    onDone = { 
                        scope.launch { 
                            db.scrollDao().saveSetting(UserSetting("alert_gap_ig", igLimit))
                            Toast.makeText(context, "Instagram frequency saved", Toast.LENGTH_SHORT).show()
                        } 
                    }
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
                    unit = "shorts",
                    instruction = "Alert after every",
                    footer = "An alert screen will appear after scrolling $ytLimit shorts",
                    onDone = { 
                        scope.launch { 
                            db.scrollDao().saveSetting(UserSetting("alert_gap_yt", ytLimit))
                            Toast.makeText(context, "YouTube frequency saved", Toast.LENGTH_SHORT).show()
                        } 
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                SectionTitle("Alert Behavior", Icons.Default.Timer)

                TrackingControlCardReplica(
                    title = "Enable Alerts",
                    isTracking = alertsEnabled,
                    onToggle = { 
                        alertsEnabled = !alertsEnabled
                        scope.launch { db.scrollDao().saveSetting(UserSetting("alert_screen_enabled", alertsEnabled.toString())) }
                    },
                    gradient = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF059669))),
                    icon = Icons.Default.Notifications
                )

                Spacer(modifier = Modifier.height(12.dp))

                val isStrictMode = !alertOnlyAfterLimit
                TrackingControlCardReplica(
                    title = "Strict Mode",
                    isTracking = isStrictMode,
                    onToggle = { 
                        val newStrictMode = !isStrictMode
                        alertOnlyAfterLimit = !newStrictMode
                        scope.launch { db.scrollDao().saveSetting(UserSetting("alert_only_after_limit", alertOnlyAfterLimit.toString())) }
                    },
                    gradient = Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF2563EB))),
                    icon = Icons.Default.Lock
                )
                Text(
                    text = if (isStrictMode) "Strict Mode ON: Alerts will appear regularly based on alert frequency, even before the daily limit." else "Strict Mode OFF: Alerts will only appear after daily limit is reached.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                SectionTitle("Tracking Control", Icons.Default.Dns)

                TrackingControlCardReplica(
                    title = "Instagram Tracking",
                    isTracking = trackIG,
                    onToggle = { 
                        trackIG = !trackIG
                        scope.launch { db.scrollDao().saveSetting(UserSetting("track_ig", trackIG.toString())) }
                    },
                    gradient = Brush.linearGradient(listOf(Color(0xFF9333EA), Color(0xFFDB2777))),
                    icon = Icons.Default.PhotoCamera
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                TrackingControlCardReplica(
                    title = "YouTube Tracking",
                    isTracking = trackYT,
                    onToggle = { 
                        trackYT = !trackYT
                        scope.launch { db.scrollDao().saveSetting(UserSetting("track_yt", trackYT.toString())) }
                    },
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
