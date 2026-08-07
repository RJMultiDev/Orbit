package com.qx.orbit.core.model.content

/** 动态/Opus（对应 Android `data/model/Dynamic.kt` 与 `data/model/Emote.kt`）。 */

data class Dynamic(
    val dynamicId: String = "",
    val type: String = "",
    val comment_id: Long = 0,
    val comment_type: Int = 0,
    val title: String = "",
    val userInfo: UserInfo? = null,
    val content: String = "",
    val pubTime: String = "",
    val stats: Stats? = null,
    val major_type: String = "",
    val major_object: Any? = null,
    val dynamic_forward: Dynamic? = null,
    val canDelete: Boolean = false,
    val images: List<String> = emptyList(),
    val cover: String = "",
    val bvid: String = "",
    val archiveTitle: String = "",
    val emotes: Map<String, Emote> = emptyMap(),
    val members: Map<String, Long> = emptyMap(),
    val liveState: Int = 0
) {
    companion object {
        const val DYNAMIC_TYPE_UGC_SEASON = "DYNAMIC_TYPE_UGC_SEASON"

        fun fromWire(fields: Map<String, Any?>): Dynamic = Dynamic(
            dynamicId = Wire.str(fields, "dynamic_id", "dynamicId", "id"),
            type = Wire.str(fields, "type"),
            comment_id = Wire.long(fields, "comment_id", "commentId"),
            comment_type = Wire.int(fields, "comment_type", "commentType"),
            title = Wire.str(fields, "title"),
            userInfo = Wire.map(fields, "userInfo", "user_info").let { it?.let(UserInfo::fromWire) },
            content = Wire.str(fields, "content"),
            pubTime = Wire.str(fields, "pubTime", "pub_time"),
            stats = Wire.map(fields, "stats", "stat").let { it?.let(Stats::fromWire) },
            major_type = Wire.str(fields, "major_type"),
            major_object = fields["major_object"],
            dynamic_forward = Wire.map(fields, "dynamic_forward", "forward").let { it?.let(::fromWire) },
            canDelete = Wire.bool(fields, "canDelete", "can_delete"),
            images = Wire.strList(fields, "images"),
            cover = Wire.str(fields, "cover", "pic"),
            bvid = Wire.str(fields, "bvid"),
            archiveTitle = Wire.str(fields, "archiveTitle", "archive_title"),
            emotes = (fields["emotes"] as? Map<*, *>)?.mapNotNull { (k, v) ->
                val key = k?.toString() ?: return@mapNotNull null
                val emote = Wire.toStrMap(v)?.let { Emote.fromWire(it) } ?: return@mapNotNull null
                key to emote
            }?.toMap() ?: emptyMap(),
            members = Wire.longMap(fields, "members"),
            liveState = Wire.int(fields, "liveState", "live_state")
        )
    }
}

data class Opus(
    val id: Long = 0,
    val type: Int = TYPE_DYNAMIC,
    val commentId: Long = 0,
    val commentType: Int = 0,
    val title: String = "",
    val cover: String = "",
    val content: String = "",
    val pubTime: String = "",
    val upInfo: UserInfo? = null,
    val stats: Stats? = null,
    val topImages: List<String> = emptyList(),
    val paragraphs: Array<OpusParagraph>? = null,
    val parsedId: Long = 0
) {
    companion object {
        const val TYPE_DYNAMIC = 1
        const val TYPE_ARTICLE = 2
        const val TYPE_DYNAMIC_OLD_STYLE = 3

        fun fromWire(fields: Map<String, Any?>): Opus = Opus(
            id = Wire.long(fields, "id"),
            type = Wire.int(fields, "type").takeUnless { it == 0 } ?: TYPE_DYNAMIC,
            commentId = Wire.long(fields, "comment_id", "commentId"),
            commentType = Wire.int(fields, "comment_type", "commentType"),
            title = Wire.str(fields, "title"),
            cover = Wire.str(fields, "cover", "pic"),
            content = Wire.str(fields, "content"),
            pubTime = Wire.str(fields, "pubTime", "pub_time"),
            upInfo = Wire.map(fields, "upInfo", "up_info", "userInfo").let { it?.let(UserInfo::fromWire) },
            stats = Wire.map(fields, "stats", "stat").let { it?.let(Stats::fromWire) },
            topImages = Wire.strList(fields, "topImages", "top_images"),
            paragraphs = Wire.mapList(fields, "paragraphs").let {
                if (it.isEmpty()) null else it.map(OpusParagraph::fromWire).toTypedArray()
            },
            parsedId = Wire.long(fields, "parsedId", "parsed_id")
        )
    }
}

data class OpusParagraph(
    val align: Int = 0,
    val type: Int = 0,
    val textNodes: List<OpusTextNode> = emptyList(),
    val pics: List<String> = emptyList()
) {
    companion object {
        const val TYPE_TEXT = 1
        const val TYPE_PIC = 2
        const val TYPE_DIVIDER = 3
        const val TYPE_TEXT_BLOCKQUOTE = 4
        const val TYPE_LIST = 5
        const val TYPE_HEADING = 8
        const val TYPE_TEXT_OPUS = 99
        const val TYPE_VIDEO = 100
        const val TYPE_DYNAMIC = 101
        const val TYPE_ARTICLE = 102

        fun fromWire(fields: Map<String, Any?>): OpusParagraph = OpusParagraph(
            align = Wire.int(fields, "align"),
            type = Wire.int(fields, "type"),
            textNodes = Wire.mapList(fields, "textNodes", "text_nodes").map { OpusTextNode.fromWire(it) },
            pics = Wire.strList(fields, "pics")
        )
    }
}

data class OpusTextNode(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val color: String? = null,
    val fontSize: Int = 17,
    val jumpUrl: String? = null,
    val emoteUrl: String? = null,
    val emoteSize: Int = 1,
    val memberId: Long? = null
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): OpusTextNode = OpusTextNode(
            text = Wire.str(fields, "text"),
            bold = Wire.bool(fields, "bold"),
            italic = Wire.bool(fields, "italic"),
            color = Wire.strOrNull(fields, "color"),
            fontSize = Wire.int(fields, "fontSize", "font_size").takeUnless { it == 0 } ?: 17,
            jumpUrl = Wire.strOrNull(fields, "jumpUrl", "jump_url"),
            emoteUrl = Wire.strOrNull(fields, "emoteUrl", "emote_url"),
            emoteSize = Wire.int(fields, "emoteSize", "emote_size").takeUnless { it == 0 } ?: 1,
            memberId = Wire.long(fields, "memberId", "member_id").let { if (fields.containsKey("memberId") || fields.containsKey("member_id")) it else null }
        )
    }
}

data class OpusCard(
    val title: String = "",
    val id: Long = 0,
    val cover: String = "",
    val upName: String = "",
    val view: String = ""
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): OpusCard {
            val viewRaw = fields["view"]
            val view = when (viewRaw) {
                is Number -> Wire.toWan(viewRaw.toLong())
                is String -> viewRaw
                else -> ""
            }
            return OpusCard(
                title = Wire.str(fields, "title"),
                id = Wire.long(fields, "id"),
                cover = Wire.str(fields, "cover", "pic"),
                upName = Wire.str(fields, "upName", "up_name"),
                view = view
            )
        }
    }
}

/** 表情（对应 Android `data/model/Emote.kt` 的 `Emote`；动态 `emotes` 映射值）。 */
data class Emote(
    val id: Int = 0,
    val packageId: Int = 0,
    val name: String = "",
    val alias: String = "",
    val url: String = "",
    val size: Int = 0
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): Emote = Emote(
            id = Wire.int(fields, "id"),
            packageId = Wire.int(fields, "packageId", "package_id"),
            name = Wire.str(fields, "name"),
            alias = Wire.str(fields, "alias"),
            url = Wire.str(fields, "url"),
            size = Wire.int(fields, "size")
        )
    }
}
