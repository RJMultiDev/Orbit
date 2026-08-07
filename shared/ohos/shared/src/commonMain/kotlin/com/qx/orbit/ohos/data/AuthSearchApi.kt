package com.qx.orbit.ohos.data

import com.qx.orbit.ohos.components.currentTimeMillis
import com.qx.orbit.ohos.util.WbiKeys
import com.qx.orbit.ohos.util.WbiSigner
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * 认证 / 推荐 / 搜索数据层扩展（独占文件）。
 *
 * 仅新增本文件；模型 + parse* 同文件，风格对齐 BiliModels.kt。BiliClient 内
 * mixinKey 为私有，扩展侧不可读，故在 [wbiSignerForExtension] 里重新拉 nav 计算并缓存。
 */
private const val HOST = "https://api.bilibili.com"

// ---- 模型 ----

/** Cookie 刷新结果（对齐 Android CookieRefreshApi.RefreshResult）。 */
data class CookieRefreshResult(
    val isSuccess: Boolean,
    val message: String,
    val isNeedRefresh: Boolean = false,
)

/**
 * 搜索综合结果卡片（对齐 Android SearchApi 的拍平策略）：
 * [type] 为 result_type，取值如 video / bili_user / media_bangumi / media_ft / live_room / article。
 * 各类型只填自身相关字段，其余为默认值。
 */
data class SearchCard(
    val type: String,
    val title: String,
    val cover: String,
    val desc: String,
    val aid: Long,
    val bvid: String,
    val cid: Long,
    val mid: Long,
    val author: String,
    val view: Long,
    val danmaku: Long,
    val duration: Long,
    val pubdate: Long,
    val fans: Long,
    val level: Int,
    val roomid: Long,
    val online: Long,
    val seasonId: Long,
    val epId: Long,
    val mediaId: Long,
)

// ---- 解析函数 ----

fun parseCookieRefreshResult(j: JSONObject): CookieRefreshResult = CookieRefreshResult(
    isSuccess = j.optInt("code", -1) == 0,
    message = j.optString("message", ""),
    isNeedRefresh = j.optJSONObject("data")?.optBoolean("refresh") ?: false,
)

fun parseSearchCard(j: JSONObject, type: String): SearchCard = SearchCard(
    type = type,
    title = htmlToString(j.optString("title", "")),
    cover = fixCover(
        j.optString("pic", "")
            .ifEmpty { j.optString("cover", "") }
            .ifEmpty { j.optString("upic", "") }
            .ifEmpty { j.optString("user_cover", "") },
    ),
    desc = j.optString("description", "").ifEmpty { j.optString("usign", "") },
    aid = j.optLong("aid"),
    bvid = j.optString("bvid", ""),
    cid = j.optLong("cid"),
    mid = j.optLong("mid").let { if (it != 0L) it else j.optLong("uid") },
    author = j.optString("author", "").ifEmpty { j.optString("uname", "") },
    view = j.optLong("play").let { if (it != 0L) it else j.optLong("fans") },
    danmaku = j.optLong("danmaku"),
    duration = parseDuration(j.optString("duration", "")),
    pubdate = j.optLong("pubdate"),
    fans = j.optLong("fans"),
    level = j.optInt("level"),
    roomid = j.optLong("roomid").let { if (it != 0L) it else j.optLong("short_id") },
    online = j.optLong("online"),
    seasonId = j.optLong("season_id"),
    epId = j.optLong("ep_id"),
    mediaId = j.optLong("media_id"),
)

private fun htmlToString(input: String): String = input
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&#39;", "'")
    .replace("&quot;", "\"")
    .replace("&amp;", "&")

private fun fixCover(url: String): String = when {
    url.startsWith("//") -> "https:$url"
    url.startsWith("http://") -> url.replace("http://", "https://")
    else -> url
}

/** 搜索结果的时长 "MM:SS" / "H:MM:SS" → 秒。 */
private fun parseDuration(duration: String): Long {
    val parts = duration.split(":")
    return when (parts.size) {
        1 -> parts[0].toLongOrNull() ?: 0L
        2 -> (parts[0].toLongOrNull() ?: 0L) * 60 + (parts[1].toLongOrNull() ?: 0L)
        3 -> (parts[0].toLongOrNull() ?: 0L) * 3600 +
            (parts[1].toLongOrNull() ?: 0L) * 60 +
            (parts[2].toLongOrNull() ?: 0L)
        else -> 0L
    }
}

// ---- 扩展函数 ----

/** 文件级 WBI mixinKey 缓存（BiliClient 内私有字段不可读，扩展侧自算）。 */
private var extMixinKey: String = ""

/**
 * 扩展侧 WBI 签名器。BiliClient.ensureWbiKey() 为公开，先调用（内部幂等缓存）；
 * 但 mixinKey 字段私有不可读，故扩展内再拉一次 nav 计算并缓存。
 */
private suspend fun BiliClient.wbiSignerForExtension(): WbiSigner? {
    ensureWbiKey()
    if (extMixinKey.isEmpty()) {
        val resp = request("$HOST/x/web-interface/nav")
        val wbi = resp?.optJSONObject("data")?.optJSONObject("wbi_img") ?: return null
        extMixinKey = WbiKeys.calcMixinKey(wbi.optString("img_url", ""), wbi.optString("sub_url", ""))
    }
    return WbiSigner({ extMixinKey }, { currentTimeMillis() / 1000L })
}

/** 综合搜索（WBI 签名，对齐 Android SearchApi.search）。 */
internal suspend fun BiliClient.search(keyword: String, page: Int): List<SearchCard> {
    val signer = wbiSignerForExtension() ?: return emptyList()
    val signed = signer.signParams(
        mapOf(
            "keyword" to keyword,
            "page" to page.toString(),
        )
    )
    val resp = request("$HOST/x/web-interface/wbi/search/all/v2", signed)
    val groups = resp?.optJSONObject("data")?.optJSONArray("result") ?: return emptyList()
    val list = mutableListOf<SearchCard>()
    for (i in 0 until groups.length()) {
        val group = groups.optJSONObject(i) ?: continue
        val type = group.optString("result_type", "")
        if (type.isEmpty()) continue
        val items = group.optJSONArray("data") ?: continue
        for (j in 0 until items.length()) {
            val item = items.optJSONObject(j) ?: continue
            list.add(parseSearchCard(item, type))
        }
    }
    return list
}

/** 搜索建议词（对齐 Android SearchApi.getSearchSuggestions）。 */
internal suspend fun BiliClient.searchSuggest(term: String): List<String> {
    val resp = request("https://s.search.bilibili.com/main/suggest", mapOf("term" to term))
    val data = resp?.optJSONObject("data") ?: return emptyList()
    val out = mutableListOf<String>()
    data.optJSONArray("result")?.let { arr ->
        for (i in 0 until arr.length()) {
            val v = arr.optJSONObject(i)?.optString("value", "") ?: ""
            if (v.isNotEmpty()) out.add(v)
        }
    }
    if (out.isEmpty()) {
        data.optJSONArray("tag")?.let { arr ->
            for (i in 0 until arr.length()) {
                val v = arr.optJSONObject(i)?.optString("value", "") ?: ""
                if (v.isNotEmpty()) out.add(v)
            }
        }
    }
    return out
}

/** 排行榜（rank/v2，对齐 Android RecommendApi.getRanking）。 */
internal suspend fun BiliClient.ranking(rid: Int = 0, type: String = "all"): List<RecommendCard> {
    val resp = request(
        "$HOST/x/web-interface/ranking/v2",
        mapOf("rid" to rid.toString(), "type" to type),
    )
    val arr = resp?.optJSONObject("data")?.optJSONArray("list") ?: return emptyList()
    val list = mutableListOf<RecommendCard>()
    for (i in 0 until arr.length()) {
        arr.optJSONObject(i)?.let { list.add(parseRecommendCard(it)) }
    }
    return list
}

/** 热门榜（popular，对齐 Android RecommendApi.getPopular）。 */
internal suspend fun BiliClient.popular(page: Int): List<RecommendCard> {
    val resp = request(
        "$HOST/x/web-interface/popular",
        mapOf("pn" to page.toString(), "ps" to "20"),
    )
    val arr = resp?.optJSONObject("data")?.optJSONArray("list") ?: return emptyList()
    val list = mutableListOf<RecommendCard>()
    for (i in 0 until arr.length()) {
        arr.optJSONObject(i)?.let { list.add(parseRecommendCard(it)) }
    }
    return list
}

// ---- 登录增强 ----

/** 当前登录用户信息：nav 已覆盖，此处作为语义化入口。 */
internal suspend fun BiliClient.getNavUserInfo(): NavUser? = nav()

/**
 * Cookie 刷新（对齐 Android CookieRefreshApi.doCookieRefresh）。
 *
 * 检查阶段（cookie/info）已实现；真正刷新需 RSA-OAEP 加密生成
 * correspond/1/{path}（Android getCorrespondPath），commonMain 无 Java 加密、
 * OHOS 侧暂无 RSA 桥，命中需要刷新时降级提示重新登录。
 */
internal suspend fun BiliClient.refreshCookie(auto: Boolean = false): CookieRefreshResult {
    val user = nav() ?: return CookieRefreshResult(false, "网络错误")
    if (!user.isLogin) return CookieRefreshResult(false, "未登录")

    val resp = request("$HOST/x/passport-login/web/cookie/info")
    if (resp == null) return CookieRefreshResult(false, "检查刷新状态失败")
    if (resp.optInt("code", -1) != 0) {
        return CookieRefreshResult(false, "API错误 ${resp.optInt("code")}: ${resp.optString("message", "")}")
    }
    val needRefresh = resp.optJSONObject("data")?.optBoolean("refresh") ?: false
    if (!needRefresh) return CookieRefreshResult(true, "无需刷新")

    // 完整刷新链路被 RSA 桥阻塞：server 会话仍有效，仅提示重新登录。
    return CookieRefreshResult(false, "需要刷新，但 correspond path 需平台 RSA 加密桥（OHOS 侧未实现）", isNeedRefresh = true)
}

/**
 * 退出登录（对齐 Android UserInfoApi.exitLogin）：POST logout 后清空本地凭证
 * （logout() 为 BiliClient 公开成员，从扩展可直接调用）。
 */
internal suspend fun BiliClient.exitLogin(): Boolean {
    val resp = request("https://passport.bilibili.com/login/exit/v2", isPost = true)
    logout()
    return resp != null
}
