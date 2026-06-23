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

object FollowApi {

    internal data class FollowListData(
        @SerializedName("list") val list: List<FollowItem>? = null,
        @SerializedName("total") val total: Int = 0
    )

    internal data class FollowItem(
        @SerializedName("mid") val mid: Long = 0,
        @SerializedName("uname") val uname: String? = null,
        @SerializedName("face") val face: String? = null,
        @SerializedName("sign") val sign: String? = null,
        @SerializedName("official_verify") val official_verify: UserInfoApi.OfficialInfo? = null,
        @SerializedName("vip") val vip: UserInfoApi.VipInfoData? = null,
        @SerializedName("follower") val follower: Int = 0
    )

    internal data class TagItem(
        @SerializedName("tagid") val tagid: Int = 0,
        @SerializedName("name") val name: String? = null,
        @SerializedName("count") val count: Int = 0
    )

    suspend fun getFollowingList(mid: Long, page: Int): Pair<Int, List<UserInfo>> = withContext(Dispatchers.IO) {
        val url = "https://api.bilibili.com/x/relation/followings?vmid=$mid&pn=$page&ps=20&order=desc"
        val json = httpGet(url)
        val type = object : TypeToken<ApiResponse<FollowListData>>() {}.type
        val resp: ApiResponse<FollowListData>? = GsonConfig.gson.fromJson(json, type)
        if (resp == null || !resp.isSuccess) return@withContext Pair(-1, emptyList<UserInfo>())
        val data = resp.data ?: return@withContext Pair(1, emptyList<UserInfo>())
        val list = data.list
        if (list.isNullOrEmpty()) return@withContext Pair(1, emptyList<UserInfo>())
        val users = list.map { item ->
            UserInfo(
                mid = item.mid,
                name = item.uname ?: "",
                avatar = item.face ?: "",
                sign = item.sign ?: "",
                fans = item.follower,
                official = item.official_verify?.type ?: -1,
                officialDesc = item.official_verify?.desc ?: "",
                vip_role = item.vip?.vipType ?: 0,
                vip_nickname_color = item.vip?.nickname_color ?: ""
            )
        }
        Pair(0, users)
    }

    suspend fun getFollowerList(mid: Long, page: Int): Pair<Int, List<UserInfo>> = withContext(Dispatchers.IO) {
        val url = "https://api.bilibili.com/x/relation/followers?vmid=$mid&pn=$page&ps=20&order=desc"
        val json = httpGet(url)
        val type = object : TypeToken<ApiResponse<FollowListData>>() {}.type
        val resp: ApiResponse<FollowListData>? = GsonConfig.gson.fromJson(json, type)
        if (resp == null || !resp.isSuccess) return@withContext Pair(-1, emptyList<UserInfo>())
        val data = resp.data ?: return@withContext Pair(1, emptyList<UserInfo>())
        val list = data.list
        if (list.isNullOrEmpty()) return@withContext Pair(1, emptyList<UserInfo>())
        val users = list.map { item ->
            UserInfo(
                mid = item.mid,
                name = item.uname ?: "",
                avatar = item.face ?: "",
                sign = item.sign ?: "",
                fans = item.follower,
                official = item.official_verify?.type ?: -1,
                officialDesc = item.official_verify?.desc ?: "",
                vip_role = item.vip?.vipType ?: 0,
                vip_nickname_color = item.vip?.nickname_color ?: ""
            )
        }
        Pair(0, users)
    }

    suspend fun getFollowTags(): List<FollowTag> = withContext(Dispatchers.IO) {
        val url = "https://api.bilibili.com/x/relation/tags"
        val json = httpGet(url)
        val type = object : TypeToken<ApiResponse<List<TagItem>>>() {}.type
        val resp: ApiResponse<List<TagItem>>? = GsonConfig.gson.fromJson(json, type)
        resp?.data?.filterNotNull()?.map { item ->
            FollowTag(
                tagid = item.tagid,
                name = item.name ?: "",
                count = item.count
            )
        } ?: emptyList()
    }

    suspend fun getFollowTagUsers(tagid: Int, page: Int): Pair<Int, List<UserInfo>> = withContext(Dispatchers.IO) {
        val url = "https://api.bilibili.com/x/relation/tag?tagid=$tagid&pn=$page&ps=20"
        val json = httpGet(url)
        val type = object : TypeToken<ApiResponse<List<FollowItem>>>() {}.type
        val resp: ApiResponse<List<FollowItem>>? = GsonConfig.gson.fromJson(json, type)
        if (resp == null || !resp.isSuccess) return@withContext Pair(-1, emptyList<UserInfo>())
        val list = resp.data
        if (list.isNullOrEmpty()) return@withContext Pair(1, emptyList<UserInfo>())
        val users = list.filterNotNull().map { item ->
            UserInfo(
                mid = item.mid,
                name = item.uname ?: "",
                avatar = item.face ?: "",
                sign = item.sign ?: "",
                fans = item.follower,
                official = item.official_verify?.type ?: -1,
                officialDesc = item.official_verify?.desc ?: "",
                vip_role = item.vip?.vipType ?: 0,
                vip_nickname_color = item.vip?.nickname_color ?: ""
            )
        }
        Pair(0, users)
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
