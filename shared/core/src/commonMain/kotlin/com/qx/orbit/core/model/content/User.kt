package com.qx.orbit.core.model.content

/** 用户信息（对应 Android `data/model/User.kt` 的 `UserInfo`，去掉 Parcelable 与注解）。 */
data class UserInfo(
    val mid: Long = 0,
    val name: String = "",
    val avatar: String = "",
    val sign: String = "",
    val fans: Int = 0,
    val level: Int = 0,
    val following: Int = 0,
    val followed: Boolean = false,
    val title: String = "",
    val notice: String = "",
    val official: Int = 0,
    val officialDesc: String = "",
    val mtime: Long = 0,
    val vip_role: Int = 0,
    val vip_nickname_color: String = "",
    val current_exp: Long = 0,
    val next_exp: Long = 0,
    val medal_name: String = "",
    val medal_level: Int = 0,
    val sys_notice: String = "",
    val live_room: LiveRoom? = null,
    val is_senior_member: Int = 0,
    val is_follow_display: Boolean = false
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): UserInfo {
            val officialRaw = fields["official"] ?: fields["official_verify"]
            val official = when (officialRaw) {
                is Number -> officialRaw.toInt()
                is Map<*, *> -> {
                    val m = Wire.toStrMap(officialRaw) ?: emptyMap()
                    val role = Wire.int(m, "role")
                    if (m.containsKey("role") && role != -1) role
                    else when (Wire.int(m, "type")) {
                        0 -> 1
                        1 -> 2
                        else -> 0
                    }
                }
                else -> 0
            }
            var vipRole = when (val v = fields["vip_role"] ?: fields["vipStatus"] ?: fields["vip_status"]) {
                is Number -> v.toInt()
                is String -> v.toIntOrNull() ?: 0
                else -> 0
            }
            if (vipRole == 0) {
                Wire.map(fields, "vip")?.let { vip ->
                    vipRole = Wire.int(vip, "vipStatus", "status")
                }
            }
            return UserInfo(
                mid = Wire.long(fields, "mid"),
                name = Wire.str(fields, "name", "uname"),
                avatar = Wire.str(fields, "face", "avatar"),
                sign = Wire.str(fields, "sign"),
                fans = Wire.int(fields, "follower", "fans"),
                level = Wire.int(fields, "level"),
                following = Wire.int(fields, "following"),
                followed = Wire.bool(fields, "followed"),
                title = Wire.str(fields, "title"),
                notice = Wire.str(fields, "notice"),
                official = official,
                officialDesc = Wire.str(fields, "officialDesc", "official_desc"),
                mtime = Wire.long(fields, "mtime"),
                vip_role = vipRole,
                vip_nickname_color = Wire.str(fields, "vip_nickname_color"),
                current_exp = Wire.long(fields, "current_exp"),
                next_exp = Wire.long(fields, "next_exp"),
                medal_name = Wire.str(fields, "medal_name"),
                medal_level = Wire.int(fields, "medal_level"),
                sys_notice = Wire.str(fields, "sys_notice"),
                live_room = Wire.map(fields, "live_room", "liveRoom").let { it?.let(LiveRoom::fromWire) },
                is_senior_member = Wire.int(fields, "is_senior_member"),
                is_follow_display = Wire.bool(fields, "is_follow_display")
            )
        }
    }
}

/** 直播间信息（对应 Android `data/model/Live.kt` 的 `LiveRoom`；UserInfo 内嵌使用）。 */
data class LiveRoom(
    val roomid: Long = 0,
    val short_id: Long = 0,
    val uid: Long = 0,
    val title: String = "",
    val uname: String = "",
    val tags: String = "",
    val description: String = "",
    val online: Int = 0,
    val attention: Int = 0,
    val user_cover: String = "",
    val cover: String = "",
    val keyframe: String = "",
    val face: String = "",
    val area_parent_id: Int = 0,
    val area_parent_name: String = "",
    val area_id: Int = 0,
    val area_name: String = "",
    val live_status: Int = 0,
    val liveTime: String = "",
    val is_portrait: Boolean = false
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): LiveRoom = LiveRoom(
            roomid = Wire.long(fields, "roomid", "room_id"),
            short_id = Wire.long(fields, "short_id"),
            uid = Wire.long(fields, "uid"),
            title = Wire.str(fields, "title"),
            uname = Wire.str(fields, "uname"),
            tags = Wire.str(fields, "tags"),
            description = Wire.str(fields, "description"),
            online = Wire.int(fields, "online"),
            attention = Wire.int(fields, "attention"),
            user_cover = Wire.str(fields, "user_cover"),
            cover = Wire.str(fields, "cover"),
            keyframe = Wire.str(fields, "keyframe"),
            face = Wire.str(fields, "face"),
            area_parent_id = Wire.int(fields, "area_parent_id", "area_v2_parent_id", "parent_area_id"),
            area_parent_name = Wire.str(fields, "area_parent_name", "area_v2_parent_name", "parent_area_name"),
            area_id = Wire.int(fields, "area_id", "area_v2_id"),
            area_name = Wire.str(fields, "area_name", "area_v2_name"),
            live_status = Wire.int(fields, "live_status"),
            liveTime = Wire.str(fields, "liveTime", "live_time"),
            is_portrait = Wire.bool(fields, "is_portrait")
        )
    }
}
