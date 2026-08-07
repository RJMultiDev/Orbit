package com.qx.orbit.ohos.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.qx.orbit.ohos.data.DownloadEvent
import com.qx.orbit.ohos.data.DownloadFailureReason
import com.qx.orbit.ohos.data.DownloadState
import com.qx.orbit.ohos.data.DownloadStateMachine
import com.qx.orbit.ohos.data.DownloadTask
import com.qx.orbit.ohos.theme.OrbitColors
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.ButtonDefaults
import com.tencent.kuikly.compose.material3.LinearProgressIndicator
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import kotlinx.coroutines.delay

/**
 * 下载管理页（@Page("download")，对齐 Android DownloadManagerScreen + VideoDownloadForegroundScreen）。
 *
 * 本期为「内存演示」：任务列表与状态迁移完全在进程内完成，文件 I/O 与真实下载由 ArkTS 宿主承接。
 *
 * 对接宿主计划（TODO）：
 *  1. 任务持久化 / Range 续传 / URL 刷新 / 后台下载 → ArkTS 侧实现 shared/core 的
 *     DownloadRepository / DownloadFsPort 语义（harmony/entry 的 ets/core 或系统下载服务）。
 *  2. 宿主把引擎的进度/结果事件转成 [DownloadEvent] 喂给
 *     [DownloadStateMachine.transition]，UI 只读 [DownloadTask]（与 shared/core 同构，
 *     宿主就绪后仅替换本页的数据来源，见 data/DownloadModels.kt）。
 *  3. 状态文案对齐 Android：PENDING≈QUEUED（等待中）、RUNNING（正在下载）、
 *     PAUSED（已暂停）、SUCCESSFUL≈SUCCEEDED（已完成）、FAILED（下载失败）。
 *
 * 操作按钮：暂停（Pause）/ 继续、重试（Resume）/ 删除（Cancel，删除后回 IDLE 并从列表移除）。
 * 演示控制：「＋任务」入队演示任务，「模拟失败」用不可重试的 URL_EXPIRED 演示 FAILED 终态。
 */
@Page("download")
internal class DownloadPage : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            DownloadContent()
        }
    }
}

/** 演示任务总大小（≈45.8MB，仅用于模拟进度）。 */
private const val SIM_TOTAL_BYTES = 48_000_000L

/** 模拟推进速率（≈3.8MB/秒，约 12s 完成一个任务）。 */
private const val SIM_STEP_BYTES = 4_000_000L

@Composable
internal fun DownloadContent() {
    var tasks by remember { mutableStateOf(emptyList<DownloadTask>()) }
    var nextId by remember { mutableStateOf(0L) }

    // 模拟下载引擎：每 1s 推进一次状态机（QUEUED→RUNNING→…→SUCCEEDED）。
    // TODO(ArkTS 宿主)：真实下载由 ArkTS 侧承接（DownloadRepository/DownloadFsPort 语义），
    //  本循环删除后改为订阅宿主的任务状态回调，事件仍走 DownloadStateMachine。
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            var changed = false
            val next = tasks.map { t ->
                when (t.state) {
                    DownloadState.QUEUED -> {
                        changed = true
                        DownloadStateMachine.transition(t, DownloadEvent.Start)
                    }
                    DownloadState.RUNNING -> {
                        changed = true
                        DownloadStateMachine.transition(
                            t,
                            DownloadEvent.Progress(
                                downloadedBytes = t.downloadedBytes + SIM_STEP_BYTES,
                                totalBytes = t.totalBytes,
                            ),
                        )
                    }
                    else -> t
                }
            }
            if (changed) tasks = next
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(OrbitColors.Background)) {
        Text(
            text = "下载管理",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = OrbitColors.Primary,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        )

        // 演示控制行（真实版本移除：入队来自视频详情页，失败由引擎上报）。
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = {
                val task = DownloadStateMachine.transition(
                    DownloadTask(
                        id = nextId,
                        title = "演示视频 #$nextId",
                        totalBytes = SIM_TOTAL_BYTES,
                    ),
                    DownloadEvent.Enqueue,
                )
                nextId += 1
                tasks = tasks + task
            }) {
                Text(text = "＋任务", fontSize = 11.sp)
            }
            Button(onClick = {
                val target = tasks.firstOrNull {
                    it.state == DownloadState.RUNNING || it.state == DownloadState.QUEUED
                }
                if (target != null) {
                    // URL_EXPIRED 不可自动重试，一次到位进入 FAILED（演示用；
                    // 若要演示自动重试路径可用 NETWORK，retry 后回 QUEUED）。
                    tasks = applyEvent(tasks, target.id, DownloadEvent.Fail(DownloadFailureReason.URL_EXPIRED))
                }
            }) {
                Text(text = "模拟失败", fontSize = 11.sp)
            }
        }

        if (tasks.isEmpty()) {
            Text(
                text = "暂无下载任务\n点击「＋任务」添加演示任务",
                fontSize = 12.sp,
                color = OrbitColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(tasks) { task ->
                    DownloadTaskRow(
                        task = task,
                        onPause = { tasks = applyEvent(tasks, task.id, DownloadEvent.Pause) },
                        onResume = { tasks = applyEvent(tasks, task.id, DownloadEvent.Resume) },
                        onDelete = {
                            // 状态机先走 Cancel→IDLE，演示页随后移除；
                            // TODO(ArkTS 宿主)：真实删除还需清理本地文件（DownloadFsPort.delete 语义）。
                            tasks = tasks
                                .map { if (it.id == task.id) DownloadStateMachine.transition(it, DownloadEvent.Cancel) else it }
                                .filterNot { it.id == task.id }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadTaskRow(
    task: DownloadTask,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .background(OrbitColors.CardBackground, RoundedCornerShape(12.dp))
            .padding(10.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = task.title,
                fontSize = 12.sp,
                color = OrbitColors.OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = statusText(task),
                fontSize = 11.sp,
                color = statusColor(task),
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        // Kuikly material3 有 LinearProgressIndicator（M3 1.2 lambda 进度形态）；
        // 若真机渲染异常，可退化为 Box 宽度百分比实现（外层 Box + fillMaxWidth * fraction）。
        LinearProgressIndicator(
            progress = { task.progressFraction },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
            color = OrbitColors.Primary,
            trackColor = OrbitColors.SurfaceContainerHigh,
        )
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = detailText(task),
                fontSize = 11.sp,
                color = OrbitColors.TextSecondary,
                modifier = Modifier.weight(1f),
            )
            when (task.state) {
                DownloadState.QUEUED, DownloadState.RUNNING -> {
                    ActionButton(label = "暂停", onClick = onPause, primary = true)
                    ActionButton(label = "删除", onClick = onDelete, primary = false)
                }
                DownloadState.PAUSED -> {
                    ActionButton(label = "继续", onClick = onResume, primary = true)
                    ActionButton(label = "删除", onClick = onDelete, primary = false)
                }
                DownloadState.FAILED -> {
                    ActionButton(label = "重试", onClick = onResume, primary = true)
                    ActionButton(label = "删除", onClick = onDelete, primary = false)
                }
                else -> {
                    ActionButton(label = "删除", onClick = onDelete, primary = false)
                }
            }
        }
    }
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit, primary: Boolean) {
    Button(
        onClick = onClick,
        modifier = Modifier.padding(start = 6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) OrbitColors.Primary else OrbitColors.SurfaceContainerHigh,
            contentColor = if (primary) OrbitColors.OnPrimary else OrbitColors.OnSurface,
        ),
    ) {
        Text(text = label, fontSize = 11.sp)
    }
}

private fun statusText(task: DownloadTask): String = when (task.state) {
    DownloadState.IDLE -> "未开始"
    DownloadState.QUEUED -> "等待中…"
    DownloadState.RUNNING -> "正在下载 ${(task.progressFraction * 100).toInt()}%"
    DownloadState.PAUSED -> "已暂停"
    DownloadState.FAILED -> "下载失败"
    DownloadState.SUCCEEDED -> "已完成"
}

private fun statusColor(task: DownloadTask): Color = when (task.state) {
    DownloadState.FAILED -> OrbitColors.ErrorRed
    DownloadState.SUCCEEDED -> OrbitColors.SuccessGreen
    DownloadState.RUNNING -> OrbitColors.Primary
    else -> OrbitColors.TextSecondary
}

/** 第二行细节：进度字节 / 失败原因 / 完成大小。 */
private fun detailText(task: DownloadTask): String = when (task.state) {
    DownloadState.RUNNING, DownloadState.PAUSED, DownloadState.QUEUED ->
        if (task.totalBytes > 0L) {
            "${formatBytes(task.downloadedBytes)} / ${formatBytes(task.totalBytes)}"
        } else {
            ""
        }
    DownloadState.FAILED -> task.failureReason?.let { failureText(it) } ?: "下载失败"
    DownloadState.SUCCEEDED -> formatBytes(task.totalBytes)
    else -> ""
}

private fun failureText(reason: DownloadFailureReason): String = when (reason) {
    DownloadFailureReason.NETWORK -> "网络错误"
    DownloadFailureReason.IO -> "写入失败"
    DownloadFailureReason.INTERRUPTED -> "下载中断"
    DownloadFailureReason.HTTP -> "HTTP 错误"
    DownloadFailureReason.URL_EXPIRED -> "播放地址过期"
}

/** 字节格式化（等价 Android String.format("%.1fMB")，避开 java.lang.String.format）。 */
private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> {
            val tenths = bytes * 10 / (1024 * 1024)
            "${tenths / 10}.${tenths % 10}MB"
        }
        bytes >= 1024 -> {
            val tenths = bytes * 10 / 1024
            "${tenths / 10}.${tenths % 10}KB"
        }
        else -> "$bytes B"
    }
}

private fun applyEvent(tasks: List<DownloadTask>, id: Long, event: DownloadEvent): List<DownloadTask> =
    tasks.map { if (it.id == id) DownloadStateMachine.transition(it, event) else it }
