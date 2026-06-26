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
import java.text.SimpleDateFormat
import java.util.*

object DynamicApi {

    internal data class DynamicListData(
        @SerializedName("items") val items: List<JSONObject>? = null,
        @SerializedName("has_more") val has_more: Boolean = false,
        @SerializedName("offset") val offset: String? = null
    )

    internal data class DynamicDetailData(
        @SerializedName("item") val item: JSONObject? = null
    )

    internal data class UpdateData(
        @SerializedName("update_num") val update_num: Int = 0
    )

    internal data class PortalData(
        @SerializedName("up_list") val up_list: List<UpInfo>? = null
    )

    internal data class DynamicIdData(
        @SerializedName("dynamic_id") val dynamic_id: Long = 0
    )

    internal data class MentionData(
        @SerializedName("groups") val groups: List<MentionGroup>? = null
    )

    internal data class MentionDataInner(
        @SerializedName("data") val data: MentionData? = null
    )

    internal data class MentionGroup(
        @SerializedName("group_name") val group_name: String? = null,
        @SerializedName("items") val items: List<MentionItem>? = null
    )

    internal data class MentionItem(
        @SerializedName("mid") val mid: Long = 0,
        @SerializedName("name") val name: String? = null,
        @SerializedName("face") val face: String? = null
    )

    data class UpInfo(
        @SerializedName("mid") val mid: Long = 0,
        @SerializedName("name") val name: String? = null,
        @SerializedName("face") val face: String? = null,
        @SerializedName("has_update") val has_update: Boolean = false
    )

    suspend fun publishTextContent(content: String): Long = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("dynamic_id", "0")
            .add("type", "4")
            .add("rid", "0")
            .add("content", content)
            .add("csrf", CookieManager.getCsrf())
            .build()
        val request = Request.Builder()
            .url("https://api.bilibili.com/x/dynamic/feed/create/dyn")
            .post(body)
            .addHeader("Cookie", CookieManager.getCookie())
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Referer", "https://www.bilibili.com/")
            .build()
        val json = HttpClient.client.newCall(request).execute().body?.string() ?: ""
        val jsonObj = JSONObject(json)
        if (jsonObj.optInt("code") != 0) return@withContext -1L
        jsonObj.optJSONObject("data")?.optLong("dynamic_id", 0) ?: 0L
    }

    suspend fun likeDynamic(dyid: Long, up: Boolean): Int = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("dynamic_id", dyid.toString())
            .add("up", if (up) "1" else "0")
            .add("csrf", CookieManager.getCsrf())
            .build()
        val request = Request.Builder()
            .url("https://api.bilibili.com/x/dynamic/feed/item/thumb")
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

    suspend fun deleteDynamic(dyid: Long): Int = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("dynamic_id", dyid.toString())
            .add("csrf", CookieManager.getCsrf())
            .build()
        val request = Request.Builder()
            .url("https://api.bilibili.com/x/dynamic/feed/dyn/delete")
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

    suspend fun mentionAtFindUser(name: String): Long = withContext(Dispatchers.IO) {
        val url = "https://api.bilibili.com/x/polymer/web-dynamic/v1/mention/search?keyword=${java.net.URLEncoder.encode(name, "UTF-8")}"
        val json = httpGet(url)
        val jsonObj = JSONObject(json)
        if (jsonObj.optInt("code") != 0) return@withContext 0L
        val data = jsonObj.optJSONObject("data") ?: return@withContext 0L
        val groups = data.optJSONArray("groups") ?: return@withContext 0L
        if (groups.length() == 0) return@withContext 0L
        val firstGroup = groups.optJSONObject(0) ?: return@withContext 0L
        val items = firstGroup.optJSONArray("items") ?: return@withContext 0L
        if (items.length() == 0) return@withContext 0L
        items.optJSONObject(0)?.optLong("mid", 0) ?: 0L
    }

    suspend fun getDynamicList(
        offset: String = "",
        mid: Long = 0,
        type: Int = 0
    ): Pair<Long, List<Dynamic>> = withContext(Dispatchers.IO) {
        val rawUrl = if (mid > 0) {
            "https://api.bilibili.com/x/polymer/web-dynamic/v1/feed/space?host_mid=$mid&offset=$offset"
        } else {
            var params = "offset=$offset"
            if (type > 0) params += "&type=$type"
            "https://api.bilibili.com/x/polymer/web-dynamic/v1/feed/all?$params"
        }
        val url = ConfInfoApi.signWBI(rawUrl)
        val json = httpGet(url)
        val jsonObj = JSONObject(json)
        if (jsonObj.optInt("code") != 0) return@withContext Pair(0L, emptyList())
        val data = jsonObj.optJSONObject("data") ?: return@withContext Pair(0L, emptyList())
        val items = data.optJSONArray("items")
        val hasMore = data.optBoolean("has_more", false)
        val newOffset = data.optLong("offset", 0)
        val dynamicList = mutableListOf<Dynamic>()
        if (items != null) {
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                try {
                    dynamicList.add(analyzeDynamic(item))
                } catch (_: Exception) {
                }
            }
        }
        Pair(if (hasMore) newOffset else 0L, dynamicList)
    }

    suspend fun getDynamic(id: Long): Dynamic? = withContext(Dispatchers.IO) {
        val url = "https://api.bilibili.com/x/polymer/web-dynamic/v1/detail?id=$id"
        val json = httpGet(url)
        val jsonObj = JSONObject(json)
        if (jsonObj.optInt("code") != 0) return@withContext null
        val data = jsonObj.optJSONObject("data") ?: return@withContext null
        val item = data.optJSONObject("item") ?: return@withContext null
        try {
            analyzeDynamic(item)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun checkDynamicUpdate(type: Int, updateBaseline: String): Int = withContext(Dispatchers.IO) {
        val rawUrl = "https://api.bilibili.com/x/polymer/web-dynamic/v1/feed/all/update?type=$type&update_baseline=$updateBaseline"
        val url = ConfInfoApi.signWBI(rawUrl)
        val json = httpGet(url)
        val jsonObj = JSONObject(json)
        if (jsonObj.optInt("code") != 0) return@withContext 0
        jsonObj.optJSONObject("data")?.optInt("update_num", 0) ?: 0
    }

    suspend fun getRecentUpList(): List<UpInfo> = withContext(Dispatchers.IO) {
        val url = "https://api.bilibili.com/x/polymer/web-dynamic/v1/portal"
        val json = httpGet(url)
        val jsonObj = JSONObject(json)
        if (jsonObj.optInt("code") != 0) return@withContext emptyList()
        val data = jsonObj.optJSONObject("data") ?: return@withContext emptyList()
        val upList = data.optJSONArray("up_list") ?: return@withContext emptyList()
        val result = mutableListOf<UpInfo>()
        for (i in 0 until upList.length()) {
            val obj = upList.optJSONObject(i) ?: continue
            result.add(UpInfo(
                mid = obj.optLong("mid", 0),
                name = obj.optString("name", ""),
                face = obj.optString("face", ""),
                has_update = obj.optBoolean("has_update", false)
            ))
        }
        result
    }

    private fun analyzeDynamic(json: JSONObject): Dynamic {
        val dynamicId = json.optLong("id_str", 0)
        val type = json.optString("type", "")
        val modules = json.optJSONObject("modules") ?: JSONObject()

        val authorModule = modules.optJSONObject("module_author") ?: JSONObject()
        val mid = authorModule.optLong("mid", 0)
        val name = authorModule.optString("name", "")
        val face = authorModule.optString("face", "")
        val pubTs = authorModule.optLong("pub_ts", 0)
        val pubTime = if (pubTs > 0) {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(pubTs * 1000)
        } else ""
        val descText = authorModule.optString("desc", "")
        val canDelete = authorModule.optBoolean("is_top", false)

        val dynamicModule = modules.optJSONObject("module_dynamic") ?: JSONObject()
        val majorObj = dynamicModule.optJSONObject("major")
        val majorType = majorObj?.optString("type", "") ?: ""
        val topic = dynamicModule.optJSONObject("topic")
        val topicTitle = topic?.optString("name", "") ?: ""

        val desc = dynamicModule.optJSONObject("desc")
        var content = desc?.optString("text", "") ?: ""

        val majorObject: Any? = null
        val commentId: Long
        val commentType: Int
        val images = mutableListOf<String>()
        var cover = ""
        var bvid = ""
        var archiveTitle = ""

        when (majorType) {
            "MAJOR_TYPE_ARCHIVE" -> {
                val archive = majorObj?.optJSONObject("archive")
                commentId = archive?.optLong("aid", 0) ?: 0
                commentType = 1
                val title = archive?.optString("title", "") ?: ""
                archiveTitle = title
                cover = archive?.optString("cover", "") ?: ""
                bvid = archive?.optString("bvid", "") ?: ""
                if (content.isEmpty()) content = title
            }
            "MAJOR_TYPE_DRAW" -> {
                val draw = majorObj?.optJSONObject("draw")
                commentId = 0
                commentType = 11
                val drawItems = draw?.optJSONArray("items")
                if (drawItems != null && drawItems.length() > 0) {
                    for (i in 0 until drawItems.length()) {
                        val item = drawItems.optJSONObject(i)
                        val src = item?.optString("src", "")
                        if (!src.isNullOrEmpty()) {
                            images.add(src)
                        }
                    }
                    val drawDesc = draw?.optString("desc", "")
                    if (content.isEmpty() && !drawDesc.isNullOrEmpty()) content = drawDesc
                }
            }
            "MAJOR_TYPE_OPUS" -> {
                val opus = majorObj?.optJSONObject("opus")
                commentId = 0
                commentType = 11 // Just a default, not strictly used
                val pics = opus?.optJSONArray("pics")
                if (pics != null && pics.length() > 0) {
                    for (i in 0 until pics.length()) {
                        val pic = pics.optJSONObject(i)
                        val url = pic?.optString("url", "")
                        if (!url.isNullOrEmpty()) {
                            images.add(url)
                        }
                    }
                }
                val summary = opus?.optJSONObject("summary")?.optString("text", "") ?: ""
                val title = opus?.optString("title", "") ?: ""
                if (content.isEmpty()) content = summary.ifEmpty { title }
            }
            "MAJOR_TYPE_ARTICLE" -> {
                val article = majorObj?.optJSONObject("article")
                commentId = article?.optLong("id", 0) ?: 0
                commentType = 12
                val title = article?.optString("title", "") ?: ""
                if (content.isEmpty()) content = title
            }
            "MAJOR_TYPE_LIVE_RCMD" -> {
                commentId = 0
                commentType = 17
            }
            "MAJOR_TYPE_COMMON" -> {
                commentId = 0
                commentType = 17
                val common = majorObj?.optJSONObject("common")
                val commonTitle = common?.optString("title", "") ?: ""
                if (content.isEmpty()) content = commonTitle
            }
            else -> {
                commentId = dynamicId
                commentType = 17
            }
        }

        val userInfo = UserInfo(
            mid = mid,
            name = name,
            avatar = face
        )

        val statModule = modules.optJSONObject("module_stat") ?: JSONObject()
        val commentStat = statModule.optJSONObject("comment") ?: JSONObject()
        val likeStat = statModule.optJSONObject("like") ?: JSONObject()
        val forwardStat = statModule.optJSONObject("forward") ?: JSONObject()

        val stats = Stats(
            reply = commentStat.optInt("count", 0),
            like = likeStat.optInt("count", 0),
            share = forwardStat.optInt("count", 0),
            liked = likeStat.optBoolean("status", false)
        )

        val moreModule = modules.optJSONObject("module_more") ?: JSONObject()
        val dynIdStr = moreModule.optString("dyn_id_str", dynamicId.toString())

        val origDyn = json.optJSONObject("orig")
        val dynamicForward = if (origDyn != null) {
            try {
                analyzeDynamic(origDyn)
            } catch (_: Exception) {
                null
            }
        } else null

        return Dynamic(
            dynamicId = dynamicId,
            type = type,
            comment_id = commentId,
            comment_type = commentType,
            title = topicTitle.ifEmpty { "" },
            userInfo = userInfo,
            content = content,
            pubTime = pubTime,
            stats = stats,
            major_type = majorType,
            major_object = majorObject,
            dynamic_forward = dynamicForward,
            canDelete = canDelete,
            images = images,
            cover = cover,
            bvid = bvid,
            archiveTitle = archiveTitle
        )
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
