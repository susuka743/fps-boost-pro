package com.fpsboostpro.app

import android.app.Application
import com.topjohnwu.superuser.Shell

/**
 * App-wide init. Deliberately does NOT request root here or anywhere at
 * startup - RootShell only touches `su` when the user opens a Root Pro
 * screen or triggers a root-gated feature. See core/root/RootShell.kt.
 */
class FpsBoostApp : Application() {

    companion object {
        init {
            // Pre-configure libsu builder before any Shell.getShell() call.
            // FLAG_NON_ROOT_SHELL is NOT set - we want a real root check when
            // (and only when) something actually asks for a shell.
            Shell.enableVerboseLogging = false
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_REDIRECT_STDERR)
                    .setTimeout(10)
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
    }
}
