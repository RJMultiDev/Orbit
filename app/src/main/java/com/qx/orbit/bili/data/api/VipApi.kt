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
import org.json.JSONObject

object VipApi {

    internal data class VipPrivilegeData(
        @SerializedName("type") val type: Int = 0,
        @SerializedName("state") val state: Int = 0,
        @SerializedName("expire_time") val expire_time: Long = 0
    )

    internal data class VipPrivilegeListData(
        @SerializedName("list") val list: List<VipPrivilegeData>? = null,
        @SerializedName("vip_type") val vip_type: Int = 0,
        @SerializedName("vip_status") val vip_status: Int = 0,
        @SerializedName("vip_due_date") val vip_due_date: Long = 0
    )

    suspend fun getVipInfo(): VipDetailInfo? = withContext(Dispatchers.IO) {
        val url = "https://api.bilibili.com/x/vip/privilege/my"
        val json = httpGet(url)
        val type = object : TypeToken<ApiResponse<VipPrivilegeListData>>() {}.type
        val resp: ApiResponse<VipPrivilegeListData>? = GsonConfig.gson.fromJson(json, type)
        if (resp == null || !resp.isSuccess || resp.data == null) return@withContext null
        val data = resp.data
        val privileges = data.list?.map {
            VipDetailInfo.Privilege(type = it.type, state = it.state, expireTime = it.expire_time)
        } ?: emptyList()
        VipDetailInfo(
            isVip = data.vip_status > 0,
            level = data.vip_type,
            vipStatus = data.vip_status,
            vipType = data.vip_type,
            vipDueDate = data.vip_due_date,
            privilegeList = privileges
        )
    }

    suspend fun addExperience(): JSONObject = withContext(Dispatchers.IO) {
        val url = "https://api.bilibili.com/x/vip/experience/add"
        val body = FormBody.Builder()
            .add("csrf", CookieManager.getCsrf())
            .build()
        val request = Request.Builder().url(url)
            .post(body)
            .addHeader("Cookie", CookieManager.getCookie())
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Referer", "https://www.bilibili.com/")
            .build()
        val response = HttpClient.client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return@withContext JSONObject()
        try {
            JSONObject(responseBody)
        } catch (_: Exception) {
            JSONObject()
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
