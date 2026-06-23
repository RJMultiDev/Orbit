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
import org.json.JSONArray
import org.json.JSONObject

object EmoteApi {

    internal data class EmotePanelData(
        @SerializedName("packages") val packages: List<EmotePackageData>? = null
    )

    internal data class EmotePackageData(
        @SerializedName("id") val id: Int = 0,
        @SerializedName("text") val text: String? = null,
        @SerializedName("url") val url: String? = null,
        @SerializedName("type") val type: Int = 0,
        @SerializedName("attr") val attr: Int = 0,
        @SerializedName("size") val size: Int = 0,
        @SerializedName("item_id") val item_id: Int = 0,
        @SerializedName("emotes") val emotes: List<EmoteData>? = null,
        @SerializedName("permanent") val permanent: Boolean = false
    )

    internal data class EmoteData(
        @SerializedName("id") val id: Int = 0,
        @SerializedName("package_id") val package_id: Int = 0,
        @SerializedName("text") val text: String? = null,
        @SerializedName("url") val url: String? = null,
        @SerializedName("size") val size: Int = 0
    )

    internal data class InUsePackagesData(
        @SerializedName("packages") val packages: List<EmotePackageData>? = null
    )

    suspend fun getEmotes(business: String): List<EmotePackage> = withContext(Dispatchers.IO) {
        val url = "https://api.bilibili.com/x/emote/user/panel/web?business=$business"
        val json = httpGet(url)
        val type = object : TypeToken<ApiResponse<EmotePanelData>>() {}.type
        val resp: ApiResponse<EmotePanelData>? = GsonConfig.gson.fromJson(json, type)
        resp?.data?.packages?.map { pkg ->
            EmotePackage(
                id = pkg.id,
                text = pkg.text ?: "",
                url = pkg.url ?: "",
                type = pkg.type,
                attr = pkg.attr,
                size = pkg.size,
                item_id = pkg.item_id,
                emotes = pkg.emotes?.map { emote ->
                    Emote(
                        id = emote.id,
                        packageId = emote.package_id,
                        name = emote.text ?: "",
                        alias = emote.text ?: "",
                        url = emote.url ?: "",
                        size = emote.size
                    )
                } ?: emptyList(),
                permanent = pkg.permanent
            )
        } ?: emptyList()
    }

    suspend fun getInUsePackages(business: String): List<EmotePackage> = withContext(Dispatchers.IO) {
        val url = "https://api.bilibili.com/bapis/main.community.interface.emote.EmoteService/InUsePackages?business=$business"
        val json = httpGet(url)
        val type = object : TypeToken<ApiResponse<InUsePackagesData>>() {}.type
        val resp: ApiResponse<InUsePackagesData>? = GsonConfig.gson.fromJson(json, type)
        resp?.data?.packages?.map { pkg ->
            EmotePackage(
                id = pkg.id,
                text = pkg.text ?: "",
                url = pkg.url ?: "",
                type = pkg.type,
                attr = pkg.attr,
                size = pkg.size,
                item_id = pkg.item_id,
                emotes = pkg.emotes?.map { emote ->
                    Emote(
                        id = emote.id,
                        packageId = emote.package_id,
                        name = emote.text ?: "",
                        alias = emote.text ?: "",
                        url = emote.url ?: "",
                        size = emote.size
                    )
                } ?: emptyList(),
                permanent = pkg.permanent
            )
        } ?: emptyList()
    }

    suspend fun analyzeEmotePackages(packages: JSONArray): List<EmotePackage> = withContext(Dispatchers.IO) {
        val result = mutableListOf<EmotePackage>()
        for (i in 0 until packages.length()) {
            val pkg = packages.optJSONObject(i) ?: continue
            val emotesArray = pkg.optJSONArray("emotes")
            val emotes = mutableListOf<Emote>()
            if (emotesArray != null) {
                for (j in 0 until emotesArray.length()) {
                    val emoteObj = emotesArray.optJSONObject(j) ?: continue
                    emotes.add(
                        Emote(
                            id = emoteObj.optInt("id"),
                            packageId = emoteObj.optInt("package_id"),
                            name = emoteObj.optString("text") ?: "",
                            alias = emoteObj.optString("text") ?: "",
                            url = emoteObj.optString("url") ?: "",
                            size = emoteObj.optInt("size")
                        )
                    )
                }
            }
            result.add(
                EmotePackage(
                    id = pkg.optInt("id"),
                    text = pkg.optString("text") ?: "",
                    url = pkg.optString("url") ?: "",
                    type = pkg.optInt("type"),
                    attr = pkg.optInt("attr"),
                    size = pkg.optInt("size"),
                    item_id = pkg.optInt("item_id"),
                    emotes = emotes,
                    permanent = pkg.optBoolean("permanent")
                )
            )
        }
        result
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
