package com.qx.orbit.core.download

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Full transition table for [DownloadStateMachine].
 *
 * Every (state, event) pair is exercised and asserted against the documented rule set.
 */
class DownloadStateMachineTest {

    private fun task(state: DownloadState = DownloadState.IDLE) = DownloadTask(
        id = 1L,
        url = "https://example.com/video.mp4",
        range = ByteRange(0L),
        state = state
    )

    // --- IDLE ---

    @Test
    fun `IDLE Enqueue to QUEUED`() {
        val t = DownloadStateMachine.transition(task(DownloadState.IDLE), DownloadEvent.Enqueue)
        assertEquals(DownloadState.QUEUED, t.state)
        assertEquals(0, t.retryCount)
    }

    @Test
    fun `IDLE ignores non-enqueue events`() {
        val start = DownloadStateMachine.transition(task(DownloadState.IDLE), DownloadEvent.Start)
        assertEquals(DownloadState.IDLE, start.state)

        val pause = DownloadStateMachine.transition(task(DownloadState.IDLE), DownloadEvent.Pause)
        assertEquals(DownloadState.IDLE, pause.state)

        val fail = DownloadStateMachine.transition(task(DownloadState.IDLE), DownloadEvent.Fail(DownloadFailureReason.NETWORK))
        assertEquals(DownloadState.IDLE, fail.state)
    }

    // --- QUEUED ---

    @Test
    fun `QUEUED Start to RUNNING`() {
        val t = DownloadStateMachine.transition(task(DownloadState.QUEUED), DownloadEvent.Start)
        assertEquals(DownloadState.RUNNING, t.state)
    }

    @Test
    fun `QUEUED Pause to PAUSED`() {
        val t = DownloadStateMachine.transition(task(DownloadState.QUEUED), DownloadEvent.Pause)
        assertEquals(DownloadState.PAUSED, t.state)
    }

    @Test
    fun `QUEUED Cancel to IDLE`() {
        val t = DownloadStateMachine.transition(task(DownloadState.QUEUED), DownloadEvent.Cancel)
        assertEquals(DownloadState.IDLE, t.state)
    }

    @Test
    fun `QUEUED RefreshUrl to QUEUED with urlRefreshCount`() {
        val t = DownloadStateMachine.transition(task(DownloadState.QUEUED), DownloadEvent.RefreshUrl)
        assertEquals(DownloadState.QUEUED, t.state)
        assertEquals(1, t.urlRefreshCount)
    }

    @Test
    fun `QUEUED Fail retryable to QUEUED with retryCount`() {
        val t = DownloadStateMachine.transition(task(DownloadState.QUEUED), DownloadEvent.Fail(DownloadFailureReason.NETWORK))
        assertEquals(DownloadState.QUEUED, t.state)
        assertEquals(1, t.retryCount)
    }

    // --- RUNNING ---

    @Test
    fun `RUNNING Progress to RUNNING with bytes`() {
        val t = DownloadStateMachine.transition(
            task(DownloadState.RUNNING),
            DownloadEvent.Progress(downloadedBytes = 50, totalBytes = 100)
        )
        assertEquals(DownloadState.RUNNING, t.state)
        assertEquals(50L, t.downloadedBytes)
        assertEquals(100L, t.totalBytes)
    }

    @Test
    fun `RUNNING Progress complete to SUCCEEDED`() {
        val t = DownloadStateMachine.transition(
            task(DownloadState.RUNNING),
            DownloadEvent.Progress(downloadedBytes = 100, totalBytes = 100)
        )
        assertEquals(DownloadState.SUCCEEDED, t.state)
        assertEquals(1f, t.progressFraction)
    }

    @Test
    fun `RUNNING Pause to PAUSED`() {
        val t = DownloadStateMachine.transition(task(DownloadState.RUNNING), DownloadEvent.Pause)
        assertEquals(DownloadState.PAUSED, t.state)
    }

    @Test
    fun `RUNNING Succeed to SUCCEEDED and resets retryCount`() {
        val t = DownloadStateMachine.transition(
            task(DownloadState.RUNNING).copy(retryCount = 2),
            DownloadEvent.Succeed
        )
        assertEquals(DownloadState.SUCCEEDED, t.state)
        assertEquals(0, t.retryCount)
    }

    @Test
    fun `RUNNING Fail retryable below max to QUEUED with retryCount`() {
        val config = DownloadConfig(maxRetries = 3)
        val t = DownloadStateMachine.transition(
            task(DownloadState.RUNNING).copy(retryCount = 1),
            DownloadEvent.Fail(DownloadFailureReason.IO),
            config
        )
        assertEquals(DownloadState.QUEUED, t.state)
        assertEquals(2, t.retryCount)
        assertEquals(DownloadFailureReason.IO, t.failureReason)
    }

    @Test
    fun `RUNNING Fail exhausts retries to FAILED`() {
        val config = DownloadConfig(maxRetries = 2)
        val t = DownloadStateMachine.transition(
            task(DownloadState.RUNNING).copy(retryCount = 2),
            DownloadEvent.Fail(DownloadFailureReason.IO),
            config
        )
        assertEquals(DownloadState.FAILED, t.state)
        assertEquals(DownloadFailureReason.IO, t.failureReason)
    }

    @Test
    fun `RUNNING Fail URL_EXPIRED is non-retryable to FAILED`() {
        val t = DownloadStateMachine.transition(
            task(DownloadState.RUNNING),
            DownloadEvent.Fail(DownloadFailureReason.URL_EXPIRED)
        )
        assertEquals(DownloadState.FAILED, t.state)
        assertEquals(DownloadFailureReason.URL_EXPIRED, t.failureReason)
    }

    @Test
    fun `RUNNING RefreshUrl to QUEUED increments urlRefreshCount`() {
        val t = DownloadStateMachine.transition(
            task(DownloadState.RUNNING).copy(urlRefreshCount = 1),
            DownloadEvent.RefreshUrl
        )
        assertEquals(DownloadState.QUEUED, t.state)
        assertEquals(2, t.urlRefreshCount)
    }

    @Test
    fun `RUNNING RefreshUrl over limit to FAILED URL_EXPIRED`() {
        val config = DownloadConfig(maxUrlRefreshes = 2)
        val t = DownloadStateMachine.transition(
            task(DownloadState.RUNNING).copy(urlRefreshCount = 2),
            DownloadEvent.RefreshUrl,
            config
        )
        assertEquals(DownloadState.FAILED, t.state)
        assertEquals(DownloadFailureReason.URL_EXPIRED, t.failureReason)
    }

    @Test
    fun `RUNNING Cancel to IDLE`() {
        val t = DownloadStateMachine.transition(task(DownloadState.RUNNING), DownloadEvent.Cancel)
        assertEquals(DownloadState.IDLE, t.state)
    }

    // --- PAUSED ---

    @Test
    fun `PAUSED Resume to RUNNING`() {
        val t = DownloadStateMachine.transition(task(DownloadState.PAUSED), DownloadEvent.Resume)
        assertEquals(DownloadState.RUNNING, t.state)
    }

    @Test
    fun `PAUSED Cancel to IDLE`() {
        val t = DownloadStateMachine.transition(task(DownloadState.PAUSED), DownloadEvent.Cancel)
        assertEquals(DownloadState.IDLE, t.state)
    }

    @Test
    fun `PAUSED Enqueue restarts the task`() {
        val t = DownloadStateMachine.transition(task(DownloadState.PAUSED), DownloadEvent.Enqueue)
        assertEquals(DownloadState.RUNNING, t.state)
    }

    @Test
    fun `PAUSED ignores progress and fail`() {
        val progress = DownloadStateMachine.transition(
            task(DownloadState.PAUSED),
            DownloadEvent.Progress(downloadedBytes = 1, totalBytes = 2)
        )
        assertEquals(DownloadState.PAUSED, progress.state)

        val fail = DownloadStateMachine.transition(task(DownloadState.PAUSED), DownloadEvent.Fail(DownloadFailureReason.NETWORK))
        assertEquals(DownloadState.PAUSED, fail.state)
    }

    // --- FAILED ---

    @Test
    fun `FAILED Resume to RUNNING`() {
        val t = DownloadStateMachine.transition(task(DownloadState.FAILED), DownloadEvent.Resume)
        assertEquals(DownloadState.RUNNING, t.state)
    }

    @Test
    fun `FAILED Enqueue to QUEUED resets retryCount`() {
        val t = DownloadStateMachine.transition(
            task(DownloadState.FAILED).copy(retryCount = 2),
            DownloadEvent.Enqueue
        )
        assertEquals(DownloadState.QUEUED, t.state)
        assertEquals(0, t.retryCount)
    }

    @Test
    fun `FAILED Cancel to IDLE`() {
        val t = DownloadStateMachine.transition(task(DownloadState.FAILED), DownloadEvent.Cancel)
        assertEquals(DownloadState.IDLE, t.state)
    }

    // --- SUCCEEDED ---

    @Test
    fun `SUCCEEDED is terminal for most events`() {
        val start = DownloadStateMachine.transition(task(DownloadState.SUCCEEDED), DownloadEvent.Start)
        assertEquals(DownloadState.SUCCEEDED, start.state)

        val resume = DownloadStateMachine.transition(task(DownloadState.SUCCEEDED), DownloadEvent.Resume)
        assertEquals(DownloadState.SUCCEEDED, resume.state)

        val fail = DownloadStateMachine.transition(task(DownloadState.SUCCEEDED), DownloadEvent.Fail(DownloadFailureReason.NETWORK))
        assertEquals(DownloadState.SUCCEEDED, fail.state)
    }

    @Test
    fun `SUCCEEDED Cancel to IDLE`() {
        val t = DownloadStateMachine.transition(task(DownloadState.SUCCEEDED), DownloadEvent.Cancel)
        assertEquals(DownloadState.IDLE, t.state)
    }

    // --- Restore (process death recovery) ---

    @Test
    fun `Restore RUNNING to FAILED INTERRUPTED`() {
        val t = DownloadStateMachine.restore(task(DownloadState.RUNNING))
        assertEquals(DownloadState.FAILED, t.state)
        assertEquals(DownloadFailureReason.INTERRUPTED, t.failureReason)
    }

    @Test
    fun `Restore QUEUED to FAILED INTERRUPTED`() {
        val t = DownloadStateMachine.restore(task(DownloadState.QUEUED))
        assertEquals(DownloadState.FAILED, t.state)
        assertEquals(DownloadFailureReason.INTERRUPTED, t.failureReason)
    }

    @Test
    fun `Restore preserves PAUSED`() {
        val t = DownloadStateMachine.restore(task(DownloadState.PAUSED))
        assertEquals(DownloadState.PAUSED, t.state)
    }

    @Test
    fun `Restore preserves FAILED and SUCCEEDED`() {
        val failed = DownloadStateMachine.restore(task(DownloadState.FAILED).copy(failureReason = DownloadFailureReason.NETWORK))
        assertEquals(DownloadState.FAILED, failed.state)
        assertEquals(DownloadFailureReason.NETWORK, failed.failureReason)

        val succeeded = DownloadStateMachine.restore(task(DownloadState.SUCCEEDED))
        assertEquals(DownloadState.SUCCEEDED, succeeded.state)
    }

    // --- Full table sweep ---

    @Test
    fun `full transition table sweep`() {
        // expected resulting state for every (state, event) pair
        val table = mapOf(
            // IDLE
            (DownloadState.IDLE to DownloadEvent.Enqueue) to DownloadState.QUEUED,
            // QUEUED
            (DownloadState.QUEUED to DownloadEvent.Start) to DownloadState.RUNNING,
            (DownloadState.QUEUED to DownloadEvent.Pause) to DownloadState.PAUSED,
            (DownloadState.QUEUED to DownloadEvent.Cancel) to DownloadState.IDLE,
            (DownloadState.QUEUED to DownloadEvent.RefreshUrl) to DownloadState.QUEUED,
            (DownloadState.QUEUED to DownloadEvent.Fail(DownloadFailureReason.NETWORK)) to DownloadState.QUEUED,
            // RUNNING
            (DownloadState.RUNNING to DownloadEvent.Pause) to DownloadState.PAUSED,
            (DownloadState.RUNNING to DownloadEvent.Cancel) to DownloadState.IDLE,
            (DownloadState.RUNNING to DownloadEvent.Succeed) to DownloadState.SUCCEEDED,
            (DownloadState.RUNNING to DownloadEvent.RefreshUrl) to DownloadState.QUEUED,
            (DownloadState.RUNNING to DownloadEvent.Fail(DownloadFailureReason.IO)) to DownloadState.QUEUED,
            (DownloadState.RUNNING to DownloadEvent.Fail(DownloadFailureReason.NETWORK)) to DownloadState.QUEUED,
            // PAUSED
            (DownloadState.PAUSED to DownloadEvent.Resume) to DownloadState.RUNNING,
            (DownloadState.PAUSED to DownloadEvent.Enqueue) to DownloadState.RUNNING,
            (DownloadState.PAUSED to DownloadEvent.Cancel) to DownloadState.IDLE,
            // FAILED
            (DownloadState.FAILED to DownloadEvent.Resume) to DownloadState.RUNNING,
            (DownloadState.FAILED to DownloadEvent.Enqueue) to DownloadState.QUEUED,
            (DownloadState.FAILED to DownloadEvent.Cancel) to DownloadState.IDLE,
            // SUCCEEDED
            (DownloadState.SUCCEEDED to DownloadEvent.Cancel) to DownloadState.IDLE
        )

        val config = DownloadConfig(maxRetries = 3, maxUrlRefreshes = 2)
        val events = listOf(
            DownloadEvent.Enqueue,
            DownloadEvent.Start,
            DownloadEvent.Pause,
            DownloadEvent.Resume,
            DownloadEvent.Cancel,
            DownloadEvent.Succeed,
            DownloadEvent.RefreshUrl,
            DownloadEvent.Fail(DownloadFailureReason.NETWORK),
            DownloadEvent.Progress(downloadedBytes = 50, totalBytes = 100)
        )

        DownloadState.entries.forEach { state ->
            events.forEach { event ->
                val t = DownloadStateMachine.transition(task(state), event, config)
                val expected = table[state to event] ?: state // default: unchanged
                assertEquals(expected, t.state, "state=$state event=$event")
            }
        }
    }
}
