package com.qx.orbit.bili.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * JVM unit tests for the pure formatting helpers in FormatUtil.kt.
 */
class FormatUtilTest {

    @Test
    fun `formatCount below ten thousand returns the plain number`() {
        assertEquals("0", formatCount(0))
        assertEquals("999", formatCount(999))
        assertEquals("9999", formatCount(9999))
    }

    @Test
    fun `formatCount at or above ten thousand uses one decimal and the wan suffix`() {
        // Expected values are computed with String.format under the default locale,
        // mirroring the implementation so the tests are locale-independent.
        assertEquals(String.format("%.1f万", 10000 / 10000f), formatCount(10000))
        assertEquals(String.format("%.1f万", 12345 / 10000f), formatCount(12345))
        assertEquals(String.format("%.1f万", 99999 / 10000f), formatCount(99999))
    }

    @Test
    fun `fixCoverUrl prepends https to protocol-relative urls`() {
        assertEquals(
            "https://i0.hdslb.com/bfs/archive/a.jpg",
            "//i0.hdslb.com/bfs/archive/a.jpg".fixCoverUrl(),
        )
    }

    @Test
    fun `fixCoverUrl upgrades http to https`() {
        assertEquals(
            "https://i0.hdslb.com/bfs/archive/a.jpg",
            "http://i0.hdslb.com/bfs/archive/a.jpg".fixCoverUrl(),
        )
    }

    @Test
    fun `fixCoverUrl leaves https urls unchanged`() {
        assertEquals(
            "https://i0.hdslb.com/bfs/archive/a.jpg",
            "https://i0.hdslb.com/bfs/archive/a.jpg".fixCoverUrl(),
        )
    }

    @Test
    fun `fixCoverUrl converts avif covers to webp`() {
        assertEquals(
            "https://i0.hdslb.com/bfs/archive/a.webp",
            "https://i0.hdslb.com/bfs/archive/a.avif".fixCoverUrl(),
        )
    }
}
