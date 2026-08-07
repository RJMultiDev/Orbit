package com.qx.orbit.core.model.social

import com.qx.orbit.core.model.content.Wire

/**
 * 收藏夹领域模型（组 2）。
 *
 * 对应 Android `data/model/Favorite.kt` 的 `FavoriteFolder`；
 * 同文件的 `Collection`/`Series` 已由 A12 在 `model.content` 抽取并被 `VideoInfo` 引用，
 * 此处不再重复定义。
 */
data class FavoriteFolder(
    val id: Long = 0,
    val mediaId: Long = 0,
    val name: String = "",
    val cover: String = "",
    val videoCount: Int = 0,
    val maxCount: Int = 0,
    val isDefault: Boolean = false
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): FavoriteFolder = FavoriteFolder(
            // 与 Android `@SerializedName("fav_box", alternate = ["id", "fid"])` 一致
            id = Wire.long(fields, "fav_box", "fid", "id"),
            // 与 Android `@SerializedName("id", alternate = ["media_id"])` 一致
            mediaId = Wire.long(fields, "id", "media_id"),
            name = Wire.str(fields, "title", "name"),
            cover = Wire.str(fields, "cover", "pic"),
            videoCount = Wire.int(fields, "count", "video_count"),
            maxCount = Wire.int(fields, "max_count", "maxCount"),
            isDefault = Wire.bool(fields, "is_default", "isDefault")
        )
    }
}
