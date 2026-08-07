package com.qx.orbit.core.download

/**
 * Port (contract) for platform file I/O backing download tasks.
 *
 * commonMain never touches `java.io` / `android.*`; platform adapters (Android
 * OkHttp+files, Harmony sandbox, tests) implement this interface.
 */
interface DownloadFsPort {

    /** Bytes already persisted for [task], 0 when absent or unknown. */
    suspend fun existingBytes(task: DownloadTask): Long

    /** Append [bytes] starting at [offset]; returns bytes actually written. */
    suspend fun write(task: DownloadTask, offset: Long, bytes: ByteArray): Long

    /**
     * Verify persisted bytes against the expected total. Returns a detailed result so the
     * caller can decide to resume, retry, or mark complete.
     */
    suspend fun verify(task: DownloadTask): VerifyResult

    /** Persist task metadata so it survives process death. */
    suspend fun persist(task: DownloadTask)

    /** Finalize a successful task, returning the local URI of the completed file. */
    suspend fun commit(task: DownloadTask, localUri: String)

    /** Delete all artifacts of [task]. */
    suspend fun delete(task: DownloadTask)
}

/** Result of [DownloadFsPort.verify]. */
data class VerifyResult(
    val ok: Boolean,
    val actualBytes: Long = 0L,
    val expectedBytes: Long? = null,
    val message: String? = null
)
