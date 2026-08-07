package com.qx.orbit.core.model.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BangumiFromWireTest {

    @Test
    fun `Bangumi parses info sections and nested objects`() {
        val bangumi = Bangumi.fromWire(
            mapOf(
                "info" to mapOf(
                    "media_id" to 100L,
                    "season_id" to 200L,
                    "type" to 1,
                    "count" to 12,
                    "score" to 9.5,
                    "title" to "测试番剧（脱敏）",
                    "cover" to "https://example.com/cover.jpg",
                    "horizontal_picture" to "https://example.com/h.jpg",
                    "area_name" to "日本",
                    "index_show" to "全12话",
                    "evaluate" to "剧情简介（脱敏）",
                    "styles" to listOf("搞笑", "日常"),
                    "stat" to mapOf("favorites" to 1000, "views" to 99999),
                    "up_info" to mapOf("mid" to 8888L, "name" to "测试制作组"),
                    "series" to mapOf("series_id" to 1L, "series_title" to "系列一"),
                    "seasons" to listOf(mapOf("season_id" to 200L, "season_title" to "第一季")),
                    "publish" to mapOf("is_finish" to 1, "pub_time_show" to "2026年7月"),
                    "user_status" to mapOf("follow" to 1, "progress" to mapOf("last_ep_id" to 5L, "last_ep_index" to "5"))
                ),
                "sections" to listOf(
                    mapOf(
                        "id" to 1L,
                        "title" to "正片",
                        "episodes" to listOf(
                            mapOf("id" to 1L, "aid" to 1001L, "cid" to 2001L, "title" to "第一话（脱敏）", "long_title" to "第一话 测试")
                        )
                    )
                )
            )
        )

        val info = assertNotNull(bangumi.info)
        assertEquals("测试番剧（脱敏）", info.title)
        assertEquals(12, info.count)
        assertEquals(9.5f, info.score)
        assertEquals("https://example.com/h.jpg", info.cover_horizontal)
        assertEquals(listOf("搞笑", "日常"), info.styles)
        assertEquals(1000, info.stat?.favorites)
        assertEquals("测试制作组", info.up_info?.name)
        assertEquals(1L, info.series?.series_id)
        assertEquals("第一季", info.seasons.single().season_title)
        assertEquals(1, info.publish?.is_finish)
        assertEquals(5L, info.user_status?.progress?.last_ep_id)
        assertEquals("第一话（脱敏）", bangumi.sectionList.single().episodes.single().title)
    }

    @Test
    fun `Timeline parses day_list episodes`() {
        val timeline = Timeline.fromWire(
            mapOf(
                "day_list" to listOf(
                    mapOf(
                        "date" to "2026-08-07",
                        "date_ts" to 1750000000L,
                        "day_of_week" to 5,
                        "is_today" to 1,
                        "episodes" to listOf(
                            mapOf("episode_id" to 11L, "pub_index" to "第1话", "title" to "测试番剧（脱敏）", "season_id" to 200L)
                        )
                    )
                )
            )
        )

        val day = timeline.dayList.single()
        assertEquals("2026-08-07", day.date)
        assertEquals(1, day.is_today)
        assertEquals("第1话", day.episodes.single().pub_index)
        assertEquals(200L, day.episodes.single().season_id)
    }
}
