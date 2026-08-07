package com.qx.orbit.ohos.data

import com.qx.orbit.ohos.components.formatBiliTime
import com.qx.orbit.ohos.platform.OrbitCredentialModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * 评论（对齐 Android data/api/ReplyApi.kt 的简化移植）。
 * type=1 视频、type=17 动态；root>0 表示回复某条评论。
 */
data class ReplyItem(
    val rpid: Long = 0,
    val userName: String = "",
    val userFace: String = "",
    val message: String = "",
    val likeCount: Long = 0,
    val replyCount: Long = 0,
    val time: String = "",
)

private const val REPLY_HOST = "https://api.bilibili.com/x/v2/reply"

/** 挂起包装 Kuikly 异步回调，读取凭证 cookie（原生侧按模块名共享同一存储）。 */
private suspend fun readCookie(): String = suspendCancellableCoroutine { cont ->
    OrbitCredentialModule().getCookie { cookie ->
        cont.resume(cookie)
    }
}

/** 从 cookie 中提取 bili_jct（评论等写操作需要的 CSRF）。 */
private fun extractCsrf(cookie: String): String {
    for (part in cookie.split(";")) {
        val eq = part.indexOf('=')
        if (eq > 0 && part.substring(0, eq).trim() == "bili_jct") {
            return part.substring(eq + 1).trim()
        }
    }
    return ""
}

/**
 * 评论列表（/x/v2/reply/main，mode=2 热度排序）。
 * 分页用 `next` 游标（第 1 页传 1），返回 data.replies 顶层评论。
 */
internal suspend fun BiliClient.replies(oid: Long, type: Int, page: Int): List<ReplyItem> {
    val resp = request(
        "$REPLY_HOST/main",
        mapOf(
            "oid" to oid.toString(),
            "type" to type.toString(),
            "mode" to "2",
            "next" to page.toString(),
        ),
    )
    val arr = resp?.optJSONObject("data")?.optJSONArray("replies") ?: return emptyList()
    val list = mutableListOf<ReplyItem>()
    for (i in 0 until arr.length()) {
        arr.optJSONObject(i)?.let { list.add(parseReplyItem(it)) }
    }
    return list
}

/**
 * 发评论/回复（/x/v2/reply/add，POST 表单）。
 * 需 biliCSRF 从 cookie（bili_jct）取；root=0 发表新评论，root>0 回复某条评论。
 */
internal suspend fun BiliClient.sendReply(
    oid: Long,
    type: Int,
    message: String,
    root: Long = 0,
): Boolean {
    if (message.isBlank()) return false
    val csrf = extractCsrf(readCookie())
    if (csrf.isEmpty()) return false
    val resp = request(
        "$REPLY_HOST/add",
        mapOf(
            "oid" to oid.toString(),
            "type" to type.toString(),
            "root" to root.toString(),
            "message" to message,
            "csrf" to csrf,
        ),
        isPost = true,
    )
    return resp?.optInt("code", -1) == 0
}

private fun parseReplyItem(j: JSONObject): ReplyItem {
    val member = j.optJSONObject("member")
    val content = j.optJSONObject("content")
    return ReplyItem(
        rpid = j.optLong("rpid"),
        userName = member?.optString("uname", "") ?: "",
        userFace = member?.optString("avatar", "") ?: "",
        message = content?.optString("message", "") ?: "",
        likeCount = j.optLong("like"),
        replyCount = j.optLong("rcount"),
        time = formatBiliTime(j.optLong("ctime")),
    )
}
