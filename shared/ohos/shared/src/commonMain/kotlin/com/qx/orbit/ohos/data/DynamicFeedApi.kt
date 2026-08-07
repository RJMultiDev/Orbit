package com.qx.orbit.ohos.data

import com.qx.orbit.ohos.components.formatBiliTime
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * 动态 Feed 条目（Android data/model/Dynamic.kt 的简化投影：纯文本正文、
 * 无富文本/转发嵌套；图片区由页面按 type 占位）。
 */
data class DynamicItem(
    val id: String,
    val type: String,
    val text: String,
    val userName: String,
    val userFace: String,
    val time: String,
    val likeCount: Long,
    val commentCount: Long,
    val forwardCount: Long,
)

/**
 * 动态 Feed（对齐 Android DynamicApi.getDynamicList → /x/polymer/web-dynamic/v1/feed/all）。
 *
 * 注意：Android 侧该接口经 WBI 签名（ConfInfoApi.signWBI），而 BiliClient 的
 * wbiSigner/mixinKey 均为 private，扩展函数无法复用签名，当前直连可能返回
 * code=-403/-352。TODO：待 BiliClient 开放签名能力（或改为成员函数）后修正。
 */
internal suspend fun BiliClient.dynamicFeed(): List<DynamicItem> {
    val features =
        "itemOpusStyle,listOnlyfans,opusBigCover,onlyfansVote,forwardListHidden,decorationCard,commentsNewVersion,onlyfansAssetsV2,ugcDelete,onlyfansQaCard,avatarAutoTheme,sunflowerStyle,eva3CardOpus,eva3CardVideo,eva3CardComment"
    val resp = request(
        "https://api.bilibili.com/x/polymer/web-dynamic/v1/feed/all",
        mapOf("features" to features),
    ) ?: return emptyList()
    if (resp.optInt("code", -1) != 0) return emptyList()
    val arr = resp.optJSONObject("data")?.optJSONArray("items") ?: return emptyList()
    val list = mutableListOf<DynamicItem>()
    for (i in 0 until arr.length()) {
        arr.optJSONObject(i)?.let { list.add(parseDynamicItem(it)) }
    }
    return list
}

private fun parseDynamicItem(j: JSONObject): DynamicItem {
    val modules = j.optJSONObject("modules")
    val author = modules?.optJSONObject("module_author")
    val dynDesc = modules?.optJSONObject("module_dynamic")?.optJSONObject("desc")
    val stat = modules?.optJSONObject("module_stat")

    // 转发动态自身正文为空，退回到被转发动态的正文（简化：不渲染转发嵌套块）
    val ownText = dynDesc?.optString("text", "") ?: ""
    val origText = j.optJSONObject("orig")
        ?.optJSONObject("modules")
        ?.optJSONObject("module_dynamic")
        ?.optJSONObject("desc")
        ?.optString("text", "")
        ?: ""

    val pubTs = author?.optLong("pub_ts") ?: 0L
    return DynamicItem(
        id = j.optString("id_str", ""),
        type = j.optString("type", ""),
        text = ownText.ifEmpty { origText },
        userName = author?.optString("name", "") ?: "",
        userFace = author?.optString("face", "") ?: "",
        time = if (pubTs > 0L) formatBiliTime(pubTs) else "",
        likeCount = stat?.optJSONObject("like")?.optLong("count") ?: 0L,
        commentCount = stat?.optJSONObject("comment")?.optLong("count") ?: 0L,
        forwardCount = stat?.optJSONObject("forward")?.optLong("count") ?: 0L,
    )
}
