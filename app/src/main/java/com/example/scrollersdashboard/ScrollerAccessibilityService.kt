package com.example.scrollersdashboard

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.lifecycle.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

class ScrollerAccessibilityService : AccessibilityService() {

    private lateinit var db: AppDatabase
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var overlayScope: CoroutineScope? = null
    private val TAG = "ScrollerService"
    
    private var overlayView: View? = null
    private lateinit var windowManager: WindowManager

    private val pendingCounts = mutableMapOf<String, Int>()
    private var flushJob: Job? = null
    private val BATCH_SIZE = 2 

    private val lastAlertedCount = mutableMapOf<String, Int>()
    private var lastAlertDate: String? = null
    private var lastForegroundApp: String? = null
    private var lastIgYtPackage: String? = null

    private val instagramTracker = InstagramTracker()
    private val youtubeTracker = YouTubeTracker()

    private var syncJob: Job? = null
    private var isOverlayVisible = false
    private var inReelsOrShorts = false
    private var overlayContextJob: Job? = null
    private var lastOverlayAppType: String? = null
    private var mediaPausedByOverlay = false

    private val mediaControlReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PAUSE_MEDIA -> mediaPausedByOverlay = tryMediaControl(pause = true)
                ACTION_RESUME_MEDIA -> {
                    if (mediaPausedByOverlay) {
                        tryMediaControl(pause = false)
                        mediaPausedByOverlay = false
                    }
                }
            }
        }
    }

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
        observeOverlaySetting()

        LocalBroadcastManager.getInstance(this).registerReceiver(
            mediaControlReceiver,
            IntentFilter().apply {
                addAction(ACTION_PAUSE_MEDIA)
                addAction(ACTION_RESUME_MEDIA)
            }
        )
    }

    private fun tryMediaControl(pause: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        return try {
            val keywords = if (pause) {
                listOf("pause", "paused", "tap to pause", "pause video")
            } else {
                listOf("play", "resume", "tap to play", "play video")
            }
            val target = findMediaControlNode(root, keywords)
            val clicked = target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
            target?.recycle()
            clicked
        } finally {
            root.recycle()
        }
    }

    private fun findMediaControlNode(
        node: AccessibilityNodeInfo,
        keywords: List<String>
    ): AccessibilityNodeInfo? {
        val desc = node.contentDescription?.toString()?.lowercase(Locale.getDefault()) ?: ""
        val text = node.text?.toString()?.lowercase(Locale.getDefault()) ?: ""
        val id = node.viewIdResourceName?.lowercase(Locale.getDefault()) ?: ""
        if (node.isClickable && keywords.any { k -> desc.contains(k) || text.contains(k) || id.contains(k) }) {
            return AccessibilityNodeInfo.obtain(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findMediaControlNode(child, keywords)
            child.recycle()
            if (found != null) return found
        }
        return null
    }

    private fun observeOverlaySetting() {
        serviceScope.launch {
            db.scrollDao().getSettingFlow("overlay_counter_enabled").collectLatest { enabled ->
                val isEnabled = enabled?.toBoolean() ?: false
                if (!isEnabled) {
                    stopCounterOverlay()
                }
            }
        }
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
        try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            val endOfDay = System.currentTimeMillis()

            val stats = usageStatsManager.queryAndAggregateUsageStats(startOfDay, endOfDay)
            
            var igTime = 0L
            var ytTime = 0L

            stats.forEach { (pkg, usage) ->
                val p = pkg.lowercase()
                if (p.contains("instagram")) {
                    igTime += usage.totalTimeInForeground
                } else if (p.contains("youtube") && p != packageName.lowercase()) {
                    ytTime += usage.totalTimeInForeground
                }
            }

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            
            serviceScope.launch {
                updateAppScreenTime(today, "Instagram", igTime)
                updateAppScreenTime(today, "YouTube", ytTime)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing screen time: ${e.message}")
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
        
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            
            val appType = when {
                packageName.contains("instagram") -> "Instagram"
                packageName.contains("youtube") && packageName != this.packageName.lowercase() -> "YouTube"
                else -> null
            }

            if (appType != null) {
                lastIgYtPackage = packageName
                lastForegroundApp = packageName
                checkContextualOverlay(appType)
            } else if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                val leftIgYt = lastIgYtPackage != null &&
                    !packageName.contains("instagram") &&
                    !(packageName.contains("youtube") && packageName != this.packageName.lowercase())
                if (leftIgYt && (isOverlayVisible || inReelsOrShorts)) {
                    inReelsOrShorts = false
                    lastOverlayAppType = null
                    lastIgYtPackage = null
                    stopCounterOverlay(immediate = true)
                }
                lastForegroundApp = packageName
            }
        }

        val screenHeight = getScreenHeight()

        when {
            packageName.contains("instagram") -> {
                instagramTracker.processEvent(event, screenHeight) {
                    recordScrollBuffered("Instagram")
                }
            }
            packageName.contains("youtube") && packageName != this.packageName.lowercase() -> {
                youtubeTracker.processEvent(event, screenHeight) {
                    recordScrollBuffered("YouTube")
                }
            }
        }
    }

    private fun checkContextualOverlay(appType: String) {
        overlayContextJob?.cancel()
        overlayContextJob = serviceScope.launch {
            delay(120)
            val root = rootInActiveWindow
            if (root == null) return@launch
            val screenHeight = getScreenHeight()
            val isInShortsView = try {
                isSpecificallyInShortsOrReels(root, appType, screenHeight)
            } finally {
                root.recycle()
            }

            withContext(Dispatchers.Main) {
                if (isInShortsView) {
                    lastOverlayAppType = appType
                    if (!inReelsOrShorts) {
                        inReelsOrShorts = true
                        triggerOverlayForApp(appType)
                    } else {
                        refreshOverlayForApp(appType)
                    }
                } else if (inReelsOrShorts && lastOverlayAppType == appType) {
                    inReelsOrShorts = false
                    lastOverlayAppType = null
                    stopCounterOverlay()
                }
            }
        }
    }

    private fun isSpecificallyInShortsOrReels(node: AccessibilityNodeInfo?, appType: String, screenHeight: Int): Boolean {
        if (node == null) return false

        val viewId = node.viewIdResourceName?.lowercase(Locale.getDefault())
        val className = node.className?.toString()?.lowercase(Locale.getDefault()) ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase(Locale.getDefault()) ?: ""
        val text = node.text?.toString()?.lowercase(Locale.getDefault()) ?: ""

        if (appType == "Instagram") {
            if (viewId != null && (
                    viewId.contains("reel") ||
                        viewId.contains("clips") ||
                        viewId.contains("clipping_frame")
                    )
            ) {
                return true
            }
            if (contentDesc.contains("reel") || text.contains("reel")) return true
        } else if (appType == "YouTube") {
            if (viewId != null && (
                    viewId.contains("reel") ||
                        viewId.contains("short") ||
                        viewId.contains("shorts") ||
                        viewId.contains("player")
                    )
            ) {
                return true
            }
            if (contentDesc.contains("short") || text.contains("short")) return true
        }

        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (node.isScrollable && rect.height() > screenHeight * 0.70f && rect.width() > screenHeight * 0.5f) {
            if (rect.top <= screenHeight * 0.12f) return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = isSpecificallyInShortsOrReels(child, appType, screenHeight)
            child.recycle()
            if (found) return true
        }

        return false
    }

    private fun triggerOverlayForApp(appType: String) {
        serviceScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val dao = db.scrollDao()
            val isEnabled = dao.getSetting("overlay_counter_enabled")?.toBoolean() ?: false
            if (!isEnabled) return@launch

            val record = dao.getRecord(today, appType)
            val count = record?.scrollCount ?: 0
            val limitKey = if (appType == "Instagram") "limit_ig" else "limit_yt"
            val limit = dao.getSetting(limitKey)?.toIntOrNull() ?: 100

            withContext(Dispatchers.Main) {
                startCounterOverlay(count, limit, appType)
            }
        }
    }

    private fun refreshOverlayForApp(appType: String) {
        if (!isOverlayVisible) {
            triggerOverlayForApp(appType)
            return
        }
        serviceScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val dao = db.scrollDao()
            val record = dao.getRecord(today, appType)
            val count = record?.scrollCount ?: 0
            val limitKey = if (appType == "Instagram") "limit_ig" else "limit_yt"
            val limit = dao.getSetting(limitKey)?.toIntOrNull() ?: 100
            broadcastCount(count, limit, appType)
        }
    }

    private fun startCounterOverlay(initialCount: Int, limit: Int, appName: String) {
        val intent = Intent(this, CounterOverlayService::class.java).apply {
            action = CounterOverlayService.ACTION_SHOW_OVERLAY
            putExtra(CounterOverlayService.EXTRA_COUNT, initialCount)
            putExtra(CounterOverlayService.EXTRA_LIMIT, limit)
            putExtra(CounterOverlayService.EXTRA_APP_NAME, appName)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        isOverlayVisible = true
    }

    private fun stopCounterOverlay(immediate: Boolean = false) {
        if (!isOverlayVisible) return
        isOverlayVisible = false
        val intent = Intent(this, CounterOverlayService::class.java).apply {
            action = CounterOverlayService.ACTION_HIDE_OVERLAY
            putExtra(CounterOverlayService.EXTRA_HIDE_IMMEDIATE, immediate)
        }
        try {
            startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "stopCounterOverlay: ${e.message}")
        }
    }

    private fun broadcastCount(count: Int, limit: Int, appName: String) {
        val intent = Intent(CounterOverlayService.ACTION_UPDATE_COUNT).apply {
            putExtra(CounterOverlayService.EXTRA_COUNT, count)
            putExtra(CounterOverlayService.EXTRA_LIMIT, limit)
            putExtra(CounterOverlayService.EXTRA_APP_NAME, appName)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (overlayView != null && event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_UP) {
                removeOverlay()
            }
            return true 
        }
        return super.onKeyEvent(event)
    }

    private fun getScreenHeight(): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                windowManager.currentWindowMetrics.bounds.height()
            } else {
                val dm = DisplayMetrics()
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getRealMetrics(dm)
                dm.heightPixels
            }
        } catch (e: Exception) {
            1920 
        }
    }

    private fun recordScrollBuffered(appType: String) {
        if (!::db.isInitialized) return
        serviceScope.launch {
            val dao = db.scrollDao()
            val trackKey = if (appType == "Instagram") "track_ig" else "track_yt"
            val isTrackingEnabled = dao.getSetting(trackKey)?.toBoolean() ?: true
            
            if (!isTrackingEnabled) return@launch

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            dao.insertEvent(ScrollEvent(timestamp = System.currentTimeMillis(), appType = appType, date = today))

            synchronized(pendingCounts) {
                val current = pendingCounts[appType] ?: 0
                val updatedPending = current + 1
                pendingCounts[appType] = updatedPending

                if (isOverlayVisible) {
                    broadcastOptimisticCount(appType)
                    flushBuffer()
                } else if (updatedPending >= BATCH_SIZE) {
                    flushBuffer()
                } else {
                    if (flushJob == null || flushJob?.isCompleted == true) {
                        flushJob = serviceScope.launch {
                            delay(2000)
                            flushBuffer()
                        }
                    }
                }
            }
        }
    }

    private fun broadcastOptimisticCount(appType: String) {
        serviceScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val dao = db.scrollDao()
            val record = dao.getRecord(today, appType)
            val pending = synchronized(pendingCounts) { pendingCounts[appType] ?: 0 }
            val count = (record?.scrollCount ?: 0) + pending
            val limitKey = if (appType == "Instagram") "limit_ig" else "limit_yt"
            val limit = dao.getSetting(limitKey)?.toIntOrNull() ?: 100
            broadcastCount(count, limit, appType)
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
                val limitValue = dao.getSetting(limitKey)
                val limit = limitValue?.toIntOrNull() ?: 100

                if (isOverlayVisible) {
                    broadcastCount(newCount, limit, appType)
                }
                handleAlerts(appType, newCount, today, limit)
            }
        }
    }

    private fun handleAlerts(appType: String, currentTotalCount: Int, today: String, limit: Int) {
        serviceScope.launch {
            val dao = db.scrollDao()
            val isAlertEnabled = dao.getSetting("alert_screen_enabled")?.toBoolean() ?: true
            if (!isAlertEnabled) return@launch

            val alertOnlyAfterLimit = dao.getSetting("alert_only_after_limit")?.toBoolean() ?: true
            val gapKey = if (appType == "Instagram") "alert_gap_ig" else "alert_gap_yt"
            val gapValue = dao.getSetting(gapKey)
            val gap = gapValue?.toIntOrNull() ?: 10
            
            val shouldShow = synchronized(lastAlertedCount) {
                if (lastAlertDate != today) {
                    lastAlertedCount.clear()
                    lastAlertDate = today
                }
                val lastAlerted = lastAlertedCount[appType] ?: 0
                
                if (alertOnlyAfterLimit) {
                    if (currentTotalCount >= limit) {
                        if (lastAlerted < limit || (currentTotalCount - lastAlerted) >= gap) {
                            lastAlertedCount[appType] = currentTotalCount
                            true
                        } else false
                    } else false
                } else {
                    if (currentTotalCount > 0 && (currentTotalCount - lastAlerted) >= gap) {
                        lastAlertedCount[appType] = currentTotalCount
                        true
                    } else {
                        false
                    }
                }
            }
            
            if (shouldShow) {
                withContext(Dispatchers.Main) {
                    showLimitReachedOverlay(appType)
                }
            }
        }
    }

    private fun showLimitReachedOverlay(appType: String) {
        if (overlayView != null) return
        
        try {
            val contextWrapper = ContextThemeWrapper(this, R.style.Theme_ScrollersDashboard)
            
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or 
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply { 
                gravity = Gravity.CENTER
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
                screenOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            
            val inflater = LayoutInflater.from(contextWrapper)
            val view = inflater.inflate(R.layout.layout_alert_screen, null)
            
            overlayScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
            setupAlertView(view, appType, contextWrapper)
            
            windowManager.addView(view, params)
            overlayView = view
        } catch (e: Exception) {
            Log.e(TAG, "Error adding overlay: ${e.message}")
            overlayScope?.cancel()
            overlayScope = null
        }
    }

    private fun setupAlertView(view: View, appType: String, themedContext: Context) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val dao = db.scrollDao()

        val headerContainer = view.findViewById<View>(R.id.headerContainer)
        val tvStats = view.findViewById<TextView>(R.id.tvStats)
        val tvLimit = view.findViewById<TextView>(R.id.tvLimit)
        
        // Sections & Tabs
        val habitsSection = view.findViewById<View>(R.id.habitsSection)
        val tasksSection = view.findViewById<View>(R.id.tasksSection)
        val tabHabits = view.findViewById<TextView>(R.id.tabHabits)
        val tabTasks = view.findViewById<TextView>(R.id.tabTasks)

        val rvHabits = view.findViewById<RecyclerView>(R.id.rvHabits)
        val rvTasks = view.findViewById<RecyclerView>(R.id.rvTasks)
        val pbHabits = view.findViewById<ProgressBar>(R.id.pbHabits)
        val pbTasks = view.findViewById<ProgressBar>(R.id.pbTasks)
        val tvHabitsCount = view.findViewById<TextView>(R.id.tvHabitsCount)
        val tvTasksCount = view.findViewById<TextView>(R.id.tvTasksCount)

        val etHabit = view.findViewById<EditText>(R.id.etHabit)
        val btnAddHabit = view.findViewById<ImageButton>(R.id.btnAddHabit)
        val etTask = view.findViewById<EditText>(R.id.etTask)
        val btnAddTask = view.findViewById<ImageButton>(R.id.btnAddTask)

        val btnQuit = view.findViewById<MaterialButton>(R.id.btnQuit)
        val btnContinue = view.findViewById<MaterialButton>(R.id.btnContinue)

        if (appType == "Instagram") {
            headerContainer.setBackgroundResource(R.drawable.bg_alert_header_instagram)
        } else {
            headerContainer.setBackgroundResource(R.drawable.bg_alert_header_youtube)
        }

        tabHabits.setOnClickListener {
            habitsSection.visibility = View.VISIBLE
            tasksSection.visibility = View.GONE
            tabHabits.setBackgroundResource(R.drawable.bg_tab_selected)
            tabHabits.setTextColor(themedContext.getColor(R.color.white))
            tabTasks.background = null
            tabTasks.setTextColor(themedContext.getColor(R.color.gray_500))
        }

        tabTasks.setOnClickListener {
            habitsSection.visibility = View.GONE
            tasksSection.visibility = View.VISIBLE
            tabTasks.setBackgroundResource(R.drawable.bg_tab_selected)
            tabTasks.setTextColor(themedContext.getColor(R.color.white))
            tabHabits.background = null
            tabHabits.setTextColor(themedContext.getColor(R.color.gray_500))
        }

        val habitsAdapter = TaskAdapter(emptyList(), 
            onToggle = { task -> 
                val habit = task as HabitTask
                val isDone = habit.lastCompletedDate == today
                serviceScope.launch { dao.insertHabit(habit.copy(lastCompletedDate = if (isDone) "" else today)) }
            },
            onDelete = { task -> serviceScope.launch { dao.deleteHabit((task as HabitTask).id) } }
        )
        rvHabits.layoutManager = LinearLayoutManager(themedContext)
        rvHabits.adapter = habitsAdapter

        val tasksAdapter = TaskAdapter(emptyList(),
            onToggle = { task ->
                val todo = task as TodoTask
                serviceScope.launch { dao.insertTodo(todo.copy(isCompleted = !todo.isCompleted)) }
            },
            onDelete = { task -> serviceScope.launch { dao.deleteTodo((task as TodoTask).id) } }
        )
        rvTasks.layoutManager = LinearLayoutManager(themedContext)
        rvTasks.adapter = tasksAdapter

        overlayScope?.launch {
            val record = withContext(Dispatchers.IO) { dao.getRecord(today, appType) }
            val limitKey = if (appType == "Instagram") "limit_ig" else "limit_yt"
            val limitValue = withContext(Dispatchers.IO) { dao.getSetting(limitKey) }
            val limit = limitValue?.toIntOrNull() ?: 100
            
            tvStats.text = "${record?.scrollCount ?: 0} ${if (appType == "Instagram") "reels" else "shorts"} scrolled"
            tvLimit.text = limit.toString()

            launch {
                dao.getHabitTasks().collect { habits ->
                    val completed = habits.count { it.lastCompletedDate == today }
                    tvHabitsCount.text = "$completed/${habits.size} completed"
                    pbHabits.progress = if (habits.isNotEmpty()) (completed * 100 / habits.size) else 0
                    habitsAdapter.updateTasks(habits)
                }
            }
            
            launch {
                dao.getSettingFlow("refresh_daily_todo").collectLatest { refreshDailyStr ->
                    val isRefreshDaily = refreshDailyStr?.toBoolean() ?: true
                    val todoDate = if (isRefreshDaily) today else "permanent_todo"
                    
                    dao.getTodoTasks(todoDate).collect { tasks ->
                        val completed = tasks.count { it.isCompleted }
                        tvTasksCount.text = "$completed/${tasks.size} completed"
                        pbTasks.progress = if (tasks.isNotEmpty()) (completed * 100 / tasks.size) else 0
                        tasksAdapter.updateTasks(tasks)
                    }
                }
            }
        }

        fun hideKeyboard(v: View) {
            val imm = themedContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(v.windowToken, 0)
        }

        btnAddHabit.setOnClickListener {
            val text = etHabit.text.toString()
            if (text.isNotBlank()) {
                serviceScope.launch {
                    dao.insertHabit(HabitTask(title = text))
                    withContext(Dispatchers.Main) { 
                        etHabit.text.clear()
                        hideKeyboard(etHabit)
                    }
                }
            }
        }

        etHabit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                btnAddHabit.performClick()
                true
            } else false
        }

        btnAddTask.setOnClickListener {
            val text = etTask.text.toString()
            if (text.isNotBlank()) {
                serviceScope.launch {
                    val isRefreshDaily = dao.getSetting("refresh_daily_todo")?.toBoolean() ?: true
                    val todoDate = if (isRefreshDaily) today else "permanent_todo"
                    dao.insertTodo(TodoTask(title = text, date = todoDate))
                    withContext(Dispatchers.Main) { 
                        etTask.text.clear()
                        hideKeyboard(etTask)
                    }
                }
            }
        }

        etTask.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                btnAddTask.performClick()
                true
            } else false
        }

        btnQuit.setOnClickListener {
            removeOverlay()
            performGlobalAction(GLOBAL_ACTION_HOME)
        }

        btnContinue.setOnClickListener {
            removeOverlay()
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay: ${e.message}")
            }
            overlayView = null
        }
        overlayScope?.cancel()
        overlayScope = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("Wellness", "Alerts", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onInterrupt() {
        flushBuffer()
    }
    
    override fun onDestroy() {
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mediaControlReceiver)
        } catch (_: Exception) {}
        syncJob?.cancel()
        serviceScope.cancel()
        flushBuffer()
        removeOverlay()
        stopCounterOverlay()
        super.onDestroy()
    }

    companion object {
        const val ACTION_PAUSE_MEDIA = "com.example.scrollersdashboard.PAUSE_MEDIA"
        const val ACTION_RESUME_MEDIA = "com.example.scrollersdashboard.RESUME_MEDIA"
    }
}
