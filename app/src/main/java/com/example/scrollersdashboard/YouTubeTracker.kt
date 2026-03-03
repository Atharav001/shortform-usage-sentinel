package com.example.scrollersdashboard

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class YouTubeTracker {
    private val TAG = "YouTubeTracker"
    private var lastScrollTime = 0L
    private val DEBOUNCE_THRESHOLD = 800L // More lenient debounce for YouTube

    fun processEvent(event: AccessibilityEvent, screenHeight: Int, onScrollDetected: () -> Unit) {
        // Primary focus on scroll gestures
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return

        val source = event.source ?: return
        val className = event.className?.toString() ?: ""
        
        val rect = Rect()
        source.getBoundsInScreen(rect)
        
        // More lenient height check for YouTube Shorts (50% instead of 70-80%)
        // This helps catch the player even if it's partially obscured or reported differently
        if (rect.height() < screenHeight * 0.50) {
            return
        }

        // YouTube uses various container names, including RecyclerView and ViewPager
        // We'll be more inclusive here
        val isPotentiallyShorts = className.contains("RecyclerView") || 
                                 className.contains("ViewPager") || 
                                 className.contains("Pager") ||
                                 className.contains("FrameLayout")

        if (isPotentiallyShorts) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastScrollTime > DEBOUNCE_THRESHOLD) {
                lastScrollTime = currentTime
                Log.d(TAG, "YouTube Shorts scroll gesture detected")
                onScrollDetected()
            }
        }
    }
}
