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

object ArticleApi {

    internal data class ArticleViewData(
        @SerializedName("id") val id: Long = 0,
        @SerializedName("title") val title: String? = null,
        @SerializedName("summary") val summary: String? = null,
        @SerializedName("banner_url") val banner_url: String? = null,
        @SerializedName("author") val author: AuthorData? = null,
        @SerializedName("ctime") val ctime: Long = 0,
        @SerializedName("stats") val stats: StatsData? = null,
        @SerializedName("words") val words: Int = 0,
        @SerializedName("dynamic") val dynamic: String? = null,
        @SerializedName("content") val content: String? = null
    )

    internal data class AuthorData(
        @SerializedName("mid") val mid: Long = 0,
        @SerializedName("name") val name: String? = null,
        @SerializedName("face") val face: String? = null
    )

    internal data class StatsData(
        @SerializedName("view") val view: Int = 0,
        @SerializedName("like") val like: Int = 0,
        @SerializedName("reply") val reply: Int = 0,
        @SerializedName("coin") val coin: Int = 0,
        @SerializedName("share") val share: Int = 0,
        @SerializedName("favorite") val favorite: Int = 0
    )

    suspend fun getArticle(id: Long): ArticleInfo? = withContext(Dispatchers.IO) {
        val rawUrl = "https://api.bilibili.com/x/article/view?id=$id"
        val url = ConfInfoApi.signWBI(rawUrl)
        val json = httpGet(url)
        val typeToken = object : TypeToken<ApiResponse<ArticleViewData>>() {}.type
        val resp: ApiResponse<ArticleViewData>? = GsonConfig.gson.fromJson(json, typeToken)
        if (resp == null || !resp.isSuccess || resp.data == null) return@withContext null
        val data = resp.data
        val author = data.author
        val stats = data.stats
        ArticleInfo(
            id = data.id,
            title = data.title ?: "",
            summary = data.summary ?: "",
            banner = data.banner_url ?: "",
            upInfo = author?.let {
                UserInfo(mid = it.mid, name = it.name ?: "", avatar = it.face ?: "")
            },
            ctime = data.ctime,
            stats = stats?.let {
                Stats(
                    view = it.view, like = it.like, reply = it.reply,
                    coin = it.coin, share = it.share, favorite = it.favorite
                )
            },
            wordCount = data.words,
            keywords = data.dynamic ?: "",
            content = data.content ?: ""
        )
    }

    suspend fun like(cvid: Long, type: Int): Int = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("id", cvid.toString())
            .add("type", type.toString())
            .add("csrf", CookieManager.getCsrf())
            .build()
        val request = Request.Builder()
            .url("https://api.bilibili.com/x/article/like")
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

    suspend fun addCoin(cvid: Long, upid: Long, multiply: Int = 1): Int = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("aid", cvid.toString())
            .add("upid", upid.toString())
            .add("multiply", multiply.toString())
            .add("csrf", CookieManager.getCsrf())
            .build()
        val request = Request.Builder()
            .url("https://api.bilibili.com/x/web-interface/coin/add")
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

    suspend fun favorite(cvid: Long): Int = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("id", cvid.toString())
            .add("csrf", CookieManager.getCsrf())
            .build()
        val request = Request.Builder()
            .url("https://api.bilibili.com/x/article/favorites/add")
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

    suspend fun delFavorite(cvid: Long): Int = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("id", cvid.toString())
            .add("csrf", CookieManager.getCsrf())
            .build()
        val request = Request.Builder()
            .url("https://api.bilibili.com/x/article/favorites/del")
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
