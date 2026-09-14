package com.galaxya12.cleaner.accessibility

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Optional accessibility support for Galaxy Cleaner.
 *
 * NOTE: As per Android security guidelines, this service does NOT perform
 * any automated UI clicks or settings manipulation. It is purely an optional
 * accessibility companion service that remains idle.
 */
class CleanerAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "CleanerAccessibilityService connected.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Strictly no hidden automation, no clicking Clear Cache or Clear Data
    }

    override fun onInterrupt() {
        Log.i(TAG, "CleanerAccessibilityService interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "CleanerAccessibilityService destroyed.")
    }

    companion object {
        private const val TAG = "CleanerA11yService"
    }
}
