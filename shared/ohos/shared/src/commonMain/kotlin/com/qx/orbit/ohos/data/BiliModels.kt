package com.qx.orbit.ohos.data

import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/** B 站接口响应包装。 */
class ApiResp(val code: Int, val message: String, val data: JSONObject?) {
    val isSuccess: Boolean get() = code == 0
}

data class NavUser(val isLogin: Boolean, val mid: Long, val uname: String, val face: String)
data class WbiImg(val imgUrl: String, val subUrl: String)

data class QrLogin(val url: String, val qrcodeKey: String)

data class RecommendCard(
    val id: Long,
    val bvid: String,
    val cid: Long,
    val pic: String,
    val title: String,
    val ownerName: String,
    val ownerFace: String,
    val view: Long,
    val danmaku: Long,
    val duration: Long,
)

data class VideoOwner(val mid: Long, val name: String, val face: String)
data class VideoStat(val view: Long, val danmaku: Long, val like: Long, val coin: Long,
                     val favorite: Long, val share: Long, val reply: Long)
data class VideoPage(val cid: Long, val page: Int, val part: String, val duration: Long)

data class VideoDetail(
    val bvid: String,
    val aid: Long,
    val cid: Long,
    val pic: String,
    val title: String,
    val desc: String,
    val duration: Long,
    val owner: VideoOwner,
    val stat: VideoStat,
    val pages: List<VideoPage>,
)

data class DashTrack(val id: Long, val baseUrl: String, val backupUrl: List<String>,
                     val bandwidth: Long, val mimeType: String, val codecs: String,
                     val width: Int, val height: Int)

data class PlayUrl(
    val quality: Int,
    val format: String,
    val durationMs: Long,
    val durl: List<String>,
    val dashVideo: List<DashTrack>,
    val dashAudio: List<DashTrack>,
)

// ---- 解析函数（顶层，避免对象内扩展函数的调用歧义） ----

fun parseApiResp(j: JSONObject): ApiResp = ApiResp(
    code = j.optInt("code", -1),
    message = j.optString("message", ""),
    data = j.optJSONObject("data"),
)

fun parseNavUser(j: JSONObject): NavUser = NavUser(
    isLogin = j.optBoolean("isLogin"),
    mid = j.optLong("mid"),
    uname = j.optString("uname", ""),
    face = j.optString("face", ""),
)

fun parseWbiImg(j: JSONObject): WbiImg = WbiImg(
    imgUrl = j.optString("img_url", ""),
    subUrl = j.optString("sub_url", ""),
)

fun parseQrLogin(j: JSONObject): QrLogin = QrLogin(
    url = j.optString("url", ""),
    qrcodeKey = j.optString("qrcode_key", ""),
)

fun parseRecommendCard(j: JSONObject): RecommendCard {
    val owner = j.optJSONObject("owner")
    val stat = j.optJSONObject("stat")
    return RecommendCard(
        id = j.optLong("id"),
        bvid = j.optString("bvid", ""),
        cid = j.optLong("cid"),
        pic = j.optString("pic", ""),
        title = j.optString("title", ""),
        ownerName = owner?.optString("name", "") ?: "",
        ownerFace = owner?.optString("face", "") ?: "",
        view = stat?.optLong("view") ?: 0,
        danmaku = stat?.optLong("danmaku") ?: 0,
        duration = j.optLong("duration"),
    )
}

fun parseVideoDetail(j: JSONObject): VideoDetail {
    val owner = j.optJSONObject("owner")
    val stat = j.optJSONObject("stat")
    val pagesArr = j.optJSONArray("pages")
    val pages = mutableListOf<VideoPage>()
    if (pagesArr != null) {
        for (i in 0 until pagesArr.length()) {
            val p = pagesArr.optJSONObject(i) ?: continue
            pages.add(
                VideoPage(
                    cid = p.optLong("cid"),
                    page = p.optInt("page"),
                    part = p.optString("part", ""),
                    duration = p.optLong("duration"),
                )
            )
        }
    }
    return VideoDetail(
        bvid = j.optString("bvid", ""),
        aid = j.optLong("aid"),
        cid = j.optLong("cid"),
        pic = j.optString("pic", ""),
        title = j.optString("title", ""),
        desc = j.optString("desc", ""),
        duration = j.optLong("duration"),
        owner = VideoOwner(
            mid = owner?.optLong("mid") ?: 0,
            name = owner?.optString("name", "") ?: "",
            face = owner?.optString("face", "") ?: "",
        ),
        stat = VideoStat(
            view = stat?.optLong("view") ?: 0,
            danmaku = stat?.optLong("danmaku") ?: 0,
            like = stat?.optLong("like") ?: 0,
            coin = stat?.optLong("coin") ?: 0,
            favorite = stat?.optLong("favorite") ?: 0,
            share = stat?.optLong("share") ?: 0,
            reply = stat?.optLong("reply") ?: 0,
        ),
        pages = pages,
    )
}

fun parseDashTrack(j: JSONObject): DashTrack {
    val backups = mutableListOf<String>()
    j.optJSONArray("backup_url")?.let { arr ->
        for (i in 0 until arr.length()) {
            val u = arr.optString(i) ?: ""
            if (u.isNotEmpty()) backups.add(u)
        }
    }
    return DashTrack(
        id = j.optLong("id"),
        baseUrl = j.optString("baseUrl", ""),
        backupUrl = backups,
        bandwidth = j.optLong("bandwidth"),
        mimeType = j.optString("mimeType", ""),
        codecs = j.optString("codecs", ""),
        width = j.optInt("width"),
        height = j.optInt("height"),
    )
}

fun parsePlayUrl(j: JSONObject): PlayUrl {
    val durl = mutableListOf<String>()
    j.optJSONArray("durl")?.let { arr ->
        for (i in 0 until arr.length()) {
            val u = arr.optJSONObject(i)?.optString("url", "") ?: ""
            if (u.isNotEmpty()) durl.add(u)
        }
    }
    val dash = j.optJSONObject("dash")
    val dashVideo = mutableListOf<DashTrack>()
    val dashAudio = mutableListOf<DashTrack>()
    dash?.optJSONArray("video")?.let { arr ->
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { dashVideo.add(parseDashTrack(it)) }
        }
    }
    dash?.optJSONArray("audio")?.let { arr ->
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { dashAudio.add(parseDashTrack(it)) }
        }
    }
    return PlayUrl(
        quality = j.optInt("quality"),
        format = j.optString("format", ""),
        durationMs = j.optLong("timelength"),
        durl = durl,
        dashVideo = dashVideo,
        dashAudio = dashAudio,
    )
}
