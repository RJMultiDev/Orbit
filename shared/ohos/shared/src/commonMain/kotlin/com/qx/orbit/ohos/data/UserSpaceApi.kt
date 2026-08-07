package com.qx.orbit.ohos.data

import com.qx.orbit.ohos.util.TimeUtil
import com.qx.orbit.ohos.util.WbiKeys
import com.qx.orbit.ohos.util.WbiSigner
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * 用户空间 / 关注 / 收藏 / 历史 / 稍后再看 数据层（独占本文件）。
 *
 * 对齐 Android app/src/main/java/com/qx/orbit/bili/data/api/ 下
 * UserInfoApi / FollowApi / FavoriteApi / HistoryApi / WatchLaterApi 的只读接口。
 * 写操作（关注/取关、稍后再看增删、历史删除等）依赖 csrf（bili_jct Cookie），统一留 TODO。
 *
 * 注意：BiliClient 内部的 mixinKey / wbiSigner 均为 private，扩展函数无法读取，
 * 故 WBI 签名在本地重新拉取 wbi_img 计算 mixinKey 并缓存（见 [fetchMixinKey]）。
 */

// ============================== 模型 ==============================

/** 用户空间：卡片信息 + 投稿视频列表。 */
data class UserSpace(
    val card: UserCard? = null,
    val videos: List<VideoItem> = emptyList(),
)

/** 用户卡片（/x/space/wbi/acc/info 的 data）。 */
data class UserCard(
    val mid: Long = 0,
    val name: String = "",
    val face: String = "",
    val sign: String = "",
    val level: Int = 0,
    val currentExp: Long = 0,
    val nextExp: Long = 0,
    val isFollowed: Boolean = false,
    /** official.type：-1=无认证 0=个人 1=机构。 */
    val official: Int = -1,
    val officialDesc: String = "",
    val vipStatus: Int = 0,
    val vipNicknameColor: String = "",
    val sysNotice: String = "",
    val isSeniorMember: Int = 0,
    val liveRoom: LiveRoom? = null,
)

/** 直播间（acc/info 的 live_room，roomStatus=1 表示开播状态有效）。 */
data class LiveRoom(
    val roomId: Long = 0,
    val title: String = "",
    val cover: String = "",
    val url: String = "",
    val online: Long = 0,
    val roomStatus: Int = 0,
    val liveStatus: Int = 0,
)

/**
 * 通用视频条目：统一「投稿列表 / 收藏夹 / 稍后再看」三种 JSON 形态。
 * - 投稿列表（arc/search vlist）：play→view、video_review→danmaku、length→时长文本
 * - 收藏夹（fav/resource medias）：cnt_info→stat、upper→owner、id→aid、bv_id→bvid
 * - 稍后再看（toview list）：stat→stat、owner→owner、duration 为秒数
 */
data class VideoItem(
    val bvid: String = "",
    val aid: Long = 0,
    val cid: Long = 0,
    val title: String = "",
    val pic: String = "",
    val description: String = "",
    /** 时长（秒）；投稿列表由 "MM:SS" 解析得出。 */
    val duration: Long = 0,
    /** 投稿列表时长原文（"10:23"）。 */
    val lengthText: String = "",
    val view: Long = 0,
    val danmaku: Long = 0,
    val comment: Long = 0,
    /** 投稿时间（秒）。 */
    val created: Long = 0,
    /** 观看进度（秒，仅稍后再看/历史有意义）。 */
    val progress: Long = 0,
    val owner: VideoOwner? = null,
)

/** 关注用户（/x/relation/followings 的 data.list）。 */
data class FollowUser(
    val mid: Long = 0,
    val name: String = "",
    val face: String = "",
    val sign: String = "",
    val fans: Long = 0,
    val official: Int = -1,
    val officialDesc: String = "",
    val vipStatus: Int = 0,
)

/** 收藏夹（/x/v3/fav/folder/created/list-all 的 data.list）。 */
data class FavoriteFolder(
    /** media_id，取收藏夹资源列表（/x/v3/fav/resource/list）用。 */
    val id: Long = 0,
    val fid: Long = 0,
    val name: String = "",
    val cover: String = "",
    val mediaCount: Long = 0,
    /** attr 第 0 位为默认收藏夹。 */
    val isDefault: Boolean = false,
)

/** 历史记录（/x/v2/history 的 data.list）。 */
data class HistoryItem(
    val title: String = "",
    val authorName: String = "",
    val cover: String = "",
    val aid: Long = 0,
    val bvid: String = "",
    val cid: Long = 0,
    val progress: Long = 0,
    val duration: Long = 0,
    val kid: Long = 0,
    /** archive / pgc / article ...（由 history.business 给出）。 */
    val business: String = "",
    val view: Long = 0,
    val viewAt: Long = 0,
)

// ============================== parse ==============================

fun parseUserCard(j: JSONObject): UserCard {
    val levelInfo = j.optJSONObject("level_info")
    val official = j.optJSONObject("official")
    val vip = j.optJSONObject("vip")
    val liveRoom = j.optJSONObject("live_room")
    return UserCard(
        mid = j.optLong("mid"),
        name = j.optString("name") ?: "",
        face = j.optString("face") ?: "",
        sign = j.optString("sign") ?: "",
        level = levelInfo?.optInt("current_level") ?: j.optInt("level"),
        currentExp = levelInfo?.optLong("current_exp") ?: 0,
        nextExp = levelInfo?.optLong("next_exp") ?: 0,
        isFollowed = j.optBoolean("is_followed"),
        official = official?.optInt("type", -1) ?: -1,
        officialDesc = official?.optString("desc") ?: "",
        vipStatus = vip?.optInt("vipStatus") ?: 0,
        vipNicknameColor = vip?.optString("nickname_color") ?: "",
        sysNotice = j.optJSONObject("sys_notice")?.optString("content") ?: "",
        isSeniorMember = j.optInt("is_senior_member"),
        liveRoom = liveRoom?.let { parseLiveRoom(it) },
    )
}

fun parseLiveRoom(j: JSONObject): LiveRoom = LiveRoom(
    roomId = j.optLong("roomid"),
    title = j.optString("title") ?: "",
    cover = j.optString("cover") ?: "",
    url = j.optString("url") ?: "",
    online = j.optLong("online"),
    roomStatus = j.optInt("roomStatus"),
    liveStatus = j.optInt("liveStatus"),
)

fun parseVideoItem(j: JSONObject): VideoItem {
    val ownerObj = j.optJSONObject("owner") ?: j.optJSONObject("upper")
    val stat = j.optJSONObject("stat") ?: j.optJSONObject("cnt_info")
    val duration = if (j.optLong("duration") > 0) {
        j.optLong("duration")
    } else {
        parseDuration(j.optString("length") ?: "")
    }
    val owner = ownerObj?.let {
        VideoOwner(
            mid = it.optLong("mid"),
            name = it.optString("name") ?: "",
            face = it.optString("face") ?: "",
        )
    } ?: if (j.optLong("mid") != 0L) {
        // arc/search vlist 的 UP 信息在顶层 author/mid，无 face
        VideoOwner(mid = j.optLong("mid"), name = j.optString("author") ?: "", face = "")
    } else null
    return VideoItem(
        bvid = (j.optString("bvid") ?: "").ifEmpty { j.optString("bv_id") ?: "" },
        aid = if (j.optLong("aid") > 0) j.optLong("aid") else j.optLong("id"),
        cid = j.optLong("cid"),
        title = j.optString("title") ?: "",
        pic = (j.optString("pic") ?: "").ifEmpty { j.optString("cover") ?: "" },
        description = (j.optString("description") ?: "").ifEmpty { j.optString("intro") ?: "" },
        duration = duration,
        lengthText = j.optString("length") ?: "",
        view = stat?.optLong("view") ?: j.optLong("play"),
        danmaku = stat?.optLong("danmaku") ?: j.optLong("video_review"),
        comment = j.optLong("comment"),
        created = j.optLong("created"),
        progress = j.optLong("progress"),
        owner = owner,
    )
}

fun parseFollowUser(j: JSONObject): FollowUser {
    val official = j.optJSONObject("official_verify")
    val vip = j.optJSONObject("vip")
    return FollowUser(
        mid = j.optLong("mid"),
        name = j.optString("uname") ?: "",
        face = j.optString("face") ?: "",
        sign = j.optString("sign") ?: "",
        fans = j.optLong("follower"),
        official = official?.optInt("type", -1) ?: -1,
        officialDesc = official?.optString("desc") ?: "",
        vipStatus = vip?.optInt("vipStatus") ?: 0,
    )
}

fun parseFavoriteFolder(j: JSONObject): FavoriteFolder = FavoriteFolder(
    id = j.optLong("id"),
    fid = if (j.optLong("fid") > 0) j.optLong("fid") else j.optLong("id"),
    name = j.optString("title") ?: "",
    cover = j.optString("cover") ?: "",
    mediaCount = j.optLong("media_count"),
    isDefault = j.optInt("attr") and 1 == 1,
)

fun parseHistoryItem(j: JSONObject): HistoryItem {
    val history = j.optJSONObject("history")
    val covers = j.optJSONArray("covers")
    return HistoryItem(
        title = j.optString("title") ?: "",
        authorName = (j.optString("author_name") ?: "")
            .ifEmpty { j.optJSONObject("author")?.optString("name") ?: "" },
        cover = if (covers != null && covers.length() > 0) {
            covers.optString(0) ?: ""
        } else {
            j.optString("cover") ?: ""
        },
        aid = if (j.optLong("aid") > 0) j.optLong("aid") else history?.optLong("oid") ?: 0,
        bvid = (j.optString("bvid") ?: "").ifEmpty { history?.optString("bvid") ?: "" },
        cid = if (j.optLong("cid") > 0) j.optLong("cid") else history?.optLong("cid") ?: 0,
        progress = j.optLong("progress"),
        duration = j.optLong("duration"),
        kid = j.optLong("kid"),
        business = history?.optString("business") ?: "",
        view = j.optJSONObject("stat")?.optLong("view") ?: 0,
        viewAt = j.optLong("view_at"),
    )
}

private fun parseDuration(length: String): Long {
    val parts = length.split(":")
    return when {
        parts.size == 3 -> parts[0].toLongOrNull()?.let { h ->
            h * 3600 + (parts[1].toLongOrNull() ?: 0) * 60 + (parts[2].toLongOrNull() ?: 0)
        } ?: 0
        parts.size == 2 -> parts[0].toLongOrNull()?.let { m ->
            m * 60 + (parts[1].toLongOrNull() ?: 0)
        } ?: 0
        else -> 0
    }
}

private fun parseVideoItems(arr: JSONArray?): List<VideoItem> {
    val list = mutableListOf<VideoItem>()
    if (arr != null) {
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { list.add(parseVideoItem(it)) }
        }
    }
    return list
}

private fun parseFollowUsers(arr: JSONArray?): List<FollowUser> {
    val list = mutableListOf<FollowUser>()
    if (arr != null) {
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { list.add(parseFollowUser(it)) }
        }
    }
    return list
}

private fun parseFavoriteFolders(arr: JSONArray?): List<FavoriteFolder> {
    val list = mutableListOf<FavoriteFolder>()
    if (arr != null) {
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { list.add(parseFavoriteFolder(it)) }
        }
    }
    return list
}

private fun parseHistoryItems(arr: JSONArray?): List<HistoryItem> {
    val list = mutableListOf<HistoryItem>()
    if (arr != null) {
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { list.add(parseHistoryItem(it)) }
        }
    }
    return list
}

// ============================== WBI 签名辅助 ==============================

private const val BILI_API = "https://api.bilibili.com"

private var wbiMixinKey: String = ""

/**
 * WBI mixinKey：BiliClient 的 mixinKey 为 private 不可读，这里本地拉取一份缓存。
 * 首次调用多发一次 /x/web-interface/nav，之后复用；失败返回 null（调用方自行降级）。
 */
private suspend fun BiliClient.fetchMixinKey(): String? {
    if (wbiMixinKey.isNotEmpty()) return wbiMixinKey
    val resp = request("$BILI_API/x/web-interface/nav")
    val img = resp?.optJSONObject("data")?.optJSONObject("wbi_img") ?: return null
    wbiMixinKey = WbiKeys.calcMixinKey(
        img.optString("img_url") ?: "",
        img.optString("sub_url") ?: "",
    )
    return wbiMixinKey
}

/** 带真实时间戳的 WBI 签名器（wts 为当前 epoch 秒；BiliClient 内建 signer 固定 wts=0）。 */
private fun wbiSigner(mixinKey: String): WbiSigner =
    WbiSigner({ mixinKey }, { TimeUtil.nowMillis() / 1000 })

// ============================== BiliClient 扩展 ==============================

/** 用户空间：卡片信息 + 首页投稿视频列表（/x/space/wbi/acc/info、/x/space/wbi/arc/search，WBI 签名）。 */
internal suspend fun BiliClient.userSpace(mid: Long): UserSpace {
    val key = fetchMixinKey() ?: return UserSpace()
    val signer = wbiSigner(key)
    val accResp = request(
        "$BILI_API/x/space/wbi/acc/info",
        signer.signParams(mapOf("mid" to mid.toString())),
    )
    val arcResp = request(
        "$BILI_API/x/space/wbi/arc/search",
        signer.signParams(
            mapOf(
                "mid" to mid.toString(),
                "pn" to "1",
                "ps" to "30",
                "tid" to "0",
                "order" to "pubdate",
                "order_avoided" to "true",
                "web_location" to "333.999",
            )
        ),
    )
    val card = accResp?.optJSONObject("data")?.let { parseUserCard(it) }
    val videos = parseVideoItems(
        arcResp?.optJSONObject("data")?.optJSONObject("list")?.optJSONArray("vlist")
    )
    return UserSpace(card = card, videos = videos)
}

/** 关注列表（/x/relation/followings）。 */
internal suspend fun BiliClient.followList(mid: Long, page: Int): List<FollowUser> {
    val resp = request(
        "$BILI_API/x/relation/followings",
        mapOf("vmid" to mid.toString(), "pn" to page.toString(), "ps" to "20", "order" to "desc"),
    )
    return parseFollowUsers(resp?.optJSONObject("data")?.optJSONArray("list"))
}

/** 我的收藏夹（默认 + 自建，/x/v3/fav/folder/created/list-all）。 */
internal suspend fun BiliClient.favoriteFolders(): List<FavoriteFolder> {
    val mid = nav()?.mid ?: return emptyList()
    val resp = request(
        "$BILI_API/x/v3/fav/folder/created/list-all",
        mapOf("up_mid" to mid.toString()),
    )
    return parseFavoriteFolders(resp?.optJSONObject("data")?.optJSONArray("list"))
}

/** 收藏夹内视频（/x/v3/fav/resource/list，入参 fid 即 media_id）。 */
internal suspend fun BiliClient.favoriteDetail(fid: Long): List<VideoItem> {
    val resp = request(
        "$BILI_API/x/v3/fav/resource/list",
        mapOf("media_id" to fid.toString(), "pn" to "1", "ps" to "20", "platform" to "web"),
    )
    return parseVideoItems(resp?.optJSONObject("data")?.optJSONArray("medias"))
}

/** 观看历史（/x/v2/history，pn 分页）。 */
internal suspend fun BiliClient.history(page: Int = 1): List<HistoryItem> {
    val resp = request("$BILI_API/x/v2/history", mapOf("pn" to page.toString(), "ps" to "30"))
    return parseHistoryItems(resp?.optJSONObject("data")?.optJSONArray("list"))
}

/** 稍后再看（/x/v2/fav/video/list）。 */
internal suspend fun BiliClient.watchLater(): List<VideoItem> {
    val resp = request("$BILI_API/x/v2/fav/video/list", mapOf("pn" to "1", "ps" to "20"))
    return parseVideoItems(resp?.optJSONObject("data")?.optJSONArray("list"))
}

// TODO(写操作，依赖 csrf = Cookie 中 bili_jct，需在 credential 侧提供)：
//  - 关注/取关：POST /x/relation/modify（fid、act=1/2、csrf）
//  - 稍后再看添加/删除：POST /x/v2/history/toview/add、/x/v2/history/toview/del（aid、csrf）
//  - 历史删除：POST /x/v2/history/delete（kid、csrf）
