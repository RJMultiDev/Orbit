package com.qx.orbit.core.danmaku

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DanmakuParserTest {

    private val xmlSample = """
        <?xml version="1.0" encoding="UTF-8"?>
        <i>
          <chatserver>chat.bilibili.com</chatserver>
          <chatid>233</chatid>
          <maxlimit>500</maxlimit>
          <d p="5.5,1,25,16777215,1700000000,0,abcd1234,1001">你好世界</d>
          <d p="6.25,4,25,16711680,1700000001,0,efgh5678,1002">底部弹幕</d>
          <d p="7.0,7,25,65280,1700000002,0,ijkl9012,1003">[特殊弹幕]</d>
          <d p="8.0,5,25,255,1700000003,0,mnop3456,1004">&amp;lt;tag&amp;gt;</d>
        </i>
    """.trimIndent()

    @Test
    fun parseXml_basicFields() {
        val events = DanmakuParser.parseXml(xmlSample)
        assertEquals(4, events.size)

        val e0 = events[0]
        assertEquals(5500L, e0.timeMs)
        assertEquals(DanmakuMode.SCROLL, e0.mode)
        assertEquals("你好世界", e0.text)
        assertEquals(0xFFFFFFFFL, e0.color)
        assertEquals(25f, e0.fontSize)
        assertEquals(1700000000L, e0.sentAtSec)
        assertEquals(0, e0.pool)
        assertEquals("abcd1234", e0.userHash)
        assertEquals(1001L, e0.rowId)
        assertEquals(0, e0.sourceIndex)

        assertEquals(DanmakuMode.FIX_BOTTOM, events[1].mode)
        assertEquals(6250L, events[1].timeMs)
        assertEquals(0xFFFF0000L, events[1].color)
        assertEquals("efgh5678", events[1].userHash)

        assertEquals(DanmakuMode.SPECIAL, events[2].mode)
        assertEquals(7000L, events[2].timeMs)
        assertEquals(0xFF00FF00L, events[2].color)

        assertEquals(DanmakuMode.FIX_TOP, events[3].mode)
        assertEquals(8000L, events[3].timeMs)
        assertEquals(0xFF0000FFL, events[3].color)
    }

    @Test
    fun parseXml_entityDecodedOnce() {
        val events = DanmakuParser.parseXml(xmlSample)
        // "&amp;lt;tag&amp;gt;" 单遍解码为 "&lt;tag&gt;"，不做二次解码
        assertEquals("&lt;tag&gt;", events[3].text)
    }

    @Test
    fun parseXml_cdataKeptRaw() {
        val xml = """<i><d p="1.0,1,25,16777215,0,0,u,5"><![CDATA[你好 <世界> & 大家]]></d></i>"""
        val events = DanmakuParser.parseXml(xml)
        assertEquals(1, events.size)
        assertEquals("你好 <世界> & 大家", events[0].text)
    }

    @Test
    fun parseXml_numericEntity() {
        val xml = """<i><d p="1.0,1,25,16777215,0,0,u,5">&#x1F600; &#233;</d></i>"""
        val events = DanmakuParser.parseXml(xml)
        assertEquals(1, events.size)
        assertEquals("\uD83D\uDE00 \u00E9", events[0].text)
    }

    @Test
    fun parseXml_mode2and3MapToScroll() {
        val xml = """<i><d p="1,2,25,16777215,0,0,u,1">a</d><d p="2,3,25,16777215,0,0,u,2">b</d></i>"""
        val events = DanmakuParser.parseXml(xml)
        assertEquals(DanmakuMode.SCROLL, events[0].mode)
        assertEquals(DanmakuMode.SCROLL, events[1].mode)
    }

    @Test
    fun parseXml_malformedReturnsEmptyOrSkips() {
        assertTrue(DanmakuParser.parseXml("not xml at all").isEmpty())
        assertTrue(DanmakuParser.parseXml("<i></i>").isEmpty())
        // 非法 p 属性（time 非数字）的条目被跳过，不影响其余条目
        val xml = """<i><d p="abc,1,25,16777215,0,0,u,1">bad</d><d p="2.0,1,25,16777215,0,0,u,2">ok</d></i>"""
        val events = DanmakuParser.parseXml(xml)
        assertEquals(1, events.size)
        assertEquals("ok", events[0].text)
    }

    @Test
    fun parseJson_objectForm() {
        val json = """[
            {"p": "1.0,1,25,16777215,1700000000,0,user1,11", "m": "第一条"},
            {"p": "2.5,5,25,255,1700000001,0,user2,12", "m": "顶部弹幕"},
            {"p": "3.0,7,25,65280,1700000002,0,user3,13", "text": "用 text 键"}
        ]"""
        val events = DanmakuParser.parseJson(json)
        assertEquals(3, events.size)

        val e0 = events[0]
        assertEquals(1000L, e0.timeMs)
        assertEquals(DanmakuMode.SCROLL, e0.mode)
        assertEquals("第一条", e0.text)
        assertEquals("user1", e0.userHash)

        assertEquals(DanmakuMode.FIX_TOP, events[1].mode)
        assertEquals(2500L, events[1].timeMs)
        assertEquals(0xFF0000FFL, events[1].color)

        assertEquals(DanmakuMode.SPECIAL, events[2].mode)
        assertEquals("用 text 键", events[2].text)
    }

    @Test
    fun parseJson_arrayForm() {
        val json = """[
            [1.0, 1, 25, 16777215, 1700000000, 0, "u1", 21, "数组弹幕"],
            [2.0, 4, 25, 16711680, 1700000001, 0, "u2", 22, "底部"]
        ]"""
        val events = DanmakuParser.parseJson(json)
        assertEquals(2, events.size)

        val e0 = events[0]
        assertEquals(1000L, e0.timeMs)
        assertEquals(DanmakuMode.SCROLL, e0.mode)
        assertEquals("数组弹幕", e0.text)
        assertEquals(0xFFFFFFFFL, e0.color)

        assertEquals(DanmakuMode.FIX_BOTTOM, events[1].mode)
        assertEquals(0xFFFF0000L, events[1].color)
        assertEquals("u2", events[1].userHash)
    }

    @Test
    fun parseJson_unicodeEscape() {
        val json = """[[1.0,1,25,16777215,0,0,"u",1,"\u4f60\u597d"]]"""
        val events = DanmakuParser.parseJson(json)
        assertEquals(1, events.size)
        assertEquals("你好", events[0].text)
    }

    @Test
    fun parseJson_malformedReturnsEmpty() {
        assertTrue(DanmakuParser.parseJson("").isEmpty())
        assertTrue(DanmakuParser.parseJson("not json").isEmpty())
        assertTrue(DanmakuParser.parseJson("[]").isEmpty())
        // 顶层不是数组
        assertTrue(DanmakuParser.parseJson("{\"p\":\"1,1,25,1,0,0,u,1\",\"m\":\"x\"}").isEmpty())
    }

    @Test
    fun parsePAttribute_invalidInputsReturnNull() {
        assertNull(DanmakuParser.parsePAttribute("1.0", "x"))               // 字段数不足
        assertNull(DanmakuParser.parsePAttribute("abc,1,25,1", "x"))        // time 非数字
        assertNull(DanmakuParser.parsePAttribute("-1.0,1,25,1", "x"))       // 负数时间
        assertNull(DanmakuParser.parsePAttribute("1.0,1,25,1", "  "))       // 空文本
    }

    @Test
    fun modeMapping_matchesBiliRawValues() {
        assertEquals(DanmakuMode.SCROLL, DanmakuMode.fromRaw(1))
        assertEquals(DanmakuMode.SCROLL, DanmakuMode.fromRaw(2))
        assertEquals(DanmakuMode.SCROLL, DanmakuMode.fromRaw(3))
        assertEquals(DanmakuMode.FIX_BOTTOM, DanmakuMode.fromRaw(4))
        assertEquals(DanmakuMode.FIX_TOP, DanmakuMode.fromRaw(5))
        assertEquals(DanmakuMode.REVERSE, DanmakuMode.fromRaw(6))
        assertEquals(DanmakuMode.SPECIAL, DanmakuMode.fromRaw(7))
        assertEquals(DanmakuMode.CODE, DanmakuMode.fromRaw(8))
        assertEquals(DanmakuMode.BAS, DanmakuMode.fromRaw(9))
        assertEquals(DanmakuMode.UNKNOWN, DanmakuMode.fromRaw(99))
    }

    @Test
    fun parseXml_timeFloatRounding() {
        // 浮点秒 → 毫秒（与现有引擎 *1000 语义一致）
        val events = DanmakuParser.parseXml("""<i><d p="0.001,1,25,1,0,0,u,1">x</d></i>""")
        assertEquals(1L, events[0].timeMs)
    }
}
