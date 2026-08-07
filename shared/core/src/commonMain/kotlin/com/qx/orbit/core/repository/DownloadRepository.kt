package com.qx.orbit.core.repository

import com.qx.orbit.core.download.DownloadTask
import com.qx.orbit.core.error.BizResult

/** 新建下载任务的请求参数（对应 Android `VideoDownloadManager.enqueue()` 入参）。 */
data class DownloadRequest(
    val url: String,
    val title: String,
    val filename: String,
    val aid: Long = 0,
    val cid: Long = 0,
    val bvid: String = "",
    val qn: Int = 0,
    /** MP4 / AUDIO_AND_SUBTITLE（对应 Android DownloadInfo.type）。 */
    val type: String = "MP4",
    /** 1 视频 / 2 Bangumi（对应 PlayerData.TYPE_VIDEO / TYPE_BANGUMI）。 */
    val mediaType: Int = 1,
    val coverUrl: String = "",
    val duration: Int = 0,
)

/**
 * 下载领域 repository（port）：任务持久化 / URL 刷新 / Range 恢复。
 *
 * 任务状态机与 Range 规则由 `download` 包实现（`DownloadStateMachine`/`RangeRule`/`DownloadFsPort`）；
 * 本 repository 是平台 adapter 对上层的操作入口。Android 保留 `VideoDownloadManager` 的文件 I/O，
 * Harmony 使用应用沙盒 / 系统媒体与后台任务。
 *
 * Android 映射（app/src/main/java/com/qx/orbit/bili/util/VideoDownloadManager.kt）：
 *  - 任务持久化：`enqueue()`/`pause()`/`resume()`/`getAllDownloads()`/`persistTasks()`（JSON 到 TASKS_FILE）
 *  - URL 刷新：`refreshUrlAndRetry()`（403 时经 PlayerApi 重新取流后重试）
 *  - Range 恢复：`executeDownload()`（Range: bytes={existingLength}-，206 续传，下载完成后附带弹幕/字幕）
 */
interface DownloadRepository {

    /** 全部任务（含已完成/失败，按 id 升序）。 */
    suspend fun tasks(): BizResult<List<DownloadTask>>

    /** 新建任务并开始下载；已存在相同 (aid,cid,type,qn) 的活动任务时返回其 id。 */
    suspend fun enqueue(request: DownloadRequest): BizResult<Long>

    /** 暂停。 */
    suspend fun pause(id: Long): BizResult<Unit>

    /** 恢复（从已下载字节续传）。 */
    suspend fun resume(id: Long): BizResult<Unit>

    /** 取消（取消进行中的请求，任务回到 IDLE）。 */
    suspend fun cancel(id: Long): BizResult<Unit>

    /** 删除任务（可选删除本地文件，平台实现决定）。 */
    suspend fun delete(id: Long, deleteFile: Boolean = false): BizResult<Unit>

    /** 刷新失效的取流 URL（403 等）并返回刷新后的任务。 */
    suspend fun refreshUrl(taskId: Long): BizResult<DownloadTask>

    /** 查询任务进度（`DownloadTask.downloadedBytes/totalBytes/progressFraction` 供 UI 与续传）。 */
    suspend fun progress(id: Long): BizResult<DownloadTask>
}
