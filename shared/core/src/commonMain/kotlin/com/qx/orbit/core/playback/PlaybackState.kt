package com.qx.orbit.core.playback

/**
 * Lifecycle state of the media item currently handled by a [PlaybackController].
 *
 * This is the platform-neutral projection of the states surfaced by the Android
 * adapters (Media3 `Player.STATE_*`, IJK prepared/info callbacks) and the
 * HarmonyOS media framework. It deliberately carries no platform enum or
 * `Surface`/`View` types.
 */
enum class PlaybackState {
    /** No media loaded, or after [PlaybackController.reset]/[PlaybackController.release]. */
    IDLE,

    /** Preparing or rebuffering; playback cannot advance yet. */
    BUFFERING,

    /** Media prepared and a position is seekable, but playback has not started. */
    READY,

    /** Playback is advancing. */
    PLAYING,

    /** Playback is paused at a position. */
    PAUSED,

    /** Media reached its end. */
    ENDED,

    /** A fatal error occurred; details are delivered through [PlaybackController.errors]. */
    ERROR;

    companion object {
        private val VALID_TRANSITIONS: Map<PlaybackState, Set<PlaybackState>> = mapOf(
            IDLE to setOf(BUFFERING, ERROR),
            BUFFERING to setOf(READY, ERROR, IDLE),
            READY to setOf(PLAYING, PAUSED, BUFFERING, ENDED, ERROR, IDLE),
            PLAYING to setOf(PAUSED, BUFFERING, ENDED, READY, ERROR, IDLE),
            PAUSED to setOf(PLAYING, BUFFERING, READY, ENDED, ERROR, IDLE),
            ENDED to setOf(PLAYING, READY, BUFFERING, IDLE, ERROR),
            ERROR to setOf(IDLE, BUFFERING),
        )

        /**
         * Whether [from] may legally transition to [to]. Any state may enter
         * [ERROR]; [ERROR] only exits through [IDLE] (reset) or [BUFFERING] (retry).
         */
        fun isValidTransition(from: PlaybackState, to: PlaybackState): Boolean =
            from == to || to == ERROR || VALID_TRANSITIONS[from]?.contains(to) == true
    }
}

/**
 * Position/duration snapshot of the current media item.
 *
 * @property positionMs current playback position in milliseconds.
 * @property durationMs total duration in milliseconds; `0` or `-1` when unknown
 *   (e.g. live streams without a fixed duration).
 * @property bufferedMs buffered amount in milliseconds relative to [positionMs].
 */
data class PlaybackPosition(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedMs: Long = 0L,
) {
    /** Fraction `0..1` of the media buffered, or `0` when duration is unknown. */
    val bufferedFraction: Float
        get() = if (durationMs > 0L) (bufferedMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}
