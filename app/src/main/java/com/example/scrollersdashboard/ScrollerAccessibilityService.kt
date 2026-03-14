package com.example.scrollersdashboard

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.scrollersdashboard.ui.TodoHabitPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ScrollerAccessibilityService : AccessibilityService() {

    private lateinit var db: AppDatabase
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val TAG = "ScrollerService"
    
    private var overlayView: View? = null
    private lateinit var windowManager: WindowManager
    private var currentLifecycleOwner: OverlayLifecycleOwner? = null

    private val pendingCounts = mutableMapOf<String, Int>()
    private var flushJob: Job? = null
    private val BATCH_SIZE = 3 

    private val lastAlertedCount = mutableMapOf<String, Int>()
    private var lastAlertDate: String? = null

    private val instagramTracker = InstagramTracker()
    private val youtubeTracker = YouTubeTracker()

    private var syncJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getDatabase(this)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        Log.d(TAG, "Service Connected and Ready")
        
        startScreenTimeSync()
    }

    private fun startScreenTimeSync() {
        syncJob?.cancel()
        syncJob = serviceScope.launch {
            while (true) {
                syncScreenTimeWithSystem()
                delay(30000) 
            }
        }
    }

    private fun syncScreenTimeWithSystem() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        val endOfDay = System.currentTimeMillis()

        val stats = usageStatsManager.queryAndAggregateUsageStats(startOfDay, endOfDay)
        
        val igTime = stats["com.instagram.android"]?.totalTimeInForeground ?: 0L
        val ytTime = stats["com.google.android.youtube"]?.totalTimeInForeground ?: 0L

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        serviceScope.launch {
            updateAppScreenTime(today, "Instagram", igTime)
            updateAppScreenTime(today, "YouTube", ytTime)
        }
    }

    private suspend fun updateAppScreenTime(date: String, appType: String, totalMillis: Long) {
        val dao = db.scrollDao()
        val record = dao.getRecord(date, appType)
        if (record == null) {
            dao.insert(ScrollRecord(date = date, appType = appType, scrollCount = 0, screenTimeMillis = totalMillis))
        } else {
            dao.insert(record.copy(screenTimeMillis = totalMillis))
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString()?.lowercase() ?: return
        
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (packageName.contains("launcher") || packageName.contains("systemui")) {
                serviceScope.launch(Dispatchers.Main) {
                    removeOverlay()
                }
            }
            serviceScope.launch { syncScreenTimeWithSystem() }
        }

        val screenHeight = getScreenHeight()

        when {
            packageName.contains("instagram") -> {
                instagramTracker.processEvent(event, screenHeight) {
                    recordScrollBuffered("Instagram")
                }
            }
            packageName.contains("youtube") -> {
                youtubeTracker.processEvent(event, screenHeight) {
                    recordScrollBuffered("YouTube")
                }
            }
        }
    }

    private fun getScreenHeight(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.height()
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(dm)
            dm.heightPixels
        }
    }

    private fun recordScrollBuffered(appType: String) {
        if (!::db.isInitialized) return
        serviceScope.launch {
            val dao = db.scrollDao()
            val trackKey = if (appType == "Instagram") "track_ig" else "track_yt"
            val isTrackingEnabled = dao.getSetting(trackKey)?.toBoolean() ?: true
            
            if (!isTrackingEnabled) return@launch

            // Insert individual scroll event for detailed activity analysis
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            dao.insertEvent(ScrollEvent(timestamp = System.currentTimeMillis(), appType = appType, date = today))

            synchronized(pendingCounts) {
                val current = pendingCounts[appType] ?: 0
                pendingCounts[appType] = current + 1
                
                if (pendingCounts[appType]!! >= BATCH_SIZE) {
                    flushBuffer()
                } else {
                    if (flushJob == null || flushJob?.isCompleted == true) {
                        flushJob = serviceScope.launch {
                            delay(3000) 
                            flushBuffer()
                        }
                    }
                }
            }
        }
    }

    private fun flushBuffer() {
        val toFlush = synchronized(pendingCounts) {
            if (pendingCounts.isEmpty()) return
            val copy = pendingCounts.toMap()
            pendingCounts.clear()
            copy
        }

        serviceScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val dao = db.scrollDao()

            toFlush.forEach { (appType, count) ->
                val record = dao.getRecord(today, appType)
                val newCount = (record?.scrollCount ?: 0) + count
                
                if (record == null) {
                    dao.insert(ScrollRecord(date = today, appType = appType, scrollCount = count))
                } else {
                    dao.insert(record.copy(scrollCount = newCount))
                }

                val limitKey = if (appType == "Instagram") "limit_ig" else "limit_yt"
                val limit = dao.getSetting(limitKey)?.toIntOrNull() ?: 100
                
                if (newCount >= limit) {
                    handleOverLimitAlerts(appType, newCount, today)
                }
            }
        }
    }

    private fun handleOverLimitAlerts(appType: String, currentTotalCount: Int, today: String) {
        if (lastAlertDate != today) {
            lastAlertedCount.clear()
            lastAlertDate = today
        }

        serviceScope.launch {
            val dao = db.scrollDao()
            val isAlertEnabled = dao.getSetting("alert_screen_enabled")?.toBoolean() ?: true
            if (!isAlertEnabled) return@launch

            val gapKey = if (appType == "Instagram") "alert_gap_ig" else "alert_gap_yt"
            val n = dao.getSetting(gapKey)?.toIntOrNull() ?: 10
            
            val lastAlerted = lastAlertedCount[appType] ?: 0
            
            if (lastAlerted == 0 || currentTotalCount < lastAlerted || (currentTotalCount - lastAlerted) >= n) {
                lastAlertedCount[appType] = currentTotalCount
                serviceScope.launch(Dispatchers.Main) {
                    showLimitReachedOverlay(appType)
                }
            }
        }
    }

    private fun showLimitReachedOverlay(appType: String) {
        if (overlayView != null) return
        Log.d(TAG, "showLimitReachedOverlay for $appType")
        
        val lifecycleOwner = OverlayLifecycleOwner()
        lifecycleOwner.performRestore(null)
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply { 
            gravity = Gravity.CENTER
            flags = flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        }
        
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                TodoHabitPanel(db = db, appName = appType, onDismiss = { removeOverlay() })
            }
        }
        
        try {
            windowManager.addView(composeView, params)
            overlayView = composeView
            currentLifecycleOwner = lifecycleOwner
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding overlay: ${e.message}")
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            currentLifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            currentLifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            currentLifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {}
            overlayView = null
            currentLifecycleOwner = null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("Wellness", "Alerts", NotificationManager.IMPORTANCE_DEFAULT)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onInterrupt() {
        flushBuffer()
    }
    
    override fun onDestroy() {
        syncJob?.cancel()
        flushBuffer()
        removeOverlay()
        super.onDestroy()
    }

    private class OverlayLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateRegistryController = SavedStateRegistryController.create(this)
        private val store = ViewModelStore()

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
        override val viewModelStore: ViewModelStore get() = store

        fun performRestore(savedState: Bundle?) {
            savedStateRegistryController.performRestore(savedState)
        }

        fun handleLifecycleEvent(event: Lifecycle.Event) {
            lifecycleRegistry.handleLifecycleEvent(event)
        }
    }
}
