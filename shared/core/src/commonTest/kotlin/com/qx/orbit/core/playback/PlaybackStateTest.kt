package com.qx.orbit.core.playback

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackStateTest {

    @Test
    fun identityTransitionsAreLegal() {
        PlaybackState.entries.forEach { state ->
            assertTrue(
                PlaybackState.isValidTransition(state, state),
                "$state -> $state should be legal",
            )
        }
    }

    @Test
    fun anyStateMayEnterError() {
        PlaybackState.entries.forEach { state ->
            assertTrue(
                PlaybackState.isValidTransition(state, PlaybackState.ERROR),
                "$state -> ERROR should be legal",
            )
        }
    }

    @Test
    fun errorOnlyExitsThroughIdleOrBuffering() {
        assertTrue(PlaybackState.isValidTransition(PlaybackState.ERROR, PlaybackState.IDLE))
        assertTrue(PlaybackState.isValidTransition(PlaybackState.ERROR, PlaybackState.BUFFERING))
        assertFalse(PlaybackState.isValidTransition(PlaybackState.ERROR, PlaybackState.PLAYING))
        assertFalse(PlaybackState.isValidTransition(PlaybackState.ERROR, PlaybackState.PAUSED))
        assertFalse(PlaybackState.isValidTransition(PlaybackState.ERROR, PlaybackState.READY))
        assertFalse(PlaybackState.isValidTransition(PlaybackState.ERROR, PlaybackState.ENDED))
    }

    @Test
    fun happyPathLifecycleIsLegal() {
        // Re-buffering resumes through READY (Media3: BUFFERING -> READY -> playing).
        val path = listOf(
            PlaybackState.IDLE,
            PlaybackState.BUFFERING,
            PlaybackState.READY,
            PlaybackState.PLAYING,
            PlaybackState.PAUSED,
            PlaybackState.PLAYING,
            PlaybackState.BUFFERING,
            PlaybackState.READY,
            PlaybackState.PLAYING,
            PlaybackState.ENDED,
            PlaybackState.PLAYING, // replay
            PlaybackState.ENDED,
            PlaybackState.IDLE, // reset
        )
        path.zipWithNext().forEach { (from, to) ->
            assertTrue(PlaybackState.isValidTransition(from, to), "$from -> $to")
        }
    }

    @Test
    fun invalidJumpsAreRejected() {
        // Cannot jump to PLAYING/PAUSED/READY from IDLE without preparing.
        assertFalse(PlaybackState.isValidTransition(PlaybackState.IDLE, PlaybackState.PLAYING))
        assertFalse(PlaybackState.isValidTransition(PlaybackState.IDLE, PlaybackState.PAUSED))
        assertFalse(PlaybackState.isValidTransition(PlaybackState.IDLE, PlaybackState.READY))
        assertFalse(PlaybackState.isValidTransition(PlaybackState.IDLE, PlaybackState.ENDED))
        // Cannot reach ENDED straight from BUFFERING (must become READY first),
        // and ERROR only exits through IDLE/BUFFERING.
        assertFalse(PlaybackState.isValidTransition(PlaybackState.BUFFERING, PlaybackState.ENDED))
        assertFalse(PlaybackState.isValidTransition(PlaybackState.ERROR, PlaybackState.ENDED))
    }

    @Test
    fun positionDefaultsAndFraction() {
        assertEquals(PlaybackPosition(), PlaybackPosition(0L, 0L, 0L))

        val pos = PlaybackPosition(positionMs = 10_000, durationMs = 60_000, bufferedMs = 30_000)
        assertEquals(0.5f, pos.bufferedFraction)

        // Unknown duration -> no buffered fraction.
        val live = PlaybackPosition(positionMs = 10_000, durationMs = 0, bufferedMs = 5_000)
        assertEquals(0f, live.bufferedFraction)

        // Clamped to 1.0 when buffer claims more than the duration.
        val over = PlaybackPosition(positionMs = 0, durationMs = 10_000, bufferedMs = 99_000)
        assertEquals(1f, over.bufferedFraction)
    }
}
