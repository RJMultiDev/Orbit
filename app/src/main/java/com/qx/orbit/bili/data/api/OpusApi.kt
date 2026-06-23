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

object OpusApi {

    suspend fun getOpus(id: Long): Opus? = withContext(Dispatchers.IO) {
        val url = "https://www.bilibili.com/opus/$id"
        val request = Request.Builder().url(url)
            .addHeader("Cookie", CookieManager.getCookie())
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Referer", "https://www.bilibili.com/")
            .build()
        val html = HttpClient.client.newCall(request).execute().body?.string() ?: ""
        if (html.isEmpty()) return@withContext null
        try {
            val marker = "window.__INITIAL_STATE__="
            val startIdx = html.indexOf(marker)
            if (startIdx < 0) return@withContext null
            val jsonStart = startIdx + marker.length
            val endIdx = html.indexOf(";(function()", jsonStart)
            if (endIdx < 0) return@withContext null
            val jsonStr = html.substring(jsonStart, endIdx)
            val jsonObj = JSONObject(jsonStr)
            val detailObj = jsonObj.optJSONObject("detail") ?: jsonObj
            val item = detailObj.optJSONObject("item") ?: detailObj
            parseOpusFromHtml(item, id)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseOpusFromHtml(item: JSONObject, id: Long): Opus {
        val dynId = item.optLong("id_str", id)
        val type = item.optInt("type", Opus.TYPE_DYNAMIC)
        val modules = item.optJSONObject("modules") ?: JSONObject()
        val authorModule = modules.optJSONObject("module_author") ?: JSONObject()
        val name = authorModule.optString("name", "")
        val face = authorModule.optString("face", "")
        val mid = authorModule.optLong("mid", 0)
        val pubTs = authorModule.optLong("pub_ts", 0)
        val pubTime = if (pubTs > 0) {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(pubTs * 1000)
        } else ""

        val dynamicModule = modules.optJSONObject("module_dynamic") ?: JSONObject()
        val majorObj = dynamicModule.optJSONObject("major")
        val majorType = majorObj?.optString("type", "") ?: ""
        val desc = dynamicModule.optJSONObject("desc")
        val content = desc?.optString("text", "") ?: ""

        val topic = dynamicModule.optJSONObject("topic")
        val title = topic?.optString("name", "") ?: ""

        val topImages = mutableListOf<String>()
        if (majorType == "MAJOR_TYPE_DRAW") {
            val draw = majorObj?.optJSONObject("draw")
            val drawItems = draw?.optJSONArray("items")
            if (drawItems != null) {
                for (i in 0 until drawItems.length()) {
                    val drawItem = drawItems.optJSONObject(i) ?: continue
                    val src = drawItem.optString("src", "")
                    if (src.isNotEmpty()) topImages.add(src)
                }
            }
        }

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

        var commentId = 0L
        var commentType = 0
        var cover = ""
        when (majorType) {
            "MAJOR_TYPE_ARCHIVE" -> {
                val archive = majorObj?.optJSONObject("archive")
                commentId = archive?.optLong("aid", 0) ?: 0
                commentType = 1
                cover = archive?.optString("cover", "") ?: ""
            }
            "MAJOR_TYPE_ARTICLE" -> {
                val article = majorObj?.optJSONObject("article")
                commentId = article?.optLong("id", 0) ?: 0
                commentType = 12
                cover = article?.optString("image_urls")?.let {
                    try {
                        val arr = org.json.JSONArray(it)
                        if (arr.length() > 0) arr.optString(0) else ""
                    } catch (_: Exception) { "" }
                } ?: article?.optString("banner_url", "") ?: ""
            }
            else -> {
                commentId = dynId
                commentType = 17
            }
        }

        val paragraphs = parseParagraphs(content)

        return Opus(
            id = dynId,
            type = type,
            commentId = commentId,
            commentType = commentType,
            title = title,
            cover = cover,
            content = content,
            pubTime = pubTime,
            upInfo = UserInfo(mid = mid, name = name, avatar = face),
            stats = stats,
            topImages = topImages,
            paragraphs = paragraphs,
            parsedId = id
        )
    }

    private fun parseParagraphs(content: String): Array<OpusParagraph>? {
        if (content.isEmpty()) return null
        val lines = content.split("\n")
        return lines.map { line ->
            OpusParagraph(
                align = 0,
                type = OpusParagraph.TYPE_TEXT,
                content = line
            )
        }.toTypedArray()
    }

    suspend fun likeOpus(dynId: Long, up: Boolean): Int = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("dynamic_id", dynId.toString())
            .add("up", if (up) "1" else "0")
            .add("csrf", CookieManager.getCsrf())
            .build()
        val request = Request.Builder()
            .url("https://api.bilibili.com/x/dynamic/feed/dyn/thumb")
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

    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.95 Safari/537.36"
}
