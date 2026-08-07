package com.qx.orbit.core.model.social

import com.qx.orbit.core.model.content.Emote
import com.qx.orbit.core.model.content.Wire

/**
 * 表情包领域模型（组 2）。
 *
 * 对应 Android `data/model/Emote.kt` 的 `EmotePackage`。
 * 单个 `Emote` 已由 A12 在 `model.content`（`Dynamic.kt`）抽取并被 `Dynamic.emotes`
 * / 本包 [Reply] 复用，此处不重复定义。
 */
data class EmotePackage(
    val id: Int = 0,
    val text: String = "",
    val url: String = "",
    val type: Int = 0,
    val attr: Int = 0,
    val size: Int = 0,
    val item_id: Int = 0,
    val emotes: List<Emote> = emptyList(),
    val permanent: Boolean = false
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): EmotePackage = EmotePackage(
            id = Wire.int(fields, "id"),
            text = Wire.str(fields, "text", "name"),
            url = Wire.str(fields, "url"),
            type = Wire.int(fields, "type"),
            attr = Wire.int(fields, "attr"),
            size = Wire.int(fields, "size"),
            item_id = Wire.int(fields, "item_id", "itemId"),
            emotes = Wire.mapList(fields, "emotes").map { Emote.fromWire(it) },
            permanent = Wire.bool(fields, "permanent")
        )
    }
}
