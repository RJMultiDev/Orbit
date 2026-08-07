package com.qx.orbit.core.model.social

import com.qx.orbit.core.model.content.Emote
import com.qx.orbit.core.model.content.UserInfo
import com.qx.orbit.core.model.content.Wire

/**
 * 评论/回复领域模型（组 2）。
 *
 * 对应 Android `data/model/Reply.kt`：去掉 Parcelable 注解，
 * `sender` 复用 `model.content.UserInfo`，`emotes` 复用 `model.content.Emote`
 * （与 `Dynamic.emotes` 同一类型，避免双份表情模型）。
 */
enum class ContentType(val typeCode: Int) {
    Video(1), Topic(2), Activity(4), ShortVideo(5), BlackRoom(6),
    Announcement(7), Live(8), ActivityContent(9), LiveAnnouncement(10),
    Photo(11), Article(12), Ticket(13), Audio(14), Judgement(15),
    Review(16), Dynamic(17), VideoPlaylist(18), AudioPlaylist(19),
    Manga_1(20), Manga_2(21), Manga_3(22), Opus(23), Course(33);

    companion object {
        fun fromCode(code: Int) = entries.firstOrNull { it.typeCode == code } ?: Video
    }
}

data class Reply(
    val rpid: Long = 0,
    val oid: Long = 0,
    val root: Long = 0,
    val parent: Long = 0,
    val forceDelete: Boolean = false,
    val ofBvid: String = "",
    val pubTime: String = "",
    val sender: UserInfo? = null,
    val message: String = "",
    val pictureList: List<String> = emptyList(),
    val likeCount: Int = 0,
    val upLiked: Boolean = false,
    val upReplied: Boolean = false,
    val liked: Boolean = false,
    val childCount: Int = 0,
    val isDynamic: Boolean = false,
    val childMsgList: List<Reply> = emptyList(),
    val isTop: Boolean = false,
    val isUp: Boolean = false,
    val emotes: Map<String, Emote> = emptyMap(),
    val members: Map<String, Long> = emptyMap()
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): Reply {
            val content = Wire.map(fields, "content")
            val upAction = Wire.map(fields, "up_action")
            val replyControl = Wire.map(fields, "reply_control")
            val pictureList = if (fields.containsKey("pictureList") || fields.containsKey("picture_list")) {
                Wire.strList(fields, "pictureList", "picture_list")
            } else {
                Wire.mapList(fields, "pictures").map { Wire.str(it, "img_src", "src") }
            }
            return Reply(
                rpid = Wire.long(fields, "rpid"),
                oid = Wire.long(fields, "oid"),
                root = Wire.long(fields, "root"),
                parent = Wire.long(fields, "parent"),
                forceDelete = Wire.bool(fields, "forceDelete", "force_delete"),
                ofBvid = Wire.str(fields, "ofBvid", "of_bvid"),
                pubTime = Wire.str(fields, "pubTime", "pub_time"),
                sender = Wire.map(fields, "member", "sender").let { it?.let(UserInfo::fromWire) },
                message = if (fields.containsKey("message")) {
                    Wire.str(fields, "message")
                } else {
                    content?.let { Wire.str(it, "message") }.orEmpty()
                },
                pictureList = pictureList,
                likeCount = Wire.int(fields, "like", "likeCount", "like_count"),
                upLiked = upAction?.let { Wire.bool(it, "like") } ?: Wire.bool(fields, "upLiked", "up_liked"),
                upReplied = upAction?.let { Wire.bool(it, "reply") } ?: Wire.bool(fields, "upReplied", "up_replied"),
                liked = Wire.bool(fields, "liked", "liked_by_me"),
                childCount = Wire.int(fields, "rcount", "childCount", "child_count"),
                isDynamic = Wire.bool(fields, "isDynamic", "is_dynamic"),
                childMsgList = Wire.mapList(fields, "replies", "childMsgList", "child_replies").map { Reply.fromWire(it) },
                isTop = replyControl?.let { Wire.bool(it, "is_up_top", "is_top") } ?: Wire.bool(fields, "isTop", "is_top"),
                isUp = replyControl?.let { Wire.bool(it, "is_up") } ?: Wire.bool(fields, "isUp", "is_up"),
                emotes = (fields["emotes"] as? Map<*, *>)?.mapNotNull { (k, v) ->
                    val key = k?.toString() ?: return@mapNotNull null
                    val emote = Wire.toStrMap(v)?.let { Emote.fromWire(it) } ?: return@mapNotNull null
                    key to emote
                }?.toMap() ?: emptyMap(),
                members = Wire.longMap(fields, "members")
            )
        }
    }
}
