package com.qx.orbit.ohos.data

import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * 社区数据（评论/动态/番剧/直播/文章/专栏）。
 *
 * 说明：本文件是社区域约定的入口，但评论/动态/番剧/直播/文章的主接口已由
 * 并行创建的兄弟文件实现（ReplyApi.kt / DynamicFeedApi.kt / BangumiApi.kt /
 * LiveApi.kt / ArticleApi.kt），为避免重复声明冲突，本文件只补齐尚未实现的
 * **专栏（Opus）视图**，对应 Android data/api/OpusApi.kt 的 getOpus。
 *
 * - 评论列表/发送：`BiliClient.replies` / `BiliClient.sendReply`（ReplyApi.kt）
 * - 动态流/详情：`BiliClient.dynamicFeed` / `BiliClient.dynamicDetail`
 * - 番剧信息：`BiliClient.bangumiInfo`
 * - 直播间：`BiliClient.liveRoomInfo` / `BiliClient.livePlayUrl`
 * - 文章：`BiliClient.articleView`
 */

/**
 * Opus 富文本节点（对应 Android data/model/Dynamic.kt 的 OpusTextNode）。
 * `emoteUrl` 非空表示该节点是行内表情图；`jumpUrl` 非空表示是跳转富文本。
 */
data class OpusTextNode(
    val text: String = "",
    val bold: Boolean = false,
    val italic: Boolean = false,
    val fontSize: Int = 17,
    val color: String = "",
    val emoteUrl: String = "",
    val emoteSize: Int = 1,
    val jumpUrl: String = "",
)

/**
 * Opus 段落（对应 Android OpusParagraph；para_type 常量对齐 B 站接口取值）。
 */
data class OpusParagraph(
    val type: Int = TYPE_TEXT,
    val align: Int = 0,
    val textNodes: List<OpusTextNode> = emptyList(),
    val pics: List<String> = emptyList(),
) {
    companion object {
        const val TYPE_TEXT = 1
        const val TYPE_PIC = 2
        const val TYPE_DIVIDER = 3
        const val TYPE_BLOCKQUOTE = 4
        const val TYPE_HEADING = 8
    }
}

/**
 * 专栏动态详情（对齐 Android data/model/Dynamic.kt 的 Opus 精简投影）。
 */
data class OpusDetail(
    val id: Long = 0,
    val type: String = "",
    val title: String = "",
    val content: String = "",
    val cover: String = "",
    val images: List<String> = emptyList(),
    val paragraphs: List<OpusParagraph> = emptyList(),
    val pubTs: Long = 0,
    val upMid: Long = 0,
    val upName: String = "",
    val upFace: String = "",
    val likeCount: Long = 0,
    val commentCount: Long = 0,
    val forwardCount: Long = 0,
    val liked: Boolean = false,
    val commentId: Long = 0,
    val commentType: Int = 0,
)

private const val HOST = "https://api.bilibili.com"
private const val DYNAMIC_FEATURES =
    "itemOpusStyle,listOnlyfans,opusBigCover,onlyfansVote,forwardListHidden,decorationCard,commentsNewVersion,onlyfansAssetsV2,ugcDelete,onlyfansQaCard,avatarAutoTheme,sunflowerStyle,eva3CardOpus,eva3CardVideo,eva3CardComment"

/**
 * 专栏（Opus）详情：GET /x/polymer/web-dynamic/v1/detail?id=..，取 data.item 解析。
 *
 * 注：Android OpusApi 用 `https://www.bilibili.com/opus/{id}` 页面内嵌
 * `window.__INITIAL_STATE__` 抓取；`BiliClient.request` 只回 JSON，这里改走
 * web-dynamic/detail（opus 与动态共用同一 item 结构，含 module_content.paragraphs）。
 */
internal suspend fun BiliClient.opusView(id: Long): OpusDetail? {
    if (id <= 0L) return null
    val resp = request(
        "$HOST/x/polymer/web-dynamic/v1/detail",
        mapOf("id" to id.toString(), "features" to DYNAMIC_FEATURES),
    )
    if (resp?.optInt("code", -1) != 0) return null
    val item = resp.optJSONObject("data")?.optJSONObject("item") ?: return null
    return parseOpusDetail(item, id)
}

private fun parseOpusDetail(j: JSONObject, fallbackId: Long): OpusDetail {
    val dynId = (j.optString("id_str", "").toLongOrNull() ?: 0L).takeIf { it != 0L } ?: j.optLong("id").takeIf { it != 0L } ?: fallbackId
    val type = j.optString("type", "")

    val modules = j.optJSONObject("modules")
    val author = modules?.optJSONObject("module_author")
    val dynContent = modules?.optJSONObject("module_dynamic")
    val stat = modules?.optJSONObject("module_stat")
    val contentModule = modules?.optJSONObject("module_content")

    val upMid = author?.optMid() ?: 0L
    val upName = (author?.optString("name", "") ?: "")
        .ifEmpty { author?.optJSONObject("user")?.optString("name", "") ?: "" }
    val upFace = (author?.optString("face", "") ?: "")
        .ifEmpty { author?.optJSONObject("user")?.optString("face", "") ?: "" }
    val pubTs = author?.optLong("pub_ts") ?: 0L

    val majorObj = dynContent?.optJSONObject("major")
    val majorType = majorObj?.optString("type", "") ?: ""
    var content = dynContent?.optJSONObject("desc")?.optString("text", "") ?: ""
    val title = dynContent?.optJSONObject("topic")?.optString("name", "") ?: ""

    // 顶部图：MAJOR_TYPE_DRAW 的图片 + module_top.display.album.pics
    val topImages = mutableListOf<String>()
    if (majorType == "MAJOR_TYPE_DRAW") {
        majorObj?.optJSONObject("draw")?.optJSONArray("items")?.let { arr ->
            for (i in 0 until arr.length()) {
                val src = arr.optJSONObject(i)?.optString("src", "") ?: ""
                if (src.isNotEmpty()) topImages.add(src)
            }
        }
    }
    modules?.optJSONObject("module_top")
        ?.optJSONObject("display")
        ?.optJSONObject("album")
        ?.optJSONArray("pics")?.let { arr ->
            for (i in 0 until arr.length()) {
                val url = arr.optJSONObject(i)?.optString("url", "") ?: ""
                if (url.isNotEmpty()) topImages.add(url)
            }
        }

    val paragraphs = mutableListOf<OpusParagraph>()
    contentModule?.optJSONArray("paragraphs")?.let { arr ->
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { paragraphs.addAll(parseOpusParagraphs(it)) }
        }
    }
    // 无结构化段落时退化为按行拆分的纯文本段落
    if (paragraphs.isEmpty() && content.isNotEmpty()) {
        for (line in content.split("\n")) {
            paragraphs.add(
                OpusParagraph(type = OpusParagraph.TYPE_TEXT, align = 0, textNodes = listOf(OpusTextNode(text = line)))
            )
        }
    }

    val likeStat = stat?.optJSONObject("like")
    val statsLike = likeStat?.optLong("count") ?: 0L

    val basic = j.optJSONObject("basic")
    var commentId = basic?.optString("comment_id_str", "")?.toLongOrNull() ?: 0L
    var commentType = basic?.optInt("comment_type") ?: 0
    var cover = ""
    when (majorType) {
        "MAJOR_TYPE_ARCHIVE" -> {
            val archive = majorObj?.optJSONObject("archive")
            if (commentId == 0L) commentId = archive?.optLong("aid") ?: 0L
            if (commentType == 0) commentType = 1
            cover = archive?.optString("cover", "") ?: ""
        }
        "MAJOR_TYPE_ARTICLE" -> {
            val article = majorObj?.optJSONObject("article")
            if (commentId == 0L) commentId = article?.optLong("id") ?: 0L
            if (commentType == 0) commentType = 12
        }
        else -> {
            if (commentId == 0L) commentId = dynId
            if (commentType == 0) commentType = 17
        }
    }

    return OpusDetail(
        id = dynId,
        type = type,
        title = title,
        content = content,
        cover = cover,
        images = topImages,
        paragraphs = paragraphs,
        pubTs = pubTs,
        upMid = upMid,
        upName = upName,
        upFace = upFace,
        likeCount = statsLike,
        commentCount = stat?.optJSONObject("comment")?.optLong("count") ?: 0L,
        forwardCount = stat?.optJSONObject("forward")?.optLong("count") ?: 0L,
        liked = likeStat?.optBoolean("status") ?: false,
        commentId = commentId,
        commentType = commentType,
    )
}

/** 单个 paragraph → 0..n 个 OpusParagraph（图片段可含多图，只产出一个段落）。 */
private fun parseOpusParagraphs(p: JSONObject): List<OpusParagraph> {
    val paraType = p.optInt("para_type")
    val align = p.optInt("align")
    return when (paraType) {
        OpusParagraph.TYPE_TEXT, OpusParagraph.TYPE_HEADING, OpusParagraph.TYPE_BLOCKQUOTE -> {
            val nodes = parseOpusTextNodes(p.optJSONObject("text")?.optJSONArray("nodes"))
            if (nodes.isNotEmpty()) listOf(OpusParagraph(type = paraType, align = align, textNodes = nodes)) else emptyList()
        }
        OpusParagraph.TYPE_PIC -> {
            val urls = mutableListOf<String>()
            p.optJSONObject("pic")?.optJSONArray("pics")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val url = arr.optJSONObject(i)?.optString("url", "") ?: ""
                    if (url.isNotEmpty()) urls.add(url)
                }
            }
            if (urls.isNotEmpty()) listOf(OpusParagraph(type = paraType, align = align, pics = urls)) else emptyList()
        }
        OpusParagraph.TYPE_DIVIDER -> listOf(OpusParagraph(type = paraType, align = align))
        else -> emptyList()
    }
}

private fun parseOpusTextNodes(nodes: JSONArray?): List<OpusTextNode> {
    if (nodes == null) return emptyList()
    val list = mutableListOf<OpusTextNode>()
    for (i in 0 until nodes.length()) {
        val node = nodes.optJSONObject(i) ?: continue
        when (node.optString("type", "")) {
            "TEXT_NODE_TYPE_WORD" -> {
                val word = node.optJSONObject("word") ?: continue
                val style = word.optJSONObject("style")
                list.add(
                    OpusTextNode(
                        text = word.optString("words", ""),
                        bold = style?.optBoolean("bold") ?: false,
                        italic = style?.optBoolean("italic") ?: false,
                        fontSize = word.optInt("font_size", 17),
                        color = word.optString("color", ""),
                    )
                )
            }
            "TEXT_NODE_TYPE_RICH" -> {
                val rich = node.optJSONObject("rich") ?: continue
                val text = rich.optString("text", "")
                when (rich.optString("type", "")) {
                    "RICH_TEXT_NODE_TYPE_EMOJI" -> {
                        val emoji = rich.optJSONObject("emoji")
                        list.add(
                            OpusTextNode(
                                text = text,
                                emoteUrl = emoji?.optString("icon_url", "") ?: "",
                                emoteSize = emoji?.optInt("size") ?: 1,
                            )
                        )
                    }
                    "RICH_TEXT_NODE_TYPE_RICH", "RICH_TEXT_NODE_TYPE_TOPIC", "RICH_TEXT_NODE_TYPE_AT" ->
                        list.add(
                            OpusTextNode(
                                text = text,
                                jumpUrl = rich.optString("jump_url", ""),
                                color = "#FB7299",
                            )
                        )
                    else -> list.add(OpusTextNode(text = text))
                }
            }
        }
    }
    return list
}

/** mid 字段在回复/动态 JSON 中可能是字符串或数字，统一取 Long。 */
private fun JSONObject.optMid(): Long {
    val s = optString("mid", "")
    if (s.isNotEmpty()) return s.toLongOrNull() ?: 0L
    return optLong("mid")
}
