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

object WatchLaterApi {

    internal data class WatchLaterData(
        @SerializedName("list") val list: List<WatchLaterItem>? = null
    )

    internal data class WatchLaterItem(
        @SerializedName("aid") val aid: Long = 0,
        @SerializedName("title") val title: String? = null,
        @SerializedName("pic") val pic: String? = null,
        @SerializedName("owner") val owner: WatchLaterOwner? = null,
        @SerializedName("stat") val stat: WatchLaterStat? = null,
        @SerializedName("bvid") val bvid: String? = null,
        @SerializedName("cid") val cid: Long = 0
    )

    internal data class WatchLaterOwner(
        @SerializedName("name") val name: String? = null
    )

    internal data class WatchLaterStat(
        @SerializedName("view") val view: Int = 0
    )

    suspend fun getWatchLaterList(): List<VideoCard> = withContext(Dispatchers.IO) {
        val url = "https://api.bilibili.com/x/v2/history/toview/web"
        val json = httpGet(url)
        val resp: ApiResponse<WatchLaterData>? = GsonConfig.gson.fromJson(json, object : TypeToken<ApiResponse<WatchLaterData>>() {}.type)
        if (resp == null || !resp.isSuccess || resp.data == null) return@withContext emptyList()
        resp.data.list?.filterNotNull()?.map {
            VideoCard(title = it.title ?: "", upName = it.owner?.name ?: "", view = StringUtil.toWan(it.stat?.view?.toLong() ?: 0), cover = it.pic ?: "", aid = it.aid, bvid = it.bvid ?: "", cid = it.cid)
        } ?: emptyList()
    }

    suspend fun delete(aid: Long): Int = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("aid", aid.toString())
            .add("csrf", CookieManager.getCsrf())
            .build()
        val request = Request.Builder()
            .url("https://api.bilibili.com/x/v2/history/toview/del")
            .post(body)
            .addHeader("Cookie", CookieManager.getCookie())
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Referer", "https://www.bilibili.com/")
            .build()
        val json = HttpClient.client.newCall(request).execute().body?.string() ?: ""
        val resp: ApiResponse<*>? = GsonConfig.gson.fromJson(json, object : TypeToken<ApiResponse<*>>() {}.type)
        resp?.code ?: -1
    }

    suspend fun add(aid: Long): Int = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("aid", aid.toString())
            .add("csrf", CookieManager.getCsrf())
            .build()
        val request = Request.Builder()
            .url("https://api.bilibili.com/x/v2/history/toview/add")
            .post(body)
            .addHeader("Cookie", CookieManager.getCookie())
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Referer", "https://www.bilibili.com/")
            .build()
        val json = HttpClient.client.newCall(request).execute().body?.string() ?: ""
        val resp: ApiResponse<*>? = GsonConfig.gson.fromJson(json, object : TypeToken<ApiResponse<*>>() {}.type)
        resp?.code ?: -1
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
