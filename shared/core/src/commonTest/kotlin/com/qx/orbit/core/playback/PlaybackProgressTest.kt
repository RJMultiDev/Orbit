package com.qx.orbit.core.playback

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackProgressTest {

    @Test
    fun `defaults are zero`() {
        val p = PlaybackProgress()
        assertEquals(0L, p.positionMs)
        assertEquals(0L, p.durationMs)
        assertEquals(0f, p.fraction)
        assertFalse(p.isPlaying)
        assertFalse(p.isAtEnd)
    }

    @Test
    fun `advanceTo clamps to duration`() {
        val p = PlaybackProgress(durationMs = 100_000L)
        val t = p.advanceTo(positionMs = 120_000L, nowMs = 1_000L)
        assertEquals(100_000L, t.positionMs)
        assertEquals(1_000L, t.updatedAtMs)
    }

    @Test
    fun `advanceTo clamps negative to zero`() {
        val p = PlaybackProgress(durationMs = 100_000L)
        val t = p.advanceTo(positionMs = -50L, nowMs = 1_000L)
        assertEquals(0L, t.positionMs)
    }

    @Test
    fun `seekTo changes position but not timestamp`() {
        val p = PlaybackProgress(durationMs = 100_000L, updatedAtMs = 42L)
        val t = p.seekTo(positionMs = 30_000L)
        assertEquals(30_000L, t.positionMs)
        assertEquals(42L, t.updatedAtMs)
    }

    @Test
    fun `tick advances only while playing`() {
        val paused = PlaybackProgress(positionMs = 10_000L, durationMs = 100_000L, isPlaying = false, updatedAtMs = 0L)
        assertEquals(10_000L, paused.tick(5_000L).positionMs)

        val playing = PlaybackProgress(positionMs = 10_000L, durationMs = 100_000L, isPlaying = true, updatedAtMs = 0L)
        val t = playing.tick(5_000L)
        assertEquals(15_000L, t.positionMs)
        assertEquals(5_000L, t.updatedAtMs)
    }

    @Test
    fun `tick caps at duration`() {
        val p = PlaybackProgress(positionMs = 99_000L, durationMs = 100_000L, isPlaying = true, updatedAtMs = 0L)
        val t = p.tick(10_000L)
        assertEquals(100_000L, t.positionMs)
        assertTrue(t.isAtEnd)
    }

    @Test
    fun `fraction is clamped`() {
        val p = PlaybackProgress(positionMs = 50_000L, durationMs = 100_000L)
        assertEquals(0.5f, p.fraction)
        assertEquals(50L, p.positionSec)
    }

    @Test
    fun `setDuration and setPlaying`() {
        val p = PlaybackProgress()
        val t = p.setDuration(200_000L).setPlaying(true)
        assertEquals(200_000L, t.durationMs)
        assertTrue(t.isPlaying)
    }

    @Test
    fun `reset clears state`() {
        val p = PlaybackProgress(positionMs = 50_000L, durationMs = 100_000L, isPlaying = true, updatedAtMs = 9L)
        val r = p.reset()
        assertEquals(0L, r.positionMs)
        assertEquals(0L, r.durationMs)
        assertFalse(r.isPlaying)
    }
}
