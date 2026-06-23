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

object LoginRecordApi {

    internal data class LoginRecordData(
        @SerializedName("list") val list: List<LoginRecordItem>? = null
    )

    internal data class LoginRecordItem(
        @SerializedName("mid") val mid: Long = 0,
        @SerializedName("device") val device: String? = null,
        @SerializedName("login_type") val login_type: String? = null,
        @SerializedName("time") val time: String? = null,
        @SerializedName("location") val location: String? = null,
        @SerializedName("ip") val ip: String? = null
    )

    suspend fun getLoginRecord(mid: Long, buvid: String): List<LoginRecord> = withContext(Dispatchers.IO) {
        val url = "https://api.bilibili.com/x/safecenter/login_notice?mid=$mid&buvid=$buvid"
        val json = httpGet(url)
        val type = object : TypeToken<ApiResponse<LoginRecordData>>() {}.type
        val resp: ApiResponse<LoginRecordData>? = GsonConfig.gson.fromJson(json, type)
        resp?.data?.list?.map {
            LoginRecord(
                mid = it.mid,
                deviceName = it.device ?: "",
                loginType = it.login_type ?: "",
                loginTime = it.time ?: "",
                location = it.location ?: "",
                ip = it.ip ?: ""
            )
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
