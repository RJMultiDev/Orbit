package com.qx.orbit.bili.data.api

import com.qx.orbit.bili.data.remote.CookieManager
import com.qx.orbit.bili.data.remote.GsonConfig
import com.qx.orbit.bili.data.remote.HttpClient
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.qx.orbit.bili.data.model.ApiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object CookiesApi {

    internal data class BuvidData(
        @SerializedName("b_3") val b_3: String? = null
    )

    internal data class WebBuvidData(
        @SerializedName("b_3") val b_3: String? = null,
        @SerializedName("b_4") val b_4: String? = null
    )

    internal data class TicketData(
        @SerializedName("ticket") val ticket: String? = null,
        @SerializedName("created_at") val created_at: Int = 0
    )

    suspend fun activeCookieInfo(): Int = withContext(Dispatchers.IO) {
        val url = "https://api.bilibili.com/x/internal/gaia-gateway/ExClimbWuzhi"
        val body = FormBody.Builder()
            .add("payload", "{}")
            .add("csrf", CookieManager.getCsrf())
            .build()
        val request = Request.Builder().url(url)
            .post(body)
            .addHeader("Cookie", CookieManager.getCookie())
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Referer", "https://www.bilibili.com/")
            .build()
        val json = HttpClient.client.newCall(request).execute().body?.string() ?: ""
        val typeToken = object : TypeToken<ApiResponse<*>>() {}.type
        val resp: ApiResponse<*>? = GsonConfig.gson.fromJson(json, typeToken)
        resp?.code ?: -1
    }

    suspend fun getBuvid3Only(): String = withContext(Dispatchers.IO) {
        val url = "https://api.bilibili.com/x/web-frontend/getbuvid"
        val json = httpGet(url)
        val type = object : TypeToken<ApiResponse<BuvidData>>() {}.type
        val resp: ApiResponse<BuvidData>? = GsonConfig.gson.fromJson(json, type)
        resp?.data?.b_3 ?: ""
    }

    suspend fun getWebBuvids(): Pair<String, String> = withContext(Dispatchers.IO) {
        val url = "https://api.bilibili.com/x/frontend/finger/spi"
        val json = httpGet(url)
        val type = object : TypeToken<ApiResponse<WebBuvidData>>() {}.type
        val resp: ApiResponse<WebBuvidData>? = GsonConfig.gson.fromJson(json, type)
        Pair(resp?.data?.b_3 ?: "", resp?.data?.b_4 ?: "")
    }

    suspend fun genBiliTicket(): Pair<String, Int> = withContext(Dispatchers.IO) {
        val url = "https://api.bilibili.com/bapis/bilibili.api.ticket.v1.Ticket/GenWebTicket"
        val jsonBody = JSONObject().apply {
            put("key_id", "ec02")
        }.toString()
        val body = jsonBody.toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url)
            .post(body)
            .addHeader("Cookie", CookieManager.getCookie())
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Referer", "https://www.bilibili.com/")
            .build()
        val response = HttpClient.client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return@withContext Pair("", 0)
        try {
            val json = JSONObject(responseBody)
            val data = json.optJSONObject("data")
            Pair(data?.optString("ticket") ?: "", data?.optInt("created_at") ?: 0)
        } catch (_: Exception) {
            Pair("", 0)
        }
    }

    suspend fun checkCookies() = withContext(Dispatchers.IO) {
        activeCookieInfo()
        getBuvid3Only()
        val webBuvids = getWebBuvids()
        if (webBuvids.first.isNotEmpty()) {
            CookieManager.putCookie("buvid3", webBuvids.first)
        }
        if (webBuvids.second.isNotEmpty()) {
            CookieManager.putCookie("buvid4", webBuvids.second)
        }
        genBiliTicket()
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
