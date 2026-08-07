package com.qx.orbit.ohos.data

import com.qx.orbit.ohos.components.formatBiliTime
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * 动态详情（对应 Android data/model/Dynamic.kt 的精简版，仅含详情页所需字段）。
 */
data class DynamicDetail(
    val id: String,
    val type: String,
    val text: String,
    val userName: String,
    val userFace: String,
    val time: String,
    val likeCount: Long,
    val commentCount: Long,
    val forwardCount: Long,
    val bvid: String,
)

private const val HOST = "https://api.bilibili.com"

/**
 * 动态详情：GET /x/polymer/web-dynamic/v1/detail?id=..，取 data.item 解析。
 */
internal suspend fun BiliClient.dynamicDetail(id: String): DynamicDetail? {
    val resp = request("$HOST/x/polymer/web-dynamic/v1/detail", mapOf("id" to id))
    return resp?.optJSONObject("data")?.optJSONObject("item")?.let { parseDynamicDetail(it) }
}

private fun parseDynamicDetail(j: JSONObject): DynamicDetail {
    val modules = j.optJSONObject("modules")
    val author = modules?.optJSONObject("module_author")
    val user = author?.optJSONObject("user")
    val pubTs = author?.optLong("pub_ts") ?: 0L

    val dynamic = modules?.optJSONObject("module_dynamic")
    val desc = dynamic?.optJSONObject("desc")
    val major = dynamic?.optJSONObject("major")
    val majorType = major?.optString("type", "") ?: ""

    var text = desc?.optString("text", "") ?: ""
    var bvid = ""
    if (majorType == "MAJOR_TYPE_ARCHIVE") {
        val archive = major?.optJSONObject("archive")
        bvid = archive?.optString("bvid", "") ?: ""
        if (text.isEmpty()) text = archive?.optString("title", "") ?: ""
    }

    val stat = modules?.optJSONObject("module_stat")
    return DynamicDetail(
        id = j.optString("id_str", ""),
        type = j.optString("type", ""),
        text = text,
        userName = user?.optString("name", "") ?: "",
        userFace = user?.optString("face", "") ?: "",
        time = if (pubTs > 0) formatBiliTime(pubTs) else "",
        likeCount = stat?.optJSONObject("like")?.optLong("count") ?: 0L,
        commentCount = stat?.optJSONObject("comment")?.optLong("count") ?: 0L,
        forwardCount = stat?.optJSONObject("forward")?.optLong("count") ?: 0L,
        bvid = bvid,
    )
}
