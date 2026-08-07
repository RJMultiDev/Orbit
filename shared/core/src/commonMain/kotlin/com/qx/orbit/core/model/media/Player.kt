package com.qx.orbit.core.model.media

import com.qx.orbit.core.model.content.Wire
import kotlin.math.abs

/**
 * 播放/直播拉流领域模型（组 2）。
 *
 * 对应 Android `data/model/Player.kt` 的 `PlayerData` / `DashData` 等：
 * 去掉 Gson 注解、`JSONObject` 与 `Array`/`IntArray`，字段统一为平台无关类型；
 * 通过 [PlayerData.fromWire] 等工厂从 B 站播放接口（`x/player/playurl` 等）原始字段构造。
 */

data class PlayerData(
    val title: String = "",
    val videoUrl: String = "",
    val danmakuUrl: String = "",
    val qn: Int = -1,
    val audioQn: Int = -1,
    val qnStrList: List<String>? = null,
    val qnValueList: List<Int>? = null,
    val aid: Long = 0,
    val bvid: String = "",
    val cid: Long = 0,
    val mid: Long = 0,
    val sid: Long = 0,
    val progress: Int = 0,
    val cidHistory: Long = 0,
    val type: Int = TYPE_VIDEO,
    val epid: Long = 0,
    val timeStamp: Long = 0,
    val pagenames: List<String> = emptyList(),
    val cids: List<Long> = emptyList(),
    val epids: List<Long> = emptyList(),
    val aids: List<Long> = emptyList(),
    val currentPageIndex: Int = 0,
    val dashData: DashData? = null,
    val audioUrl: String = "",
    val cover: String = ""
) {
    companion object {
        const val TYPE_VIDEO = 0
        const val TYPE_BANGUMI = 1
        const val TYPE_LIVE = 2
        const val TYPE_LOCAL = 4

        fun fromWire(fields: Map<String, Any?>): PlayerData {
            val durl = Wire.mapList(fields, "durl")
            val pages = Wire.mapList(fields, "pages")
            val pagenames = if (fields.containsKey("pagenames")) {
                Wire.strList(fields, "pagenames")
            } else {
                pages.map { Wire.str(it, "part") }
            }
            val cids = if (fields.containsKey("cids")) {
                Wire.longList(fields, "cids")
            } else {
                pages.map { Wire.long(it, "cid") }
            }
            val epList = Wire.mapList(fields, "ep_list", "episodes")
            val epids = if (fields.containsKey("epids")) {
                Wire.longList(fields, "epids")
            } else {
                epList.map { Wire.long(it, "id", "epid") }
            }
            val aids = if (fields.containsKey("aids")) {
                Wire.longList(fields, "aids")
            } else {
                epList.map { Wire.long(it, "aid") }
            }
            return PlayerData(
                title = Wire.str(fields, "title"),
                videoUrl = Wire.str(fields, "videoUrl", "video_url", "url").ifEmpty {
                    durl.firstOrNull()?.let { Wire.str(it, "url") }.orEmpty()
                },
                danmakuUrl = Wire.str(fields, "danmakuUrl", "danmaku_url", "dm_url"),
                qn = Wire.int(fields, "quality", "qn").let {
                    if (fields.containsKey("quality") || fields.containsKey("qn")) it else -1
                },
                audioQn = Wire.int(fields, "audioQn", "audio_qn", "audio_quality").let {
                    if (fields.containsKey("audioQn") || fields.containsKey("audio_qn") || fields.containsKey("audio_quality")) it else -1
                },
                qnStrList = Wire.strList(fields, "accept_description").takeIf { fields.containsKey("accept_description") },
                qnValueList = Wire.intList(fields, "accept_quality").takeIf { fields.containsKey("accept_quality") },
                aid = Wire.long(fields, "aid"),
                bvid = Wire.str(fields, "bvid"),
                cid = Wire.long(fields, "cid"),
                mid = Wire.long(fields, "mid"),
                sid = Wire.long(fields, "sid"),
                progress = Wire.int(fields, "progress"),
                cidHistory = Wire.long(fields, "cidHistory", "cid_history"),
                type = Wire.int(fields, "type"),
                epid = Wire.long(fields, "epid"),
                timeStamp = Wire.long(fields, "timeStamp", "timestamp", "time"),
                pagenames = pagenames,
                cids = cids,
                epids = epids,
                aids = aids,
                currentPageIndex = Wire.int(fields, "currentPageIndex", "current_page_index", "page"),
                dashData = Wire.map(fields, "dash").let { it?.let(DashData::fromWire) },
                audioUrl = Wire.str(fields, "audioUrl", "audio_url"),
                cover = Wire.str(fields, "cover", "pic")
            )
        }
    }
}

/** DASH 播放信息（对应 Android `DashData`，保留其选流辅助逻辑）。 */
data class DashData(
    val duration: Int = 0,
    val minBufferTime: Double = 0.0,
    val videoStreams: List<DashVideoStream> = emptyList(),
    val audioStreams: List<DashAudioStream> = emptyList(),
    val dolbyAudio: DashAudioStream? = null,
    val flacAudio: DashAudioStream? = null
) {
    /** 选一条不超过 [qn] 的 H.264（codecid==7）视频流，找不到则就近回落。 */
    fun getVideoStream(qn: Int): DashVideoStream? {
        val matches = videoStreams.filter { it.id == qn }
        val exact = matches.find { it.codecid == 7 } ?: matches.firstOrNull()
        if (exact != null) return exact

        val fallbackStreams = videoStreams.filter { it.codecid == 7 }.takeIf { it.isNotEmpty() } ?: videoStreams
        return fallbackStreams.filter { it.id <= qn }.maxByOrNull { it.id }
            ?: fallbackStreams.minByOrNull { abs(it.id - qn) }
    }

    /** 优先 FLAC，其次杜比，最后选码率最高的普通音轨。 */
    fun getBestAudioStream(): DashAudioStream? {
        if (flacAudio != null) return flacAudio
        if (dolbyAudio != null) return dolbyAudio
        return audioStreams.maxByOrNull { it.bandwidth }
    }

    /** 按 qn 取音轨；30250=杜比、30251=FLAC，否则回落最佳音轨。 */
    fun getAudioStream(qn: Int): DashAudioStream? {
        if (qn == 30251 && flacAudio != null) return flacAudio
        if (qn == 30250 && dolbyAudio != null) return dolbyAudio
        return audioStreams.find { it.id == qn } ?: getBestAudioStream()
    }

    companion object {
        fun fromWire(fields: Map<String, Any?>): DashData = DashData(
            duration = Wire.int(fields, "duration"),
            minBufferTime = when (val raw = fields["minBufferTime"]) {
                is Number -> raw.toDouble()
                is String -> raw.toDoubleOrNull() ?: 0.0
                else -> 0.0
            },
            videoStreams = Wire.mapList(fields, "video").map { DashVideoStream.fromWire(it) },
            audioStreams = Wire.mapList(fields, "audio").map { DashAudioStream.fromWire(it) },
            dolbyAudio = Wire.map(fields, "dolby")?.let { dolby ->
                Wire.mapList(dolby, "audio").firstOrNull()?.let { DashAudioStream.fromWire(it) }
            },
            flacAudio = Wire.map(fields, "flac")?.let { flac ->
                Wire.mapList(flac, "audio").firstOrNull()?.let { DashAudioStream.fromWire(it) }
            }
        )
    }
}

/** DASH 视频流（对应 Android `DashVideoStream`）。 */
data class DashVideoStream(
    val id: Int = 0,
    val baseUrl: String = "",
    val backupUrl: List<String> = emptyList(),
    val bandwidth: Long = 0,
    val mimeType: String = "",
    val codecs: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val frameRate: String = "",
    val codecid: Int = 0
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): DashVideoStream = DashVideoStream(
            id = Wire.int(fields, "id"),
            baseUrl = Wire.str(fields, "baseUrl", "base_url"),
            backupUrl = Wire.strList(fields, "backupUrl", "backup_url"),
            bandwidth = Wire.long(fields, "bandwidth"),
            mimeType = Wire.str(fields, "mimeType", "mime_type"),
            codecs = Wire.str(fields, "codecs"),
            width = Wire.int(fields, "width"),
            height = Wire.int(fields, "height"),
            frameRate = Wire.str(fields, "frameRate", "frame_rate"),
            codecid = Wire.int(fields, "codecid")
        )
    }
}

/** DASH 音频流（对应 Android `DashAudioStream`）。 */
data class DashAudioStream(
    val id: Int = 0,
    val baseUrl: String = "",
    val backupUrl: List<String> = emptyList(),
    val bandwidth: Long = 0,
    val mimeType: String = "",
    val codecs: String = "",
    val codecid: Int = 0
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): DashAudioStream = DashAudioStream(
            id = Wire.int(fields, "id"),
            baseUrl = Wire.str(fields, "baseUrl", "base_url"),
            backupUrl = Wire.strList(fields, "backupUrl", "backup_url"),
            bandwidth = Wire.long(fields, "bandwidth"),
            mimeType = Wire.str(fields, "mimeType", "mime_type"),
            codecs = Wire.str(fields, "codecs"),
            codecid = Wire.int(fields, "codecid")
        )
    }
}
