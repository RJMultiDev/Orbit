package com.qx.orbit.core.model.social

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EmoteFromWireTest {

    @Test
    fun `EmotePackage maps package with emotes`() {
        val pkg = EmotePackage.fromWire(
            mapOf(
                "id" to 3,
                "text" to "测试表情包（脱敏）",
                "url" to "https://example.com/pkg.jpg",
                "type" to 2,
                "attr" to 0,
                "size" to 4,
                "item_id" to 1,
                "emotes" to listOf(
                    mapOf(
                        "id" to 7,
                        "package_id" to 3,
                        "name" to "doge",
                        "alias" to "狗头",
                        "url" to "https://example.com/doge.png",
                        "size" to 1
                    )
                ),
                "permanent" to true
            )
        )

        assertEquals(3, pkg.id)
        assertEquals("测试表情包（脱敏）", pkg.text)
        assertEquals(2, pkg.type)
        assertEquals(4, pkg.size)
        assertTrue(pkg.permanent)

        val emote = pkg.emotes.single()
        assertEquals(7, emote.id)
        assertEquals(3, emote.packageId)
        assertEquals("doge", emote.name)
        assertEquals("狗头", emote.alias)
        assertEquals("https://example.com/doge.png", emote.url)
    }
}
