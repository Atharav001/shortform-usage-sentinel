package com.example.scrollersdashboard

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.TextView
import androidx.room.Room
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

    private val pendingCounts = mutableMapOf<String, Int>()
    private var flushJob: Job? = null
    private val BATCH_SIZE = 3 

    // Tracker for alert intervals
    private val lastAlertedCount = mutableMapOf<String, Int>()
    private var lastAlertDate: String? = null

    private val instagramTracker = InstagramTracker()
    private val youtubeTracker = YouTubeTracker()

    private var syncJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "scroller-db"
        ).fallbackToDestructiveMigration().build()
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
                delay(30000) // Sync every 30 seconds
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
            // Trigger an immediate sync when app state changes
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
        serviceScope.launch {
            val dao = db.scrollDao()
            val trackKey = if (appType == "Instagram") "track_ig" else "track_yt"
            val isTrackingEnabled = dao.getSetting(trackKey)?.toBoolean() ?: true
            
            if (!isTrackingEnabled) return@launch

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
            val currentTime = System.currentTimeMillis()
            val dao = db.scrollDao()

            toFlush.forEach { (appType, count) ->
                val focusUntil = dao.getSetting("focus_until")?.toLongOrNull() ?: 0L
                if (currentTime < focusUntil) return@forEach

                repeat(count) { i ->
                    dao.insertEvent(ScrollEvent(timestamp = currentTime - (i * 10), appType = appType, date = today))
                }
                
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
            val gapKey = if (appType == "Instagram") "alert_gap_ig" else "alert_gap_yt"
            val n = dao.getSetting(gapKey)?.toIntOrNull() ?: 10
            
            val lastAlerted = lastAlertedCount[appType] ?: 0
            
            // Show alert if:
            // 1. Never alerted before today (lastAlerted == 0)
            // 2. User reset the count (currentTotalCount < lastAlerted)
            // 3. n reels/shorts have passed since last alert
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
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }
        
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.overlay_limit_reached, null)
        
        overlayView?.findViewById<TextView>(R.id.overlay_text)?.text = "Daily Limit Reached for $appType"
        overlayView?.findViewById<Button>(R.id.close_overlay_button)?.setOnClickListener {
            removeOverlay()
        }
        windowManager.addView(overlayView, params)
    }

    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {}
            overlayView = null
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
        super.onDestroy()
    }
}
