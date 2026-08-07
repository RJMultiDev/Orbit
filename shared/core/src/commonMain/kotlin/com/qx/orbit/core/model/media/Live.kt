package com.qx.orbit.core.model.media

import com.qx.orbit.core.model.content.LiveRoom
import com.qx.orbit.core.model.content.UserInfo
import com.qx.orbit.core.model.content.Wire

/**
 * 直播房间与拉流信息（对应 Android `data/model/Live.kt` 的 `LivePlayInfo`/`LiveInfo`）。
 *
 * `LiveRoom` 已由 A12 在 `model.content`（`User.kt`）抽取并被 `UserInfo` 引用，
 * 此处复用而非重复定义；直播流地址树 `Playurl` 见同包的 [PlayUrl.kt]。
 */
data class LivePlayInfo(
    val roomid: Long = 0,
    val short_id: Long = 0,
    val uid: Long = 0,
    val isHidden: Boolean = false,
    val isLocked: Boolean = false,
    val isPortrait: Boolean = false,
    val live_status: Int = 0,
    val encrypted: Boolean = false,
    val pwd_verified: Boolean = false,
    val live_time: Long = 0,
    val playurl_info: PlayurlInfo? = null,
    val official_type: Int = 0,
    val official_room_id: Int = 0,
    val risk_with_delay: Int = 0
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): LivePlayInfo = LivePlayInfo(
            roomid = Wire.long(fields, "room_id", "roomid"),
            short_id = Wire.long(fields, "short_id"),
            uid = Wire.long(fields, "uid"),
            isHidden = Wire.bool(fields, "is_hidden"),
            isLocked = Wire.bool(fields, "is_locked"),
            isPortrait = Wire.bool(fields, "is_portrait"),
            live_status = Wire.int(fields, "live_status"),
            encrypted = Wire.bool(fields, "encrypted"),
            pwd_verified = Wire.bool(fields, "pwd_verified"),
            live_time = Wire.long(fields, "live_time"),
            playurl_info = Wire.map(fields, "playurl_info").let { it?.let(PlayurlInfo::fromWire) },
            official_type = Wire.int(fields, "official_type"),
            official_room_id = Wire.int(fields, "official_room_id"),
            risk_with_delay = Wire.int(fields, "risk_with_delay")
        )
    }
}

/** 直播 playurl 外层包装（对应 Android `PlayurlInfo`）。 */
data class PlayurlInfo(
    val playurl: Playurl? = null
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): PlayurlInfo = PlayurlInfo(
            playurl = Wire.map(fields, "playurl").let { it?.let(Playurl::fromWire) }
        )
    }
}

/** 直播聚合信息（主播 + 房间 + 拉流信息，对应 Android `LiveInfo`）。 */
data class LiveInfo(
    val userInfo: UserInfo? = null,
    val liveRoom: LiveRoom? = null,
    val livePlayInfo: LivePlayInfo? = null
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): LiveInfo = LiveInfo(
            userInfo = Wire.map(fields, "userInfo", "user_info").let { it?.let(UserInfo::fromWire) },
            liveRoom = Wire.map(fields, "liveRoom", "live_room", "room").let { it?.let(LiveRoom::fromWire) },
            livePlayInfo = Wire.map(fields, "livePlayInfo", "live_play_info", "play_info").let { it?.let(LivePlayInfo::fromWire) }
        )
    }
}
