package com.qx.orbit.core.subtitle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SubtitleParserTest {

    /** 脱敏字幕样例：时间轴 0-2 / 2-4 / 4-6 秒。 */
    private val sampleJson = """
        {"body":[
          {"from":0.0,"to":2.0,"content":"第一句"},
          {"from":2.0,"to":4.0,"content":"第二句"},
          {"from":4.0,"to":6.0,"content":"第三句"}
        ]}
    """.trimIndent()

    // ---- 解析 ----

    @Test
    fun `parse typical body json`() {
        val cues = SubtitleParser.parseBody(sampleJson)
        assertEquals(3, cues.size)
        assertEquals(SubtitleCue("第一句", 0.0, 2.0), cues[0])
        assertEquals(SubtitleCue("第二句", 2.0, 4.0), cues[1])
        assertEquals(SubtitleCue("第三句", 4.0, 6.0), cues[2])
    }

    @Test
    fun `parse root array json`() {
        val json = """[{"from":1,"to":3,"content":"直接数组"}]"""
        val cues = SubtitleParser.parseBody(json)
        assertEquals(listOf(SubtitleCue("直接数组", 1.0, 3.0)), cues)
    }

    @Test
    fun `parse handles string escapes`() {
        val json = """{"body":[{"from":0.5,"to":1.5,"content":"换行\n与\"引号\"与\/斜杠"}]}"""
        val cues = SubtitleParser.parseBody(json)
        assertEquals("换行\n与\"引号\"与/斜杠", cues[0].content)
    }

    @Test
    fun `malformed json returns empty`() {
        assertTrue(SubtitleParser.parseBody("not json").isEmpty())
        assertTrue(SubtitleParser.parseBody("").isEmpty())
        assertTrue(SubtitleParser.parseBody("""{"body":null}""").isEmpty())
    }

    // ---- 时间轴定位 ----

    private val timeline = SubtitleTimeline(SubtitleParser.parseBody(sampleJson))

    @Test
    fun `text at each segment boundary`() {
        assertEquals("第一句", timeline.textAt(0))          // 0.0s
        assertEquals("第一句", timeline.textAt(1999))       // 1.999s
        assertEquals("第二句", timeline.textAt(2000))       // 2.0s
        assertEquals("第二句", timeline.textAt(3999))
        assertEquals("第三句", timeline.textAt(4000))
        assertEquals("第三句", timeline.textAt(5999))
        assertNull(timeline.textAt(6000))                   // 6.0s 已结束
    }

    @Test
    fun `no cue at gap returns null`() {
        val timeline = SubtitleTimeline(listOf(SubtitleCue("A", 10.0, 12.0)))
        assertNull(timeline.textAt(5000))                   // 5.0s 早于首条
        assertEquals("A", timeline.textAt(11000))
        assertNull(timeline.textAt(13000))                  // 13.0s 晚于末条
    }

    @Test
    fun `seek backward relocates via binary search`() {
        assertEquals("第三句", timeline.textAt(5500))
        // 回退到 1.5s，应从第三句退回第一句
        assertEquals("第一句", timeline.textAt(1500))
        // 再回退到 2.5s
        assertEquals("第二句", timeline.textAt(2500))
    }

    @Test
    fun `forward progression advances cursor`() {
        assertEquals("第一句", timeline.textAt(500))
        assertEquals("第二句", timeline.textAt(2500))
        assertEquals("第三句", timeline.textAt(4500))
    }

    @Test
    fun `reset returns to beginning`() {
        timeline.textAt(4500)
        timeline.reset()
        assertEquals("第一句", timeline.textAt(500))
    }

    @Test
    fun `cues are sorted by from`() {
        val shuffled = SubtitleTimeline(
            listOf(SubtitleCue("B", 4.0, 6.0), SubtitleCue("A", 0.0, 2.0), SubtitleCue("C", 2.0, 4.0))
        )
        assertEquals(listOf("A", "C", "B"), shuffled.cues.map { it.content })
    }

    @Test
    fun `progress in milliseconds converts to seconds`() {
        // 1050ms = 1.05s，落在第一句 [0,2)
        assertEquals("第一句", timeline.textAt(1050))
    }
}
