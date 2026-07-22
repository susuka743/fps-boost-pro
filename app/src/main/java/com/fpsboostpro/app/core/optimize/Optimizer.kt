package com.fpsboostpro.app.core.optimize

/**
 * Every optimization feature implements this. [requiresRoot] is declared
 * up-front so the UI can honestly show a lock icon before the user taps it,
 * rather than taking the tap and then failing.
 */
interface Optimizer {
    val id: String
    val displayName: String
    val requiresRoot: Boolean

    /** Executes the optimization and returns an honest, specific result.
     * Implementations must not report success/bytes-freed/etc. they did not
     * actually measure. */
    suspend fun execute(): OptimizationResult
}

data class OptimizationResult(
    val success: Boolean,
    val message: String,
    val bytesFreed: Long? = null,
    val itemsAffected: Int? = null
)
