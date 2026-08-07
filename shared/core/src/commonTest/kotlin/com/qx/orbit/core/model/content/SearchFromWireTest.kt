package com.qx.orbit.core.model.content

import kotlin.test.Test
import kotlin.test.assertEquals

class SearchFromWireTest {

    @Test
    fun `SearchData parses result groups with raw data maps`() {
        val search = SearchData.fromWire(
            mapOf(
                "seid" to "search-seed-1",
                "result" to listOf(
                    mapOf(
                        "result_type" to "video",
                        "data" to listOf(
                            mapOf("aid" to 123456L, "title" to "搜索视频（脱敏）", "author" to "测试UP主"),
                            mapOf("aid" to 654321L, "title" to "搜索视频二（脱敏）")
                        )
                    ),
                    mapOf("result_type" to "bili_user", "data" to listOf(mapOf("mid" to 8888L, "uname" to "测试用户甲")))
                )
            )
        )

        assertEquals("search-seed-1", search.seid)
        val videoGroup = search.result.first()
        assertEquals("video", videoGroup.result_type)
        assertEquals(2, videoGroup.data.size)
        assertEquals("搜索视频（脱敏）", videoGroup.data[0]["title"])
        assertEquals("测试用户甲", search.result[1].data.single()["uname"])
    }

    @Test
    fun `SearchResultGroup from wire`() {
        val group = SearchResultGroup.fromWire(
            mapOf("result_type" to "media_bangumi", "data" to listOf(mapOf("season_id" to 200L)))
        )
        assertEquals("media_bangumi", group.result_type)
        assertEquals(200L, group.data.single()["season_id"])
    }
}
