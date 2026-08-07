package com.qx.orbit.core.download

/**
 * Half-open byte range for HTTP Range requests.
 *
 * [start] is the first byte to fetch (resume offset); [endInclusive] is the last byte
 * requested, or null for an open-ended range ("bytes=start-").
 */
data class ByteRange(
    val start: Long,
    val endInclusive: Long? = null
) {
    init {
        require(start >= 0L) { "range start must be >= 0, was $start" }
        if (endInclusive != null) {
            require(endInclusive >= start) { "range end must be >= start" }
        }
    }

    val isResume: Boolean get() = start > 0L

    /** Bytes covered by this range (endInclusive - start + 1), or null when open-ended. */
    val length: Long? get() = endInclusive?.let { it - start + 1 }

    fun toHeader(): String =
        if (endInclusive == null) "bytes=$start-" else "bytes=$start-$endInclusive"
}

/**
 * Pure HTTP Range / resume logic.
 *
 * Rules extracted from the Android `VideoDownloadManager`:
 *  - an existing local file of length N resumes at offset N ("bytes=N-");
 *  - a fresh file requests "bytes=0-";
 *  - a 206 response means the server honored the resume;
 *  - when resuming, the total length is `existing + contentLength`.
 */
object RangeRule {

    /** Resume offset = bytes already persisted locally (never negative). */
    fun resumeOffset(existingBytes: Long): Long = existingBytes.coerceAtLeast(0L)

    /** Build the range for a request given already-downloaded bytes. */
    fun buildRange(existingBytes: Long): ByteRange {
        val start = resumeOffset(existingBytes)
        return ByteRange(start = start)
    }

    /** Convenience header builder for the common resume case. */
    fun buildHeader(existingBytes: Long): String = buildRange(existingBytes).toHeader()

    /** RFC 7233 status code meaning "partial content" — server honored the Range. */
    fun isPartialContent(statusCode: Int): Boolean = statusCode == 206

    /**
     * Total file size after a response:
     *  - 206 (resume honored): existingBytes + body contentLength;
     *  - 200 (fresh full body): body contentLength.
     */
    fun totalBytesAfterResponse(existingBytes: Long, statusCode: Int, contentLength: Long): Long {
        val len = contentLength.coerceAtLeast(0L)
        return if (isPartialContent(statusCode)) resumeOffset(existingBytes) + len else len
    }

    /** Offset where the next write should start (0 for a fresh 200 response). */
    fun writeOffset(existingBytes: Long, statusCode: Int): Long =
        if (isPartialContent(statusCode)) resumeOffset(existingBytes) else 0L

    /** A task whose local bytes already reach the known total is complete. */
    fun isAlreadyComplete(existingBytes: Long, totalBytes: Long): Boolean =
        totalBytes > 0L && existingBytes >= totalBytes
}
