package com.qx.orbit.core.download

/**
 * Lifecycle states of a download task.
 *
 * Mapping to the legacy Android [android.app.DownloadManager] statuses (kept for reference
 * during migration): PENDING ≈ QUEUED, RUNNING, PAUSED, SUCCESSFUL ≈ SUCCEEDED, FAILED.
 * IDLE is the pre-enqueue / post-cancel placeholder.
 */
enum class DownloadState {
    /** Created but not yet enqueued / terminal state after cancel. */
    IDLE,

    /** Enqueued; URL refresh or scheduling in progress, transfer not started. */
    QUEUED,

    /** Transfer active (bytes flowing). */
    RUNNING,

    /** User-requested pause; can be resumed. */
    PAUSED,

    /** Terminal failure; may be retried/resumed manually if the cause is recoverable. */
    FAILED,

    /** Terminal success; file is complete. */
    SUCCEEDED
}

/** Why a task failed. Roughly mirrors the Android manager's `reason` codes. */
enum class DownloadFailureReason {
    /** Network/transport failure (legacy reason -1). */
    NETWORK,

    /** Local I/O error while writing (legacy reason -2). */
    IO,

    /** Interrupted by process death / app restart (legacy reason -3). */
    INTERRUPTED,

    /** HTTP status other than 2xx/206 (legacy reason = response code). */
    HTTP,

    /** Playback URL expired (403); refresh failed (legacy reason 403). */
    URL_EXPIRED
}

/**
 * Input events driving the download state machine.
 */
sealed interface DownloadEvent {
    /** A new task is added to the queue. */
    data object Enqueue : DownloadEvent

    /** Transfer starts (or restarts after a queued retry). */
    data object Start : DownloadEvent

    /** Progress tick. */
    data class Progress(val downloadedBytes: Long, val totalBytes: Long) : DownloadEvent

    /** User pauses the task. */
    data object Pause : DownloadEvent

    /** User resumes a paused or failed task. */
    data object Resume : DownloadEvent

    /** Task cancelled/removed. */
    data object Cancel : DownloadEvent

    /** Transfer or pre-flight failed. */
    data class Fail(val reason: DownloadFailureReason, val httpCode: Int? = null) : DownloadEvent

    /** Transfer completed successfully. */
    data object Succeed : DownloadEvent

    /** Playback URL is stale (403 / empty); refresh the URL. */
    data object RefreshUrl : DownloadEvent

    /** App restarted; recover persisted tasks. */
    data object Restore : DownloadEvent
}

/** Tuning knobs for the state machine. */
data class DownloadConfig(
    val maxRetries: Int = 3,
    val maxUrlRefreshes: Int = 2
)

/**
 * Pure transition rules for [DownloadState].
 *
 * Returns a copy of [task] with the next state. Unknown/illegal event–state pairs are
 * ignored (task returned unchanged). No side effects — the adapter decides what to do
 * with the result.
 */
object DownloadStateMachine {

    fun transition(task: DownloadTask, event: DownloadEvent, config: DownloadConfig = DownloadConfig()): DownloadTask {
        return when (task.state) {
            DownloadState.IDLE -> idle(task, event)
            DownloadState.QUEUED -> queued(task, event, config)
            DownloadState.RUNNING -> running(task, event, config)
            DownloadState.PAUSED -> paused(task, event)
            DownloadState.FAILED -> failed(task, event, config)
            DownloadState.SUCCEEDED -> succeeded(task, event)
        }
    }

    /**
     * Recover a task after process death / app restart (Restore event).
     *
     * Mirrors the legacy `loadTasks` behavior: tasks that were RUNNING or QUEUED when the
     * process died become FAILED(INTERRUPTED); PAUSED/FAILED/SUCCEEDED survive as-is.
     */
    fun restore(task: DownloadTask): DownloadTask {
        return when (task.state) {
            DownloadState.RUNNING, DownloadState.QUEUED ->
                task.copy(state = DownloadState.FAILED, failureReason = DownloadFailureReason.INTERRUPTED)
            else -> task
        }
    }

    /** Whether the failure can be retried given the config. */
    fun isRetryable(reason: DownloadFailureReason): Boolean = reason != DownloadFailureReason.URL_EXPIRED

    private fun idle(task: DownloadTask, event: DownloadEvent): DownloadTask = when (event) {
        DownloadEvent.Enqueue -> task.copy(state = DownloadState.QUEUED, retryCount = 0)
        else -> task
    }

    private fun queued(task: DownloadTask, event: DownloadEvent, config: DownloadConfig): DownloadTask = when (event) {
        DownloadEvent.Start -> task.copy(state = DownloadState.RUNNING)
        DownloadEvent.Pause -> task.copy(state = DownloadState.PAUSED)
        DownloadEvent.Cancel -> task.copy(state = DownloadState.IDLE)
        is DownloadEvent.Fail -> fail(task, event, config)
        DownloadEvent.RefreshUrl -> refreshUrl(task, config)
        else -> task
    }

    private fun running(task: DownloadTask, event: DownloadEvent, config: DownloadConfig): DownloadTask = when (event) {
        is DownloadEvent.Progress -> progress(task, event)
        DownloadEvent.Pause -> task.copy(state = DownloadState.PAUSED)
        DownloadEvent.Cancel -> task.copy(state = DownloadState.IDLE)
        is DownloadEvent.Fail -> fail(task, event, config)
        DownloadEvent.Succeed -> task.copy(state = DownloadState.SUCCEEDED, retryCount = 0)
        DownloadEvent.RefreshUrl -> refreshUrl(task, config)
        else -> task
    }

    private fun paused(task: DownloadTask, event: DownloadEvent): DownloadTask = when (event) {
        DownloadEvent.Resume -> task.copy(state = DownloadState.RUNNING)
        // Re-enqueuing an existing paused task restarts it (legacy enqueue() behavior).
        DownloadEvent.Enqueue -> task.copy(state = DownloadState.RUNNING)
        DownloadEvent.Cancel -> task.copy(state = DownloadState.IDLE)
        else -> task
    }

    private fun failed(task: DownloadTask, event: DownloadEvent, config: DownloadConfig): DownloadTask = when (event) {
        // Manual resume from FAILED: allowed for recoverable causes (network/IO); always
        // allowed after the retry budget is exhausted by user intent.
        DownloadEvent.Resume -> task.copy(
            state = DownloadState.RUNNING,
            retryCount = if (task.retryCount >= config.maxRetries) 0 else task.retryCount
        )
        DownloadEvent.Enqueue -> task.copy(state = DownloadState.QUEUED, retryCount = 0)
        DownloadEvent.Cancel -> task.copy(state = DownloadState.IDLE)
        else -> task
    }

    private fun succeeded(task: DownloadTask, event: DownloadEvent): DownloadTask = when (event) {
        DownloadEvent.Cancel -> task.copy(state = DownloadState.IDLE)
        else -> task
    }

    private fun progress(task: DownloadTask, event: DownloadEvent.Progress): DownloadTask {
        val downloaded = event.downloadedBytes.coerceAtLeast(0L)
        val total = event.totalBytes.coerceAtLeast(0L)
        val completed = total > 0L && downloaded >= total
        return task.copy(
            downloadedBytes = downloaded,
            totalBytes = total,
            state = if (completed) DownloadState.SUCCEEDED else DownloadState.RUNNING
        )
    }

    private fun fail(task: DownloadTask, event: DownloadEvent.Fail, config: DownloadConfig): DownloadTask {
        val reason = event.reason
        val retryable = isRetryable(reason) && task.retryCount < config.maxRetries
        return if (retryable) {
            task.copy(
                state = DownloadState.QUEUED,
                retryCount = task.retryCount + 1,
                failureReason = reason
            )
        } else {
            task.copy(state = DownloadState.FAILED, failureReason = reason)
        }
    }

    private fun refreshUrl(task: DownloadTask, config: DownloadConfig): DownloadTask {
        return if (task.urlRefreshCount < config.maxUrlRefreshes) {
            task.copy(
                state = DownloadState.QUEUED,
                urlRefreshCount = task.urlRefreshCount + 1
            )
        } else {
            task.copy(
                state = DownloadState.FAILED,
                failureReason = DownloadFailureReason.URL_EXPIRED
            )
        }
    }
}
