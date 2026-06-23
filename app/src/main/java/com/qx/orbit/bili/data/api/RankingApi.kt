package com.qx.orbit.bili.data.api

import com.qx.orbit.bili.data.model.*
import com.qx.orbit.bili.data.remote.GsonConfig
import com.qx.orbit.bili.data.remote.HttpClient
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

object RankingApi {

    internal data class RankingItem(
        @SerializedName("aid") val aid: Long = 0,
        @SerializedName("bvid") val bvid: String? = null,
        @SerializedName("cid") val cid: Long = 0,
        @SerializedName("title") val title: String? = null,
        @SerializedName("pic") val pic: String? = null,
        @SerializedName("duration") val duration: Int = 0,
        @SerializedName("owner") val owner: RecommendApi.Owner? = null,
        @SerializedName("stat") val stat: RecommendApi.Stat? = null
    )

    internal data class RankingResponse(
        @SerializedName("list") val list: List<RankingItem>? = null
    )

    suspend fun getRanking(rid: Int, type: String): List<VideoCard> = withContext(Dispatchers.IO) {
        val rawUrl = "https://api.bilibili.com/x/web-interface/ranking/v2?rid=$rid&type=$type"
        val url = ConfInfoApi.signWBI(rawUrl)
        val json = httpGet(url)
        val typeToken = object : TypeToken<ApiResponse<RankingResponse>>() {}.type
        val resp: ApiResponse<RankingResponse>? = GsonConfig.gson.fromJson(json, typeToken)
        if (resp == null || !resp.isSuccess || resp.data == null) return@withContext emptyList()
        resp.data.list?.filterNotNull()?.map { it.toVideoCard() } ?: emptyList()
    }

    private fun RankingItem.toVideoCard(): VideoCard = VideoCard(
        title = title ?: "",
        upName = owner?.name ?: "",
        view = StringUtil.toWan(stat?.view?.toLong() ?: 0),
        cover = pic ?: "",
        aid = aid,
        bvid = bvid ?: "",
        cid = cid
    )

    private fun httpGet(url: String): String {
        val request = Request.Builder().url(url)
            .addHeader("Cookie", com.qx.orbit.bili.data.remote.CookieManager.getCookie())
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.95 Safari/537.36")
            .addHeader("Referer", "https://www.bilibili.com/")
            .build()
        return HttpClient.client.newCall(request).execute().body?.string() ?: ""
    }
}
