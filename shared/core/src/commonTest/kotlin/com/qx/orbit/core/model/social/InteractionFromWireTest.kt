package com.qx.orbit.core.model.social

import kotlin.test.Test
import kotlin.test.assertEquals

class InteractionFromWireTest {

    @Test
    fun `InteractionVideoData maps story and choices`() {
        val data = InteractionVideoData.fromWire(
            mapOf(
                "title" to "互动视频示例（脱敏）",
                "edge_id" to 10L,
                "story_list" to listOf(
                    mapOf(
                        "node_id" to 1L,
                        "edge_id" to 10L,
                        "title" to "开端",
                        "cid" to 101L,
                        "start_pos" to 0L,
                        "cover" to "https://example.com/story.jpg",
                        "is_current" to 1
                    )
                ),
                "edges" to mapOf(
                    "questions" to listOf(
                        mapOf(
                            "id" to 5L,
                            "type" to 1,
                            "start_time" to 3000L,
                            "duration" to 5000L,
                            "pause_video" to 1,
                            "title" to "选择去向",
                            "choices" to listOf(
                                mapOf("id" to 1L, "cid" to 102L, "option" to "选项A", "is_default" to 0, "is_hidden" to 0),
                                mapOf("id" to 2L, "cid" to 103L, "option" to "选项B")
                            )
                        )
                    )
                ),
                "is_leaf" to 0
            )
        )

        assertEquals("互动视频示例（脱敏）", data.title)
        assertEquals(10L, data.edgeId)
        assertEquals(0, data.isLeaf)

        val story = data.storyList.single()
        assertEquals(1L, story.nodeId)
        assertEquals("开端", story.title)
        assertEquals(101L, story.cid)
        assertEquals(1, story.isCurrent)

        val question = data.edges?.questions?.single()
        assertEquals(5L, question?.id)
        assertEquals(3000L, question?.startTimeR)
        assertEquals(5000L, question?.duration)
        assertEquals(1, question?.pauseVideo)
        assertEquals("选择去向", question?.title)
        assertEquals(2, question?.choices?.size)
        assertEquals("选项A", question?.choices?.first()?.option)
        assertEquals("选项B", question?.choices?.last()?.option)
        assertEquals(103L, question?.choices?.last()?.cid)
    }
}
