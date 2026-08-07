package com.qx.orbit.core.playback

import kotlinx.coroutines.flow.Flow

/**
 * Platform-neutral playback controller contract.
 *
 * Implemented by the Android adapter (Media3/IJK wrapping [com.qx.orbit.bili.util.player.OrbitPlayer])
 * and the HarmonyOS adapter (API 22 media capabilities). No `Surface`/`View`
 * types appear here: rendering surfaces stay on the platform side and are bound
 * by the adapter itself.
 */
interface PlaybackController {

    // --- Commands ---

    /** Load [source] and start preparing at [startPositionMs]. */
    fun load(source: MediaSource, startPositionMs: Long = 0L)

    /** Start or resume playback. */
    fun play()

    /** Pause playback, keeping the current position. */
    fun pause()

    /** Seek to [positionMs]. */
    fun seekTo(positionMs: Long)

    /** Change playback speed factor (e.g. `1.0`, `2.0`). */
    fun setSpeed(speed: Float)

    /** Switch to a track previously advertised through [tracks]. */
    fun switchTrack(track: Track)

    /** Stop current media and drop state, keeping the controller reusable. */
    fun reset()

    /** Release all resources; the controller must not be used afterwards. */
    fun release()

    // --- Queries ---

    val currentState: PlaybackState
    val position: PlaybackPosition

    // --- Observation ---

    /** State transitions (initial value: [PlaybackState.IDLE]). */
    val states: Flow<PlaybackState>

    /** Periodic position/buffer updates. */
    val positions: Flow<PlaybackPosition>

    /** Non-fatal and fatal errors, with [PlaybackError] details. */
    val errors: Flow<PlaybackError>

    /** Currently available tracks (empty until media loads). */
    val tracks: Flow<List<Track>>

    companion object {
        /** Normal playback speed. */
        const val NORMAL_SPEED = 1.0f

        /** Guards [setSpeed]; adapters may clamp instead of failing. */
        fun isValidSpeed(speed: Float): Boolean = speed > 0f && speed <= 16f
    }
}
