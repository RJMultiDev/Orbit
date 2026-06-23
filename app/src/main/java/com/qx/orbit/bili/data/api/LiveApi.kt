package com.qx.orbit.bili.data.api

import com.qx.orbit.bili.data.model.*
import com.qx.orbit.bili.data.remote.CookieManager
import com.qx.orbit.bili.data.remote.GsonConfig
import com.qx.orbit.bili.data.remote.HttpClient
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

object LiveApi {

    internal data class LiveRoomListData(
        @SerializedName("list") val list: List<LiveRoom>? = null,
        @SerializedName("rooms") val rooms: List<LiveRoom>? = null
    )

    internal data class RoomInfoData(
        @SerializedName("room_id") val room_id: Long = 0,
        @SerializedName("short_id") val short_id: Long = 0,
        @SerializedName("uid") val uid: Long = 0,
        @SerializedName("title") val title: String? = null,
        @SerializedName("uname") val uname: String? = null,
        @SerializedName("tags") val tags: String? = null,
        @SerializedName("description") val description: String? = null,
        @SerializedName("online") val online: Int = 0,
        @SerializedName("attention") val attention: Int = 0,
        @SerializedName("user_cover") val user_cover: String? = null,
        @SerializedName("cover") val cover: String? = null,
        @SerializedName("keyframe") val keyframe: String? = null,
        @SerializedName("face") val face: String? = null,
        @SerializedName("area_id") val area_id: Int = 0,
        @SerializedName("area_name") val area_name: String? = null,
        @SerializedName("area_parent_id") val area_parent_id: Int = 0,
        @SerializedName("area_parent_name") val area_parent_name: String? = null,
        @SerializedName("live_status") val live_status: Int = 0,
        @SerializedName("live_time") val liveTime: String? = null,
        @SerializedName("is_portrait") val is_portrait: Boolean = false
    )

    internal data class RoomPlayInfoData(
        @SerializedName("room_id") val room_id: Long = 0,
        @SerializedName("short_id") val short_id: Long = 0,
        @SerializedName("uid") val uid: Long = 0,
        @SerializedName("is_hidden") val isHidden: Boolean = false,
        @SerializedName("is_locked") val isLocked: Boolean = false,
        @SerializedName("is_portrait") val isPortrait: Boolean = false,
        @SerializedName("live_status") val live_status: Int = 0,
        @SerializedName("encrypted") val encrypted: Boolean = false,
        @SerializedName("pwd_verified") val pwd_verified: Boolean = false,
        @SerializedName("live_time") val live_time: Long = 0,
        @SerializedName("playurl_info") val playurl_info: PlayurlInfo? = null,
        @SerializedName("official_type") val official_type: Int = 0,
        @SerializedName("official_room_id") val official_room_id: Int = 0,
        @SerializedName("risk_with_delay") val risk_with_delay: Int = 0
    )

    suspend fun getRecommend(page: Int): List<LiveRoom> = withContext(Dispatchers.IO) {
        val rawUrl = "https://api.live.bilibili.com/xlive/web-interface/v1/second/getUserRecommend?page=$page&page_size=10"
        val url = ConfInfoApi.signWBI(rawUrl)
        val json = httpGet(url)
        val type = object : TypeToken<ApiResponse<LiveRoomListData>>() {}.type
        val resp: ApiResponse<LiveRoomListData>? = GsonConfig.gson.fromJson(json, type)
        if (resp == null || !resp.isSuccess || resp.data == null) return@withContext emptyList()
        resp.data.list ?: resp.data.rooms ?: emptyList()
    }

    suspend fun getFollowed(page: Int): List<LiveRoom> = withContext(Dispatchers.IO) {
        val url = "https://api.live.bilibili.com/xlive/web-ucenter/v1/xfetter/GetWebList?page=$page&page_size=10"
        val json = httpGet(url)
        val type = object : TypeToken<ApiResponse<LiveRoomListData>>() {}.type
        val resp: ApiResponse<LiveRoomListData>? = GsonConfig.gson.fromJson(json, type)
        if (resp == null || !resp.isSuccess || resp.data == null) return@withContext emptyList()
        resp.data.list ?: resp.data.rooms ?: emptyList()
    }

    suspend fun getRoomInfo(roomId: Long): LiveRoom? = withContext(Dispatchers.IO) {
        val url = "https://api.live.bilibili.com/room/v1/Room/get_info?room_id=$roomId"
        val json = httpGet(url)
        val type = object : TypeToken<ApiResponse<RoomInfoData>>() {}.type
        val resp: ApiResponse<RoomInfoData>? = GsonConfig.gson.fromJson(json, type)
        if (resp == null || !resp.isSuccess || resp.data == null) return@withContext null
        val d = resp.data
        LiveRoom(
            roomid = d.room_id,
            short_id = d.short_id,
            uid = d.uid,
            title = d.title ?: "",
            uname = d.uname ?: "",
            tags = d.tags ?: "",
            description = d.description ?: "",
            online = d.online,
            attention = d.attention,
            user_cover = d.user_cover ?: "",
            cover = d.cover ?: "",
            keyframe = d.keyframe ?: "",
            face = d.face ?: "",
            area_parent_id = d.area_parent_id,
            area_parent_name = d.area_parent_name ?: "",
            area_id = d.area_id,
            area_name = d.area_name ?: "",
            live_status = d.live_status,
            liveTime = d.liveTime ?: "",
            is_portrait = d.is_portrait
        )
    }

    suspend fun getRoomPlayInfo(roomId: Long, qn: Int): LivePlayInfo? = withContext(Dispatchers.IO) {
        val url = "https://api.live.bilibili.com/xlive/web-room/v2/index/getRoomPlayInfo?room_id=$roomId&qn=$qn"
        val json = httpGet(url)
        val type = object : TypeToken<ApiResponse<RoomPlayInfoData>>() {}.type
        val resp: ApiResponse<RoomPlayInfoData>? = GsonConfig.gson.fromJson(json, type)
        if (resp == null || !resp.isSuccess || resp.data == null) return@withContext null
        val d = resp.data
        LivePlayInfo(
            roomid = d.room_id,
            short_id = d.short_id,
            uid = d.uid,
            isHidden = d.isHidden,
            isLocked = d.isLocked,
            isPortrait = d.isPortrait,
            live_status = d.live_status,
            encrypted = d.encrypted,
            pwd_verified = d.pwd_verified,
            live_time = d.live_time,
            playurl_info = d.playurl_info,
            official_type = d.official_type,
            official_room_id = d.official_room_id,
            risk_with_delay = d.risk_with_delay
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
