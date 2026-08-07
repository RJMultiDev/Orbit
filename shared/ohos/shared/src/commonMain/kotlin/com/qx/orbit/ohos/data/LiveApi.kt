package com.qx.orbit.ohos.data

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * 直播间信息（直播详情页所需最小字段，对应 Android data/model/Live.kt 的 LiveRoom 精简映射）。
 */
private const val LIVE_HOST = "https://api.live.bilibili.com"

/**
 * 直播间信息：GET /xlive/web-room/v1/index/getInfoByRoom?room_id=..
 *
 * 解析 data.roomInfo（title/online/cover）+ data.anchorInfo.base_info.uname；
 * playUrl 顺带取 data.playUrl.durl[0].url（直播流 FLV）。
 */
internal suspend fun BiliClient.liveRoomInfo(roomId: Long): LiveRoom? {
    val resp = request(
        "$LIVE_HOST/xlive/web-room/v1/index/getInfoByRoom",
        mapOf("room_id" to roomId.toString()),
    )
    val data = resp?.optJSONObject("data") ?: return null
    val roomInfo = data.optJSONObject("roomInfo") ?: data
    val uname = data.optJSONObject("anchorInfo")
        ?.optJSONObject("base_info")?.optString("uname", "") ?: ""
    return LiveRoom(
        title = roomInfo.optString("title", ""),
        uname = uname,
        online = roomInfo.optLong("online"),
        cover = liveCover(roomInfo),
        playUrl = data.optJSONObject("playUrl")?.optJSONArray("durl")
            ?.optJSONObject(0)?.optString("url", "") ?: "",
    )
}

/**
 * 直播取流地址：
 * 1) 优先 getInfoByRoom 自带 playUrl.durl[0].url（FLV）；
 * 2) 兜底 /xlive/web-room/v2/index/getRoomPlayInfo 解析
 *    data.playurl_info.playurl.stream[].format[].codec[].url_info[]
 *    中首个可用分片地址（host + base_url + extra）。
 */
internal suspend fun BiliClient.livePlayUrl(roomId: Long): String? {
    val resp = request(
        "$LIVE_HOST/xlive/web-room/v1/index/getInfoByRoom",
        mapOf("room_id" to roomId.toString()),
    )
    val quick = resp?.optJSONObject("data")?.optJSONObject("playUrl")
        ?.optJSONArray("durl")?.optJSONObject(0)?.optString("url", "")
        ?.takeIf { it.isNotEmpty() }
    if (quick != null) return quick

    val resp2 = request(
        "$LIVE_HOST/xlive/web-room/v2/index/getRoomPlayInfo",
        mapOf(
            "room_id" to roomId.toString(),
            "protocol" to "0,1",
            "format" to "0,1,2",
            "codec" to "0,1,2",
            "platform" to "web",
            "ptype" to "8",
            "qn" to "10000",
        ),
    )
    val playurl = resp2?.optJSONObject("data")
        ?.optJSONObject("playurl_info")?.optJSONObject("playurl") ?: return null
    val streams = playurl.optJSONArray("stream") ?: return null
    for (i in 0 until streams.length()) {
        val formats = streams.optJSONObject(i)?.optJSONArray("format") ?: continue
        for (j in 0 until formats.length()) {
            val codecs = formats.optJSONObject(j)?.optJSONArray("codec") ?: continue
            for (k in 0 until codecs.length()) {
                val codec = codecs.optJSONObject(k) ?: continue
                val baseUrl = codec.optString("base_url", "")
                val infos = codec.optJSONArray("url_info") ?: continue
                for (l in 0 until infos.length()) {
                    val info = infos.optJSONObject(l) ?: continue
                    val host = info.optString("host", "")
                    if (host.isNotEmpty() && baseUrl.isNotEmpty()) {
                        return "$host$baseUrl${info.optString("extra", "")}"
                    }
                }
            }
        }
    }
    return null
}

/** 封面优先级：user_cover → cover → keyframe（对齐 Android LiveInfoPage 的封面选择）。 */
private fun liveCover(roomInfo: JSONObject): String {
    val userCover = roomInfo.optString("user_cover", "")
    if (userCover.isNotEmpty()) return userCover
    val cover = roomInfo.optString("cover", "")
    if (cover.isNotEmpty()) return cover
    return roomInfo.optString("keyframe", "")
}
