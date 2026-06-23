package com.qx.orbit.bili.data.api

import com.qx.orbit.bili.data.model.*
import com.qx.orbit.bili.data.remote.GsonConfig
import com.qx.orbit.bili.data.remote.HttpClient
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray

object SearchApi {

    internal data class SearchVideoItem(
        @SerializedName("aid") val aid: Long = 0,
        @SerializedName("bvid") val bvid: String? = null,
        @SerializedName("title") val title: String? = null,
        @SerializedName("author") val author: String? = null,
        @SerializedName("pic") val pic: String? = null,
        @SerializedName("play") val play: Int = 0,
        @SerializedName("duration") val duration: String? = null,
        @SerializedName("cid") val cid: Long = 0
    )

    internal data class SearchUserItem(
        @SerializedName("mid") val mid: Long = 0,
        @SerializedName("uname") val uname: String? = null,
        @SerializedName("usign") val usign: String? = null,
        @SerializedName("fans") val fans: Int = 0,
        @SerializedName("level") val level: Int = 0,
        @SerializedName("upic") val upic: String? = null
    )

    internal data class SearchArticleItem(
        @SerializedName("id") val id: Long = 0,
        @SerializedName("title") val title: String? = null,
        @SerializedName("image_urls") val image_urls: List<String>? = null,
        @SerializedName("author_name") val author_name: String? = null,
        @SerializedName("view") val view: Int = 0
    )

    internal data class SearchLiveItem(
        @SerializedName("roomid") val roomid: Long = 0,
        @SerializedName("short_id") val short_id: Long = 0,
        @SerializedName("uid") val uid: Long = 0,
        @SerializedName("title") val title: String? = null,
        @SerializedName("uname") val uname: String? = null,
        @SerializedName("online") val online: Int = 0,
        @SerializedName("user_cover") val user_cover: String? = null,
        @SerializedName("cover") val cover: String? = null,
        @SerializedName("live_status") val live_status: Int = 0
    )

    internal data class SearchTypeData(
        @SerializedName("seid") val seid: String? = null,
        @SerializedName("page") val page: Int = 0,
        @SerializedName("pagesize") val pagesize: Int = 0,
        @SerializedName("numResults") val numResults: Int = 0,
        @SerializedName("result") val result: JsonElement? = null
    )

    internal data class SearchResult(
        @SerializedName("type") val type: String? = null,
        @SerializedName("data") val data: JsonElement? = null
    )

    internal data class TagItem(
        @SerializedName("tag_id") val tag_id: Long = 0,
        @SerializedName("tag_name") val tag_name: String? = null
    )

    internal data class DefaultSearchData(
        @SerializedName("id") val id: Long = 0,
        @SerializedName("show_name") val show_name: String? = null
    )

    suspend fun search(keyword: String, page: Int): JSONArray? = withContext(Dispatchers.IO) {
        val rawUrl = "https://api.bilibili.com/x/web-interface/wbi/search/all/v2?keyword=${java.net.URLEncoder.encode(keyword, "UTF-8")}&page=$page"
        val url = ConfInfoApi.signWBI(rawUrl)
        val json = httpGet(url)
        val jsonObj = org.json.JSONObject(json)
        if (jsonObj.optInt("code") != 0) return@withContext null
        val resultArray = jsonObj.optJSONObject("data")?.optJSONArray("result") ?: return@withContext null
        // search/all/v2 returns: [ { "result_type": "video", "data": [...] }, ... ]
        // Flatten into a single array with "type" on each item for downstream parsers
        val flattened = JSONArray()
        for (i in 0 until resultArray.length()) {
            val group = resultArray.optJSONObject(i) ?: continue
            val resultType = group.optString("result_type", "")
            val dataArr = group.optJSONArray("data") ?: continue
            for (j in 0 until dataArr.length()) {
                val item = dataArr.optJSONObject(j) ?: continue
                item.put("type", resultType)
                flattened.put(item)
            }
        }
        flattened
    }

    suspend fun searchType(keyword: String, page: Int, type: String): JsonElement? = withContext(Dispatchers.IO) {
        val rawUrl = "https://api.bilibili.com/x/web-interface/wbi/search/type?keyword=${java.net.URLEncoder.encode(keyword, "UTF-8")}&page=$page&search_type=$type"
        val url = ConfInfoApi.signWBI(rawUrl)
        val json = httpGet(url)
        val resp: ApiResponse<SearchTypeData>? = GsonConfig.gson.fromJson(json, object : TypeToken<ApiResponse<SearchTypeData>>() {}.type)
        if (resp == null || !resp.isSuccess || resp.data == null) return@withContext null
        resp.data.result
    }

    suspend fun getVideosFromSearchResult(input: JSONArray?, first: Boolean = false): List<VideoCard> = withContext(Dispatchers.IO) {
        if (input == null) return@withContext emptyList()
        val result = mutableListOf<VideoCard>()
        for (i in 0 until input.length()) {
            val obj = input.optJSONObject(i) ?: continue
            val type = obj.optString("type")
            if (type != "video") continue
            val item = GsonConfig.gson.fromJson(obj.toString(), SearchVideoItem::class.java) ?: continue
            result.add(VideoCard(
                title = htmlToString(item.title ?: ""),
                upName = item.author ?: "",
                view = StringUtil.toWan(item.play.toLong()),
                cover = fixCoverUrl(item.pic ?: ""),
                aid = item.aid,
                bvid = item.bvid ?: "",
                cid = item.cid
            ))
            if (first && result.isNotEmpty()) break
        }
        result
    }

    suspend fun getUsersFromSearchResult(input: JSONArray?): List<UserInfo> = withContext(Dispatchers.IO) {
        if (input == null) return@withContext emptyList()
        val result = mutableListOf<UserInfo>()
        for (i in 0 until input.length()) {
            val obj = input.optJSONObject(i) ?: continue
            val item = GsonConfig.gson.fromJson(obj.toString(), SearchUserItem::class.java) ?: continue
            result.add(UserInfo(
                mid = item.mid,
                name = item.uname ?: "",
                sign = item.usign ?: "",
                fans = item.fans,
                level = item.level,
                avatar = fixCoverUrl(item.upic ?: "")
            ))
        }
        result
    }

    suspend fun getArticlesFromSearchResult(input: JSONArray?): List<ArticleCard> = withContext(Dispatchers.IO) {
        if (input == null) return@withContext emptyList()
        val result = mutableListOf<ArticleCard>()
        for (i in 0 until input.length()) {
            val obj = input.optJSONObject(i) ?: continue
            val item = GsonConfig.gson.fromJson(obj.toString(), SearchArticleItem::class.java) ?: continue
            result.add(ArticleCard(
                title = htmlToString(item.title ?: ""),
                id = item.id,
                cover = fixCoverUrl(item.image_urls?.firstOrNull() ?: ""),
                upName = item.author_name ?: "",
                view = StringUtil.toWan(item.view.toLong())
            ))
        }
        result
    }

    suspend fun getLiveFromSearchResult(input: JSONArray?): List<LiveRoom> = withContext(Dispatchers.IO) {
        if (input == null) return@withContext emptyList()
        val result = mutableListOf<LiveRoom>()
        for (i in 0 until input.length()) {
            val obj = input.optJSONObject(i) ?: continue
            val item = GsonConfig.gson.fromJson(obj.toString(), SearchLiveItem::class.java) ?: continue
            val finalRoomId = if (item.roomid <= 0) item.short_id else item.roomid
            result.add(LiveRoom(
                roomid = finalRoomId,
                short_id = item.short_id,
                uid = item.uid,
                title = htmlToString(item.title ?: ""),
                uname = item.uname ?: "",
                online = item.online,
                user_cover = fixCoverUrl(item.user_cover ?: ""),
                cover = fixCoverUrl(item.cover ?: ""),
                live_status = item.live_status
            ))
        }
        result
    }

    suspend fun getSearchSuggestions(keyword: String): List<String> = withContext(Dispatchers.IO) {
        val url = "https://s.search.bilibili.com/main/suggest?term=${java.net.URLEncoder.encode(keyword, "UTF-8")}"
        val json = httpGet(url)
        val jsonObj = org.json.JSONObject(json)
        val tags = jsonObj.optJSONArray("tag") ?: return@withContext emptyList()
        val result = mutableListOf<String>()
        for (i in 0 until tags.length()) {
            val obj = tags.optJSONObject(i) ?: continue
            obj.optString("value").takeIf { it.isNotEmpty() }?.let { result.add(it) }
        }
        result
    }

    suspend fun getDefaultSearchContent(): String? = withContext(Dispatchers.IO) {
        val rawUrl = "https://api.bilibili.com/x/web-interface/wbi/search/default"
        val url = ConfInfoApi.signWBI(rawUrl)
        val json = httpGet(url)
        val resp: ApiResponse<DefaultSearchData>? = GsonConfig.gson.fromJson(json, object : TypeToken<ApiResponse<DefaultSearchData>>() {}.type)
        if (resp == null || !resp.isSuccess || resp.data == null) return@withContext null
        resp.data.show_name
    }

    private fun htmlToString(input: String): String = input
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&#39;", "'")
        .replace("&quot;", "\"")
        .replace("&amp;", "&")

    private fun fixCoverUrl(url: String): String {
        if (url.startsWith("//")) return "https:$url"
        return url
    }

    private fun httpGet(url: String): String {
        val request = Request.Builder().url(url)
            .addHeader("Cookie", com.qx.orbit.bili.data.remote.CookieManager.getCookie())
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.95 Safari/537.36")
            .addHeader("Referer", "https://www.bilibili.com/")
            .build()
        return HttpClient.client.newCall(request).execute().body?.string() ?: ""
    }
}
