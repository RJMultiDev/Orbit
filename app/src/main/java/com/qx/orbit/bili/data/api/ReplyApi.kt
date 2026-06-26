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
import java.text.SimpleDateFormat
import java.util.*

object ReplyApi {

    const val REPLY_TYPE_VIDEO = 1
    const val REPLY_TYPE_VIDEO_CHILD = 0
    const val REPLY_TYPE_ARTICLE = 12
    const val REPLY_TYPE_DYNAMIC = 17
    const val REPLY_TYPE_DYNAMIC_CHILD = 11

    internal data class ReplyListData(
        @SerializedName("replies") val replies: List<ReplyRootData>? = null,
        @SerializedName("top_replies") val top_replies: List<ReplyRootData>? = null,
        @SerializedName("page") val page: PageData? = null
    )

    internal data class PageData(
        @SerializedName("num") val num: Int = 0,
        @SerializedName("size") val size: Int = 0,
        @SerializedName("count") val count: Int = 0
    )

    internal data class ReplyLazyData(
        @SerializedName("replies") val replies: List<ReplyRootData>? = null,
        @SerializedName("top_replies") val top_replies: List<ReplyRootData>? = null,
        @SerializedName("cursor") val cursor: CursorData? = null
    )

    internal data class CursorData(
        @SerializedName("all_count") val all_count: Int = 0,
        @SerializedName("next") val next: Int = 0,
        @SerializedName("is_end") val is_end: Boolean = false,
        @SerializedName("pagination_reply") val pagination_reply: PaginationReply? = null
    )

    internal data class PaginationReply(
        @SerializedName("next_offset") val next_offset: String? = null
    )

    internal data class ReplyRootData(
        @SerializedName("rpid") val rpid: Long = 0,
        @SerializedName("oid") val oid: Long = 0,
        @SerializedName("root") val root: Long = 0,
        @SerializedName("parent") val parent: Long = 0,
        @SerializedName("ctime") val ctime: Long = 0,
        @SerializedName("member") val member: MemberData? = null,
        @SerializedName("content") val content: ContentData? = null,
        @SerializedName("like") val like: Int = 0,
        @SerializedName("reply_control") val reply_control: ReplyControlData? = null,
        @SerializedName("replies") val replies: List<ReplyRootData>? = null,
        @SerializedName("rcount") val rcount: Int = 0,
        @SerializedName("up_action") val up_action: UpActionData? = null,
        @SerializedName("reply_replies") val reply_replies: List<ReplyRootData>? = null,
        @SerializedName("action") val action: Int = 0
    )

    internal data class MemberData(
        @SerializedName("mid") val mid: String? = null,
        @SerializedName("uname") val uname: String? = null,
        @SerializedName("avatar") val avatar: String? = null,
        @SerializedName("sign") val sign: String? = null,
        @SerializedName("level_info") val level_info: LevelInfoData? = null,
        @SerializedName("official_verify") val official_verify: OfficialVerifyData? = null,
        @SerializedName("vip") val vip: VipData? = null,
        @SerializedName("user_senior") val user_senior: SeniorData? = null
    )

    internal data class LevelInfoData(
        @SerializedName("current_level") val current_level: Int = 0
    )

    internal data class OfficialVerifyData(
        @SerializedName("type") val type: Int = -1,
        @SerializedName("desc") val desc: String? = null
    )

    internal data class VipData(
        @SerializedName("vipType") val vipType: Int = 0,
        @SerializedName("nickname_color") val nickname_color: String? = null
    )

    internal data class SeniorData(
        @SerializedName("status") val status: Int = 0
    )

    internal data class ContentData(
        @SerializedName("message") val message: String? = null,
        @SerializedName("pictures") val pictures: List<PictureData>? = null,
        @SerializedName("emote") val emote: Map<String, EmoteData>? = null
    )

    internal data class EmoteData(
        @SerializedName("id") val id: Int = 0,
        @SerializedName("package_id") val packageId: Int = 0,
        @SerializedName("text") val text: String? = null,
        @SerializedName("url") val url: String? = null,
        @SerializedName("meta") val meta: EmoteMetaData? = null
    )

    internal data class EmoteMetaData(
        @SerializedName("size") val size: Int = 1,
        @SerializedName("alias") val alias: String? = null
    )

    internal data class PictureData(
        @SerializedName("img_src") val img_src: String? = null
    )

    internal data class ReplyControlData(
        @SerializedName("is_top") val is_top: Boolean = false
    )

    internal data class UpActionData(
        @SerializedName("like") val like: Boolean = false,
        @SerializedName("reply") val reply: Boolean = false
    )

    internal data class ReplyCountData(
        @SerializedName("count") val count: Long = 0
    )

    suspend fun getReplies(
        oid: Long,
        rpid: Long = 0,
        pageNumber: Int = 1,
        type: Int = REPLY_TYPE_VIDEO,
        sort: Int = 1
    ): List<Reply> = withContext(Dispatchers.IO) {
        val url = if (rpid > 0) {
            "https://api.bilibili.com/x/v2/reply/reply?oid=$oid&root=$rpid&pn=$pageNumber&type=$type&sort=$sort"
        } else {
            "https://api.bilibili.com/x/v2/reply?oid=$oid&pn=$pageNumber&type=$type&sort=$sort"
        }
        val json = httpGet(url)
        val typeToken = object : TypeToken<ApiResponse<ReplyListData>>() {}.type
        val resp: ApiResponse<ReplyListData>? = GsonConfig.gson.fromJson(json, typeToken)
        if (resp == null || !resp.isSuccess || resp.data == null) return@withContext emptyList()
        val isDynamic = type == REPLY_TYPE_DYNAMIC || type == REPLY_TYPE_DYNAMIC_CHILD
        val topList = resp.data.top_replies?.filterNotNull()?.map { parseReply(it, isDynamic, oid) } ?: emptyList()
        val normalList = resp.data.replies?.filterNotNull()?.map { parseReply(it, isDynamic, oid) } ?: emptyList()
        val topIds = topList.map { it.rpid }.toSet()
        topList + normalList.filter { it.rpid !in topIds }
    }

    suspend fun getRepliesLazy(
        oid: Long,
        rpid: Long = 0,
        pagination: String? = null,
        type: Int = REPLY_TYPE_VIDEO,
        sort: Int = 1
    ): Triple<Int, String?, List<Reply>> = withContext(Dispatchers.IO) {
        val params = mutableMapOf(
            "oid" to oid.toString(),
            "type" to type.toString(),
            "mode" to sort.toString()
        )
        if (rpid > 0) params["root"] = rpid.toString()
        if (!pagination.isNullOrEmpty()) params["pagination_str"] = "{\"offset\":\"$pagination\"}"
        val signedUrl = WbiSigner.signUrl(
            "https://api.bilibili.com/x/v2/reply/wbi/main?" +
                    params.entries.joinToString("&") { "${it.key}=${it.value}" }
        )
        val json = httpGet(signedUrl)
        val typeToken = object : TypeToken<ApiResponse<ReplyLazyData>>() {}.type
        val resp: ApiResponse<ReplyLazyData>? = GsonConfig.gson.fromJson(json, typeToken)
        if (resp == null || !resp.isSuccess || resp.data == null) return@withContext Triple(0, null, emptyList())
        val isDynamic = type == REPLY_TYPE_DYNAMIC || type == REPLY_TYPE_DYNAMIC_CHILD
        val count = resp.data.cursor?.all_count ?: 0
        val nextOffset = resp.data.cursor?.pagination_reply?.next_offset
        val topList = resp.data.top_replies?.filterNotNull()?.map { parseReply(it, isDynamic, oid) } ?: emptyList()
        val normalList = resp.data.replies?.filterNotNull()?.map { parseReply(it, isDynamic, oid) } ?: emptyList()
        val topIds = topList.map { it.rpid }.toSet()
        Triple(count, nextOffset, topList + normalList.filter { it.rpid !in topIds })
    }

    suspend fun sendReply(
        oid: Long,
        root: Long = 0,
        parent: Long = 0,
        text: String,
        type: Int = REPLY_TYPE_VIDEO
    ): Pair<Int, Reply?> = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("oid", oid.toString())
            .add("root", root.toString())
            .add("parent", parent.toString())
            .add("message", text)
            .add("type", type.toString())
            .add("csrf", CookieManager.getCsrf())
            .build()
        val request = Request.Builder()
            .url("https://api.bilibili.com/x/v2/reply/add")
            .post(body)
            .addHeader("Cookie", CookieManager.getCookie())
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Referer", "https://www.bilibili.com/")
            .build()
        val json = HttpClient.client.newCall(request).execute().body?.string() ?: ""
        val typeToken = object : TypeToken<ApiResponse<ReplyRootData>>() {}.type
        val resp: ApiResponse<ReplyRootData>? = GsonConfig.gson.fromJson(json, typeToken)
        if (resp == null) return@withContext Pair(-1, null)
        if (!resp.isSuccess) return@withContext Pair(resp.code, null)
        val isDynamic = type == REPLY_TYPE_DYNAMIC || type == REPLY_TYPE_DYNAMIC_CHILD
        val reply = resp.data?.let { parseReply(it, isDynamic, oid) }
        Pair(0, reply)
    }

    suspend fun likeReply(oid: Long, rpid: Long, action: Int): Int = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("oid", oid.toString())
            .add("rpid", rpid.toString())
            .add("type", "1")
            .add("action", action.toString())
            .add("csrf", CookieManager.getCsrf())
            .build()
        val request = Request.Builder()
            .url("https://api.bilibili.com/x/v2/reply/action")
            .post(body)
            .addHeader("Cookie", CookieManager.getCookie())
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Referer", "https://www.bilibili.com/")
            .build()
        val json = HttpClient.client.newCall(request).execute().body?.string() ?: ""
        val typeToken = object : TypeToken<ApiResponse<Unit>>() {}.type
        val resp: ApiResponse<Unit>? = GsonConfig.gson.fromJson(json, typeToken)
        resp?.code ?: -1
    }

    suspend fun deleteReply(oid: Long, rpid: Long, type: Int = REPLY_TYPE_VIDEO): Int = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("oid", oid.toString())
            .add("rpid", rpid.toString())
            .add("type", type.toString())
            .add("csrf", CookieManager.getCsrf())
            .build()
        val request = Request.Builder()
            .url("https://api.bilibili.com/x/v2/reply/del")
            .post(body)
            .addHeader("Cookie", CookieManager.getCookie())
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Referer", "https://www.bilibili.com/")
            .build()
        val json = HttpClient.client.newCall(request).execute().body?.string() ?: ""
        val typeToken = object : TypeToken<ApiResponse<Unit>>() {}.type
        val resp: ApiResponse<Unit>? = GsonConfig.gson.fromJson(json, typeToken)
        resp?.code ?: -1
    }

    suspend fun getReplyCount(oid: Long, type: Int = REPLY_TYPE_VIDEO): Long = withContext(Dispatchers.IO) {
        val url = "https://api.bilibili.com/x/v2/reply/count?oid=$oid&type=$type"
        val json = httpGet(url)
        val typeToken = object : TypeToken<ApiResponse<ReplyCountData>>() {}.type
        val resp: ApiResponse<ReplyCountData>? = GsonConfig.gson.fromJson(json, typeToken)
        if (resp == null || !resp.isSuccess || resp.data == null) return@withContext 0L
        resp.data.count
    }

    private fun parseReply(data: ReplyRootData, isDynamic: Boolean, oid: Long): Reply {
        val mid = data.member?.mid?.toLongOrNull() ?: 0
        val sender = UserInfo(
            mid = mid,
            name = data.member?.uname ?: "",
            avatar = data.member?.avatar ?: "",
            sign = data.member?.sign ?: "",
            level = data.member?.level_info?.current_level ?: 0,
            official = data.member?.official_verify?.type ?: -1,
            officialDesc = data.member?.official_verify?.desc ?: "",
            vip_role = data.member?.vip?.vipType ?: 0,
            vip_nickname_color = data.member?.vip?.nickname_color ?: "",
            is_senior_member = data.member?.user_senior?.status ?: 0
        )
        val pictures = data.content?.pictures?.filterNotNull()?.map { it.img_src ?: "" } ?: emptyList()
        val pubTime = if (data.ctime > 0) {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(data.ctime * 1000)
        } else ""
        val childList = data.replies?.filterNotNull()?.map { parseReply(it, isDynamic, oid) } ?: emptyList()
        return Reply(
            rpid = data.rpid,
            oid = data.oid,
            root = data.root,
            parent = data.parent,
            pubTime = pubTime,
            sender = sender,
            message = data.content?.message ?: "",
            pictureList = pictures,
            likeCount = data.like,
            upLiked = data.up_action?.like ?: false,
            upReplied = data.up_action?.reply ?: false,
            liked = data.action == 1,
            childCount = data.rcount,
            isDynamic = isDynamic,
            childMsgList = childList,
            isTop = data.reply_control?.is_top ?: false,
            emotes = data.content?.emote?.mapValues { 
                Emote(
                    id = it.value.id,
                    packageId = it.value.packageId,
                    name = it.key,
                    alias = it.value.meta?.alias ?: "",
                    url = it.value.url ?: "",
                    size = it.value.meta?.size ?: 1
                ) 
            } ?: emptyMap()
        )
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
