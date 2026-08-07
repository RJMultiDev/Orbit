package com.qx.orbit.core.model.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayerFromWireTest {

    @Test
    fun `PlayerData maps player playurl wire fields and dash`() {
        val player = PlayerData.fromWire(
            mapOf(
                "bvid" to "BV1TEST456",
                "aid" to 123456L,
                "cid" to 101L,
                "mid" to 9999L,
                "title" to "示例播放（脱敏）",
                "pic" to "https://example.com/cover.jpg",
                "quality" to 64,
                "accept_description" to listOf("高清 1080P", "高清 720P", "清晰 480P"),
                "accept_quality" to listOf(80, 64, 32),
                "durl" to listOf(
                    mapOf(
                        "order" to 1,
                        "url" to "https://example.com/video.flv",
                        "backup_url" to listOf("https://example.com/video-bak.flv")
                    )
                ),
                "pages" to listOf(
                    mapOf("part" to "P1", "cid" to 101L),
                    mapOf("part" to "P2", "cid" to 102L)
                ),
                "dash" to mapOf(
                    "duration" to 93,
                    "minBufferTime" to 1.5,
                    "video" to listOf(
                        mapOf(
                            "id" to 64,
                            "baseUrl" to "https://example.com/v64.m4s",
                            "backupUrl" to listOf("https://example.com/v64-bak.m4s"),
                            "bandwidth" to 100000,
                            "mimeType" to "video/mp4",
                            "codecs" to "avc1.640028",
                            "width" to 1280,
                            "height" to 720,
                            "frameRate" to "30000/1001",
                            "codecid" to 7
                        ),
                        mapOf("id" to 80, "baseUrl" to "https://example.com/v80.m4s", "bandwidth" to 200000, "codecid" to 7)
                    ),
                    "audio" to listOf(
                        mapOf("id" to 30216, "baseUrl" to "https://example.com/a.m4s", "bandwidth" to 50000, "mimeType" to "audio/mp4", "codecs" to "mp4a.40.2")
                    )
                )
            )
        )

        assertEquals("BV1TEST456", player.bvid)
        assertEquals(123456L, player.aid)
        assertEquals(101L, player.cid)
        assertEquals("示例播放（脱敏）", player.title)
        assertEquals("https://example.com/cover.jpg", player.cover)
        assertEquals(64, player.qn)
        assertEquals(listOf("高清 1080P", "高清 720P", "清晰 480P"), player.qnStrList)
        assertEquals(listOf(80, 64, 32), player.qnValueList)
        assertEquals("https://example.com/video.flv", player.videoUrl)
        assertEquals(listOf("P1", "P2"), player.pagenames)
        assertEquals(listOf(101L, 102L), player.cids)

        val dash = assertNotNull(player.dashData)
        assertEquals(93, dash.duration)
        assertEquals(1.5, dash.minBufferTime)
        assertEquals(2, dash.videoStreams.size)
        val v64 = dash.videoStreams.first { it.id == 64 }
        assertEquals("avc1.640028", v64.codecs)
        assertEquals(listOf("https://example.com/v64-bak.m4s"), v64.backupUrl)
        assertEquals(7, v64.codecid)
        assertEquals("30000/1001", v64.frameRate)
        assertEquals(1280, v64.width)
        val audio = assertNotNull(dash.audioStreams.firstOrNull())
        assertEquals(30216, audio.id)
        assertEquals("https://example.com/a.m4s", audio.baseUrl)
        assertEquals(50000L, audio.bandwidth)

        // 选流辅助逻辑与 Android DashData 一致
        assertEquals(64, dash.getVideoStream(64)?.id)
        assertEquals(80, dash.getVideoStream(80)?.id)
        assertEquals(80, dash.getVideoStream(100)?.id) // 取不超过 qn 的最高
        assertEquals(30216, dash.getAudioStream(30216)?.id)
        assertEquals(30216, dash.getBestAudioStream()?.id)
    }

    @Test
    fun `PlayerData falls back to defaults when keys absent`() {
        val player = PlayerData.fromWire(mapOf("bvid" to "BV1TEST456", "cid" to 101L))
        assertEquals(-1, player.qn)
        assertEquals(-1, player.audioQn)
        assertEquals(PlayerData.TYPE_VIDEO, player.type)
        assertEquals("", player.videoUrl)
        assertEquals("", player.audioUrl)
        assertEquals(0L, player.epid)
        assertTrue(player.cids.isEmpty())
        assertNull(player.qnStrList)
        assertNull(player.qnValueList)
    }

    @Test
    fun `DashData picks dolby and flac audio`() {
        val dash = DashData.fromWire(
            mapOf(
                "dolby" to mapOf("audio" to listOf(mapOf("id" to 30250, "baseUrl" to "https://example.com/dolby.m4s"))),
                "flac" to mapOf("audio" to listOf(mapOf("id" to 30251, "baseUrl" to "https://example.com/flac.m4s")))
            )
        )
        assertEquals(30251, dash.getBestAudioStream()?.id) // FLAC 优先
        assertEquals(30250, dash.getAudioStream(30250)?.id) // 杜比按 qn 直达
        assertEquals(30251, dash.getAudioStream(30251)?.id) // FLAC 按 qn 直达
    }

    @Test
    fun `Dash streams accept snake_case aliases`() {
        val stream = DashVideoStream.fromWire(
            mapOf("id" to 64, "base_url" to "https://example.com/v64.m4s", "frame_rate" to "30")
        )
        assertEquals("https://example.com/v64.m4s", stream.baseUrl)
        assertEquals("30", stream.frameRate)

        val audio = DashAudioStream.fromWire(mapOf("id" to 30216, "base_url" to "https://example.com/a.m4s"))
        assertEquals(30216, audio.id)
        assertEquals("https://example.com/a.m4s", audio.baseUrl)
    }
}
