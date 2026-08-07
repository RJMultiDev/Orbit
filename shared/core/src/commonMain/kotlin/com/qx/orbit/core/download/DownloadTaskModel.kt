package com.qx.orbit.core.download

/**
 * Domain model of a download task.
 *
 * Pure data — no platform I/O, no Android/Java imports. The [range] field records the
 * byte range requested for the current attempt; the actual persistence is done via
 * [DownloadFsPort].
 */
data class DownloadTask(
    val id: Long,
    val url: String = "",
    val range: ByteRange? = null,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val state: DownloadState = DownloadState.IDLE,
    val retryCount: Int = 0,
    val urlRefreshCount: Int = 0,
    val failureReason: DownloadFailureReason? = null,
    val localUri: String? = null
) {
    /** Fractional progress in [0f, 1f]; 0f while total is unknown. */
    val progressFraction: Float
        get() = if (totalBytes > 0L) {
            (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val isComplete: Boolean
        get() = state == DownloadState.SUCCEEDED
}
