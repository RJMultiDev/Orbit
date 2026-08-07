package com.qx.orbit.core.model.content

/** 账号/会员相关（对应 Android `data/model/Account.kt` 与 `data/api/UserInfoApi.kt` 的 NavInfo 部分）。 */

data class LoginRecord(
    val mid: Long = 0,
    val deviceName: String = "",
    val loginType: String = "",
    val loginTime: String = "",
    val location: String = "",
    val ip: String = ""
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): LoginRecord = LoginRecord(
            mid = Wire.long(fields, "mid"),
            deviceName = Wire.str(fields, "deviceName", "device_name"),
            loginType = Wire.str(fields, "loginType", "login_type"),
            loginTime = Wire.str(fields, "loginTime", "login_time"),
            location = Wire.str(fields, "location"),
            ip = Wire.str(fields, "ip")
        )
    }
}

data class ExpLog(
    val delta: Int = 0,
    val time: String = "",
    val reason: String = ""
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): ExpLog = ExpLog(
            delta = Wire.int(fields, "delta"),
            time = Wire.str(fields, "time"),
            reason = Wire.str(fields, "reason")
        )
    }
}

data class CoinLog(
    val time: String = "",
    val delta: Int = 0,
    val reason: String = ""
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): CoinLog = CoinLog(
            time = Wire.str(fields, "time"),
            delta = Wire.int(fields, "delta"),
            reason = Wire.str(fields, "reason")
        )
    }
}

data class MedalInfo(
    val target_id: Long = 0,
    val level: Int = 0,
    val medal_name: String = "",
    val medal_color_start: Int = 0,
    val medal_color_end: Int = 0,
    val medal_color_border: Int = 0,
    val guard_level: Int = 0,
    val wearing_status: Int = 0,
    val medal_id: Long = 0,
    val target_name: String = "",
    val target_icon: String = ""
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): MedalInfo = MedalInfo(
            target_id = Wire.long(fields, "target_id"),
            level = Wire.int(fields, "level"),
            medal_name = Wire.str(fields, "medal_name"),
            medal_color_start = Wire.int(fields, "medal_color_start"),
            medal_color_end = Wire.int(fields, "medal_color_end"),
            medal_color_border = Wire.int(fields, "medal_color_border"),
            guard_level = Wire.int(fields, "guard_level"),
            wearing_status = Wire.int(fields, "wearing_status"),
            medal_id = Wire.long(fields, "medal_id"),
            target_name = Wire.str(fields, "target_name"),
            target_icon = Wire.str(fields, "target_icon")
        )
    }
}

data class FollowTag(
    val tagid: Int = 0,
    val name: String = "",
    val count: Int = 0
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): FollowTag = FollowTag(
            tagid = Wire.int(fields, "tagid"),
            name = Wire.str(fields, "name"),
            count = Wire.int(fields, "count")
        )
    }
}

data class VipDetailInfo(
    val isVip: Boolean = false,
    val level: Int = 0,
    val vipStatus: Int = 0,
    val vipType: Int = 0,
    val vipDueDate: Long = 0,
    val privilegeList: List<Privilege> = emptyList()
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): VipDetailInfo = VipDetailInfo(
            isVip = Wire.bool(fields, "isVip", "is_vip"),
            level = Wire.int(fields, "level"),
            vipStatus = Wire.int(fields, "vipStatus", "vip_status"),
            vipType = Wire.int(fields, "vipType", "vip_type"),
            vipDueDate = Wire.long(fields, "vipDueDate", "vip_due_date"),
            privilegeList = Wire.mapList(fields, "privilegeList", "privilege_list").map { Privilege.fromWire(it) }
        )
    }

    data class Privilege(
        val type: Int = 0,
        val state: Int = 0,
        val expireTime: Long = 0
    ) {
        companion object {
            fun fromWire(fields: Map<String, Any?>): Privilege = Privilege(
                type = Wire.int(fields, "type"),
                state = Wire.int(fields, "state"),
                expireTime = Wire.long(fields, "expireTime", "expire_time")
            )
        }
    }
}

/** 导航栏/当前登录态信息（对应 Android `data/api/UserInfoApi.kt` 的 `NavInfoData`）。 */
data class NavInfo(
    val isLogin: Boolean = false,
    val mid: Long = 0,
    val uname: String? = null,
    val face: String? = null,
    val level_info: LevelInfo? = null,
    val is_senior_member: Int = 0,
    val vip: VipInfoData? = null
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): NavInfo = NavInfo(
            isLogin = Wire.bool(fields, "isLogin", "is_login"),
            mid = Wire.long(fields, "mid"),
            uname = Wire.strOrNull(fields, "uname"),
            face = Wire.strOrNull(fields, "face"),
            level_info = Wire.map(fields, "level_info").let { it?.let(LevelInfo::fromWire) },
            is_senior_member = Wire.int(fields, "is_senior_member"),
            vip = Wire.map(fields, "vip").let { it?.let(VipInfoData::fromWire) }
        )
    }
}

data class LevelInfo(
    val current_level: Int = 0,
    val current_exp: Long = 0,
    val next_exp: Long = 0
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): LevelInfo = LevelInfo(
            current_level = Wire.int(fields, "current_level"),
            current_exp = Wire.long(fields, "current_exp"),
            next_exp = Wire.long(fields, "next_exp")
        )
    }
}

data class VipInfoData(
    val vipType: Int = 0,
    val vipStatus: Int = 0,
    val theme_type: Int = 0,
    val nickname_color: String? = null
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): VipInfoData = VipInfoData(
            vipType = Wire.int(fields, "vipType", "vip_type"),
            vipStatus = Wire.int(fields, "vipStatus", "vip_status"),
            theme_type = Wire.int(fields, "theme_type"),
            nickname_color = Wire.strOrNull(fields, "nickname_color")
        )
    }
}
