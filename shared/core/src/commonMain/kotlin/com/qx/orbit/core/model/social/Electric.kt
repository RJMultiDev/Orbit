package com.qx.orbit.core.model.social

import com.qx.orbit.core.model.content.Wire

/**
 * 充电/电电领域模型（组 2）。
 *
 * 对应 Android `data/model/Electric.kt`。嵌套 `VipInfoData` 为充电页专属
 * （字段与 `model.content` 顶层 `VipInfoData` 不同，保持嵌套避免歧义）。
 */
data class ElectricPanel(
    val count: Int = 0,
    val total_count: Int = 0,
    val total: Int = 0,
    val special_day: Int = 0,
    val list: List<ElectricUser> = emptyList()
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): ElectricPanel = ElectricPanel(
            count = Wire.int(fields, "count"),
            total_count = Wire.int(fields, "total_count", "totalCount"),
            total = Wire.int(fields, "total"),
            special_day = Wire.int(fields, "special_day", "specialDay"),
            list = Wire.mapList(fields, "list", "data").map { ElectricUser.fromWire(it) }
        )
    }
}

data class ElectricUser(
    val uname: String = "",
    val avatar: String = "",
    val mid: Long = 0,
    val pay_mid: Long = 0,
    val rank: Int = 0,
    val trend_type: Int = 0,
    val message: String = "",
    val msg_hidden: Int = 0,
    val vip_info: VipInfoData? = null
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): ElectricUser = ElectricUser(
            uname = Wire.str(fields, "uname", "name"),
            avatar = Wire.str(fields, "avatar", "face"),
            mid = Wire.long(fields, "mid"),
            pay_mid = Wire.long(fields, "pay_mid", "payMid"),
            rank = Wire.int(fields, "rank"),
            trend_type = Wire.int(fields, "trend_type", "trendType"),
            message = Wire.str(fields, "message"),
            msg_hidden = Wire.int(fields, "msg_hidden", "msgHidden"),
            vip_info = Wire.map(fields, "vip_info", "vipInfo").let { it?.let(VipInfoData::fromWire) }
        )
    }

    data class VipInfoData(
        val vipDueMsec: Long = 0,
        val vipStatus: Int = 0,
        val vipType: Int = 0
    ) {
        companion object {
            fun fromWire(fields: Map<String, Any?>): VipInfoData = VipInfoData(
                vipDueMsec = Wire.long(fields, "vipDueMsec", "vip_due_msec", "due_date"),
                vipStatus = Wire.int(fields, "vipStatus", "vip_status"),
                vipType = Wire.int(fields, "vipType", "vip_type")
            )
        }
    }
}
