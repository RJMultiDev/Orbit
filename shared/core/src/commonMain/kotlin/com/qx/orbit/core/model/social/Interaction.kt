package com.qx.orbit.core.model.social

import com.qx.orbit.core.model.content.Wire

/**
 * 互动视频（互动剧）领域模型（组 2）。
 *
 * 对应 Android `data/model/Interaction.kt`，来源为互动视频接口
 * （`x/stein-gate`）返回的 `story_list` / `edges` 结构。
 */
data class InteractionVideoData(
    val title: String = "",
    val edgeId: Long = 0,
    val storyList: List<StoryNode> = emptyList(),
    val edges: InteractionEdge? = null,
    val isLeaf: Int = 0
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): InteractionVideoData = InteractionVideoData(
            title = Wire.str(fields, "title"),
            edgeId = Wire.long(fields, "edge_id", "edgeId"),
            storyList = Wire.mapList(fields, "story_list", "storyList").map { StoryNode.fromWire(it) },
            edges = Wire.map(fields, "edges").let { it?.let(InteractionEdge::fromWire) },
            isLeaf = Wire.int(fields, "is_leaf", "isLeaf")
        )
    }

    data class StoryNode(
        val nodeId: Long = 0,
        val edgeId: Long = 0,
        val title: String = "",
        val cid: Long = 0,
        val startPos: Long = 0,
        val cover: String = "",
        val isCurrent: Int = 0
    ) {
        companion object {
            fun fromWire(fields: Map<String, Any?>): StoryNode = StoryNode(
                nodeId = Wire.long(fields, "node_id", "nodeId"),
                edgeId = Wire.long(fields, "edge_id", "edgeId"),
                title = Wire.str(fields, "title"),
                cid = Wire.long(fields, "cid"),
                startPos = Wire.long(fields, "start_pos", "startPos"),
                cover = Wire.str(fields, "cover", "pic"),
                isCurrent = Wire.int(fields, "is_current", "isCurrent")
            )
        }
    }

    data class InteractionEdge(
        val questions: List<Question> = emptyList()
    ) {
        companion object {
            fun fromWire(fields: Map<String, Any?>): InteractionEdge = InteractionEdge(
                questions = Wire.mapList(fields, "questions").map { Question.fromWire(it) }
            )
        }
    }

    data class Question(
        val id: Long = 0,
        val type: Int = 0,
        val startTimeR: Long = 0,
        val duration: Long = 0,
        val pauseVideo: Int = 0,
        val title: String = "",
        val choices: List<Choice> = emptyList()
    ) {
        companion object {
            fun fromWire(fields: Map<String, Any?>): Question = Question(
                id = Wire.long(fields, "id"),
                type = Wire.int(fields, "type"),
                startTimeR = Wire.long(fields, "start_time", "startTimeR", "startTime"),
                duration = Wire.long(fields, "duration"),
                pauseVideo = Wire.int(fields, "pause_video", "pauseVideo"),
                title = Wire.str(fields, "title"),
                choices = Wire.mapList(fields, "choices").map { Choice.fromWire(it) }
            )
        }
    }

    data class Choice(
        val id: Long = 0,
        val cid: Long = 0,
        val option: String = "",
        val isDefault: Int = 0,
        val isHidden: Int = 0
    ) {
        companion object {
            fun fromWire(fields: Map<String, Any?>): Choice = Choice(
                id = Wire.long(fields, "id"),
                cid = Wire.long(fields, "cid"),
                option = Wire.str(fields, "option"),
                isDefault = Wire.int(fields, "is_default", "isDefault"),
                isHidden = Wire.int(fields, "is_hidden", "isHidden")
            )
        }
    }
}
