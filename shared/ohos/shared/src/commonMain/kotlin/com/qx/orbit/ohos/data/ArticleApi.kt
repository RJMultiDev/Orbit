package com.qx.orbit.ohos.data

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * 专栏文章详情（对应 Android data/api/ArticleApi.kt 的 getArticle，字段按精简模型映射）。
 */
data class ArticleDetail(
    val title: String,
    val authorName: String,
    val authorFace: String,
    val publishTime: Long,
    val content: String,
    val viewCount: Long,
    val likeCount: Long,
    val favoriteCount: Long,
    val commentCount: Long,
)

private const val HOST = "https://api.bilibili.com"

/** 文章详情：GET /x/article/view?id=..，取 data 对象解析。 */
internal suspend fun BiliClient.articleView(id: Long): ArticleDetail? {
    val resp = request("$HOST/x/article/view", mapOf("id" to id.toString()))
    return resp?.optJSONObject("data")?.let { parseArticleDetail(it) }
}

private fun parseArticleDetail(j: JSONObject): ArticleDetail {
    val author = j.optJSONObject("author")
    val stats = j.optJSONObject("stats")
    return ArticleDetail(
        title = j.optString("title", ""),
        authorName = author?.optString("name", "") ?: "",
        authorFace = author?.optString("face", "") ?: "",
        publishTime = j.optLong("ctime"),
        content = j.optString("content", ""),
        viewCount = stats?.optLong("view") ?: 0,
        likeCount = stats?.optLong("like") ?: 0,
        favoriteCount = stats?.optLong("favorite") ?: 0,
        commentCount = stats?.optLong("reply") ?: 0,
    )
}
