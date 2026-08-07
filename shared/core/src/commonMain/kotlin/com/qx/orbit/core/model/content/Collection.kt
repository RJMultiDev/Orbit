package com.qx.orbit.core.model.content

/** 视频合集（对应 Android `data/model/Favorite.kt` 的 `Collection`，被 [VideoInfo] 引用）。 */
data class Collection(
    val id: Int = 0,
    val title: String = "",
    val cover: String = "",
    val intro: String = "",
    val mid: Long = 0,
    val sections: List<Section> = emptyList(),
    val cards: List<VideoCard> = emptyList(),
    val view: String = ""
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): Collection = Collection(
            id = Wire.int(fields, "id"),
            title = Wire.str(fields, "title"),
            cover = Wire.str(fields, "cover", "pic"),
            intro = Wire.str(fields, "intro"),
            mid = Wire.long(fields, "mid"),
            sections = Wire.mapList(fields, "sections").map { Section.fromWire(it) },
            cards = Wire.mapList(fields, "cards").map { VideoCard.fromWire(it) },
            view = Wire.str(fields, "view")
        )
    }

    data class Section(
        val season_id: Int = 0,
        val id: Int = 0,
        val title: String = "",
        val type: Int = 0,
        val episodes: List<Episode> = emptyList()
    ) {
        companion object {
            fun fromWire(fields: Map<String, Any?>): Section = Section(
                season_id = Wire.int(fields, "season_id"),
                id = Wire.int(fields, "id"),
                title = Wire.str(fields, "title"),
                type = Wire.int(fields, "type"),
                episodes = Wire.mapList(fields, "episodes").map { Episode.fromWire(it) }
            )
        }
    }

    data class Episode(
        val season_id: Int = 0,
        val section_id: Int = 0,
        val id: Long = 0,
        val aid: Long = 0,
        val cid: Long = 0,
        val title: String = "",
        val bvid: String = "",
        val arc: VideoInfo? = null
    ) {
        companion object {
            fun fromWire(fields: Map<String, Any?>): Episode = Episode(
                season_id = Wire.int(fields, "season_id"),
                section_id = Wire.int(fields, "section_id"),
                id = Wire.long(fields, "id"),
                aid = Wire.long(fields, "aid"),
                cid = Wire.long(fields, "cid"),
                title = Wire.str(fields, "title"),
                bvid = Wire.str(fields, "bvid"),
                arc = Wire.map(fields, "arc").let { it?.let(VideoInfo::fromWire) }
            )
        }
    }
}

/** 追番/追更系列（对应 Android `data/model/Favorite.kt` 顶层 `Series`）。 */
data class Series(
    val type: String = "series",
    val id: Int = 0,
    val title: String = "",
    val cover: String = "",
    val intro: String = "",
    val mid: Long = 0,
    val total: String = ""
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): Series = Series(
            type = Wire.str(fields, "type").ifEmpty { "series" },
            id = Wire.int(fields, "id"),
            title = Wire.str(fields, "title"),
            cover = Wire.str(fields, "cover", "pic"),
            intro = Wire.str(fields, "intro"),
            mid = Wire.long(fields, "mid"),
            total = Wire.str(fields, "total")
        )
    }
}
