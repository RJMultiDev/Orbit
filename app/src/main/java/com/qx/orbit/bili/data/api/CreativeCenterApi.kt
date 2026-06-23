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
import org.json.JSONObject

object CreativeCenterApi {

    internal data class ScrollsData(
        @SerializedName("be_up") val be_up: BeUpData? = null
    )

    internal data class BeUpData(
        @SerializedName("be_up_minutes") val be_up_minutes: Int = 0,
        @SerializedName("be_up_stat") val be_up_stat: Int = 0
    )

    suspend fun getVideoStat(): JSONObject? = withContext(Dispatchers.IO) {
        val url = "https://member.bilibili.com/x/web/index/stat"
        val json = httpGet(url)
        try {
            val obj = JSONObject(json)
            if (obj.optInt("code") == 0) obj.optJSONObject("data") else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getBeUPTime(): ApiResult = withContext(Dispatchers.IO) {
        val url = "https://member.bilibili.com/x/web/index/scrolls"
        val json = httpGet(url)
        val type = object : TypeToken<ApiResponse<ScrollsData>>() {}.type
        val resp: ApiResponse<ScrollsData>? = GsonConfig.gson.fromJson(json, type)
        if (resp == null || !resp.isSuccess || resp.data == null) {
            return@withContext ApiResult(code = resp?.code ?: -1, message = resp?.message ?: "")
        }
        ApiResult(
            code = 0,
            message = "ok",
            business = resp.data.be_up?.be_up_minutes.toString()
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
