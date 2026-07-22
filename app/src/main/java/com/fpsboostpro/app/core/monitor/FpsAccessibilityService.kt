package com.fpsboostpro.app.core.monitor

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Powers the app's honest "which app is currently in the foreground"
 * detection, used for the optional per-app FPS overlay and Background Apps
 * suggestions.
 *
 * IMPORTANT CONTRACT:
 *  - Never enabled programmatically. Android does not allow apps to silently
 *    enable an AccessibilityService — the user must explicitly grant it via
 *    system Settings, and the app must clearly explain why in-app first.
 *  - Does not read screen content beyond the foreground package name
 *    (TYPE_WINDOW_STATE_CHANGED), despite canRetrieveWindowContent being
 *    true in the config — that flag is required by the API surface but this
 *    implementation never calls performAction or reads node text.
 */
class FpsAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var currentForegroundPackage: String? = null
            private set
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.packageName?.toString()?.let { pkg ->
                currentForegroundPackage = pkg
            }
        }
    }

    override fun onInterrupt() {
        // No-op: nothing to clean up between events.
    }

    override fun onDestroy() {
        currentForegroundPackage = null
        super.onDestroy()
    }
}
