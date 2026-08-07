package com.qx.orbit.ohos.data

import com.qx.orbit.ohos.platform.OrbitCredentialModule
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val HOST = "https://api.bilibili.com"

/**
 * 视频详情页互动操作（三连 / 关注 / 稍后再看），独占本文件。
 *
 * 对齐 Android data/api/ 的 web 接口：
 *  - 点赞：`x/web-interface/archive/like`（like=1 赞、2 取消）
 *  - 投币：`x/web-interface/coin/add`
 *  - 三连：`x/web-interface/archive/like/triple`
 *  - 收藏：`medialist/gateway/coll/resource/deal`（rid=aid，media_id=收藏夹）
 *  - 稍后再看：`x/v2/history/toview/add`
 *  - 关注：`x/relation/modify`（act=1 关注、2 取关）
 *
 * 写操作都需要 csrf（= bili_jct cookie），未登录时 [readCsrf] 返回空串，
 * 直接提示「请先登录」，避免打无效请求。
 *
 * 注意：BiliClient.request 的 POST 会把参数同时放进 query 与 JSON body；
 * B 站 web 接口按 query 参数同样能通过 csrf 校验（Android 端 web 接口即此类用法）。
 * 若真机验证失败，再改用 application/x-www-form-urlencoded 编码（TODO）。
 */
object VideoDetailApi {

    /** 操作结果：success + 展示给用户的提示文本。 */
    data class ActionResult(val success: Boolean, val message: String)

    /** 点赞 / 取消点赞。 */
    suspend fun like(
        client: BiliClient,
        credential: OrbitCredentialModule,
        aid: Long,
        liked: Boolean,
    ): ActionResult = post(
        client, credential,
        "$HOST/x/web-interface/archive/like",
        mapOf("aid" to aid.toString(), "like" to if (liked) "1" else "2"),
        if (liked) "点赞成功" else "已取消点赞",
    )

    /** 投币（multiply=1 或 2）。 */
    suspend fun coin(
        client: BiliClient,
        credential: OrbitCredentialModule,
        aid: Long,
        multiply: Int = 1,
    ): ActionResult = post(
        client, credential,
        "$HOST/x/web-interface/coin/add",
        mapOf("aid" to aid.toString(), "multiply" to multiply.toString()),
        "投币成功",
    )

    /** 三连：点赞 + 投币(2) + 收藏。 */
    suspend fun triple(
        client: BiliClient,
        credential: OrbitCredentialModule,
        aid: Long,
    ): ActionResult = post(
        client, credential,
        "$HOST/x/web-interface/archive/like/triple",
        mapOf("aid" to aid.toString()),
        "三连成功",
    )

    /** 收藏到指定收藏夹（mediaId 来自 [com.qx.orbit.ohos.data.favoriteFolders]）。 */
    suspend fun favorite(
        client: BiliClient,
        credential: OrbitCredentialModule,
        aid: Long,
        mediaId: Long,
    ): ActionResult = post(
        client, credential,
        "$HOST/medialist/gateway/coll/resource/deal",
        mapOf("rid" to aid.toString(), "media_id" to mediaId.toString()),
        "收藏成功",
    )

    /** 加入稍后再看。 */
    suspend fun watchLater(
        client: BiliClient,
        credential: OrbitCredentialModule,
        aid: Long,
    ): ActionResult = post(
        client, credential,
        "$HOST/x/v2/history/toview/add",
        mapOf("aid" to aid.toString()),
        "已加入稍后再看",
    )

    /** 关注 / 取关 UP 主。 */
    suspend fun follow(
        client: BiliClient,
        credential: OrbitCredentialModule,
        mid: Long,
        follow: Boolean,
    ): ActionResult = post(
        client, credential,
        "$HOST/x/relation/modify",
        mapOf("fid" to mid.toString(), "act" to if (follow) "1" else "2"),
        if (follow) "已关注" else "已取关",
    )

    private suspend fun post(
        client: BiliClient,
        credential: OrbitCredentialModule,
        url: String,
        params: Map<String, String>,
        successMsg: String,
    ): ActionResult {
        val csrf = readCsrf(credential)
        if (csrf.isEmpty()) {
            return ActionResult(false, "请先登录（缺 csrf）")
        }
        val resp = client.request(url, params + ("csrf" to csrf), isPost = true)
        val parsed = resp?.let { parseApiResp(it) }
        return if (parsed != null && parsed.isSuccess) {
            ActionResult(true, successMsg)
        } else {
            ActionResult(false, parsed?.message?.ifEmpty { "操作失败" } ?: "操作失败")
        }
    }

    /** 读 bili_jct cookie 作为 csrf；未登录返回空串。 */
    private suspend fun readCsrf(credential: OrbitCredentialModule): String =
        suspendCancellableCoroutine { cont ->
            credential.getCookie { cookie ->
                cont.resume(extractCsrf(cookie))
            }
        }

    private fun extractCsrf(cookie: String): String {
        for (part in cookie.split(";")) {
            val eq = part.indexOf('=')
            if (eq > 0 && part.substring(0, eq).trim() == "bili_jct") {
                return part.substring(eq + 1).trim()
            }
        }
        return ""
    }
}
