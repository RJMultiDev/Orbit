package com.qx.orbit.bili.data.model

data class Dynamic(
    val dynamicId: Long = 0,
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
    val archiveTitle: String = ""
) {
    companion object {
        const val DYNAMIC_TYPE_UGC_SEASON = "DYNAMIC_TYPE_UGC_SEASON"
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
    }
}

data class OpusParagraph(
    val align: Int = 0,
    val type: Int = 0,
    val content: Any? = null
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
    }
}

data class OpusCard(
    val title: String = "",
    val id: Long = 0,
    val cover: String = "",
    val upName: String = "",
    val view: String = ""
)
