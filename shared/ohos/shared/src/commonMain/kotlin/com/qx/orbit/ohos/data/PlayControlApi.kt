package com.qx.orbit.ohos.data

import com.qx.orbit.ohos.util.WbiKeys
import com.qx.orbit.ohos.util.WbiSigner

/**
 * 播放流控制（独占）：按目标 qn 重取播放地址。
 *
 * 与 [BiliClient.playUrl]（qn 写死 64/720p）的区别是 qn 可配置：
 * 画质切换、分 P 切换都需要按新的 qn/cid 重新签名取流，独立成类避免污染
 * BiliClient 的业务接口。WBI key 自行拉取（nav），不复用 BiliClient 内部状态。
 */
class PlayControlApi(private val client: BiliClient) {

    private var mixinKey: String = ""

    private val signer: WbiSigner get() = WbiSigner({ mixinKey }, { 0 })

    private suspend fun ensureWbiKey() {
        if (mixinKey.isNotEmpty()) return
        val resp = client.request("$HOST/x/web-interface/nav")
        val img = resp?.optJSONObject("data")?.optJSONObject("wbi_img") ?: return
        mixinKey = WbiKeys.calcMixinKey(
            img.optString("img_url"),
            img.optString("sub_url"),
        )
    }

    /**
     * 按指定清晰度取流。qn 映射：16=360P、32=480P、64=720P、80=1080P。
     * 服务端可能按登录态/权限降级（[PlayUrl.quality] 为实际档位），
     * 无可用流（null）时由调用方保留上一份可用流。
     */
    suspend fun playUrl(bvid: String, cid: Long, qn: Int): PlayUrl? {
        ensureWbiKey()
        val signed = signer.signParams(
            mapOf(
                "bvid" to bvid,
                "cid" to cid.toString(),
                "qn" to qn.toString(),
                "fnval" to "16",
                "fourk" to "1",
            )
        )
        val resp = client.request("$HOST/x/player/wbi/playurl", signed)
        return resp?.optJSONObject("data")?.let { parsePlayUrl(it) }
    }

    private companion object {
        const val HOST = "https://api.bilibili.com"
    }
}
