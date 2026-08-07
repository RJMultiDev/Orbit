package com.qx.orbit.core.model.media

import com.qx.orbit.core.model.content.Wire

/**
 * 弹幕分段模型（对应 Android `data/model/Danmaku.kt`）。
 *
 * 来自弹幕分段接口（`x/v2/dm/web/seg.so`）的 JSON 项；纯字段映射，
 * 弹幕协议解析/时间轴逻辑在 `com.qx.orbit.core.danmaku` 包（A10/A12 已有）。
 */
data class DanmakuElem(
    val id: Long = 0,
    val progress: Int = 0,
    val mode: Int = 1,
    val fontsize: Int = 25,
    val color: Int = 0,
    val midHash: String = "",
    val content: String = "",
    val ctime: Long = 0,
    val weight: Int = 0,
    val action: String = "",
    val pool: Int = 0,
    val idStr: String = "",
    val attr: Int = 0,
    val animation: String = ""
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): DanmakuElem = DanmakuElem(
            id = Wire.long(fields, "id"),
            progress = Wire.int(fields, "progress"),
            mode = Wire.int(fields, "mode").let { if (fields.containsKey("mode")) it else 1 },
            fontsize = Wire.int(fields, "fontsize").let { if (fields.containsKey("fontsize")) it else 25 },
            color = Wire.int(fields, "color"),
            midHash = Wire.str(fields, "midHash", "mid_hash"),
            content = Wire.str(fields, "content"),
            ctime = Wire.long(fields, "ctime"),
            weight = Wire.int(fields, "weight"),
            action = Wire.str(fields, "action"),
            pool = Wire.int(fields, "pool"),
            idStr = Wire.str(fields, "idStr", "id_str"),
            attr = Wire.int(fields, "attr"),
            animation = Wire.str(fields, "animation")
        )
    }
}

/** 弹幕分段响应（对应 Android `DmSegMobileReply`）。 */
data class DmSegMobileReply(
    val elems: List<DanmakuElem> = emptyList()
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): DmSegMobileReply = DmSegMobileReply(
            elems = Wire.mapList(fields, "elems").map { DanmakuElem.fromWire(it) }
        )
    }
}
