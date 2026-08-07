package com.qx.orbit.core.model.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MiscFromWireTest {

    @Test
    fun `Announcement and Tutorial parse from wire`() {
        val notice = Announcement.fromWire(
            mapOf("id" to 1, "ctime" to "2026-08-07", "title" to "测试公告（脱敏）", "content" to "公告内容（脱敏）")
        )
        assertEquals("测试公告（脱敏）", notice.title)

        val tutorial = Tutorial.fromWire(
            mapOf(
                "name" to "测试教程（脱敏）",
                "description" to "教程说明",
                "imgid" to "img-1",
                "type" to 2,
                "content" to listOf(mapOf("type" to 1, "text" to "步骤一（脱敏）", "style" to "bold", "color" to "#000000"))
            )
        )
        assertEquals("测试教程（脱敏）", tutorial.name)
        assertEquals("步骤一（脱敏）", tutorial.content.single().text)
        assertEquals("bold", tutorial.content.single().style)
    }

    @Test
    fun `SettingSection and LocalVideo parse from wire`() {
        val section = SettingSection.fromWire(
            mapOf("type" to "switch", "id" to "k1", "name" to "测试设置", "desc" to "设置说明", "defaultValue" to "1")
        )
        assertEquals("switch", section.type)
        assertEquals("1", section.defaultValue)

        val local = LocalVideo.fromWire(
            mapOf(
                "cover" to "https://example.com/cover.jpg",
                "title" to "本地视频（脱敏）",
                "page_list" to listOf("P1"),
                "video_file_list" to listOf("/tmp/1.mp4"),
                "danmaku_file_list" to listOf("/tmp/1.xml"),
                "size_list" to listOf(1024L),
                "size" to 1024L
            )
        )
        assertEquals(listOf("P1"), local.pageList)
        assertEquals(listOf(1024L), local.sizeList)
    }

    @Test
    fun `DownloadSection and PageInfo parse from wire`() {
        val download = DownloadSection.fromWire(
            mapOf(
                "id" to 9L, "type" to "mp4", "aid" to 123456L, "cid" to 101L, "qn" to 64,
                "url_cover" to "https://example.com/cover.jpg", "title" to "下载视频（脱敏）",
                "child" to "P1", "state" to "finished", "download_type" to "video", "audio_url" to "https://example.com/a.m4s"
            )
        )
        assertEquals(64, download.qn)
        assertEquals("finished", download.state)
        assertEquals("video", download.downloadType)
        assertEquals("https://example.com/a.m4s", download.audioUrl)

        val page = PageInfo.fromWire(mapOf("page_num" to 2, "require_ps" to 20, "total" to 100, "return_ps" to 20))
        assertEquals(2, page.page_num)
        assertEquals(100, page.total)
    }

    @Test
    fun `ApiResult defaults and overrides`() {
        val empty = ApiResult.fromWire(emptyMap())
        assertEquals(-1, empty.code)

        val result = ApiResult.fromWire(
            mapOf("code" to 0, "message" to "ok", "offset" to 10L, "timestamp" to 1750000000L, "business" to "测试", "isBottom" to true)
        )
        assertEquals(0, result.code)
        assertEquals("ok", result.message)
        assertTrue(result.isBottom)
        assertFalse(empty.isBottom)
    }
}
