package com.qx.orbit.core.repository

import com.qx.orbit.core.error.BizResult
import com.qx.orbit.core.mediaselection.DashAudioTrack
import com.qx.orbit.core.mediaselection.DashTrackList
import com.qx.orbit.core.mediaselection.DashVideoTrack
import com.qx.orbit.core.mediaselection.DurlSource
import com.qx.orbit.core.model.content.History
import com.qx.orbit.core.subtitle.SubtitleCue

/** 取流结果（对应 Android `PlayerApi.getVideoDash/getVideo/getBangumi` 填充的 PlayerData 网络部分）。 */
data class PlayUrl(
    val quality: Int = 0,
    val timelength: Int = 0,
    val acceptQuality: List<Int> = emptyList(),
    val acceptDescription: List<String> = emptyList(),
    val dash: DashTrackList? = null,
    /** MP4 直链（durl 模式）。 */
    val mp4: DurlSource? = null,
    val videoProject: Boolean = false,
)

/** 字幕轨道（对应 Android `SubtitleLink`；正文为 [SubtitleCue]）。 */
data class SubtitleLink(
    val id: Long = 0,
    val isAI: Boolean = false,
    val lang: String = "",
    val url: String = "",
)

/** 剧情看点/分镜（对应 Android `ViewPoint`）。 */
data class ViewPoint(
    val content: String = "",
    val from: Int = 0,
    val to: Int = 0,
    val type: Int = 0,
    val imgUrl: String = "",
    val logoUrl: String = "",
)

/** 高能进度条（对应 Android `HighEnergyData`）。 */
data class HighEnergyData(
    val stepSec: Int = 0,
    val tagStr: String = "",
    val events: FloatArray = floatArrayOf(),
)

/**
 * 播放领域 repository：取流（playurl）、DASH、字幕、看点、高能、历史进度。
 *
 * Android 映射（app/src/main/java/com/qx/orbit/bili/data/api/）：
 *  - DASH 取流：`PlayerApi.getVideoDash()`/`getVideo()`（BiliApiService.getPlayUrl，GET /x/player/wbi/playurl，WBI 签名）
 *  - Bangumi 取流：`PlayerApi.getBangumi()`（GET /pgc/player/web/playurl）
 *  - 字幕/看点/互动图/进度：`PlayerApi.getSubtitleLinks()`/`getViewPoints()`/`getHistoryProgress()`
 *    （BiliApiService.getPlayerV2，/x/player/wbi/v2）
 *  - 字幕正文：`PlayerApi.getSubtitle()`/`getSubtitleRaw()`（subtitle_url 直取，[SubtitleParser] 解析）
 *  - 高能：`PlayerApi.getHighEnergyData()`（bvc.bilivideo.com /pbp/data）
 *  - 观看人数：`VideoInfoApi.getWatching()`（BiliApiService.getWatching，/x/player/online/total）
 *  - 选流纯算法由 `mediaselection.DashStreamSelector` 提供，本 repository 只负责取流与 DTO 组装。
 */
interface PlayerRepository {

    /**
     * 取流（DASH 优先；forceMp4=true 强制 MP4/durl）。
     * 参数对应 Android `PlayerData` 的 aid/cid/qn/audioQn/type。
     */
    suspend fun playUrl(
        aid: Long,
        cid: Long,
        qn: Int,
        audioQn: Int = -1,
        forceMp4: Boolean = false,
        tryLook: Boolean = false,
        isBangumi: Boolean = false,
    ): BizResult<PlayUrl>

    /** 字幕轨道列表（含 AI 字幕标识）。 */
    suspend fun subtitleLinks(aid: Long, cid: Long): BizResult<List<SubtitleLink>>

    /** 剧情看点。 */
    suspend fun viewPoints(aid: Long, cid: Long): BizResult<List<ViewPoint>>

    /** 播放历史进度（无则返回 null）。 */
    suspend fun historyProgress(aid: Long, cid: Long): BizResult<History?>

    /** 拉取字幕正文。 */
    suspend fun subtitle(url: String): BizResult<List<SubtitleCue>>

    /** 高能进度条数据。 */
    suspend fun highEnergy(cid: Long, aid: Long): BizResult<HighEnergyData?>

    /** 正在观看人数。 */
    suspend fun watching(aid: Long, cid: Long): BizResult<String>
}
