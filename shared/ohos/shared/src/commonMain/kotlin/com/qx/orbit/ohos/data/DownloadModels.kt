package com.qx.orbit.ohos.data

/**
 * 下载任务领域模型 + 纯状态机（OHOS 端镜像）。
 *
 * 权威定义在 `shared/core` 的 `com.qx.orbit.core.download` 包
 * （DownloadStateMachine / DownloadTaskModel / RangeRule / DownloadFsPort）。
 * `shared/ohos` 是独立 Gradle 构建（settings.gradle.kts 只 include :shared，不依赖 :core），
 * 因此把「状态枚举 + 迁移规则」原样镜像到这里，保证与共享领域保持一致。
 *
 * 对接宿主计划（TODO，见 pages/DownloadPage.kt）：
 *  - 文件 I/O、Range 续传、URL 刷新、后台下载由 ArkTS 宿主承接（对应 shared/core 的
 *    DownloadFsPort / DownloadRepository 语义；Android 映射见 DownloadRepository.kt 的 KDoc）。
 *  - UI 只读 [DownloadTask]；宿主把真实进度/结果转成 [DownloadEvent] 喂给
 *    [DownloadStateMachine.transition] 即可驱动本状态机，无需改动 UI。
 *  - 与 shared/core 的差异：`url` / `range`（ByteRange 断点续传）由宿主管理，未移植；
 *    另增加 [DownloadTask.title] 供 UI 展示（core 里 title 在 DownloadRequest 上）。
 */

/** 下载任务生命周期状态（镜像 shared/core DownloadState）。 */
enum class DownloadState {
    /** 已创建但未入队 / 取消后的占位终态。 */
    IDLE,

    /** 已入队；等待开始传输。 */
    QUEUED,

    /** 传输进行中（字节流动）。 */
    RUNNING,

    /** 用户暂停，可继续。 */
    PAUSED,

    /** 终态失败，可手动重试（原因可恢复时）。 */
    FAILED,

    /** 终态成功，文件完整。 */
    SUCCEEDED,
}

/** 失败原因（对齐 shared/core DownloadFailureReason）。 */
enum class DownloadFailureReason {
    /** 网络 / 传输错误。 */
    NETWORK,

    /** 本地写入错误。 */
    IO,

    /** 进程被杀 / 应用重启中断。 */
    INTERRUPTED,

    /** 非 2xx/206 的 HTTP 状态。 */
    HTTP,

    /** 取流 URL 过期（403），刷新失败。 */
    URL_EXPIRED,
}

/** 驱动下载状态机的事件（对齐 shared/core DownloadEvent）。 */
sealed interface DownloadEvent {
    /** 新任务入队。 */
    data object Enqueue : DownloadEvent

    /** 传输开始（或排队重试后重启）。 */
    data object Start : DownloadEvent

    /** 进度推进。 */
    data class Progress(val downloadedBytes: Long, val totalBytes: Long) : DownloadEvent

    /** 用户暂停。 */
    data object Pause : DownloadEvent

    /** 用户恢复（暂停后继续，或失败后重试）。 */
    data object Resume : DownloadEvent

    /** 任务取消 / 删除。 */
    data object Cancel : DownloadEvent

    /** 传输或预检失败。 */
    data class Fail(val reason: DownloadFailureReason, val httpCode: Int? = null) : DownloadEvent

    /** 传输成功完成。 */
    data object Succeed : DownloadEvent

    /** 取流 URL 失效（403 / 空），刷新后重试。 */
    data object RefreshUrl : DownloadEvent

    /** 应用重启后恢复持久化任务。 */
    data object Restore : DownloadEvent
}

/** 状态机调优参数（对齐 shared/core DownloadConfig）。 */
data class DownloadConfig(
    val maxRetries: Int = 3,
    val maxUrlRefreshes: Int = 2,
)

/** 下载任务（镜像 shared/core DownloadTask；[title] 为 OHOS UI 扩展）。 */
data class DownloadTask(
    val id: Long,
    val title: String = "",
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val state: DownloadState = DownloadState.IDLE,
    val retryCount: Int = 0,
    val urlRefreshCount: Int = 0,
    val failureReason: DownloadFailureReason? = null,
    val localUri: String? = null,
) {
    /** [0f, 1f] 的进度比例；total 未知时为 0f。 */
    val progressFraction: Float
        get() = if (totalBytes > 0L) {
            (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val isComplete: Boolean
        get() = state == DownloadState.SUCCEEDED
}

/**
 * 纯迁移规则（镜像 shared/core DownloadStateMachine，逻辑一致）：
 * 返回按事件迁移后的任务副本；非法 / 未处理的「事件×状态」对被忽略（原样返回）。
 * 无副作用——宿主拿到结果后再决定做什么（落盘、起后台任务、通知引擎）。
 */
object DownloadStateMachine {

    fun transition(task: DownloadTask, event: DownloadEvent, config: DownloadConfig = DownloadConfig()): DownloadTask {
        return when (task.state) {
            DownloadState.IDLE -> idle(task, event)
            DownloadState.QUEUED -> queued(task, event, config)
            DownloadState.RUNNING -> running(task, event, config)
            DownloadState.PAUSED -> paused(task, event)
            DownloadState.FAILED -> failed(task, event, config)
            DownloadState.SUCCEEDED -> succeeded(task, event)
        }
    }

    /** 进程死亡 / 应用重启后恢复：RUNNING / QUEUED 视为 INTERRUPTED 失败，其余保留。 */
    fun restore(task: DownloadTask): DownloadTask {
        return when (task.state) {
            DownloadState.RUNNING, DownloadState.QUEUED ->
                task.copy(state = DownloadState.FAILED, failureReason = DownloadFailureReason.INTERRUPTED)
            else -> task
        }
    }

    /** 该失败原因是否允许自动重试。 */
    fun isRetryable(reason: DownloadFailureReason): Boolean = reason != DownloadFailureReason.URL_EXPIRED

    private fun idle(task: DownloadTask, event: DownloadEvent): DownloadTask = when (event) {
        DownloadEvent.Enqueue -> task.copy(state = DownloadState.QUEUED, retryCount = 0)
        else -> task
    }

    private fun queued(task: DownloadTask, event: DownloadEvent, config: DownloadConfig): DownloadTask = when (event) {
        DownloadEvent.Start -> task.copy(state = DownloadState.RUNNING)
        DownloadEvent.Pause -> task.copy(state = DownloadState.PAUSED)
        DownloadEvent.Cancel -> task.copy(state = DownloadState.IDLE)
        is DownloadEvent.Fail -> fail(task, event, config)
        DownloadEvent.RefreshUrl -> refreshUrl(task, config)
        else -> task
    }

    private fun running(task: DownloadTask, event: DownloadEvent, config: DownloadConfig): DownloadTask = when (event) {
        is DownloadEvent.Progress -> progress(task, event)
        DownloadEvent.Pause -> task.copy(state = DownloadState.PAUSED)
        DownloadEvent.Cancel -> task.copy(state = DownloadState.IDLE)
        is DownloadEvent.Fail -> fail(task, event, config)
        DownloadEvent.Succeed -> task.copy(state = DownloadState.SUCCEEDED, retryCount = 0)
        DownloadEvent.RefreshUrl -> refreshUrl(task, config)
        else -> task
    }

    private fun paused(task: DownloadTask, event: DownloadEvent): DownloadTask = when (event) {
        DownloadEvent.Resume -> task.copy(state = DownloadState.RUNNING)
        // 重新入队已暂停任务 = 重启传输（对齐旧版 enqueue 行为）。
        DownloadEvent.Enqueue -> task.copy(state = DownloadState.RUNNING)
        DownloadEvent.Cancel -> task.copy(state = DownloadState.IDLE)
        else -> task
    }

    private fun failed(task: DownloadTask, event: DownloadEvent, config: DownloadConfig): DownloadTask = when (event) {
        // 手动从 FAILED 恢复：重试预算耗尽后按用户意图清零重试计数。
        DownloadEvent.Resume -> task.copy(
            state = DownloadState.RUNNING,
            retryCount = if (task.retryCount >= config.maxRetries) 0 else task.retryCount,
        )
        DownloadEvent.Enqueue -> task.copy(state = DownloadState.QUEUED, retryCount = 0)
        DownloadEvent.Cancel -> task.copy(state = DownloadState.IDLE)
        else -> task
    }

    private fun succeeded(task: DownloadTask, event: DownloadEvent): DownloadTask = when (event) {
        DownloadEvent.Cancel -> task.copy(state = DownloadState.IDLE)
        else -> task
    }

    private fun progress(task: DownloadTask, event: DownloadEvent.Progress): DownloadTask {
        val downloaded = event.downloadedBytes.coerceAtLeast(0L)
        val total = event.totalBytes.coerceAtLeast(0L)
        val completed = total > 0L && downloaded >= total
        return task.copy(
            downloadedBytes = downloaded,
            totalBytes = total,
            state = if (completed) DownloadState.SUCCEEDED else DownloadState.RUNNING,
        )
    }

    private fun fail(task: DownloadTask, event: DownloadEvent.Fail, config: DownloadConfig): DownloadTask {
        val reason = event.reason
        val retryable = isRetryable(reason) && task.retryCount < config.maxRetries
        return if (retryable) {
            task.copy(
                state = DownloadState.QUEUED,
                retryCount = task.retryCount + 1,
                failureReason = reason,
            )
        } else {
            task.copy(state = DownloadState.FAILED, failureReason = reason)
        }
    }

    private fun refreshUrl(task: DownloadTask, config: DownloadConfig): DownloadTask {
        return if (task.urlRefreshCount < config.maxUrlRefreshes) {
            task.copy(
                state = DownloadState.QUEUED,
                urlRefreshCount = task.urlRefreshCount + 1,
            )
        } else {
            task.copy(
                state = DownloadState.FAILED,
                failureReason = DownloadFailureReason.URL_EXPIRED,
            )
        }
    }
}
