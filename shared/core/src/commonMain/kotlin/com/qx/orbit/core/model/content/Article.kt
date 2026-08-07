package com.qx.orbit.core.model.content

/** 文章内容（对应 Android `data/model/Article.kt`）。 */

data class ArticleInfo(
    val id: Long = 0,
    val title: String = "",
    val summary: String = "",
    val banner: String = "",
    val upInfo: UserInfo? = null,
    val ctime: Long = 0,
    val stats: Stats? = null,
    val wordCount: Int = 0,
    val keywords: String = "",
    val content: String = ""
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): ArticleInfo = ArticleInfo(
            id = Wire.long(fields, "id"),
            title = Wire.str(fields, "title"),
            summary = Wire.str(fields, "summary"),
            banner = Wire.str(fields, "banner").ifEmpty { Wire.strList(fields, "image_urls").firstOrNull().orEmpty() },
            upInfo = Wire.map(fields, "upInfo", "up_info", "author").let { it?.let(UserInfo::fromWire) },
            ctime = Wire.long(fields, "ctime", "publish_time"),
            stats = Wire.map(fields, "stats", "stat").let { it?.let(Stats::fromWire) },
            wordCount = Wire.int(fields, "wordCount", "word_count"),
            keywords = Wire.str(fields, "keywords"),
            content = Wire.str(fields, "content")
        )
    }
}

data class ArticleCard(
    val title: String = "",
    val id: Long = 0,
    val cover: String = "",
    val upName: String = "",
    val view: String = ""
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): ArticleCard {
            val viewRaw = fields["view"]
            val view = when (viewRaw) {
                is Number -> Wire.toWan(viewRaw.toLong())
                is String -> viewRaw
                else -> ""
            }
            return ArticleCard(
                title = Wire.str(fields, "title"),
                id = Wire.long(fields, "id"),
                cover = Wire.str(fields, "cover", "pic").ifEmpty { Wire.strList(fields, "image_urls").firstOrNull().orEmpty() },
                upName = Wire.str(fields, "upName", "up_name", "author_name"),
                view = view
            )
        }
    }
}

data class ArticleLine(
    val type: Int = 0,
    val content: String = "",
    val extra: String = ""
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): ArticleLine = ArticleLine(
            type = Wire.int(fields, "type"),
            content = Wire.str(fields, "content"),
            extra = Wire.str(fields, "extra")
        )
    }
}
