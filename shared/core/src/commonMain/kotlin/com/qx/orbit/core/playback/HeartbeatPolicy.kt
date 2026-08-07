package com.qx.orbit.core.playback

/** What kind of content is playing. Mirrors `PlayerData.TYPE_*`. */
enum class PlaybackMediaType {
    VIDEO,
    BANGUMI,
    LIVE,
    LOCAL
}

/** Identity of the currently playing item, needed to build heartbeat/history payloads. */
data class PlaybackIdentity(
    val aid: Long,
    val bvid: String,
    val cid: Long,
    val mediaType: PlaybackMediaType,
    val epid: Long? = null,
    val sid: Long? = null
) {
    val isBangumi: Boolean
        get() = mediaType == PlaybackMediaType.BANGUMI && (epid ?: 0L) > 0L

    /** Only VIDEO/BANGUMI report to the server (LIVE/LOCAL skip entirely). */
    val reportsToServer: Boolean
        get() = mediaType == PlaybackMediaType.VIDEO || mediaType == PlaybackMediaType.BANGUMI
}

/** Payload for the B 站 heartbeat endpoint. */
data class HeartbeatReport(
    val aid: Long,
    val bvid: String,
    val cid: Long,
    val playedTimeSec: Long,
    val videoDurationSec: Long,
    val startTsSec: Long,
    val realtimeSec: Long,
    val type: String, // "3" = video, "4" = bangumi
    val subType: String?, // "1" = bangumi
    val epid: Long?,
    val sid: Long?,
    val atEpochSec: Long
) {
    /** Identity of this report for duplicate detection. */
    val dedupKey: String
        get() = "$aid:$cid:$type:$playedTimeSec:$subType:$epid:$sid"
}

/** Payload for the history-report endpoint (archive videos only). */
data class HistoryReport(
    val aid: Long,
    val cid: Long,
    val progressSec: Long,
    val atEpochSec: Long
)

/**
 * Pure heartbeat / progress-report policy, with wall-clock injected so it is testable.
 *
 * Rules extracted from `PlayerViewModel`:
 *  - heartbeat is sent every [intervalMs] while playing;
 *  - progress is only reported when it advanced by at least [minProgressAdvanceMs];
 *  - identical reports within [dedupWindowMs] are dropped;
 *  - history is reported only for non-bangumi archive videos (bangumi uses heartbeat type "4");
 *  - LIVE / LOCAL never report.
 */
data class HeartbeatPolicy(
    val intervalMs: Long = 15_000L,
    val minProgressAdvanceMs: Long = 1_000L,
    val dedupWindowMs: Long = 60_000L
) {

    /** Milliseconds until the next heartbeat is due. */
    fun nextDelay(lastSentAtMs: Long?, nowMs: Long): Long {
        val last = lastSentAtMs ?: return 0L
        return (last + intervalMs - nowMs).coerceAtLeast(0L)
    }

    /** Whether a heartbeat is due now (elapsed >= interval, or none sent yet). */
    fun shouldSend(lastSentAtMs: Long?, nowMs: Long): Boolean {
        val last = lastSentAtMs ?: return true
        return nowMs - last >= intervalMs
    }

    /**
     * Whether a progress-based report (history + heartbeat) should be issued.
     * Throttles to [intervalMs] and requires real progress advance.
     */
    fun shouldReportProgress(
        nowMs: Long,
        lastReportedAtMs: Long?,
        currentPositionMs: Long,
        lastReportedPositionMs: Long?
    ): Boolean {
        if (!shouldSend(lastReportedAtMs, nowMs)) return false
        val lastPos = lastReportedPositionMs ?: return true
        return currentPositionMs - lastPos >= minProgressAdvanceMs
    }

    /** Duplicate detection: same report identity seen within [dedupWindowMs] is dropped. */
    fun isDuplicate(nowMs: Long, lastReportedAtMs: Long?, next: HeartbeatReport, previous: HeartbeatReport?): Boolean {
        if (previous == null) return false
        val lastAt = lastReportedAtMs ?: return false
        if (nowMs - lastAt > dedupWindowMs) return false
        return previous.dedupKey == next.dedupKey
    }

    /** History is reported only for archive videos (never bangumi / live / local). */
    fun shouldReportHistory(identity: PlaybackIdentity): Boolean =
        identity.mediaType == PlaybackMediaType.VIDEO

    /**
     * Build the heartbeat payload. `type`/`subType`/`epid`/`sid` follow the bangumi rule:
     * bangumi → "4"/"1" + ids; anything else → "3"/null.
     */
    fun buildHeartbeat(
        identity: PlaybackIdentity,
        progressMs: Long,
        durationMs: Long,
        startTsSec: Long,
        nowSec: Long
    ): HeartbeatReport {
        val bangumi = identity.isBangumi
        val playedTimeSec = (progressMs / 1000).coerceAtLeast(0L)
        return HeartbeatReport(
            aid = identity.aid,
            bvid = identity.bvid,
            cid = identity.cid,
            playedTimeSec = playedTimeSec,
            videoDurationSec = (durationMs / 1000).coerceAtLeast(0L),
            startTsSec = startTsSec,
            realtimeSec = playedTimeSec,
            type = if (bangumi) "4" else "3",
            subType = if (bangumi) "1" else null,
            epid = if (bangumi) identity.epid else null,
            sid = if (bangumi) identity.sid else null,
            atEpochSec = nowSec
        )
    }

    /** Build the history-report payload, or null when not applicable. */
    fun buildHistory(identity: PlaybackIdentity, progressMs: Long, nowSec: Long): HistoryReport? {
        if (!shouldReportHistory(identity)) return null
        return HistoryReport(
            aid = identity.aid,
            cid = identity.cid,
            progressSec = (progressMs / 1000).coerceAtLeast(0L),
            atEpochSec = nowSec
        )
    }
}
