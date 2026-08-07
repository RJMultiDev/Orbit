package com.qx.orbit.ohos.data

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * 番剧详情模型（对齐 Android data/model/Bangumi.kt 的简化子集：只保留
 * 季番信息与「正片」剧集，去掉了 media/season 归属、多 section、用户进度等）。
 */
data class BangumiEp(
    val id: Long = 0,
    val title: String = "",
    val cid: Long = 0,
    val bvid: String = "",
    val duration: Long = 0,
)

data class BangumiDetail(
    val title: String = "",
    val cover: String = "",
    val desc: String = "",
    val score: Float = 0f,
    val eps: List<BangumiEp> = emptyList(),
)

/**
 * 番剧详情：GET https://api.bilibili.com/pgc/view/web/season?season_id=..
 *
 * 注意：pgc 系接口把负载放在顶层 `result`（非 `data`），
 * `BiliClient.request` 返回整包 JSON，这里显式取 `result`。
 * 剧集优先取 `result.episodes`，部分响应只在 `result.main_section.episodes`，
 * 用首个非空数组兜底。
 */
internal suspend fun BiliClient.bangumiInfo(seasonId: Long): BangumiDetail? {
    if (seasonId <= 0L) return null
    val resp = request(
        "https://api.bilibili.com/pgc/view/web/season",
        mapOf("season_id" to seasonId.toString()),
    )
    val result = resp?.optJSONObject("result") ?: return null
    return parseBangumiDetail(result)
}

private fun parseBangumiDetail(result: JSONObject): BangumiDetail {
    // 评分：rating.score（部分接口在顶层 score）
    val rating = result.optJSONObject("rating")
    val score = (rating?.optDouble("score", 0.0) ?: result.optDouble("score", 0.0)).toFloat()

    val eps = mutableListOf<BangumiEp>()
    val episodes = result.optJSONArray("episodes")
        ?: result.optJSONObject("main_section")?.optJSONArray("episodes")
    if (episodes != null) {
        for (i in 0 until episodes.length()) {
            val e = episodes.optJSONObject(i) ?: continue
            // 展示名优先 long_title（如「第1话 xxx」），兜底 title
            val title = e.optString("long_title", "").ifEmpty { e.optString("title", "") }
            eps.add(
                BangumiEp(
                    id = e.optLong("id"),
                    title = title,
                    cid = e.optLong("cid"),
                    bvid = e.optString("bvid", ""),
                    duration = e.optLong("duration"),
                )
            )
        }
    }

    return BangumiDetail(
        title = result.optString("title", ""),
        cover = result.optString("cover", ""),
        desc = result.optString("evaluate", ""),
        score = score,
        eps = eps,
    )
}

/**
 * TODO(番剧): 追番/追剧状态切换接口。
 * - 当前状态：/pgc/view/web/season 的 result.user_status.follow（1=已追番）。
 *   模型未携带该字段，页面本地态初始为 false，待模型扩展后从响应注入。
 * - 追番：POST https://api.bilibili.com/pgc/web/follow/add
 * - 取消：POST https://api.bilibili.com/pgc/web/follow/del
 * - 请求体均为表单 {season_id, csrf}，csrf 需从 Cookie 提取；
 *   `BiliClient.request(url, params, isPost=true)` 已支持把 params 放入 POST body，
 *   接上 csrf 来源后即可在 BiliClient 上新增 follow/unfollow 扩展函数。
 */
