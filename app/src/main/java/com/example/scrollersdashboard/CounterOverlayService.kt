package com.example.scrollersdashboard

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.animation.OvershootInterpolator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.WindowManager
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.scrollersdashboard.ui.OverlayDashboardPopup
import com.example.scrollersdashboard.ui.PremiumCounterCapsule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CounterOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private var capsuleComposeView: ComposeView? = null
    private var detailPopupView: ComposeView? = null

    private val currentCount = mutableIntStateOf(0)
    private val currentLimit = mutableIntStateOf(100)
    private val currentAppName = mutableStateOf("")
    private val isVisibleState = mutableStateOf(false)
    private val showBrainEmojisState = mutableStateOf(true)
    private val overlayOpacityState = mutableStateOf(0.85f)
    private val summaryState = mutableStateOf(OverlayDaySummary())
    private val totalDailyScrolls = mutableIntStateOf(0)
    private var recordsObserverJob: Job? = null

    private lateinit var capsuleParams: WindowManager.LayoutParams
    private lateinit var popupParams: WindowManager.LayoutParams

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    /** Session-only drag offset from default top-center anchor */
    private var sessionDragX = 0
    private var sessionDragY = 0

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var db: AppDatabase
    private var prevBroadcastCount = 0

    private val countReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_UPDATE_COUNT -> {
                    val count = intent.getIntExtra(EXTRA_COUNT, currentCount.intValue)
                    val limit = intent.getIntExtra(EXTRA_LIMIT, currentLimit.intValue)
                    val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: currentAppName.value
                    val hitTen = count > 0 && count % 10 == 0 && count != prevBroadcastCount
                    currentCount.intValue = count
                    currentLimit.intValue = limit
                    currentAppName.value = appName
                    prevBroadcastCount = count
                    isVisibleState.value = true
                    if (hitTen) {
                        capsuleComposeView?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                }
                ACTION_SHOW_OVERLAY -> {
                    currentCount.intValue = intent.getIntExtra(EXTRA_COUNT, currentCount.intValue)
                    currentLimit.intValue = intent.getIntExtra(EXTRA_LIMIT, currentLimit.intValue)
                    currentAppName.value = intent.getStringExtra(EXTRA_APP_NAME) ?: ""
                    serviceScope.launch { refreshOverlaySettings() }
                    showCapsule()
                }
                ACTION_HIDE_OVERLAY -> {
                    val immediate = intent.getBooleanExtra(EXTRA_HIDE_IMMEDIATE, false)
                    hideCapsule(immediate = immediate)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        db = AppDatabase.getDatabase(applicationContext)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Scroll counter active"))

        setupParams()

        val filter = IntentFilter().apply {
            addAction(ACTION_UPDATE_COUNT)
            addAction(ACTION_SHOW_OVERLAY)
            addAction(ACTION_HIDE_OVERLAY)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(countReceiver, filter)
        serviceScope.launch { refreshOverlaySettings() }
        startDayRecordsObserver()
    }

    private fun startDayRecordsObserver() {
        recordsObserverJob?.cancel()
        recordsObserverJob = serviceScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            db.scrollDao().getRecordsForDate(today).collectLatest { records ->
                val ig = records.find { it.appType == "Instagram" }
                val yt = records.find { it.appType == "YouTube" }
                val igCount = ig?.scrollCount ?: 0
                val ytCount = yt?.scrollCount ?: 0
                val limits = withContext(Dispatchers.IO) {
                    val dao = db.scrollDao()
                    dao.getSetting("limit_ig")?.toIntOrNull() to dao.getSetting("limit_yt")?.toIntOrNull()
                }
                totalDailyScrolls.intValue = igCount + ytCount
                summaryState.value = OverlayDaySummary(
                    igCount = igCount,
                    ytCount = ytCount,
                    igTimeMillis = ig?.screenTimeMillis ?: 0L,
                    ytTimeMillis = yt?.screenTimeMillis ?: 0L,
                    igLimit = limits.first ?: 100,
                    ytLimit = limits.second ?: 100
                )
                val app = currentAppName.value
                when {
                    app.contains("Instagram", ignoreCase = true) -> currentCount.intValue = igCount
                    app.contains("YouTube", ignoreCase = true) -> currentCount.intValue = ytCount
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setupParams() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        capsuleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        applyDefaultCapsulePosition()

        popupParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
    }

    private fun defaultTopOffsetPx(): Int {
        val statusBarRes = resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBar = if (statusBarRes > 0) resources.getDimensionPixelSize(statusBarRes) else dpToPx(28f)
        return statusBar + dpToPx(16f)
    }

    private fun applyDefaultCapsulePosition() {
        capsuleParams.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        capsuleParams.x = sessionDragX
        capsuleParams.y = defaultTopOffsetPx() + sessionDragY
    }

    private fun showCapsule() {
        if (capsuleComposeView != null) {
            applyDefaultCapsulePosition()
            capsuleComposeView?.alpha = 1f
            capsuleComposeView?.scaleX = 1f
            capsuleComposeView?.scaleY = 1f
            isVisibleState.value = true
            try {
                windowManager.updateViewLayout(capsuleComposeView, capsuleParams)
            } catch (_: Exception) {}
            return
        }

        val themedContext = ContextThemeWrapper(this, R.style.Theme_ScrollersDashboard)
        capsuleComposeView = ComposeView(themedContext).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                val count by currentCount
                val dailyTotal by totalDailyScrolls
                val appName by currentAppName
                val show by isVisibleState
                val showBrain by showBrainEmojisState
                val overlayAlpha by overlayOpacityState

                AnimatedVisibility(
                    visible = show,
                    enter = fadeIn(tween(180)),
                    exit = fadeOut(tween(250))
                ) {
                    PremiumCounterCapsule(
                        appCount = count,
                        totalDailyScrolls = dailyTotal,
                        app = appName,
                        showBrain = showBrain,
                        overlayAlpha = overlayAlpha
                    )
                }
            }
        }

        attachComposeOwners(capsuleComposeView)

        capsuleComposeView?.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = capsuleParams.x
                    initialY = capsuleParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = kotlin.math.abs(event.rawX - initialTouchX)
                    val dy = kotlin.math.abs(event.rawY - initialTouchY)
                    if (dx > 12f || dy > 12f) isDragging = true
                    if (isDragging) {
                        capsuleParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        capsuleParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(capsuleComposeView, capsuleParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        v.performClick()
                    } else {
                        sessionDragX = capsuleParams.x
                        sessionDragY = capsuleParams.y - defaultTopOffsetPx()
                    }
                    true
                }
                else -> false
            }
        }

        capsuleComposeView?.setOnClickListener { showDetailPopup() }

        try {
            applyDefaultCapsulePosition()
            windowManager.addView(capsuleComposeView, capsuleParams)
            isVisibleState.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Show capsule failed: ${e.message}")
        }
    }

    private fun hideCapsule(immediate: Boolean = false) {
        hideDetailPopup()
        val view = capsuleComposeView ?: run {
            isVisibleState.value = false
            return
        }
        if (immediate) {
            isVisibleState.value = false
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {}
            capsuleComposeView = null
            return
        }
        val startY = capsuleParams.y
        val targetY = -dpToPx(72f)
        val startScale = 1f

        val slide = ValueAnimator.ofInt(startY, targetY).apply {
            duration = 420
            interpolator = OvershootInterpolator(0.6f)
            addUpdateListener { anim ->
                capsuleParams.y = anim.animatedValue as Int
                try {
                    windowManager.updateViewLayout(view, capsuleParams)
                } catch (_: Exception) {}
            }
        }

        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", startScale, 0.35f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", startScale, 0.35f)
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)

        AnimatorSet().apply {
            playTogether(slide, scaleX, scaleY, alpha)
            duration = 420
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    isVisibleState.value = false
                    view.scaleX = 1f
                    view.scaleY = 1f
                    view.alpha = 1f
                    applyDefaultCapsulePosition()
                    try {
                        windowManager.removeView(view)
                    } catch (_: Exception) {}
                    capsuleComposeView = null
                }
            })
            start()
        }
    }

    private suspend fun refreshOverlaySettings() {
        withContext(Dispatchers.IO) {
            val dao = db.scrollDao()
            val emojis = dao.getSetting("overlay_brain_emojis")?.toBoolean() ?: true
            val opacityPct = dao.getSetting("overlay_opacity")?.toIntOrNull() ?: 85
            withContext(Dispatchers.Main) {
                showBrainEmojisState.value = emojis
                overlayOpacityState.value = (opacityPct / 100f).coerceIn(0.5f, 1f)
            }
        }
    }

    private fun showDetailPopup() {
        if (detailPopupView != null) return

        val themedContext = ContextThemeWrapper(this, R.style.Theme_ScrollersDashboard)
        detailPopupView = ComposeView(themedContext).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                val summary by summaryState

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ComposeColor.Black.copy(alpha = 0.55f))
                        .clickable(
                            interactionSource = MutableInteractionSource(),
                            indication = null
                        ) { hideDetailPopup() },
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 48.dp, start = 18.dp, end = 18.dp)
                            .clickable(
                                interactionSource = MutableInteractionSource(),
                                indication = null,
                                onClick = {}
                            )
                    ) {
                        OverlayDashboardPopup(
                            summary = summary,
                            onDismiss = { hideDetailPopup() },
                            onOpenDeepAnalysis = { openDeepAnalysis() }
                        )
                    }
                }
            }
        }

        attachComposeOwners(detailPopupView)

        LocalBroadcastManager.getInstance(this).sendBroadcast(
            Intent(ScrollerAccessibilityService.ACTION_PAUSE_MEDIA)
        )

        try {
            windowManager.addView(detailPopupView, popupParams)
        } catch (e: Exception) {
            Log.e(TAG, "Popup show failed: ${e.message}")
            detailPopupView = null
        }
    }

    private fun openDeepAnalysis() {
        hideDetailPopup()
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(MainActivity.EXTRA_OPEN_SCREEN, MainActivity.SCREEN_ACTIVITY)
        }
        startActivity(intent)
    }

    private fun hideDetailPopup() {
        if (detailPopupView != null) {
            LocalBroadcastManager.getInstance(this).sendBroadcast(
                Intent(ScrollerAccessibilityService.ACTION_RESUME_MEDIA)
            )
        }
        detailPopupView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {}
            detailPopupView = null
        }
    }

    private fun attachComposeOwners(view: ComposeView?) {
        view?.setViewTreeLifecycleOwner(this)
        view?.setViewTreeViewModelStoreOwner(this)
        view?.setViewTreeSavedStateRegistryOwner(this)
    }

    private fun dpToPx(dp: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_OVERLAY -> {
                currentCount.intValue = intent.getIntExtra(EXTRA_COUNT, currentCount.intValue)
                currentLimit.intValue = intent.getIntExtra(EXTRA_LIMIT, currentLimit.intValue)
                currentAppName.value = intent.getStringExtra(EXTRA_APP_NAME) ?: ""
                showCapsule()
            }
            ACTION_HIDE_OVERLAY -> hideCapsule(
                immediate = intent?.getBooleanExtra(EXTRA_HIDE_IMMEDIATE, false) == true
            )
            ACTION_UPDATE_COUNT -> {
                currentCount.intValue = intent.getIntExtra(EXTRA_COUNT, currentCount.intValue)
                currentLimit.intValue = intent.getIntExtra(EXTRA_LIMIT, currentLimit.intValue)
                currentAppName.value = intent.getStringExtra(EXTRA_APP_NAME) ?: ""
                isVisibleState.value = true
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        LocalBroadcastManager.getInstance(this).unregisterReceiver(countReceiver)
        hideDetailPopup()
        capsuleComposeView?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        capsuleComposeView = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Overlay Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Scroller's Dashboard")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

    companion object {
        private const val TAG = "CounterOverlay"
        const val CHANNEL_ID = "counter_overlay_channel"
        const val NOTIFICATION_ID = 1002
        const val ACTION_UPDATE_COUNT = "com.example.scrollersdashboard.UPDATE_COUNT"
        const val ACTION_SHOW_OVERLAY = "com.example.scrollersdashboard.SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.example.scrollersdashboard.HIDE_OVERLAY"
        const val EXTRA_COUNT = "extra_count"
        const val EXTRA_LIMIT = "extra_limit"
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_HIDE_IMMEDIATE = "extra_hide_immediate"
    }
}
