package com.qx.orbit.core.repository

import com.qx.orbit.core.danmaku.DanmakuEvent
import com.qx.orbit.core.error.BizResult

/** 弹幕分段（seg.so 按 segment_index 分片，对应 Android `DmSegMobileReply`）。 */
data class DanmakuSegment(
    val index: Int = 1,
    val elements: List<DanmakuEvent> = emptyList(),
)

/** 发送弹幕结果。 */
data class SendDanmakuResult(
    val dmid: Long = 0,
    val code: Int = 0,
)

/**
 * 弹幕领域 repository：弹幕获取（seg.so protobuf）、发送、点赞、撤回。
 *
 * 协议解析（seg.so protobuf → [DanmakuEvent]）与 XML/JSON 弹幕文件解析由
 * `danmaku.DanmakuParser` 及后续 danmaku 包实现；本 repository 只负责取流与组装。
 *
 * Android 映射（app/src/main/java/com/qx/orbit/bili/data/api/）：
 *  - 分段获取：`DanmakuApi.getVideoDanmakuSegments()`/`getVideoDanmakuSegment()`
 *    （GET /x/v2/dm/wbi/web/seg.so，WBI 签名，protobuf 解析）、`getVideoDanmakuSegmentCount()`（/x/v2/dm/web/view）
 *  - 发送：`DanmakuApi.sendDanmaku()`（BiliApiService.sendDanmaku，POST /x/v2/dm/post）
 *  - 点赞：`DanmakuApi.likeDanmaku()`（POST /x/v2/dm/thumbup/add）
 *  - 撤回：`DanmakuApi.recallDanmaku()`（POST /x/dm/recall）
 */
interface DanmakuRepository {

    /** 拉取全部弹幕分段（内部先查段数，再并发拉取）。 */
    suspend fun danmakuSegments(aid: Long, cid: Long): BizResult<List<DanmakuSegment>>

    /** 拉取单个分段（index 从 1 开始）。 */
    suspend fun danmakuSegment(aid: Long, cid: Long, segmentIndex: Int): BizResult<DanmakuSegment?>

    /** 发送弹幕，返回服务端 dmid。 */
    suspend fun sendDanmaku(
        cid: Long,
        msg: String,
        bvid: String,
        progress: Int,
        color: Int = 0xFFFFFF,
        mode: Int = 1,
    ): BizResult<SendDanmakuResult>

    /** 点赞/取消点赞弹幕（op：1 赞 / 2 取消）。 */
    suspend fun likeDanmaku(dmid: Long, cid: Long, op: Int): BizResult<Unit>

    /** 撤回自己的弹幕。 */
    suspend fun recallDanmaku(dmid: Long, cid: Long): BizResult<Unit>
}
