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

object CoinLogApi {

    internal data class CoinLogData(
        @SerializedName("list") val list: List<CoinLogItem>? = null
    )

    internal data class CoinLogItem(
        @SerializedName("time") val time: String? = null,
        @SerializedName("delta") val delta: Int = 0,
        @SerializedName("reason") val reason: String? = null
    )

    suspend fun getCoinLog(): List<CoinLog> = withContext(Dispatchers.IO) {
        val url = "https://api.bilibili.com/x/member/web/coin/log"
        val json = httpGet(url)
        val type = object : TypeToken<ApiResponse<CoinLogData>>() {}.type
        val resp: ApiResponse<CoinLogData>? = GsonConfig.gson.fromJson(json, type)
        resp?.data?.list?.map {
            CoinLog(time = it.time ?: "", delta = it.delta, reason = it.reason ?: "")
        } ?: emptyList()
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
