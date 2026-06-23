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

object HistoryApi {

    internal data class HistoryData(
        @SerializedName("list") val list: List<HistoryItem>? = null,
        @SerializedName("cursor") val cursor: CursorData? = null
    )

    internal data class HistoryItem(
        @SerializedName("title") val title: String? = null,
        @SerializedName("author") val author: HistoryRef? = null,
        @SerializedName("pic") val pic: String? = null,
        @SerializedName("stat") val stat: HistoryStat? = null,
        @SerializedName("aid") val aid: Long = 0,
        @SerializedName("bvid") val bvid: String? = null,
        @SerializedName("cid") val cid: Long = 0,
        @SerializedName("progress") val progress: Int = 0
    )

    internal data class HistoryRef(
        @SerializedName("name") val name: String? = null
    )

    internal data class HistoryStat(
        @SerializedName("view") val view: Int = 0
    )

    internal data class CursorData(
        @SerializedName("max") val max: Long = 0,
        @SerializedName("view_at") val view_at: Long = 0,
        @SerializedName("business") val business: String? = null,
        @SerializedName("is_end") val is_end: Boolean = false
    )

    suspend fun reportHistory(aid: Long, cid: Long, progress: Long, privacy: Boolean = false) = withContext(Dispatchers.IO) {
        if (privacy) return@withContext

        val body = FormBody.Builder()
            .add("aid", aid.toString())
            .add("cid", cid.toString())
            .add("progress", progress.toString())
            .add("csrf", CookieManager.getCsrf())
            .build()
        val request = Request.Builder()
            .url("https://api.bilibili.com/x/v2/history/report")
            .post(body)
            .addHeader("Cookie", CookieManager.getCookie())
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Referer", "https://www.bilibili.com/")
            .build()
        HttpClient.client.newCall(request).execute().body?.string()
    }

    suspend fun getHistory(lastResult: ApiResult, videoList: List<VideoCard>): ApiResult = withContext(Dispatchers.IO) {
        val url = "https://api.bilibili.com/x/web-interface/history/cursor?max=${lastResult.offset}&view_at=${lastResult.timestamp}&business=${lastResult.business}"
        val json = httpGet(url)
        val resp: ApiResponse<HistoryData>? = GsonConfig.gson.fromJson(json, object : TypeToken<ApiResponse<HistoryData>>() {}.type)
        if (resp == null || !resp.isSuccess || resp.data == null) return@withContext ApiResult(code = resp?.code ?: -1, message = resp?.message ?: "")

        ApiResult(
            code = resp.code,
            offset = resp.data.cursor?.max ?: 0,
            timestamp = resp.data.cursor?.view_at ?: 0,
            business = resp.data.cursor?.business ?: "",
            isBottom = resp.data.cursor?.is_end ?: (resp.data.list.isNullOrEmpty())
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
