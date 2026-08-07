package com.qx.orbit.core.danmaku

import com.qx.orbit.core.playback.PlaybackState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DanmakuLifecycleTest {

    private val lifecycle = DanmakuLifecycle

    /** Minimal event using the shared A15 [DanmakuEvent] model. */
    private fun event(text: String) = DanmakuEvent(
        timeMs = 0L,
        mode = DanmakuMode.SCROLL,
        text = text,
        color = 0xFFFFFFFFL,
    )

    private fun prepared(state: DanmakuLifecycle.State = lifecycle.initial()) =
        lifecycle.reduce(state, DanmakuLifecycle.Event.MediaPrepared(0L))

    private fun playing(state: DanmakuLifecycle.State) =
        lifecycle.reduce(
            state,
            DanmakuLifecycle.Event.PlaybackStateChanged(PlaybackState.PLAYING, playing = true),
        )

    @Test
    fun mediaPreparedStartsClockAtPosition() {
        val result = lifecycle.reduce(
            lifecycle.initial(),
            DanmakuLifecycle.Event.MediaPrepared(12_345),
        )
        assertEquals(listOf(DanmakuLifecycle.Command.Start(12_345)), result.commands)
        assertEquals(DanmakuLifecycle.SyncState.PLAYING, result.state.sync)
        assertEquals(12_345L, result.state.positionMs)
    }

    @Test
    fun pauseFreezesClock() {
        val result = lifecycle.reduce(
            prepared().state,
            DanmakuLifecycle.Event.PlaybackStateChanged(PlaybackState.PAUSED, playing = false),
        )
        assertEquals(listOf(DanmakuLifecycle.Command.Pause), result.commands)
        assertEquals(DanmakuLifecycle.SyncState.PAUSED, result.state.sync)
    }

    @Test
    fun resumeRestartsClock() {
        var state = prepared().state
        state = lifecycle.reduce(
            state,
            DanmakuLifecycle.Event.PlaybackStateChanged(PlaybackState.PAUSED, playing = false),
        ).state
        val result = playing(state)
        assertEquals(listOf(DanmakuLifecycle.Command.Resume), result.commands)
        assertEquals(DanmakuLifecycle.SyncState.PLAYING, result.state.sync)
    }

    @Test
    fun bufferingPausesAndOnlyResumesWhenMediaPlaying() {
        var state = prepared().state

        // Buffering start -> pause (matches MEDIA_INFO_BUFFERING_START).
        val buffering = lifecycle.reduce(
            state,
            DanmakuLifecycle.Event.PlaybackStateChanged(PlaybackState.BUFFERING, playing = true),
        )
        assertEquals(listOf(DanmakuLifecycle.Command.Pause), buffering.commands)
        assertEquals(DanmakuLifecycle.SyncState.BUFFERING, buffering.state.sync)
        state = buffering.state

        // Buffering end -> READY with playing intent -> resume.
        val ready = lifecycle.reduce(
            state,
            DanmakuLifecycle.Event.PlaybackStateChanged(PlaybackState.READY, playing = true),
        )
        assertEquals(listOf(DanmakuLifecycle.Command.Resume), ready.commands)
        assertEquals(DanmakuLifecycle.SyncState.PLAYING, ready.state.sync)
        state = ready.state

        // Buffering end while paused -> must NOT resume.
        state = lifecycle.reduce(
            state,
            DanmakuLifecycle.Event.PlaybackStateChanged(PlaybackState.PAUSED, playing = false),
        ).state
        val bufferingWhilePaused = lifecycle.reduce(
            state,
            DanmakuLifecycle.Event.PlaybackStateChanged(PlaybackState.BUFFERING, playing = false),
        )
        assertEquals(emptyList(), bufferingWhilePaused.commands)
        val readyWhilePaused = lifecycle.reduce(
            bufferingWhilePaused.state,
            DanmakuLifecycle.Event.PlaybackStateChanged(PlaybackState.READY, playing = false),
        )
        assertEquals(emptyList(), readyWhilePaused.commands)
    }

    @Test
    fun repeatedPauseIsIdempotent() {
        var state = prepared().state
        state = lifecycle.reduce(
            state,
            DanmakuLifecycle.Event.PlaybackStateChanged(PlaybackState.PAUSED, playing = false),
        ).state
        val again = lifecycle.reduce(
            state,
            DanmakuLifecycle.Event.PlaybackStateChanged(PlaybackState.PAUSED, playing = false),
        )
        assertEquals(emptyList(), again.commands)
        assertEquals(DanmakuLifecycle.SyncState.PAUSED, again.state.sync)
    }

    @Test
    fun repeatedPlayIsIdempotent() {
        val again = playing(prepared().state)
        assertEquals(emptyList(), again.commands)
        assertEquals(DanmakuLifecycle.SyncState.PLAYING, again.state.sync)
    }

    @Test
    fun seekEmitsSeekEvenWhilePaused() {
        var state = prepared().state
        state = lifecycle.reduce(
            state,
            DanmakuLifecycle.Event.PlaybackStateChanged(PlaybackState.PAUSED, playing = false),
        ).state

        val result = lifecycle.reduce(state, DanmakuLifecycle.Event.Seek(77_000))
        assertEquals(listOf(DanmakuLifecycle.Command.Seek(77_000)), result.commands)
        assertEquals(77_000L, result.state.positionMs)
        // Pause state is preserved across the seek.
        assertEquals(DanmakuLifecycle.SyncState.PAUSED, result.state.sync)
    }

    @Test
    fun speedChangeEmitsSetSpeed() {
        val result = lifecycle.reduce(prepared().state, DanmakuLifecycle.Event.SpeedChanged(2.0f))
        assertEquals(listOf(DanmakuLifecycle.Command.SetSpeed(2.0f)), result.commands)
        assertEquals(2.0f, result.state.speed)
    }

    @Test
    fun liveEventIsSubmittedWithImmediateAnchor() {
        val result = lifecycle.reduce(
            prepared().state,
            DanmakuLifecycle.Event.LiveEvent(event("来了"), clockMs = 42_000),
        )
        val submit = result.commands.single()
        assertIs<DanmakuLifecycle.Command.Submit>(submit)
        assertEquals(event("来了").copy(timeMs = 42_000 + DanmakuLifecycle.LIVE_EVENT_OFFSET_MS), submit.event)
        assertEquals(42_000L, result.state.positionMs)
    }

    @Test
    fun liveEventsStillFlowWhilePaused() {
        var state = prepared().state
        state = lifecycle.reduce(
            state,
            DanmakuLifecycle.Event.PlaybackStateChanged(PlaybackState.PAUSED, playing = false),
        ).state
        val result = lifecycle.reduce(
            state,
            DanmakuLifecycle.Event.LiveEvent(event("x"), clockMs = 1_000),
        )
        assertEquals(1, result.commands.size)
        assertIs<DanmakuLifecycle.Command.Submit>(result.commands.single())
    }

    @Test
    fun resetClearsAndReturnsToInitialState() {
        var state = prepared().state
        state = playing(state).state
        state = lifecycle.reduce(state, DanmakuLifecycle.Event.SpeedChanged(2.0f)).state

        val result = lifecycle.reduce(state, DanmakuLifecycle.Event.Reset)
        assertEquals(listOf(DanmakuLifecycle.Command.Clear()), result.commands)
        assertEquals(lifecycle.initial(), result.state)
    }

    @Test
    fun endedAndErrorFreezeClock() {
        val ended = lifecycle.reduce(
            prepared().state,
            DanmakuLifecycle.Event.PlaybackStateChanged(PlaybackState.ENDED, playing = false),
        )
        assertEquals(listOf(DanmakuLifecycle.Command.Pause), ended.commands)

        var state = prepared().state
        state = lifecycle.reduce(
            state,
            DanmakuLifecycle.Event.PlaybackStateChanged(PlaybackState.ERROR, playing = false),
        ).state
        // Recovering from error through BUFFERING must not emit Resume by itself.
        val retry = lifecycle.reduce(
            state,
            DanmakuLifecycle.Event.PlaybackStateChanged(PlaybackState.BUFFERING, playing = false),
        )
        assertEquals(emptyList(), retry.commands)
    }

    @Test
    fun longPressSpeedRestoresOnRelease() {
        // 2.0 during long-press, then back to 1.0 -> exact mirrors of the app.
        val boost = lifecycle.reduce(prepared().state, DanmakuLifecycle.Event.SpeedChanged(2.0f))
        val restore = lifecycle.reduce(boost.state, DanmakuLifecycle.Event.SpeedChanged(1.0f))
        assertEquals(DanmakuLifecycle.Command.SetSpeed(2.0f), boost.commands.single())
        assertEquals(DanmakuLifecycle.Command.SetSpeed(1.0f), restore.commands.single())
    }
}
