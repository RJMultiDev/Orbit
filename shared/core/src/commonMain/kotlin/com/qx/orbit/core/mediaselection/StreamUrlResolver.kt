package com.qx.orbit.core.mediaselection

/** MP4 播放（durl）来源条目。 */
data class DurlSource(
    val url: String,
    val backupUrls: List<String> = emptyList()
)

/**
 * 播放 URL 解析纯逻辑：CDN 地址归一化、baseUrl+backup 候选顺序、durl 主选、403/失败后候选轮换。
 *
 * 语义对齐 Android 侧 `PlayerApi#normalizeUrl`、`getVideo/getBangumi` 的
 * `durl?.firstOrNull()?.url` 主选，以及 `VideoDownloadManager#refreshUrlAndRetry` 的
 * 403 → 重新取流 → 换新地址重试流程。
 */
object StreamUrlResolver {

    /**
     * 归一化 URL：与 `PlayerApi#normalizeUrl` 对齐。
     * - 空 → 空
     * - http(s):// → 原样
     * - //… → https: 补全
     * - /… → file:// 补全（本地字幕路径）
     * - 其它 → https:// 补全
     */
    fun normalizeUrl(url: String): String = when {
        url.isEmpty() -> ""
        url.startsWith("http://") || url.startsWith("https://") -> url
        url.startsWith("//") -> "https:$url"
        url.startsWith("/") -> "file://$url"
        else -> "https://$url"
    }

    /**
     * 主备候选列表：baseUrl 在前，backupUrls 去重（保留顺序）在后。
     * 结果顺序即尝试优先级。
     */
    fun candidateUrls(baseUrl: String, backupUrls: List<String>): List<String> {
        if (baseUrl.isEmpty() && backupUrls.isEmpty()) return emptyList()
        return buildList {
            if (baseUrl.isNotEmpty()) add(baseUrl)
            val seen = if (baseUrl.isNotEmpty()) mutableSetOf(baseUrl) else mutableSetOf<String>()
            for (u in backupUrls) {
                if (u.isNotEmpty() && seen.add(u)) add(u)
            }
        }
    }

    /** durl 主选：取第一个条目主地址（与 `durl?.firstOrNull()?.url` 对齐）。 */
    fun selectDurlPrimary(durls: List<DurlSource>): String = durls.firstOrNull()?.url.orEmpty()

    /**
     * 403/网络失败后选择下一个候选地址。
     * - 候选为空 → null
     * - 当前 URL 不在候选列表（说明是旧地址，已被重新取流刷新）→ 取首个候选
     * - 在列表中 → 轮换到下一个备份；无备份可换 → null（表示需要重新取流）
     */
    fun nextCandidate(currentUrl: String, candidates: List<String>): String? {
        if (candidates.isEmpty()) return null
        val idx = candidates.indexOf(currentUrl)
        if (idx < 0) return candidates.first()
        val next = idx + 1
        return if (next < candidates.size) candidates[next] else null
    }
}
