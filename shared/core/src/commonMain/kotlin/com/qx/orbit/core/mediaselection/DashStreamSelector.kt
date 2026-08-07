package com.qx.orbit.core.mediaselection

/**
 * B 站 DASH 视频轨道。
 * 字段语义与 Android 侧 `DashVideoStream` 对齐（quality id / bandwidth / codecs / dimensions）。
 */
data class DashVideoTrack(
    val id: Int,
    val baseUrl: String,
    val backupUrls: List<String> = emptyList(),
    val bandwidth: Long = 0,
    val codecs: String = "",
    val mimeType: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val frameRate: String = "",
    val codecId: Int = 0
)

/** B 站 DASH 音频轨道。 */
data class DashAudioTrack(
    val id: Int,
    val baseUrl: String,
    val backupUrls: List<String> = emptyList(),
    val bandwidth: Long = 0,
    val codecs: String = "",
    val mimeType: String = "",
    val codecId: Int = 0
)

/** 一次播放请求拿到的全部可用轨道。 */
data class DashTrackList(
    val videoTracks: List<DashVideoTrack> = emptyList(),
    val audioTracks: List<DashAudioTrack> = emptyList(),
    val dolbyTrack: DashAudioTrack? = null,
    val flacTrack: DashAudioTrack? = null
)

/**
 * 选流目标。
 *
 * @param preferredVideoQuality 首选清晰度（qn，如 80/64/32…）
 * @param preferredAudioQuality 首选音频 id；-1 表示不指定（用最优）；30250 = Dolby，30251 = FLAC
 * @param bandwidthBudget       带宽预算（bps）。作为 best-effort 硬过滤：无轨道满足时退回全部候选。
 * @param allowDolby            是否允许选择 Dolby 音轨
 * @param allowFlac             是否允许选择 FLAC 音轨
 * @param preferredCodecId      视频首选编解码（codecid），默认 7 = AVC/H.264
 * @param allowedCodecIds       编解码硬限制（按 codecid）；null = 不限
 * @param allowedCodecNames     编解码硬限制（按 codecs 前缀，如 avc1/hev1）；null = 不限
 */
data class StreamSelectionRequest(
    val preferredVideoQuality: Int,
    val preferredAudioQuality: Int = -1,
    val bandwidthBudget: Long = Long.MAX_VALUE,
    val allowDolby: Boolean = true,
    val allowFlac: Boolean = true,
    val preferredCodecId: Int = 7,
    val allowedCodecIds: Set<Int>? = null,
    val allowedCodecNames: Set<String>? = null
)

/** 视频选流命中的策略，用于可观测性与确定性断言。 */
enum class VideoSelectionStrategy {
    /** 找到 id 精确匹配 */
    EXACT,
    /** 无精确匹配，取 <= qn 的最高档 */
    DOWNGRADE,
    /** 无 <= qn 的档位，取 id 最接近的档位 */
    NEAREST
}

data class StreamSelection(
    val video: DashVideoTrack?,
    val audio: DashAudioTrack?,
    val videoStrategy: VideoSelectionStrategy? = null
)

/**
 * DASH 选流纯算法。
 *
 * 语义对齐 Android 侧 `DashData#getVideoStream / getBestAudioStream / getAudioStream`
 * （Player.kt）及 `PlayerApi` 的兜底链：视频优先 AVC（codecid==7）精确档，再降档 <= qn 最高档，
 * 再取最近档；音频优先 FLAC > Dolby > 普通流最高带宽，id 精确匹配优先。
 */
object DashStreamSelector {

    fun select(streams: DashTrackList, request: StreamSelectionRequest): StreamSelection {
        val video = selectVideo(streams.videoTracks, request)
        val audio = selectAudio(streams, request)
        return StreamSelection(video = video.track, audio = audio, videoStrategy = video.strategy)
    }

    data class VideoPick(val track: DashVideoTrack?, val strategy: VideoSelectionStrategy?)

    /**
     * 选视频轨。规则：
     * 1. 应用编解码硬限制（allowedCodecIds / allowedCodecNames），过滤后为空则无结果。
     * 2. 带宽预算 best-effort 过滤（无满足者时退回上一步分组）。
     * 3. id 精确匹配优先（同档位内优先 preferredCodecId，但同档仅有其它编解码也选中——
     *    与 `getVideoStream` 的 `matches.find { codecid == 7 } ?: matches.firstOrNull()` 对齐）。
     * 4. 无精确匹配 → 在 preferredCodecId 分组（存在时）内取 <= qn 的最高档 → 无则取 |id - qn| 最近档。
     */
    fun selectVideo(tracks: List<DashVideoTrack>, request: StreamSelectionRequest): VideoPick {
        val hardFiltered = filterByCodecLimit(tracks, request)
        if (hardFiltered.isEmpty()) return VideoPick(null, null)

        val budgeted = applyBudgetVideo(hardFiltered, request.bandwidthBudget)

        val exact = budgeted.filter { it.id == request.preferredVideoQuality }
        if (exact.isNotEmpty()) return VideoPick(preferCodec(exact, request), VideoSelectionStrategy.EXACT)

        val preferred = budgeted.filter { it.codecId == request.preferredCodecId }
            .takeIf { it.isNotEmpty() } ?: budgeted

        val downgrade = preferred.filter { it.id <= request.preferredVideoQuality }
        if (downgrade.isNotEmpty()) {
            val maxId = downgrade.maxOf { it.id }
            return VideoPick(preferCodec(downgrade.filter { it.id == maxId }, request), VideoSelectionStrategy.DOWNGRADE)
        }

        val nearest = preferred.minByOrNull { kotlin.math.abs(it.id - request.preferredVideoQuality) }
        return VideoPick(nearest, VideoSelectionStrategy.NEAREST)
    }

    /**
     * 选音频轨。规则：
     * 1. preferredAudioQuality 为 30250/30251 且对应 Dolby/FLAC 存在且被允许时直接返回。
     * 2. 普通流中 id 精确匹配优先。
     * 3. 否则最优：FLAC > Dolby > 普通流最高带宽（受带宽预算约束）。
     */
    fun selectAudio(streams: DashTrackList, request: StreamSelectionRequest): DashAudioTrack? {
        when (request.preferredAudioQuality) {
            30250 -> if (request.allowDolby && streams.dolbyTrack != null) return streams.dolbyTrack
            30251 -> if (request.allowFlac && streams.flacTrack != null) return streams.flacTrack
        }
        val exact = streams.audioTracks.find { it.id == request.preferredAudioQuality }
        if (exact != null) return exact

        if (request.allowFlac && streams.flacTrack != null) return streams.flacTrack
        if (request.allowDolby && streams.dolbyTrack != null) return streams.dolbyTrack
        val budgeted = applyBudgetAudio(streams.audioTracks, request.bandwidthBudget)
        return budgeted.maxByOrNull { it.bandwidth }
    }

    /** 与 PlayerApi 兜底链一致：选流失败时取首个 AVC 轨，否则取首轨。 */
    fun fallbackVideo(tracks: List<DashVideoTrack>): DashVideoTrack? =
        tracks.firstOrNull { it.codecId == 7 } ?: tracks.firstOrNull()

    /** 与 PlayerApi 兜底链一致：选流失败时取普通音频首轨。 */
    fun fallbackAudio(tracks: List<DashAudioTrack>): DashAudioTrack? = tracks.firstOrNull()

    private fun filterByCodecLimit(
        tracks: List<DashVideoTrack>,
        request: StreamSelectionRequest
    ): List<DashVideoTrack> {
        var result = tracks
        request.allowedCodecIds?.let { ids -> result = result.filter { it.codecId in ids } }
        request.allowedCodecNames?.let { names ->
            result = result.filter { t -> names.any { t.codecs.startsWith(it) } }
        }
        return result
    }

    /** 带宽预算 best-effort：有满足预算的候选时只用它们，否则退回原候选。 */
    private fun applyBudgetVideo(tracks: List<DashVideoTrack>, budget: Long): List<DashVideoTrack> {
        if (budget >= Long.MAX_VALUE) return tracks
        val within = tracks.filter { it.bandwidth <= budget }
        return within.ifEmpty { tracks }
    }

    private fun applyBudgetAudio(tracks: List<DashAudioTrack>, budget: Long): List<DashAudioTrack> {
        if (budget >= Long.MAX_VALUE) return tracks
        val within = tracks.filter { it.bandwidth <= budget }
        return within.ifEmpty { tracks }
    }

    /** 同一档位内优先 preferredCodecId，保持确定性（与原实现 `find { codecid == 7 } ?: first` 一致）。 */
    private fun preferCodec(tracks: List<DashVideoTrack>, request: StreamSelectionRequest): DashVideoTrack? =
        tracks.firstOrNull { it.codecId == request.preferredCodecId } ?: tracks.firstOrNull()
}
