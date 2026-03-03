package com.example.scrollersdashboard

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class InstagramTracker {
    private val TAG = "InstagramTracker"
    private var lastScrollTime = 0L
    private val DEBOUNCE_THRESHOLD = 900L

    fun processEvent(event: AccessibilityEvent, screenHeight: Int, onScrollDetected: () -> Unit) {
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return

        val source = event.source ?: return
        val className = event.className?.toString() ?: ""
        
        val rect = Rect()
        source.getBoundsInScreen(rect)
        
        // Instagram Reels are always full-screen containers.
        if (rect.height() < screenHeight * 0.70) {
            return
        }

        // Instagram paging feeds use RecyclerView or ViewPager.
        if (!className.contains("RecyclerView") && !className.contains("ViewPager")) {
            return
        }

        // Verify if any child is large enough to be a reel
        var hasFullScreenChild = false
        if (source.childCount > 0) {
            for (i in 0 until minOf(source.childCount, 2)) {
                val child = source.getChild(i) ?: continue
                val childRect = Rect()
                child.getBoundsInScreen(childRect)
                if (childRect.height() > screenHeight * 0.60) {
                    hasFullScreenChild = true
                    break
                }
            }
        }

        if (hasFullScreenChild) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastScrollTime > DEBOUNCE_THRESHOLD) {
                lastScrollTime = currentTime
                Log.d(TAG, "Instagram Reel scroll detected")
                onScrollDetected()
            }
        }
    }
}
