package com.qx.orbit.core.model.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CollectionFromWireTest {

    @Test
    fun `Collection parses sections episodes and arc`() {
        val collection = Collection.fromWire(
            mapOf(
                "id" to 7,
                "title" to "测试合集（脱敏）",
                "intro" to "合集简介（脱敏）",
                "mid" to 9999L,
                "view" to "1.2万",
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
                ),
                "cards" to listOf(mapOf("title" to "卡片（脱敏）", "aid" to 123456L))
            )
        )

        assertEquals(7, collection.id)
        assertEquals("测试合集（脱敏）", collection.title)
        assertEquals(11, collection.sections.single().id)
        val ep = collection.sections.single().episodes.single()
        assertEquals("EP1（脱敏）", ep.title)
        assertEquals(123456L, ep.arc?.aid)
        assertEquals("卡片（脱敏）", collection.cards.single().title)
    }

    @Test
    fun `Series defaults type to series`() {
        val series = Series.fromWire(
            mapOf("id" to 3, "title" to "测试追番（脱敏）", "total" to "12话")
        )
        assertEquals("series", series.type)
        assertEquals(3, series.id)
        assertEquals("12话", series.total)
    }
}
