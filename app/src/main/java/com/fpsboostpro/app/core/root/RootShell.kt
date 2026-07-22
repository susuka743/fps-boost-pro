package com.fpsboostpro.app.core.root

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Thin, honest wrapper around libsu.
 *
 * IMPORTANT CONTRACT:
 *  - This class NEVER triggers a root request on its own initiative.
 *  - [isRootAvailable] merely checks whether `su` exists and the device is
 *    already rootable/rooted. Calling it MAY show the user's existing
 *    superuser manager (Magisk/KernelSU) prompt ONLY the first time any
 *    command actually runs - same as any other rooted app - not silently,
 *    and not before the user has chosen to enter a Root Pro screen.
 *  - Every advanced tweak in the app must check [isRootAvailable] and
 *    render a locked/explainer state if it returns false. See
 *    RootProViewModel.
 */
object RootShell {

    private var initialized = false

    private fun ensureBuilderConfigured() {
        if (initialized) return
        Shell.enableVerboseLogging = false
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(10)
        )
        initialized = true
    }

    /** Non-blocking check. Returns false immediately if su binary is absent -
     *  does not prompt the user in that case. */
    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        ensureBuilderConfigured()
        try {
            Shell.isAppGrantedRoot() == true || Shell.getShell().isRoot
        } catch (e: Exception) {
            false
        }
    }

    data class ShellResult(
        val success: Boolean,
        val output: List<String>,
        val exitCode: Int
    )

    /** Executes a single root command. Caller MUST have already confirmed
     *  root availability and shown any required warning UI for destructive
     *  tweaks (see RootWarningDialog). */
    suspend fun exec(command: String): ShellResult = withContext(Dispatchers.IO) {
        ensureBuilderConfigured()
        try {
            val result = Shell.cmd(command).exec()
            ShellResult(
                success = result.isSuccess,
                output = result.out,
                exitCode = result.code
            )
        } catch (e: Exception) {
            ShellResult(success = false, output = listOf(e.message ?: "unknown error"), exitCode = -1)
        }
    }

    suspend fun execMultiple(commands: List<String>): ShellResult = withContext(Dispatchers.IO) {
        ensureBuilderConfigured()
        try {
            val result = Shell.cmd(*commands.toTypedArray()).exec()
            ShellResult(
                success = result.isSuccess,
                output = result.out,
                exitCode = result.code
            )
        } catch (e: Exception) {
            ShellResult(success = false, output = listOf(e.message ?: "unknown error"), exitCode = -1)
        }
    }

    /** Read-only convenience for values like current governor/frequency. */
    suspend fun readFile(path: String): String? = withContext(Dispatchers.IO) {
        val result = exec("cat $path 2>/dev/null")
        if (result.success && result.output.isNotEmpty()) result.output.first() else null
    }
}
