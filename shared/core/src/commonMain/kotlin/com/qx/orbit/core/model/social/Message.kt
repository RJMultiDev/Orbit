package com.qx.orbit.core.model.social

import com.qx.orbit.core.model.content.UserInfo
import com.qx.orbit.core.model.content.VideoCard
import com.qx.orbit.core.model.content.Wire

/**
 * 消息/私信领域模型（组 2）。
 *
 * 对应 Android `data/model/Message.kt`：`videoCard` 复用 `model.content.VideoCard`，
 * `replyInfo` 引用本包 [Reply]；私信 `content` 为不透明 JSON（保留原始 `Any?`，
 * 对应原 `JsonElement`，无平台序列化依赖）。
 */
data class MessageCard(
    val id: Long = 0,
    val user: List<UserInfo> = emptyList(),
    val timeStamp: Long = 0,
    val timeDesc: String = "",
    val content: String = "",
    val videoCard: VideoCard? = null,
    val replyInfo: Reply? = null,
    val subjectId: Long = 0,
    val businessId: Int = 0,
    val itemType: String = "",
    val getType: Int = 0,
    val sourceId: Long = 0,
    val rootId: Long = 0,
    val targetId: Long = 0
) {
    data class Cursor(
        val is_end: Boolean = false,
        val id: Long = 0,
        val time: Long = 0
    ) {
        companion object {
            fun fromWire(fields: Map<String, Any?>): Cursor = Cursor(
                is_end = Wire.bool(fields, "is_end", "isEnd"),
                id = Wire.long(fields, "id"),
                time = Wire.long(fields, "time", "max_time")
            )
        }
    }

    companion object {
        const val GET_TYPE_REPLY = 0
        const val GET_TYPE_AT = 1
        const val GET_TYPE_LIKE = 2

        fun fromWire(fields: Map<String, Any?>): MessageCard = MessageCard(
            id = Wire.long(fields, "id"),
            user = Wire.mapList(fields, "user", "users").map { UserInfo.fromWire(it) },
            timeStamp = Wire.long(fields, "timeStamp", "timestamp", "ts", "time"),
            timeDesc = Wire.str(fields, "timeDesc", "time_desc", "title"),
            content = Wire.str(fields, "content"),
            videoCard = Wire.map(fields, "video", "video_card", "videoCard").let { it?.let(VideoCard::fromWire) },
            replyInfo = Wire.map(fields, "reply", "reply_info", "replyInfo").let { it?.let(Reply::fromWire) },
            subjectId = Wire.long(fields, "subjectId", "subject_id"),
            businessId = Wire.int(fields, "businessId", "business_id"),
            itemType = Wire.str(fields, "itemType", "item_type"),
            getType = Wire.int(fields, "getType", "get_type"),
            sourceId = Wire.long(fields, "sourceId", "source_id"),
            rootId = Wire.long(fields, "rootId", "root_id"),
            targetId = Wire.long(fields, "targetId", "target_id")
        )
    }
}

/** 私信单条消息（对应 Android `PrivateMessage`；`content` 保留原始 JSON 值）。 */
data class PrivateMessage(
    val content: Any? = null,
    val type: Int = 0,
    val timestamp: Long = 0,
    val uid: Long = 0,
    val name: String = "",
    val msgId: Long = 0,
    val msgSeqno: Long = 0,
    val msg_source: Int = 0
) {
    companion object {
        const val TYPE_TEXT = 1
        const val TYPE_VIDEO = 7
        const val TYPE_PIC = 2
        const val TYPE_RETRACT = 5
        const val TYPE_FACE = 6
        const val TYPE_NOMAL_CARD = 10
        const val TYPE_PIC_CARD = 13
        const val TYPE_TEXT_WITH_VIDEO = 16
        const val TYPE_SYSTEM = 18

        fun fromWire(fields: Map<String, Any?>): PrivateMessage = PrivateMessage(
            content = fields["content"],
            type = Wire.int(fields, "type"),
            timestamp = Wire.long(fields, "timestamp", "ts"),
            uid = Wire.long(fields, "uid", "sender_uid"),
            name = Wire.str(fields, "name", "sender_name", "uname"),
            msgId = Wire.long(fields, "msg_id", "msgId", "id"),
            msgSeqno = Wire.long(fields, "msg_seqno", "msgSeqno", "seqno"),
            msg_source = Wire.int(fields, "msg_source", "msgSource")
        )
    }
}

/** 私信会话（对应 Android `PrivateMsgSession`）。 */
data class PrivateMsgSession(
    val talkerUid: Long = 0,
    val unread: Int = 0,
    val contentType: Int = 0,
    val content: Any? = null
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): PrivateMsgSession = PrivateMsgSession(
            talkerUid = Wire.long(fields, "talker_uid", "talkerUid", "uid"),
            unread = Wire.int(fields, "unread", "unread_count"),
            contentType = Wire.int(fields, "content_type", "contentType"),
            content = fields["content"]
        )
    }
}

/** 消息设置项（对应 Android `MessageSettingItem`）。 */
data class MessageSettingItem(
    val key: String = "",
    val title: String = "",
    val desc: String = "",
    val type: Int = TYPE_SWITCH,
    val value: Boolean = false,
    val options: List<String>? = null
) {
    companion object {
        const val TYPE_SWITCH = 0
        const val TYPE_CHOOSE = 1

        fun fromWire(fields: Map<String, Any?>): MessageSettingItem = MessageSettingItem(
            key = Wire.str(fields, "key"),
            title = Wire.str(fields, "title"),
            desc = Wire.str(fields, "desc", "description"),
            type = Wire.int(fields, "type").let { if (fields.containsKey("type")) it else TYPE_SWITCH },
            value = Wire.bool(fields, "value"),
            options = Wire.strList(fields, "options").takeIf { fields.containsKey("options") }
        )
    }
}
