package com.qx.orbit.bili.data.api

import com.qx.orbit.bili.data.model.*
import com.qx.orbit.bili.data.remote.CookieManager
import com.qx.orbit.bili.data.remote.GsonConfig
import com.qx.orbit.bili.data.remote.HttpClient
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONObject

object UserInfoApi {

    internal data class CardData(
        @SerializedName("card") val card: CardDetail? = null,
        @SerializedName("following") val following: Boolean = false,
        @SerializedName("follower") val follower: Int = 0
    )

    data class NavInfoData(
        @SerializedName("isLogin") val isLogin: Boolean = false,
        @SerializedName("mid") val mid: Long = 0,
        @SerializedName("uname") val uname: String? = null,
        @SerializedName("face") val face: String? = null
    )

    internal data class CardDetail(
        @SerializedName("mid") val mid: String? = null,
        @SerializedName("name") val name: String? = null,
        @SerializedName("face") val face: String? = null,
        @SerializedName("sign") val sign: String? = null,
        @SerializedName("fans") val fans: Int = 0,
        @SerializedName("attention") val attention: Int = 0,
        @SerializedName("level_info") val level_info: LevelInfo? = null,
        @SerializedName("official_verify") val official_verify: OfficialInfo? = null,
        @SerializedName("vip") val vip: VipInfoData? = null
    )

    internal data class LevelInfo(
        @SerializedName("current_level") val current_level: Int = 0,
        @SerializedName("current_exp") val current_exp: Long = 0,
        @SerializedName("next_exp") val next_exp: Long = 0
    )

    internal data class OfficialInfo(
        @SerializedName("type") val type: Int = -1,
        @SerializedName("desc") val desc: String? = null
    )

    internal data class VipInfoData(
        @SerializedName("vipType") val vipType: Int = 0,
        @SerializedName("vipStatus") val vipStatus: Int = 0,
        @SerializedName("theme_type") val theme_type: Int = 0,
        @SerializedName("nickname_color") val nickname_color: String? = null
    )

    internal data class SpaceInfoData(
        @SerializedName("bg_img_url") val bg_img_url: String? = null
    )

    internal data class SysNotice(
        @SerializedName("id") val id: Int = 0,
        @SerializedName("content") val content: String? = null,
        @SerializedName("notice_type") val notice_type: Int = 0
    )

    internal data class SpaceLiveRoom(
        @SerializedName("roomStatus") val roomStatus: Int = 0,
        @SerializedName("liveStatus") val liveStatus: Int = 0,
        @SerializedName("url") val url: String? = null,
        @SerializedName("title") val title: String? = null,
        @SerializedName("cover") val cover: String? = null,
        @SerializedName("roomid") val roomid: Long = 0,
        @SerializedName("online") val online: Int = 0
    )

    internal data class ContractInfo(
        @SerializedName("is_contract") val is_contract: Boolean = false,
        @SerializedName("is_follow_display") val is_follow_display: Boolean = false
    )

    internal data class MyInfoData(
        @SerializedName("mid") val mid: Long = 0,
        @SerializedName("uname") val uname: String? = null,
        @SerializedName("face") val face: String? = null,
        @SerializedName("sign") val sign: String? = null,
        @SerializedName("coins") val coins: Double = 0.0,
        @SerializedName("bcoin") val bcoin: Double = 0.0,
        @SerializedName("level_info") val level_info: LevelExp? = null,
        @SerializedName("official") val official: OfficialDescInfo? = null,
        @SerializedName("vip") val vip: VipInfoData? = null,
        @SerializedName("is_senior_member") val is_senior_member: Int = 0
    )

    internal data class OfficialDescInfo(
        @SerializedName("role") val role: Int = 0,
        @SerializedName("title") val title: String? = null,
        @SerializedName("desc") val desc: String? = null
    )

    internal data class LevelExp(
        @SerializedName("current_level") val current_level: Int = 0,
        @SerializedName("current_exp") val current_exp: Long = 0,
        @SerializedName("next_exp") val next_exp: Long = 0
    )

    internal data class UserVideoData(
        @SerializedName("list") val list: UserVideoList? = null,
        @SerializedName("page") val page: VPage? = null
    )

    internal data class VPage(
        @SerializedName("count") val count: Int = 0,
        @SerializedName("pn") val pn: Int = 0,
        @SerializedName("ps") val ps: Int = 0
    )

    internal data class UserVideoList(
        @SerializedName("vlist") val vlist: List<VListItem>? = null
    )

    internal data class VListItem(
        @SerializedName("aid") val aid: Long = 0,
        @SerializedName("bvid") val bvid: String? = null,
        @SerializedName("title") val title: String? = null,
        @SerializedName("description") val description: String? = null,
        @SerializedName("play") val play: Int = 0,
        @SerializedName("pic") val pic: String? = null,
        @SerializedName("created") val created: Long = 0,
        @SerializedName("length") val length: String? = null,
        @SerializedName("comment") val comment: Int = 0,
        @SerializedName("video_review") val video_review: Int = 0,
        @SerializedName("is_union_video") val is_union_video: Int = 0
    )

    internal data class UserArticleData(
        @SerializedName("articles") val articles: List<ArticleItem>? = null,
        @SerializedName("count") val count: Int = 0,
        @SerializedName("pn") val pn: Int = 0,
        @SerializedName("ps") val ps: Int = 0
    )

    internal data class ArticleItem(
        @SerializedName("id") val id: Long = 0,
        @SerializedName("title") val title: String? = null,
        @SerializedName("image_urls") val image_urls: List<String>? = null,
        @SerializedName("summary") val summary: String? = null,
        @SerializedName("stats") val stats: ArticleStats? = null,
        @SerializedName("author") val author: ArticleAuthor? = null,
        @SerializedName("publish_time") val publish_time: Long = 0
    )

    internal data class ArticleStats(
        @SerializedName("view") val view: Int = 0,
        @SerializedName("like") val like: Int = 0,
        @SerializedName("reply") val reply: Int = 0
    )

    internal data class ArticleAuthor(
        @SerializedName("mid") val mid: Long = 0,
        @SerializedName("name") val name: String? = null,
        @SerializedName("face") val face: String? = null
    )

    internal data class NoticeData(
        @SerializedName("notice") val notice: String? = null
    )

    internal data class CoinData(
        @SerializedName("money") val money: Double = 0.0
    )

    suspend fun getUserInfo(mid: Long): UserInfo? = withContext(Dispatchers.IO) {
        val cardUrl = "https://api.bilibili.com/x/web-interface/card?mid=$mid"
        val cardJson = httpGet(cardUrl)
        val cardType = object : TypeToken<ApiResponse<CardData>>() {}.type
        val cardResp: ApiResponse<CardData>? = GsonConfig.gson.fromJson(cardJson, cardType)
        if (cardResp == null || !cardResp.isSuccess || cardResp.data == null) return@withContext null
        val card = cardResp.data.card ?: return@withContext null

        val noticeUrl = "https://api.bilibili.com/x/space/notice?mid=$mid"
        val noticeJson = httpGet(noticeUrl)
        val noticeType = object : TypeToken<ApiResponse<NoticeData>>() {}.type
        val noticeResp: ApiResponse<NoticeData>? = GsonConfig.gson.fromJson(noticeJson, noticeType)
        val notice = noticeResp?.data?.notice ?: ""

        val accInfoUrl = WbiSigner.signUrl("https://api.bilibili.com/x/space/wbi/acc/info?mid=$mid")
        val accJson = httpGet(accInfoUrl)
        @Suppress("UNCHECKED_CAST")
        val accMap = GsonConfig.gson.fromJson(accJson, Map::class.java) as? Map<String, Any?>
        val accData = accMap?.get("data") as? Map<String, Any?>
        val sysNotice = (accData?.get("sys_notice") as? Map<String, Any?>)?.get("content") as? String ?: ""

        val midLong = card.mid?.toLongOrNull() ?: 0L
        val liveRoomData = accData?.get("live_room") as? Map<String, Any?>
        val liveRoom = if (liveRoomData != null && (liveRoomData["roomStatus"] as? Number)?.toInt() == 1) {
            LiveRoom(
                roomid = (liveRoomData["roomid"] as? Number)?.toLong() ?: 0,
                title = liveRoomData["title"] as? String ?: "",
                online = (liveRoomData["online"] as? Number)?.toInt() ?: 0,
                live_status = (liveRoomData["liveStatus"] as? Number)?.toInt() ?: 0
            )
        } else null

        val contractData = accData?.get("contract") as? Map<String, Any?>
        val isFollowDisplay = contractData?.get("is_follow_display") as? Boolean ?: cardResp.data.following

        UserInfo(
            mid = midLong,
            name = card.name ?: "",
            avatar = card.face ?: "",
            sign = card.sign ?: "",
            fans = card.fans,
            following = card.attention,
            level = card.level_info?.current_level ?: 0,
            current_exp = card.level_info?.current_exp ?: 0,
            next_exp = card.level_info?.next_exp ?: 0,
            followed = cardResp.data.following,
            notice = notice,
            official = card.official_verify?.type ?: -1,
            officialDesc = card.official_verify?.desc ?: "",
            vip_role = card.vip?.vipType ?: 0,
            vip_nickname_color = card.vip?.nickname_color ?: "",
            sys_notice = sysNotice,
            live_room = liveRoom,
            is_senior_member = (accData?.get("is_senior_member") as? Number)?.toInt() ?: 0,
            is_follow_display = isFollowDisplay
        )
    }

    suspend fun getCurrentUserInfo(): UserInfo = withContext(Dispatchers.IO) {
        val url = "https://api.bilibili.com/x/space/myinfo"
        val json = httpGet(url)
        val type = object : TypeToken<ApiResponse<MyInfoData>>() {}.type
        val resp: ApiResponse<MyInfoData>? = GsonConfig.gson.fromJson(json, type)
        val data = resp?.data ?: return@withContext UserInfo()
        UserInfo(
            mid = data.mid,
            name = data.uname ?: "",
            avatar = data.face ?: "",
            sign = data.sign ?: "",
            level = data.level_info?.current_level ?: 0,
            current_exp = data.level_info?.current_exp ?: 0,
            next_exp = data.level_info?.next_exp ?: 0,
            official = data.official?.role ?: 0,
            officialDesc = data.official?.title ?: "",
            vip_role = data.vip?.vipType ?: 0,
            vip_nickname_color = data.vip?.nickname_color ?: "",
            is_senior_member = data.is_senior_member
        )
    }

    suspend fun getNavInfo(): NavInfoData? = withContext(Dispatchers.IO) {
        val url = "https://api.bilibili.com/x/web-interface/nav"
        val json = httpGet(url)
        val type = object : TypeToken<ApiResponse<NavInfoData>>() {}.type
        val resp: ApiResponse<NavInfoData>? = GsonConfig.gson.fromJson(json, type)
        if (resp != null && resp.code == 0) resp.data else null
    }

    suspend fun getCurrentUserCoin(): Int = withContext(Dispatchers.IO) {
        val url = "https://account.bilibili.com/site/getCoin"
        val json = httpGet(url)
        val type = object : TypeToken<ApiResponse<CoinData>>() {}.type
        val resp: ApiResponse<CoinData>? = GsonConfig.gson.fromJson(json, type)
        resp?.data?.money?.toInt() ?: 0
    }

    suspend fun getUserVideos(mid: Long, page: Int, searchKeyword: String = ""): Pair<Int, List<VideoCard>> = withContext(Dispatchers.IO) {
        var url = "https://api.bilibili.com/x/space/wbi/arc/search?mid=$mid&pn=$page&ps=30"
        if (searchKeyword.isNotEmpty()) {
            url += "&keyword=$searchKeyword"
        }
        url = WbiSigner.signUrl(url)
        val json = httpGet(url)
        val type = object : TypeToken<ApiResponse<UserVideoData>>() {}.type
        val resp: ApiResponse<UserVideoData>? = GsonConfig.gson.fromJson(json, type)
        if (resp == null || !resp.isSuccess) return@withContext Pair(-1, emptyList<VideoCard>())
        val data = resp.data ?: return@withContext Pair(1, emptyList<VideoCard>())
        val vlist = data.list?.vlist
        if (vlist.isNullOrEmpty()) return@withContext Pair(1, emptyList<VideoCard>())
        val cards = vlist.map { item ->
            VideoCard(
                title = item.title ?: "",
                upName = "",
                view = StringUtil.toWan(item.play.toLong()),
                cover = item.pic ?: "",
                aid = item.aid,
                bvid = item.bvid ?: ""
            )
        }
        Pair(0, cards)
    }

    suspend fun getUserArticles(mid: Long, page: Int): Pair<Int, List<ArticleCard>> = withContext(Dispatchers.IO) {
        val url = WbiSigner.signUrl("https://api.bilibili.com/x/space/wbi/article?mid=$mid&pn=$page&ps=20&sort=0")
        val json = httpGet(url)
        val type = object : TypeToken<ApiResponse<UserArticleData>>() {}.type
        val resp: ApiResponse<UserArticleData>? = GsonConfig.gson.fromJson(json, type)
        if (resp == null || !resp.isSuccess) return@withContext Pair(-1, emptyList<ArticleCard>())
        val data = resp.data ?: return@withContext Pair(1, emptyList<ArticleCard>())
        val articles = data.articles
        if (articles.isNullOrEmpty()) return@withContext Pair(1, emptyList<ArticleCard>())
        val cards = articles.map { item ->
            ArticleCard(
                title = item.title ?: "",
                id = item.id,
                cover = item.image_urls?.firstOrNull() ?: "",
                upName = item.author?.name ?: "",
                view = StringUtil.toWan(item.stats?.view?.toLong() ?: 0)
            )
        }
        Pair(0, cards)
    }

    suspend fun followUser(mid: Long, isFollow: Boolean): Int = withContext(Dispatchers.IO) {
        val url = "https://api.bilibili.com/x/relation/modify"
        val body = FormBody.Builder()
            .add("fid", mid.toString())
            .add("act", if (isFollow) "1" else "2")
            .add("re_src", "11")
            .add("csrf", CookieManager.getCsrf())
            .build()
        val request = Request.Builder().url(url)
            .post(body)
            .addHeader("Cookie", CookieManager.getCookie())
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Referer", "https://www.bilibili.com/")
            .build()
        val response = HttpClient.client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return@withContext -1
        @Suppress("UNCHECKED_CAST")
        val json = GsonConfig.gson.fromJson(responseBody, Map::class.java) as? Map<String, Any?>
        (json?.get("code") as? Number)?.toInt() ?: -1
    }

    suspend fun exitLogin() = withContext(Dispatchers.IO) {
        val url = "https://passport.bilibili.com/login/exit/v2"
        val body = FormBody.Builder()
            .add("bili_jct", CookieManager.getCsrf())
            .build()
        val request = Request.Builder().url(url)
            .post(body)
            .addHeader("Cookie", CookieManager.getCookie())
            .addHeader("User-Agent", USER_AGENT)
            .build()
        val response = HttpClient.client.newCall(request).execute()
        response.body?.string()
        CookieManager.setCookie("")
    }

    suspend fun getMedalWall(targetId: Long): JSONObject? = withContext(Dispatchers.IO) {
        val url = "https://api.bilibili.com/xlive/web-ucenter/user/MedalWall?target_id=$targetId"
        val json = httpGet(url)
        try {
            val obj = JSONObject(json)
            if (obj.optInt("code") == 0) obj.optJSONObject("data") else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun updateUserSign(userSign: String): JSONObject = withContext(Dispatchers.IO) {
        val url = "https://api.bilibili.com/x/member/web/sign/update"
        val body = FormBody.Builder()
            .add("user_sign", userSign)
            .add("csrf", CookieManager.getCsrf())
            .build()
        val request = Request.Builder().url(url)
            .post(body)
            .addHeader("Cookie", CookieManager.getCookie())
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Referer", "https://www.bilibili.com/")
            .build()
        val response = HttpClient.client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return@withContext JSONObject()
        try {
            JSONObject(responseBody)
        } catch (_: Exception) {
            JSONObject()
        }
    }

    private fun httpGet(url: String): String {
        val request = Request.Builder().url(url)
            .addHeader("Cookie", CookieManager.getCookie())
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Referer", "https://www.bilibili.com/")
            .build()
        return HttpClient.client.newCall(request).execute().body?.string() ?: ""
    }

    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.95 Safari/537.36"
}
