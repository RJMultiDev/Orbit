package com.qx.orbit.core.net

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UrlResolverTest {

    @Test
    fun isRedirectStatus_matchesOkHttp() {
        for (code in listOf(300, 301, 302, 303, 307, 308)) {
            assertTrue(UrlResolver.isRedirectStatus(code), "status $code should be redirect")
        }
        for (code in listOf(200, 201, 304, 400, 404, 500)) {
            assertFalse(UrlResolver.isRedirectStatus(code), "status $code should not be redirect")
        }
    }

    @Test
    fun isSslRedirect_httpToHttpsSameHost() {
        assertTrue(UrlResolver.isSslRedirect(false, "www.bilibili.com", "https://www.bilibili.com/"))
        assertTrue(UrlResolver.isSslRedirect(false, "b23.tv", "https://b23.tv/abc"))
    }

    @Test
    fun isSslRedirect_negativeCases() {
        assertFalse(UrlResolver.isSslRedirect(true, "www.bilibili.com", "https://www.bilibili.com/")) // 请求已 https
        assertFalse(UrlResolver.isSslRedirect(false, "a.com", "https://b.com/")) // 主机不同
        assertFalse(UrlResolver.isSslRedirect(false, "a.com", "http://a.com/")) // 非 https
        assertFalse(UrlResolver.isSslRedirect(false, "a.com", "/relative")) // 相对地址无 scheme
    }

    @Test
    fun resolveRedirect_b23BlockedUnlessSslUpgrade() {
        // b23.tv 非 https 升级：不跟随
        assertNull(UrlResolver.resolveRedirect("http://b23.tv/abc", false, 302, "http://b23.tv/xyz"))
        // b23.tv http→https 同主机升级：跟随
        assertEquals(
            "https://b23.tv/xyz",
            UrlResolver.resolveRedirect("http://b23.tv/abc", false, 302, "https://b23.tv/xyz")
        )
    }

    @Test
    fun resolveRedirect_followsNormalHost() {
        assertEquals(
            "https://api.bilibili.com/v2",
            UrlResolver.resolveRedirect("https://api.bilibili.com/v1", true, 301, "https://api.bilibili.com/v2")
        )
    }

    @Test
    fun resolveRedirect_rejectsNonRedirectAndEmpty() {
        assertNull(UrlResolver.resolveRedirect("https://a.com/", true, 200, "https://b.com/"))
        assertNull(UrlResolver.resolveRedirect("https://a.com/", true, 302, null))
        assertNull(UrlResolver.resolveRedirect("https://a.com/", true, 302, ""))
    }

    @Test
    fun resolveRedirect_enforcesHopLimit() {
        assertNull(
            UrlResolver.resolveRedirect("https://a.com/1", true, 302, "https://a.com/2", hopsTaken = 10, maxHops = 10)
        )
        assertEquals(
            "https://a.com/2",
            UrlResolver.resolveRedirect("https://a.com/1", true, 302, "https://a.com/2", hopsTaken = 9, maxHops = 10)
        )
    }

    @Test
    fun resolveUrl_absoluteLocation() {
        assertEquals("https://x/y", UrlResolver.resolveUrl("https://a/b", "https://x/y"))
    }

    @Test
    fun resolveUrl_schemeRelative() {
        assertEquals("https://cdn.bilibili.com/x", UrlResolver.resolveUrl("https://a/b", "//cdn.bilibili.com/x"))
    }

    @Test
    fun resolveUrl_rootedPath() {
        assertEquals("https://a.com/new", UrlResolver.resolveUrl("https://a.com/old", "/new"))
    }

    @Test
    fun resolveUrl_relativePathMerge() {
        assertEquals("https://a.com/dir/target", UrlResolver.resolveUrl("https://a.com/dir/page", "target"))
        assertEquals("https://a.com/dir/sub/target", UrlResolver.resolveUrl("https://a.com/dir/page", "sub/target"))
        assertEquals("https://a.com/target", UrlResolver.resolveUrl("https://a.com/dir/page", "../target"))
        assertEquals(
            "https://a.com/dir/target",
            UrlResolver.resolveUrl("https://a.com/dir/page?x=1#frag", "./target")
        )
    }

    @Test
    fun resolveUrl_queryAndFragmentRefs() {
        assertEquals("https://a.com/path?b=2", UrlResolver.resolveUrl("https://a.com/path?a=1", "?b=2"))
        assertEquals("https://a.com/path?a=1#frag", UrlResolver.resolveUrl("https://a.com/path?a=1", "#frag"))
    }
}
