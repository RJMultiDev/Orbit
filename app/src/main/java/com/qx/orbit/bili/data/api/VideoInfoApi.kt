package com.qx.orbit.bili.data.api

import com.qx.orbit.bili.data.model.*
import com.qx.orbit.bili.data.remote.GsonConfig
import com.qx.orbit.bili.data.remote.HttpClient
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.*
import com.qx.orbit.bili.data.model.Collection as BiliCollection

object VideoInfoApi {

    internal data class VideoInfoData(
        @SerializedName("title") val title: String? = null,
        @SerializedName("pic") val pic: String? = null,
        @SerializedName("desc") val desc: String? = null,
        @SerializedName("desc_v2") val desc_v2: List<DescV2>? = null,
        @SerializedName("bvid") val bvid: String? = null,
        @SerializedName("aid") val aid: Long = 0,
        @SerializedName("pubdate") val pubdate: Long = 0,
        @SerializedName("duration") val duration: Int = 0,
        @SerializedName("copyright") val copyright: Int = 0,
        @SerializedName("stat") val stat: StatData? = null,
        @SerializedName("pages") val pages: List<PageData>? = null,
        @SerializedName("is_upower_exclusive") val is_upower_exclusive: Boolean = false,
        @SerializedName("rights") val rights: RightsData? = null,
        @SerializedName("staff") val staff: List<StaffData>? = null,
        @SerializedName("owner") val owner: OwnerData? = null,
        @SerializedName("argue_info") val argue_info: ArgueInfoData? = null,
        @SerializedName("redirect_url") val redirect_url: String? = null,
        @SerializedName("ugc_season") val ugc_season: UgcSeasonData? = null
    )

    internal data class DescV2(@SerializedName("type") val type: Int = 0, @SerializedName("raw_text") val raw_text: String? = null, @SerializedName("biz_id") val biz_id: Long = 0)
    internal data class StatData(@SerializedName("view") val view: Int = 0, @SerializedName("like") val like: Int = 0, @SerializedName("coin") val coin: Int = 0, @SerializedName("reply") val reply: Int = 0, @SerializedName("danmaku") val danmaku: Int = 0, @SerializedName("favorite") val favorite: Int = 0)
    internal data class PageData(@SerializedName("part") val part: String? = null, @SerializedName("cid") val cid: Long = 0)
    internal data class RightsData(@SerializedName("is_cooperation") val is_cooperation: Int = 0, @SerializedName("is_stein_gate") val is_stein_gate: Int = 0, @SerializedName("is_360") val is_360: Int = 0)
    internal data class StaffData(@SerializedName("mid") val mid: Long = 0, @SerializedName("title") val title: String? = null, @SerializedName("name") val name: String? = null, @SerializedName("face") val face: String? = null, @SerializedName("follower") val follower: Int = 0, @SerializedName("official") val official: OfficialData? = null)
    internal data class OfficialData(@SerializedName("role") val role: Int = 0, @SerializedName("title") val title: String? = null)
    internal data class OwnerData(@SerializedName("name") val name: String? = null, @SerializedName("face") val face: String? = null, @SerializedName("mid") val mid: Long = 0)
    internal data class ArgueInfoData(@SerializedName("argue_msg") val argue_msg: String? = null)
    internal data class UgcSeasonData(@SerializedName("id") val id: Int = 0, @SerializedName("title") val title: String? = null, @SerializedName("intro") val intro: String? = null, @SerializedName("cover") val cover: String? = null, @SerializedName("mid") val mid: Long = 0, @SerializedName("stat") val stat: UgcStatData? = null, @SerializedName("sections") val sections: List<UgcSectionData>? = null)
    internal data class UgcStatData(@SerializedName("view") val view: Long = 0)
    internal data class UgcSectionData(@SerializedName("season_id") val season_id: Int = 0, @SerializedName("id") val id: Int = 0, @SerializedName("title") val title: String? = null, @SerializedName("episodes") val episodes: List<UgcEpisodeData>? = null)
    internal data class UgcEpisodeData(@SerializedName("season_id") val season_id: Int = 0, @SerializedName("section_id") val section_id: Int = 0, @SerializedName("id") val id: Int = 0, @SerializedName("aid") val aid: Long = 0, @SerializedName("cid") val cid: Long = 0, @SerializedName("title") val title: String? = null, @SerializedName("arc") val arc: VideoInfoData? = null, @SerializedName("bvid") val bvid: String? = null)
    internal data class TagData(@SerializedName("tag_name") val tag_name: String? = null)
    internal data class TotalData(@SerializedName("total") val total: Any? = null)

    suspend fun getVideoInfo(bvid: String): VideoInfo? = withContext(Dispatchers.IO) {
        val url = "https://api.bilibili.com/x/web-interface/view?bvid=$bvid"
        fetchAndBuildVideoInfo(url)
    }

    suspend fun getVideoInfo(aid: Long): VideoInfo? = withContext(Dispatchers.IO) {
        val url = "https://api.bilibili.com/x/web-interface/view?aid=$aid"
        fetchAndBuildVideoInfo(url)
    }

    private fun fetchAndBuildVideoInfo(url: String): VideoInfo? {
        val json = httpGet(url)
        val type = object : TypeToken<ApiResponse<VideoInfoData>>() {}.type
        val resp: ApiResponse<VideoInfoData>? = GsonConfig.gson.fromJson(json, type)
        if (resp == null || !resp.isSuccess || resp.data == null) return null
        val videoInfo = buildVideoInfo(resp.data)
        return videoInfo
    }

    suspend fun getTags(bvid: String): String = withContext(Dispatchers.IO) {
        fetchTags("https://api.bilibili.com/x/tag/archive/tags?bvid=$bvid")
    }

    suspend fun getTags(aid: Long): String = withContext(Dispatchers.IO) {
        fetchTags("https://api.bilibili.com/x/tag/archive/tags?aid=$aid")
    }

    private fun fetchTags(url: String): String {
        val json = httpGet(url)
        val type = object : TypeToken<ApiResponse<List<TagData>>>() {}.type
        val resp: ApiResponse<List<TagData>>? = GsonConfig.gson.fromJson(json, type)
        if (resp == null || resp.data == null) return ""
        return resp.data.filterNotNull().joinToString("/") { it.tag_name ?: "" }
    }

    suspend fun getWatching(aid: Long, cid: Long): String = withContext(Dispatchers.IO) {
        val url = "https://api.bilibili.com/x/player/online/total?aid=$aid&cid=$cid"
        val json = httpGet(url)
        val type = object : TypeToken<ApiResponse<TotalData>>() {}.type
        val resp: ApiResponse<TotalData>? = GsonConfig.gson.fromJson(json, type)
        if (resp == null || resp.data == null || resp.data.total == null) return@withContext ""
        val total = resp.data.total
        if (total is String) total
        else StringUtil.toWan((total as Number).toLong())
    }

    internal fun analyzeUgcSeason(data: UgcSeasonData): BiliCollection {
        return BiliCollection(
            id = data.id,
            title = data.title ?: "",
            intro = data.intro ?: "",
            cover = data.cover ?: "",
            mid = data.mid,
            view = StringUtil.toWan(data.stat?.view ?: 0),
            sections = data.sections?.filterNotNull()?.map { sectionData ->
                BiliCollection.Section(
                    season_id = sectionData.season_id,
                    id = sectionData.id,
                    title = sectionData.title ?: "",
                    episodes = sectionData.episodes?.filterNotNull()?.map { epData ->
                        BiliCollection.Episode(
                            season_id = epData.season_id,
                            section_id = epData.section_id,
                            id = epData.id.toLong(),
                            aid = epData.aid,
                            cid = epData.cid,
                            title = epData.title ?: "",
                            bvid = epData.bvid ?: "",
                            arc = epData.arc?.let { buildVideoInfo(it) }
                        )
                    } ?: emptyList()
                )
            } ?: emptyList()
        )
    }

    private fun buildVideoInfo(data: VideoInfoData): VideoInfo {
        val description: String
        val descAts: List<At>

        if (!data.desc_v2.isNullOrEmpty()) {
            val sb = StringBuilder()
            val ats = mutableListOf<At>()
            for (desc in data.desc_v2) {
                if (desc.type == 2) {
                    val start = sb.length
                    sb.append("@").append(desc.raw_text ?: "")
                    val end = sb.length
                    ats.add(At(desc.biz_id, start, end))
                } else {
                    sb.append(desc.raw_text ?: "")
                }
            }
            description = sb.toString()
            descAts = ats
        } else {
            description = data.desc ?: ""
            descAts = emptyList()
        }

        val timeDesc = if (data.pubdate > 0) {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(data.pubdate * 1000)
        } else ""

        val stats = data.stat?.let {
            Stats(
                view = it.view, like = it.like, coin = it.coin,
                reply = it.reply, danmaku = it.danmaku, favorite = it.favorite,
                coin_limit = if (data.copyright == VideoInfo.COPYRIGHT_REPRINT) 1 else 2
            )
        }

        val pagenames = data.pages?.filterNotNull()?.map { it.part ?: "" } ?: emptyList()
        val cids = data.pages?.filterNotNull()?.map { it.cid } ?: emptyList()

        val isCooperation = data.rights?.is_cooperation == 1
        val isSteinGate = data.rights?.is_stein_gate == 1
        val is360 = data.rights?.is_360 == 1

        val staff = if (isCooperation && !data.staff.isNullOrEmpty()) {
            data.staff.filterNotNull().map { s ->
                UserInfo(
                    mid = s.mid, sign = s.title ?: "", name = s.name ?: "",
                    avatar = s.face ?: "", fans = s.follower, level = 6,
                    official = s.official?.role ?: 0, officialDesc = s.official?.title ?: ""
                )
            }
        } else if (data.owner != null) {
            listOf(UserInfo(name = data.owner.name ?: "", avatar = data.owner.face ?: "", mid = data.owner.mid, sign = "UP主"))
        } else emptyList()

        val epid = try {
            if (!data.redirect_url.isNullOrEmpty() && data.redirect_url.contains("bangumi")) {
                data.redirect_url.replace("https://www.bilibili.com/bangumi/play/ep", "").toLong()
            } else -1
        } catch (_: Exception) { -1 }

        val collection = data.ugc_season?.let { analyzeUgcSeason(it) }

        return VideoInfo(
            title = data.title ?: "", cover = data.pic ?: "", bvid = data.bvid ?: "",
            aid = data.aid, description = description, descAts = descAts,
            duration = StringUtil.toTime(data.duration), stats = stats, timeDesc = timeDesc,
            copyright = data.copyright, pagenames = pagenames, cids = cids,
            upowerExclusive = data.is_upower_exclusive, argueMsg = data.argue_info?.argue_msg,
            isCooperation = isCooperation, isSteinGate = isSteinGate, is360 = is360,
            staff = staff, epid = epid, collection = collection
        )
    }

    private fun httpGet(url: String): String {
        val request = Request.Builder().url(url)
            .addHeader("Cookie", com.qx.orbit.bili.data.remote.CookieManager.getCookie())
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.95 Safari/537.36")
            .addHeader("Referer", "https://www.bilibili.com/")
            .build()
        return HttpClient.client.newCall(request).execute().body?.string() ?: ""
    }
}
