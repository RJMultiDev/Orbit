package com.qx.orbit.core.model.social

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FavoriteFromWireTest {

    @Test
    fun `FavoriteFolder maps wire fields`() {
        val folder = FavoriteFolder.fromWire(
            mapOf(
                "fav_box" to 10L,
                "media_id" to 20L,
                "title" to "默认收藏夹（脱敏）",
                "cover" to "https://example.com/folder.jpg",
                "count" to 5,
                "max_count" to 1000,
                "is_default" to true
            )
        )

        assertEquals(10L, folder.id)
        assertEquals(20L, folder.mediaId)
        assertEquals("默认收藏夹（脱敏）", folder.name)
        assertEquals("https://example.com/folder.jpg", folder.cover)
        assertEquals(5, folder.videoCount)
        assertEquals(1000, folder.maxCount)
        assertTrue(folder.isDefault)
    }

    @Test
    fun `FavoriteFolder accepts id and name aliases`() {
        val folder = FavoriteFolder.fromWire(
            mapOf(
                "id" to 30L,
                "name" to "收藏夹乙（脱敏）",
                "video_count" to 2,
                "isDefault" to true
            )
        )
        // 与 Android `@SerializedName` 行为一致：仅 "id" 时 id 与 mediaId 都读它
        assertEquals(30L, folder.id)
        assertEquals(30L, folder.mediaId)
        assertEquals("收藏夹乙（脱敏）", folder.name)
        assertEquals(2, folder.videoCount)
        assertTrue(folder.isDefault)
    }
}
