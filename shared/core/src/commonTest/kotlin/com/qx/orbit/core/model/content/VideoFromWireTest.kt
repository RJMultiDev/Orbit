package com.qx.orbit.core.model.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VideoFromWireTest {

    @Test
    fun `VideoInfo maps bilibili wire keys`() {
        val video = VideoInfo.fromWire(
            mapOf(
                "bvid" to "BV1TEST123",
                "aid" to 123456L,
                "title" to "示例视频标题（脱敏）",
                "pic" to "https://example.com/cover.jpg",
                "desc" to "这是脱敏后的视频简介",
                "duration" to 93,
                "pubdate" to 0,
                "copyright" to 1,
                "stat" to mapOf("view" to 1000, "like" to 200, "coin" to 30, "reply" to 12, "danmaku" to 5, "favorite" to 40),
                "req_user" to mapOf("like" to 1, "coin" to 2, "favorite" to 1, "dislike" to 0, "attention" to 1),
                "pages" to listOf(
                    mapOf("part" to "P1", "cid" to 101L),
                    mapOf("part" to "P2", "cid" to 102L)
                ),
                "rights" to mapOf("is_cooperation" to 1, "is_stein_gate" to 0, "is_360" to 1),
                "is_upower_exclusive" to true,
                "argue_info" to mapOf("argue_msg" to "争议内容提醒（脱敏）"),
                "redirect_url" to "https://www.bilibili.com/bangumi/play/ep328960",
                "staff" to listOf(
                    mapOf(
                        "mid" to 9999L, "name" to "测试UP主", "face" to "https://example.com/face.jpg", "follower" to 88,
                        "official_verify" to mapOf("type" to 0)
                    )
                )
            )
        )

        assertEquals("BV1TEST123", video.bvid)
        assertEquals(123456L, video.aid)
        assertEquals("示例视频标题（脱敏）", video.title)
        assertEquals("https://example.com/cover.jpg", video.cover)
        assertEquals("这是脱敏后的视频简介", video.description)
        assertEquals("01:33", video.duration)
        assertEquals(listOf("P1", "P2"), video.pagenames)
        assertEquals(listOf(101L, 102L), video.cids)
        assertTrue(video.isCooperation)
        assertTrue(video.is360)
        assertEquals("争议内容提醒（脱敏）", video.argueMsg)
        assertEquals(328960L, video.epid)
        assertEquals("测试UP主", video.staff.single().name)
        assertEquals(1, video.staff.single().official) // official_verify.type==0 -> 1

        val stats = assertNotNull(video.stats)
        assertEquals(1000, stats.view)
        assertTrue(stats.liked)
        assertEquals(2, stats.coined)
        assertTrue(stats.followed)
        assertEquals(2, stats.coin_limit) // copyright==1 (self) -> 2
    }

    @Test
    fun `VideoInfo accepts snake_case alias keys and empty defaults`() {
        val video = VideoInfo.fromWire(
            mapOf(
                "upowerExclusive" to true,
                "stats" to mapOf("view" to 7),
                "time_desc" to "3天前",
                "desc_ats" to listOf(mapOf("id" to 1L, "start" to 0, "end" to 5, "name" to "测试用户")),
                "history" to mapOf("cid" to 55L, "progress" to 60)
            )
        )
        assertTrue(video.upowerExclusive)
        assertEquals(7, video.stats?.view)
        assertEquals("3天前", video.timeDesc)
        assertEquals("测试用户", video.descAts.single().name)
        assertEquals(55L, video.history?.cid)
        assertEquals(-1L, video.epid)
    }

    @Test
    fun `VideoInfo builds collection from ugc_season`() {
        val video = VideoInfo.fromWire(
            mapOf(
                "ugc_season" to mapOf(
                    "id" to 7,
                    "title" to "测试合集（脱敏）",
                    "intro" to "合集简介",
                    "cover" to "https://example.com/season.jpg",
                    "mid" to 9999L,
                    "sections" to listOf(
                        mapOf(
                            "season_id" to 7,
                            "id" to 11,
                            "title" to "第1部分",
                            "episodes" to listOf(
                                mapOf(
                                    "season_id" to 7, "section_id" to 11, "id" to 1, "aid" to 123456L,
                                    "cid" to 101L, "title" to "EP1（脱敏）", "bvid" to "BV1TEST123",
                                    "arc" to mapOf("aid" to 123456L, "title" to "EP1（脱敏）")
                                )
                            )
                        )
                    )
                )
            )
        )

        val collection = assertNotNull(video.collection)
        assertEquals("测试合集（脱敏）", collection.title)
        val ep = collection.sections.single().episodes.single()
        assertEquals("EP1（脱敏）", ep.title)
        assertEquals(123456L, ep.arc?.aid)
    }

    @Test
    fun `VideoCard reads author pic and formats play count`() {
        val card = VideoCard.fromWire(
            mapOf(
                "title" to "搜索示例视频（脱敏）",
                "author" to "测试UP主",
                "pic" to "https://example.com/thumb.jpg",
                "play" to 12345,
                "aid" to 123456L,
                "bvid" to "BV1TEST123",
                "cid" to 101L
            )
        )
        assertEquals("搜索示例视频（脱敏）", card.title)
        assertEquals("测试UP主", card.upName)
        assertEquals("1.2万", card.view)
        assertEquals("video", card.type)
        assertEquals("https://example.com/thumb.jpg", card.cover)
        assertEquals(123456L, card.aid)
        assertEquals("BV1TEST123", card.bvid)
        assertEquals(101L, card.cid)
    }

    @Test
    fun `Stats parses booleans from wire ints and Strings`() {
        val stats = Stats.fromWire(
            mapOf(
                "view" to "1234",
                "liked" to 1,
                "followed" to "false",
                "coin_limit" to 1L
            )
        )
        assertEquals(1234, stats.view)
        assertTrue(stats.liked)
        assertTrue(!stats.followed)
        assertEquals(1, stats.coin_limit)
    }

    @Test
    fun `At and History from wire`() {
        val at = At.fromWire(mapOf("id" to 9L, "start" to 1, "end" to 3, "name" to "测试用户乙"))
        assertEquals(9L, at.id)
        assertEquals(3, at.end)

        val history = History.fromWire(mapOf("cid" to 77L, "progress" to 30))
        assertEquals(77L, history.cid)
        assertEquals(30, history.progress)
    }
}
