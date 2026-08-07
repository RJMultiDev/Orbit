package com.qx.orbit.core.playback

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HeartbeatPolicyTest {

    private val policy = HeartbeatPolicy(
        intervalMs = 15_000L,
        minProgressAdvanceMs = 1_000L,
        dedupWindowMs = 60_000L
    )

    private val video = PlaybackIdentity(aid = 101L, bvid = "BV1", cid = 202L, mediaType = PlaybackMediaType.VIDEO)
    private val bangumi = PlaybackIdentity(
        aid = 101L, bvid = "BV2", cid = 303L,
        mediaType = PlaybackMediaType.BANGUMI, epid = 9L, sid = 5L
    )
    private val live = PlaybackIdentity(aid = 1L, bvid = "", cid = 2L, mediaType = PlaybackMediaType.LIVE)
    private val local = PlaybackIdentity(aid = 0L, bvid = "", cid = 0L, mediaType = PlaybackMediaType.LOCAL)

    // --- heartbeat timing ---

    @Test
    fun `first heartbeat is always due`() {
        assertTrue(policy.shouldSend(lastSentAtMs = null, nowMs = 0L))
        assertEquals(0L, policy.nextDelay(lastSentAtMs = null, nowMs = 123L))
    }

    @Test
    fun `heartbeat not due before interval elapses`() {
        assertFalse(policy.shouldSend(lastSentAtMs = 1_000L, nowMs = 15_999L))
        assertEquals(1L, policy.nextDelay(lastSentAtMs = 1_000L, nowMs = 15_999L))
    }

    @Test
    fun `heartbeat due exactly at interval`() {
        assertTrue(policy.shouldSend(lastSentAtMs = 1_000L, nowMs = 16_000L))
        assertEquals(0L, policy.nextDelay(lastSentAtMs = 1_000L, nowMs = 16_000L))
    }

    @Test
    fun `heartbeat due past interval`() {
        assertTrue(policy.shouldSend(lastSentAtMs = 1_000L, nowMs = 30_000L))
        assertEquals(0L, policy.nextDelay(lastSentAtMs = 1_000L, nowMs = 30_000L))
    }

    // --- progress report throttle + advance ---

    @Test
    fun `progress report blocked by throttle`() {
        assertFalse(
            policy.shouldReportProgress(
                nowMs = 15_999L,
                lastReportedAtMs = 1_000L,
                currentPositionMs = 50_000L,
                lastReportedPositionMs = 1_000L
            )
        )
    }

    @Test
    fun `progress report requires real advance`() {
        assertFalse(
            policy.shouldReportProgress(
                nowMs = 16_000L,
                lastReportedAtMs = 1_000L,
                currentPositionMs = 50_100L, // +100ms < min 1s
                lastReportedPositionMs = 50_000L
            )
        )
    }

    @Test
    fun `progress report allowed when throttled and advanced`() {
        assertTrue(
            policy.shouldReportProgress(
                nowMs = 16_000L,
                lastReportedAtMs = 1_000L,
                currentPositionMs = 52_000L,
                lastReportedPositionMs = 50_000L
            )
        )
    }

    @Test
    fun `progress report allowed when no previous position`() {
        assertTrue(
            policy.shouldReportProgress(
                nowMs = 16_000L,
                lastReportedAtMs = 1_000L,
                currentPositionMs = 52_000L,
                lastReportedPositionMs = null
            )
        )
    }

    // --- duplicate detection ---

    private fun report(identity: PlaybackIdentity, playedMs: Long, nowSec: Long) =
        policy.buildHeartbeat(identity, progressMs = playedMs, durationMs = 100_000L, startTsSec = 1_000L, nowSec = nowSec)

    @Test
    fun `identical report within dedup window is duplicate`() {
        val first = report(video, playedMs = 30_000L, nowSec = 1_030L)
        val second = report(video, playedMs = 30_000L, nowSec = 1_040L)
        assertTrue(policy.isDuplicate(nowMs = 1_040_000L, lastReportedAtMs = 1_030_000L, next = second, previous = first))
    }

    @Test
    fun `identical report outside dedup window is not duplicate`() {
        val first = report(video, playedMs = 30_000L, nowSec = 1_030L)
        val second = report(video, playedMs = 30_000L, nowSec = 1_100L)
        assertFalse(policy.isDuplicate(nowMs = 1_100_000L, lastReportedAtMs = 1_030_000L, next = second, previous = first))
    }

    @Test
    fun `different position is not duplicate`() {
        val first = report(video, playedMs = 30_000L, nowSec = 1_030L)
        val second = report(video, playedMs = 31_000L, nowSec = 1_040L)
        assertFalse(policy.isDuplicate(nowMs = 1_040_000L, lastReportedAtMs = 1_030_000L, next = second, previous = first))
    }

    @Test
    fun `no previous report is not duplicate`() {
        val next = report(video, playedMs = 30_000L, nowSec = 1_040L)
        assertFalse(policy.isDuplicate(nowMs = 1_040_000L, lastReportedAtMs = 1_030_000L, next = next, previous = null))
    }

    // --- payload building ---

    @Test
    fun `video heartbeat uses type 3 and no ids`() {
        val hb = policy.buildHeartbeat(video, progressMs = 30_000L, durationMs = 100_000L, startTsSec = 1_000L, nowSec = 1_030L)
        assertEquals("3", hb.type)
        assertNull(hb.subType)
        assertNull(hb.epid)
        assertNull(hb.sid)
        assertEquals(30L, hb.playedTimeSec)
        assertEquals(100L, hb.videoDurationSec)
        assertEquals(1_000L, hb.startTsSec)
        assertEquals(30L, hb.realtimeSec)
        assertEquals(1_030L, hb.atEpochSec)
    }

    @Test
    fun `bangumi heartbeat uses type 4 with ids`() {
        val hb = policy.buildHeartbeat(bangumi, progressMs = 30_000L, durationMs = 100_000L, startTsSec = 1_000L, nowSec = 1_030L)
        assertEquals("4", hb.type)
        assertEquals("1", hb.subType)
        assertEquals(9L, hb.epid)
        assertEquals(5L, hb.sid)
    }

    @Test
    fun `progress is clamped at zero`() {
        val hb = policy.buildHeartbeat(video, progressMs = -5_000L, durationMs = 0L, startTsSec = 0L, nowSec = 0L)
        assertEquals(0L, hb.playedTimeSec)
        assertEquals(0L, hb.videoDurationSec)
    }

    // --- history rules ---

    @Test
    fun `history reported only for video`() {
        assertTrue(policy.shouldReportHistory(video))
        assertFalse(policy.shouldReportHistory(bangumi))
        assertFalse(policy.shouldReportHistory(live))
        assertFalse(policy.shouldReportHistory(local))
    }

    @Test
    fun `buildHistory returns payload for video`() {
        val h = policy.buildHistory(video, progressMs = 45_000L, nowSec = 1_045L)
        assertEquals(101L, h?.aid)
        assertEquals(202L, h?.cid)
        assertEquals(45L, h?.progressSec)
    }

    @Test
    fun `buildHistory returns null for bangumi live local`() {
        assertNull(policy.buildHistory(bangumi, progressMs = 45_000L, nowSec = 1_045L))
        assertNull(policy.buildHistory(live, progressMs = 45_000L, nowSec = 1_045L))
        assertNull(policy.buildHistory(local, progressMs = 45_000L, nowSec = 1_045L))
    }

    // --- reportsToServer ---

    @Test
    fun `only video and bangumi report to server`() {
        assertTrue(video.reportsToServer)
        assertTrue(bangumi.reportsToServer)
        assertFalse(live.reportsToServer)
        assertFalse(local.reportsToServer)
    }
}
