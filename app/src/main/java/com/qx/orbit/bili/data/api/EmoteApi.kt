package com.qx.orbit.bili.data.api

import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.qx.orbit.bili.data.remote.GsonConfig
import com.qx.orbit.bili.data.remote.HttpClient
import com.qx.orbit.bili.data.remote.CookieManager
import okhttp3.Request
import com.qx.orbit.bili.data.model.ApiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object EmoteApi {
    const val BUSINESS_REPLY = "reply"
    const val BUSINESS_DYNAMIC = "dynamic"

    data class Emote(
        @SerializedName("id") val id: Int = 0,
        @SerializedName("package_id") val packageId: Int = 0,
        @SerializedName("text") val name: String = "",
        @SerializedName("url") val url: String = "",
        @SerializedName("meta") val meta: EmoteMeta? = null
    ) {
        data class EmoteMeta(
            @SerializedName("size") val size: Int = 1,
            @SerializedName("alias") val alias: String? = null
        )
    }

    data class EmotePackage(
        @SerializedName("id") val id: Int = 0,
        @SerializedName("text") val text: String = "",
        @SerializedName("url") val url: String = "",
        @SerializedName("type") val type: Int = 0,
        @SerializedName("attr") val attr: Int = 0,
        @SerializedName("emote") val emotes: List<Emote> = emptyList(),
        @SerializedName("meta") val meta: PackageMeta? = null,
        @SerializedName("flags") val flags: PackageFlags? = null
    ) {
        data class PackageMeta(
            @SerializedName("size") val size: Int = 1,
            @SerializedName("item_id") val itemId: Int = -1
        )
        data class PackageFlags(
            @SerializedName("permanent") val permanent: Boolean = false
        )
    }

    data class EmotePanelData(
        @SerializedName("packages") val packages: List<EmotePackage>? = null
    )

    suspend fun getEmotes(business: String): List<EmotePackage> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.bilibili.com/x/emote/user/panel/web?business=$business"
            val json = httpGet(url)
            val resp: ApiResponse<EmotePanelData>? = GsonConfig.gson.fromJson(json, object : TypeToken<ApiResponse<EmotePanelData>>() {}.type)
            if (resp != null && resp.isSuccess) {
                return@withContext resp.data?.packages ?: emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        emptyList()
    }

    private fun httpGet(url: String): String {
        val request = Request.Builder().url(url)
            .addHeader("Cookie", CookieManager.getCookie())
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.95 Safari/537.36")
            .addHeader("Referer", "https://www.bilibili.com/")
            .build()
        return HttpClient.client.newCall(request).execute().body?.string() ?: ""
    }
}
