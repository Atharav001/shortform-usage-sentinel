package com.example.scrollersdashboard.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrollersdashboard.AppDatabase
import com.example.scrollersdashboard.UserSetting
import com.example.scrollersdashboard.ui.theme.*
import kotlinx.coroutines.launch

private fun normalizedLimit(value: String, fallback: Int) = (value.toIntOrNull() ?: fallback).coerceAtLeast(0).toString()
private fun normalizedGap(value: String, fallback: Int) = (value.toIntOrNull() ?: fallback).coerceAtLeast(1).toString()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(db: AppDatabase, isDarkMode: Boolean, onBack: (Offset) -> Unit) {
    val scope = rememberCoroutineScope()

    var dailyIgLimit by remember { mutableStateOf("100") }
    var dailyYtLimit by remember { mutableStateOf("100") }
    var igLimit by remember { mutableStateOf("50") }
    var ytLimit by remember { mutableStateOf("30") }
    var trackIG by remember { mutableStateOf(true) }
    var trackYT by remember { mutableStateOf(true) }
    var alertOnlyAfterLimit by remember { mutableStateOf(true) }
    var alertsEnabled by remember { mutableStateOf(true) }
    var overlayCounterEnabled by remember { mutableStateOf(false) }
    var overlayBrainEmojis by remember { mutableStateOf(true) }
    var overlayOpacity by remember { mutableFloatStateOf(0.85f) }

    fun saveAndGoBack(backCenter: Offset) {
        scope.launch {
            val dao = db.scrollDao()
            dao.saveSetting(UserSetting("limit_ig", normalizedLimit(dailyIgLimit, 100)))
            dao.saveSetting(UserSetting("limit_yt", normalizedLimit(dailyYtLimit, 100)))
            dao.saveSetting(UserSetting("alert_gap_ig", normalizedGap(igLimit, 50)))
            dao.saveSetting(UserSetting("alert_gap_yt", normalizedGap(ytLimit, 30)))
            dao.saveSetting(UserSetting("track_ig", trackIG.toString()))
            dao.saveSetting(UserSetting("track_yt", trackYT.toString()))
            dao.saveSetting(UserSetting("alert_only_after_limit", alertOnlyAfterLimit.toString()))
            dao.saveSetting(UserSetting("alert_screen_enabled", alertsEnabled.toString()))
            dao.saveSetting(UserSetting("overlay_counter_enabled", overlayCounterEnabled.toString()))
            dao.saveSetting(UserSetting("overlay_brain_emojis", overlayBrainEmojis.toString()))
            dao.saveSetting(UserSetting("overlay_opacity", (overlayOpacity * 100).toInt().toString()))
            onBack(backCenter)
        }
    }
    BackHandler { saveAndGoBack(Offset.Zero) }

    LaunchedEffect(Unit) {
        dailyIgLimit = db.scrollDao().getSetting("limit_ig") ?: "100"
        dailyYtLimit = db.scrollDao().getSetting("limit_yt") ?: "100"
        igLimit = db.scrollDao().getSetting("alert_gap_ig") ?: "50"
        ytLimit = db.scrollDao().getSetting("alert_gap_yt") ?: "30"
        trackIG = db.scrollDao().getSetting("track_ig")?.toBoolean() ?: true
        trackYT = db.scrollDao().getSetting("track_yt")?.toBoolean() ?: true
        alertOnlyAfterLimit = db.scrollDao().getSetting("alert_only_after_limit")?.toBoolean() ?: true
        alertsEnabled = db.scrollDao().getSetting("alert_screen_enabled")?.toBoolean() ?: true
        overlayCounterEnabled = db.scrollDao().getSetting("overlay_counter_enabled")?.toBoolean() ?: false
        overlayBrainEmojis = db.scrollDao().getSetting("overlay_brain_emojis")?.toBoolean() ?: true
        overlayOpacity = (db.scrollDao().getSetting("overlay_opacity")?.toIntOrNull() ?: 85) / 100f
    }

    Box(modifier = Modifier.fillMaxSize().marbleBackground()) {
        Column(modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())) {
            
            Box(modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    var backCenter by remember { mutableStateOf(Offset.Zero) }
                    IconButton(
                        onClick = { saveAndGoBack(backCenter) },
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
                    Text("Settings", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { visible = true }

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 8 }
                ) {
                    Column {
                        SectionTitle("Usage Limits", Icons.Default.EmojiEvents)
                        
                        AlertLimitCardReplica(
                            title = "Instagram Goal",
                            subtitle = "Daily scroll limit",
                            icon = Icons.Default.PhotoCamera,
                            gradient = Brush.linearGradient(TealGradient),
                            borderColor = GlassBorder,
                            value = dailyIgLimit,
                            onValueChange = { dailyIgLimit = it },
                            unit = "reels",
                            instruction = "Set daily target",
                            footer = "Target: $dailyIgLimit reels per day",
                            onDone = { dailyIgLimit = normalizedLimit(dailyIgLimit, 100); scope.launch { db.scrollDao().saveSetting(UserSetting("limit_ig", dailyIgLimit)) } }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        AlertLimitCardReplica(
                            title = "YouTube Goal",
                            subtitle = "Daily scroll limit",
                            icon = Icons.Default.PlayArrow,
                            gradient = Brush.linearGradient(BlueGradient),
                            borderColor = GlassBorder,
                            value = dailyYtLimit,
                            onValueChange = { dailyYtLimit = it },
                            unit = "shorts",
                            instruction = "Set daily target",
                            footer = "Target: $dailyYtLimit shorts per day",
                            onDone = { dailyYtLimit = normalizedLimit(dailyYtLimit, 100); scope.launch { db.scrollDao().saveSetting(UserSetting("limit_yt", dailyYtLimit)) } }
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        SectionTitle("Alert Configuration", Icons.Default.Notifications)
                        
                        AlertLimitCardReplica(
                            title = "Instagram Alerts",
                            subtitle = "Frequency interval",
                            icon = Icons.Default.PhotoCamera,
                            gradient = Brush.linearGradient(TealGradient),
                            borderColor = GlassBorder,
                            value = igLimit,
                            onValueChange = { igLimit = it },
                            unit = "reels",
                            instruction = "Notify every",
                            footer = "Alert will trigger every $igLimit reels",
                            onDone = { igLimit = normalizedGap(igLimit, 50); scope.launch { db.scrollDao().saveSetting(UserSetting("alert_gap_ig", igLimit)) } }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        AlertLimitCardReplica(
                            title = "YouTube Alerts",
                            subtitle = "Frequency interval",
                            icon = Icons.Default.PlayArrow,
                            gradient = Brush.linearGradient(BlueGradient),
                            borderColor = GlassBorder,
                            value = ytLimit,
                            onValueChange = { ytLimit = it },
                            unit = "shorts",
                            instruction = "Notify every",
                            footer = "Alert will trigger every $ytLimit shorts",
                            onDone = { ytLimit = normalizedGap(ytLimit, 30); scope.launch { db.scrollDao().saveSetting(UserSetting("alert_gap_yt", ytLimit)) } }
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        SectionTitle("System Control", Icons.Default.SettingsSuggest)

                        TrackingControlCardReplica(
                            title = "Global Alerts",
                            isTracking = alertsEnabled,
                            onToggle = { 
                                alertsEnabled = !alertsEnabled
                                scope.launch { db.scrollDao().saveSetting(UserSetting("alert_screen_enabled", alertsEnabled.toString())) }
                            },
                            gradient = Brush.linearGradient(BlueGradient),
                            icon = Icons.Default.NotificationsActive
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        TrackingControlCardReplica(
                            title = "Floating Counter",
                            isTracking = overlayCounterEnabled,
                            onToggle = { 
                                overlayCounterEnabled = !overlayCounterEnabled
                                scope.launch { db.scrollDao().saveSetting(UserSetting("overlay_counter_enabled", overlayCounterEnabled.toString())) }
                            },
                            gradient = Brush.linearGradient(listOf(IndigoPremium, IndigoPremium.copy(alpha = 0.6f))),
                            icon = Icons.Default.AdsClick
                        )

                        AnimatedVisibility(visible = overlayCounterEnabled) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                TrackingControlCardReplica(
                                    title = "Brain Emoji States",
                                    isTracking = overlayBrainEmojis,
                                    onToggle = {
                                        overlayBrainEmojis = !overlayBrainEmojis
                                        scope.launch {
                                            db.scrollDao().saveSetting(UserSetting("overlay_brain_emojis", overlayBrainEmojis.toString()))
                                        }
                                    },
                                    gradient = Brush.linearGradient(listOf(Color(0xFF34D399), Color(0xFFFBBF24))),
                                    icon = Icons.Default.Psychology
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(CardBackground)
                                        .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                                        .padding(16.dp)
                                ) {
                                    Column {
                                        Text(
                                            "Overlay opacity",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Slider(
                                            value = overlayOpacity,
                                            onValueChange = { overlayOpacity = it },
                                            onValueChangeFinished = {
                                                scope.launch {
                                                    db.scrollDao().saveSetting(
                                                        UserSetting("overlay_opacity", (overlayOpacity * 100).toInt().toString())
                                                    )
                                                }
                                            },
                                            valueRange = 0.5f..1f
                                        )
                                        Text(
                                            "${(overlayOpacity * 100).toInt()}%",
                                            color = Gray400,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                Text(
                                    "Drag the pill while viewing Reels or Shorts to reposition for this session.",
                                    color = Gray500,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val isStrictMode = !alertOnlyAfterLimit
                        TrackingControlCardReplica(
                            title = "Strict Enforcement",
                            isTracking = isStrictMode,
                            onToggle = { 
                                val newStrictMode = !isStrictMode
                                alertOnlyAfterLimit = !newStrictMode
                                scope.launch { db.scrollDao().saveSetting(UserSetting("alert_only_after_limit", alertOnlyAfterLimit.toString())) }
                            },
                            gradient = Brush.linearGradient(TealGradient),
                            icon = Icons.Default.GppGood
                        )
                        Text(
                            text = if (isStrictMode) "STRICT MODE ACTIVE: Alerts trigger immediately at intervals." else "STANDARD MODE: Alerts trigger only after reaching daily limit.",
                            color = if (isStrictMode) TealPremium else Gray500,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(top = 8.dp, start = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        SectionTitle("App Tracking", Icons.Default.Dns)

                        TrackingControlCardReplica(
                            title = "Instagram Service",
                            isTracking = trackIG,
                            onToggle = { 
                                trackIG = !trackIG
                                scope.launch { db.scrollDao().saveSetting(UserSetting("track_ig", trackIG.toString())) }
                            },
                            gradient = Brush.linearGradient(TealGradient),
                            icon = Icons.Default.PhotoCamera
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        TrackingControlCardReplica(
                            title = "YouTube Service",
                            isTracking = trackYT,
                            onToggle = { 
                                trackYT = !trackYT
                                scope.launch { db.scrollDao().saveSetting(UserSetting("track_yt", trackYT.toString())) }
                            },
                            gradient = Brush.linearGradient(BlueGradient),
                            icon = Icons.Default.PlayArrow
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        SectionTitle("History", Icons.Default.History)
                        Text("Recorded scrolls remain in your history. Pausing tracking stops new counts but does not erase past activity.", color = Gray400, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 8.dp))

                        Spacer(modifier = Modifier.height(120.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TrackingControlCardReplica(title: String, isTracking: Boolean, onToggle: () -> Unit, gradient: Brush, icon: ImageVector) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground)
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            .clickable { onToggle() }
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(gradient), 
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(if (isTracking) "ACTIVE" else "DISABLED", color = if (isTracking) EmeraldPremium else Gray500, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
            
            val thumbPosition by animateFloatAsState(
                targetValue = if (isTracking) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                label = "ToggleAnimation"
            )
            
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(28.dp)
                    .clip(CircleShape)
                    .background(if (isTracking) EmeraldPremium.copy(alpha = 0.12f) else ObsidianBlack)
                    .border(1.dp, GlassBorder, CircleShape)
                    .padding(4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .offset(x = (24 * thumbPosition).dp)
                        .clip(CircleShape)
                        .background(if (isTracking) EmeraldPremium else Gray500)
                )
            }
        }
    }
}

