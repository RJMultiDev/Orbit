package com.qx.orbit.core.model.content

/**
 * 视频内容领域模型（组 1）。
 *
 * 纯 Kotlin 平台无关数据类：无 Gson 注解、无 Parcelable、无 `android.*`。
 * 字段名与 Android DTO（`app/.../data/model/Video.kt`）保持一致；
 * 通过 [VideoInfo.fromWire] 等工厂从 B 站原始 JSON 字段构造，供 Harmony adapter 复用。
 */

data class VideoInfo(
    val bvid: String = "",
    val aid: Long = 0,
    val title: String = "",
    val staff: List<UserInfo> = emptyList(),
    val cover: String = "",
    val description: String = "",
    val duration: String = "",
    val stats: Stats? = null,
    val timeDesc: String = "",
    val pagenames: List<String> = emptyList(),
    val cids: List<Long> = emptyList(),
    val descAts: List<At> = emptyList(),
    val upowerExclusive: Boolean = false,
    val argueMsg: String? = null,
    val isCooperation: Boolean = false,
    val isSteinGate: Boolean = false,
    val is360: Boolean = false,
    val epid: Long = -1,
    val copyright: Int = 0,
    val collection: Collection? = null,
    val history: History? = null
) {
    companion object {
        const val COPYRIGHT_SELF = 1
        const val COPYRIGHT_REPRINT = 2

        fun fromWire(fields: Map<String, Any?>): VideoInfo {
            val rights = Wire.map(fields, "rights")
            val durationRaw = fields["duration"]
            val duration = when (durationRaw) {
                is Number -> Wire.formatDuration(durationRaw.toInt())
                is String -> durationRaw
                else -> ""
            }
            val copyright = Wire.int(fields, "copyright")
            val rawStat = Wire.map(fields, "stat", "stats")
            val stats = rawStat?.let { stat ->
                val reqUser = Wire.map(fields, "req_user")
                val merged = LinkedHashMap(stat)
                if (reqUser != null) {
                    merged["followed"] = Wire.int(reqUser, "attention") == 1
                    merged["liked"] = Wire.int(reqUser, "like") == 1
                    merged["disliked"] = Wire.int(reqUser, "dislike") == 1
                    merged["favoured"] = Wire.int(reqUser, "favorite") == 1
                    merged["coined"] = Wire.int(reqUser, "coin")
                }
                if (!merged.containsKey("coin_limit")) {
                    merged["coin_limit"] = if (copyright == COPYRIGHT_REPRINT) 1 else 2
                }
                Stats.fromWire(merged)
            }
            val pages = Wire.mapList(fields, "pages")
            val epid = run {
                val redirect = Wire.str(fields, "redirect_url")
                if (redirect.contains("bangumi/play/ep")) {
                    redirect.substringAfter("bangumi/play/ep").toLongOrNull() ?: -1L
                } else -1L
            }
            return VideoInfo(
                bvid = Wire.str(fields, "bvid"),
                aid = Wire.long(fields, "aid"),
                title = Wire.str(fields, "title"),
                staff = Wire.mapList(fields, "staff").map { UserInfo.fromWire(it) },
                cover = Wire.str(fields, "pic"),
                description = Wire.str(fields, "desc"),
                duration = duration,
                stats = stats,
                timeDesc = Wire.str(fields, "timeDesc", "time_desc"),
                pagenames = if (fields.containsKey("pagenames")) {
                    Wire.strList(fields, "pagenames")
                } else {
                    pages.map { Wire.str(it, "part") }
                },
                cids = if (fields.containsKey("cids")) {
                    Wire.longList(fields, "cids")
                } else {
                    pages.map { Wire.long(it, "cid") }
                },
                descAts = Wire.mapList(fields, "descAts", "desc_ats").map { At.fromWire(it) },
                upowerExclusive = Wire.bool(fields, "is_upower_exclusive", "upowerExclusive"),
                argueMsg = Wire.map(fields, "argue_info")?.let { Wire.strOrNull(it, "argue_msg") }
                    ?: Wire.strOrNull(fields, "argueMsg", "argue_msg"),
                isCooperation = Wire.int(rights ?: emptyMap(), "is_cooperation") == 1,
                isSteinGate = Wire.int(rights ?: emptyMap(), "is_stein_gate") == 1,
                is360 = Wire.int(rights ?: emptyMap(), "is_360") == 1,
                epid = epid,
                copyright = copyright,
                collection = Wire.map(fields, "ugc_season", "collection").let { it?.let(Collection::fromWire) },
                history = Wire.map(fields, "history").let { it?.let(History::fromWire) }
            )
        }
    }
}

data class VideoCard(
    val title: String = "",
    val upName: String = "",
    val view: String = "",
    val cover: String = "",
    val type: String = "video",
    val aid: Long = 0,
    val bvid: String = "",
    val cid: Long = 0,
    val mid: Long = 0,
    val seasonId: Long = 0,
    val progressPercent: Float? = null,
    val kid: String = ""
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): VideoCard {
            val viewRaw = fields["view"] ?: fields["play"]
            val view = when (viewRaw) {
                is Number -> Wire.toWan(viewRaw.toLong())
                is String -> viewRaw
                else -> ""
            }
            return VideoCard(
                title = Wire.str(fields, "title"),
                upName = Wire.str(fields, "upName", "up_name", "author", "uname"),
                view = view,
                cover = Wire.str(fields, "pic", "cover"),
                type = Wire.str(fields, "type").ifEmpty { "video" },
                aid = Wire.long(fields, "aid"),
                bvid = Wire.str(fields, "bvid"),
                cid = Wire.long(fields, "cid"),
                mid = Wire.long(fields, "mid"),
                seasonId = Wire.long(fields, "season_id", "seasonId"),
                progressPercent = Wire.floatOrNull(fields, "progress_percent", "progressPercent"),
                kid = Wire.str(fields, "kid")
            )
        }
    }
}

data class Stats(
    val view: Int = 0,
    val like: Int = 0,
    val reply: Int = 0,
    val coin: Int = 0,
    val share: Int = 0,
    val danmaku: Int = 0,
    val favorite: Int = 0,
    val followed: Boolean = false,
    val liked: Boolean = false,
    val disliked: Boolean = false,
    val favoured: Boolean = false,
    val coined: Int = 0,
    val like_disabled: Boolean = false,
    val coin_disabled: Boolean = false,
    val fav_disabled: Boolean = false,
    val reply_disabled: Boolean = false,
    val share_disabled: Boolean = false,
    val coin_limit: Int = 0
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): Stats = Stats(
            view = Wire.int(fields, "view"),
            like = Wire.int(fields, "like"),
            reply = Wire.int(fields, "reply"),
            coin = Wire.int(fields, "coin"),
            share = Wire.int(fields, "share"),
            danmaku = Wire.int(fields, "danmaku"),
            favorite = Wire.int(fields, "favorite"),
            followed = Wire.bool(fields, "followed"),
            liked = Wire.bool(fields, "liked"),
            disliked = Wire.bool(fields, "disliked"),
            favoured = Wire.bool(fields, "favoured"),
            coined = Wire.int(fields, "coined"),
            like_disabled = Wire.bool(fields, "like_disabled"),
            coin_disabled = Wire.bool(fields, "coin_disabled"),
            fav_disabled = Wire.bool(fields, "fav_disabled"),
            reply_disabled = Wire.bool(fields, "reply_disabled"),
            share_disabled = Wire.bool(fields, "share_disabled"),
            coin_limit = Wire.int(fields, "coin_limit")
        )
    }
}

data class At(
    val id: Long = 0,
    val start: Int = 0,
    val end: Int = 0,
    val name: String = ""
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): At = At(
            id = Wire.long(fields, "id"),
            start = Wire.int(fields, "start"),
            end = Wire.int(fields, "end"),
            name = Wire.str(fields, "name")
        )
    }
}

data class History(
    val cid: Long = 0,
    val progress: Int = 0
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): History = History(
            cid = Wire.long(fields, "cid"),
            progress = Wire.int(fields, "progress")
        )
    }
}
