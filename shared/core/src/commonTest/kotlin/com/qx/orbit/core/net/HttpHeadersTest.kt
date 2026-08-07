package com.qx.orbit.core.net

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HttpHeadersTest {

    @Test
    fun defaultHeaders_containsUaAndReferer() {
        val headers = HttpHeaders.defaultHeaders()
        assertEquals(HttpHeaders.USER_AGENT, headers["User-Agent"])
        assertEquals(HttpHeaders.REFERER, headers["Referer"])
        assertTrue(headers["User-Agent"]!!.startsWith("Mozilla/5.0"))
        assertTrue(headers["Referer"]!!.startsWith("https://"))
    }

    @Test
    fun filterSensitiveCookies_removesWafFields() {
        val raw = "SESSDATA=abc; bili_jct=xyz; refresh_token=secret; access_token=at; expires_in=15552000; mid=12345; DedeUserID=67890"
        val filtered = HttpHeaders.filterSensitiveCookies(raw)
        assertEquals("SESSDATA=abc; bili_jct=xyz; DedeUserID=67890", filtered)
    }

    @Test
    fun filterSensitiveCookies_emptyInput() {
        assertEquals("", HttpHeaders.filterSensitiveCookies(""))
    }

    @Test
    fun mergeCookies_updatesExistingAndAppendsNew() {
        val existing = "SESSDATA=old; bili_jct=abc"
        val setCookies = listOf(
            "SESSDATA=new; Domain=.bilibili.com; Path=/",
            "buvid3=z; Path=/",
            "b_nut=1; Domain=.evil.com; Path=/", // 非允许域，跳过
        )
        assertEquals("SESSDATA=new; bili_jct=abc; buvid3=z", HttpHeaders.mergeCookies(existing, setCookies))
    }

    @Test
    fun mergeCookies_skipsAllCookiesWithDisallowedDomain() {
        assertEquals("", HttpHeaders.mergeCookies("", listOf("evil=1; Domain=tracker.example")))
    }

    @Test
    fun mergeCookies_acceptsDomainlessSetCookie() {
        assertEquals("fresh=1", HttpHeaders.mergeCookies("", listOf("fresh=1; Path=/")))
    }

    @Test
    fun mergeCookies_ignoresInvalidSetCookie() {
        assertEquals("a=1", HttpHeaders.mergeCookies("a=1", listOf("=orphan", "nodisplay")))
    }
}
