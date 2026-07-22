package com.fpsboostpro.app.core.optimize

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.getSystemService

/**
 * Gaming Mode = a real, verifiable bundle of system states, not a cosmetic
 * toggle:
 *  1) Do Not Disturb ON (genuinely suppresses notifications/calls) - this
 *     requires the user to have granted "Notification policy access",
 *     a real one-time permission we deep-link to, exactly like any
 *     legitimate DND-toggling app must.
 *  2) Screen orientation lock hint + keep-screen-on flag on the game
 *     launch activity (real, via WindowManager.LayoutParams in the
 *     Activity itself - see MainActivity boost trigger).
 *  3) Optional root-only steps (CPU governor bump) - only if root granted.
 */
class GamingModeController(private val context: Context) {

    fun hasNotificationPolicyAccess(): Boolean {
        val nm = context.getSystemService<NotificationManager>()!!
        return nm.isNotificationPolicyAccessGranted
    }

    fun requestNotificationPolicyAccess() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** Returns true if it genuinely changed system DND state. */
    fun setGamingModeEnabled(enabled: Boolean): Boolean {
        if (!hasNotificationPolicyAccess()) return false
        val nm = context.getSystemService<NotificationManager>()!!
        nm.setInterruptionFilter(
            if (enabled) NotificationManager.INTERRUPTION_FILTER_PRIORITY
            else NotificationManager.INTERRUPTION_FILTER_ALL
        )
        return true
    }

    fun isGamingModeActive(): Boolean {
        val nm = context.getSystemService<NotificationManager>()!!
        return nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY
    }
}
