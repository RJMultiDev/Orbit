package com.qx.orbit.core.playback

/**
 * Progress record for a playback session and the rules to advance it.
 *
 * Pure value type; wall-clock is injected so tests can drive time deterministically.
 */
data class PlaybackProgress(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val updatedAtMs: Long = 0L
) {
    val positionSec: Long get() = positionMs / 1000
    val fraction: Float
        get() = if (durationMs > 0L) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val isAtEnd: Boolean get() = durationMs > 0L && positionMs >= durationMs

    /** Clamp the given position to [0, duration]. */
    fun clamped(positionMs: Long): Long = positionMs.coerceAtLeast(0L).coerceAtMost(durationMs.coerceAtLeast(0L))

    /** Set the current position (clamped), recorded at [nowMs]. */
    fun advanceTo(positionMs: Long, nowMs: Long): PlaybackProgress =
        copy(positionMs = clamped(positionMs), updatedAtMs = nowMs)

    /** Seek: update position (clamped) without changing the updated timestamp. */
    fun seekTo(positionMs: Long): PlaybackProgress = copy(positionMs = clamped(positionMs))

    /** Apply a wall-clock tick while playing: position += elapsed, capped at duration. */
    fun tick(elapsedMs: Long): PlaybackProgress {
        if (!isPlaying || elapsedMs <= 0L) return this
        val next = clamped(positionMs + elapsedMs)
        return copy(positionMs = next, updatedAtMs = updatedAtMs + elapsedMs)
    }

    fun setDuration(durationMs: Long): PlaybackProgress = copy(durationMs = durationMs.coerceAtLeast(0L))

    fun setPlaying(playing: Boolean): PlaybackProgress = copy(isPlaying = playing)

    fun reset(): PlaybackProgress = PlaybackProgress()
}
