package com.qx.orbit.core.danmaku

import com.qx.orbit.core.playback.PlaybackState

/**
 * Pure logic that keeps the danmaku renderer in sync with media playback.
 *
 * The Android `PlayerScreen` currently drives DFM/DFMNext imperatively:
 * `pause()` on media pause and on buffering start, `resume()` on buffering end
 * (only when playing), `seekTo()` on seek, `setSpeed()` on speed change, and
 * live events submitted with `getCurrentTime() + 100`. This object extracts
 * that semantics into a deterministic state machine: feed it
 * [DanmakuSyncEvent]s and it emits [DanmakuSyncCommand]s for the platform
 * adapter to execute against [DanmakuRenderer]. No rendering or platform state
 * is touched here.
 */
object DanmakuLifecycle {

    /** Renderer-side synchronization clock state. */
    enum class SyncState {
        /** No media prepared / after [DanmakuSyncEvent.Reset]. */
        IDLE,
        /** Rendering clock advancing. */
        PLAYING,
        /** Rendering clock frozen by media pause/buffering. */
        PAUSED,
        /** Rendering clock frozen because the media is buffering. */
        BUFFERING,
    }

    /** Command for the platform adapter to execute on [DanmakuRenderer]. */
    sealed interface Command {
        /** Start the renderer clock at [positionMs] (after prepare). */
        data class Start(val positionMs: Long) : Command
        data class Seek(val positionMs: Long) : Command
        data class SetSpeed(val speed: Float) : Command
        data class Submit(val event: DanmakuEvent) : Command
        data class Clear(val clearOnScreen: Boolean = true) : Command
        data object Pause : Command
        data object Resume : Command
    }

    /** Input event: a change in the media or danmaku source. */
    sealed interface Event {
        /** Media prepared and about to play; start the clock at [startPositionMs]. */
        data class MediaPrepared(val startPositionMs: Long) : Event
        /** Media state transition; [playing] is the play/pause intent. */
        data class PlaybackStateChanged(val state: PlaybackState, val playing: Boolean) : Event
        /** Media seeked. */
        data class Seek(val positionMs: Long) : Event
        /** Playback speed changed. */
        data class SpeedChanged(val speed: Float) : Event
        /** Live danmaku arrived at media clock [clockMs]. */
        data class LiveEvent(val event: DanmakuEvent, val clockMs: Long) : Event
        /** New media item / source switch: drop stale danmaku. */
        data object Reset : Event
    }

    /** Result of reducing an [Event]: the new [state] plus [commands] to run. */
    data class Result(
        val state: State,
        val commands: List<Command>,
    )

    /** Mutable state of the synchronization machine. */
    data class State(
        val sync: SyncState = SyncState.IDLE,
        val mediaState: PlaybackState = PlaybackState.IDLE,
        val positionMs: Long = 0L,
        val speed: Float = 1.0f,
    )

    /** Offset added to live events so they render immediately, as in the app. */
    const val LIVE_EVENT_OFFSET_MS = 100L

    fun initial(): State = State()

    /**
     * Reduce [event] against [state]. Deterministic and side-effect free: the
     * caller (platform adapter) executes the returned [Command]s on the
     * renderer, then keeps [Result.state] for the next call.
     */
    fun reduce(state: State, event: Event): Result = when (event) {
        is Event.MediaPrepared -> Result(
            state = state.copy(
                sync = SyncState.PLAYING,
                mediaState = PlaybackState.READY,
                positionMs = event.startPositionMs,
            ),
            commands = listOf(Command.Start(event.startPositionMs)),
        )

        is Event.PlaybackStateChanged -> {
            val wantsPlaying = when (event.state) {
                PlaybackState.PLAYING -> true
                PlaybackState.READY -> event.playing
                else -> false
            }
            val wasPlaying = state.sync == SyncState.PLAYING
            val command = when {
                wantsPlaying && !wasPlaying -> listOf(Command.Resume)
                !wantsPlaying && wasPlaying -> listOf(Command.Pause)
                else -> emptyList()
            }
            val sync = when {
                wantsPlaying -> SyncState.PLAYING
                event.state == PlaybackState.BUFFERING -> SyncState.BUFFERING
                else -> SyncState.PAUSED
            }
            Result(
                state = state.copy(sync = sync, mediaState = event.state),
                commands = command,
            )
        }

        is Event.Seek -> Result(
            state = state.copy(positionMs = event.positionMs),
            commands = listOf(Command.Seek(event.positionMs)),
        )

        is Event.SpeedChanged -> Result(
            state = state.copy(speed = event.speed),
            commands = listOf(Command.SetSpeed(event.speed)),
        )

        is Event.LiveEvent -> Result(
            state = state.copy(positionMs = event.clockMs),
            commands = listOf(
                Command.Submit(event.event.copy(timeMs = event.clockMs + LIVE_EVENT_OFFSET_MS)),
            ),
        )

        Event.Reset -> Result(
            state = initial(),
            commands = listOf(Command.Clear()),
        )
    }
}
