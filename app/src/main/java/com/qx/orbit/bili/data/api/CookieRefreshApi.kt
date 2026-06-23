package com.qx.orbit.bili.data.api

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

object CookieRefreshApi {

    internal data class CookieInfoData(
        @SerializedName("refresh") val refresh: Boolean = false,
        @SerializedName("timestamp") val timestamp: Long = 0
    )

    suspend fun cookieInfo(): JSONObject = withContext(Dispatchers.IO) {
        val url = "https://passport.bilibili.com/x/passport-login/web/cookie/info"
        val json = httpGet(url)
        try {
            JSONObject(json)
        } catch (_: Exception) {
            JSONObject()
        }
    }

    suspend fun refreshCookie(csrf: String): Boolean = withContext(Dispatchers.IO) {
        val url = "https://passport.bilibili.com/x/passport-login/web/cookie/refresh"
        val body = FormBody.Builder()
            .add("csrf", csrf)
            .build()
        val request = Request.Builder().url(url)
            .post(body)
            .addHeader("Cookie", CookieManager.getCookie())
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Referer", "https://www.bilibili.com/")
            .build()
        val response = HttpClient.client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return@withContext false
        try {
            val json = JSONObject(responseBody)
            if (json.optInt("code") != 0) return@withContext false
            val setCookies = response.headers.values("Set-Cookie")
            for (cookie in setCookies) {
                val parts = cookie.split(";")[0].split("=", limit = 2)
                if (parts.size == 2) {
                    CookieManager.putCookie(parts[0].trim(), parts[1].trim())
                }
            }
            val confirmBody = FormBody.Builder()
                .add("csrf", CookieManager.getCsrf())
                .build()
            val confirmRequest = Request.Builder()
                .url("https://passport.bilibili.com/x/passport-login/web/confirm/refresh")
                .post(confirmBody)
                .addHeader("Cookie", CookieManager.getCookie())
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Referer", "https://www.bilibili.com/")
                .build()
            HttpClient.client.newCall(confirmRequest).execute().body?.string()
            true
        } catch (_: Exception) {
            false
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
