package com.qx.orbit.core.net

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CookieTest {

    // ---- Set-Cookie 解析 ----

    @Test
    fun parseSetCookie_basic() {
        val c = Cookie.parseSetCookie("SESSDATA=abc123")!!
        assertEquals("SESSDATA", c.name)
        assertEquals("abc123", c.value)
        assertTrue(c.hostOnly)
        assertEquals("", c.domain)
        assertEquals("/", c.path)
        assertFalse(c.secure)
        assertFalse(c.httpOnly)
    }

    @Test
    fun parseSetCookie_attributes() {
        val c = Cookie.parseSetCookie(
            "bili_jct=xyz; Domain=.bilibili.com; Path=/; Secure; HttpOnly; SameSite=Lax"
        )!!
        assertEquals("bilibili.com", c.domain) // 去前导点 + 小写
        assertFalse(c.hostOnly)
        assertEquals("/", c.path)
        assertTrue(c.secure)
        assertTrue(c.httpOnly)
    }

    @Test
    fun parseSetCookie_attributeNamesAreCaseInsensitive() {
        val c = Cookie.parseSetCookie("k=v; DOMAIN=SUB.BILIBILI.COM; PATH=/sub; SECURE; HTTPONLY")!!
        assertEquals("sub.bilibili.com", c.domain)
        assertEquals("/sub", c.path)
        assertTrue(c.secure)
        assertTrue(c.httpOnly)
    }

    @Test
    fun parseSetCookie_valueMayContainEquals() {
        val c = Cookie.parseSetCookie("token=a=b=c")!!
        assertEquals("token", c.name)
        assertEquals("a=b=c", c.value)
    }

    @Test
    fun parseSetCookie_invalidReturnsNull() {
        assertNull(Cookie.parseSetCookie(""))
        assertNull(Cookie.parseSetCookie("=value"))
        assertNull(Cookie.parseSetCookie("name")) // 无 =
    }

    @Test
    fun parseSetCookie_emptyValueAllowed() {
        val c = Cookie.parseSetCookie("flag=; Path=/")!!
        assertEquals("flag", c.name)
        assertEquals("", c.value)
    }

    // ---- Cookie 请求头解析/序列化 ----

    @Test
    fun parseRequestHeader_handlesWhitespaceAndEqualsInValue() {
        val pairs = Cookie.parseRequestHeader("a=1; b=2=3 ;  c=4")
        assertEquals(listOf("a" to "1", "b" to "2=3", "c" to "4"), pairs)
    }

    @Test
    fun parseRequestHeader_skipsJunkParts() {
        val pairs = Cookie.parseRequestHeader("a=1;;junk;=orphan")
        assertEquals(listOf("a" to "1"), pairs)
    }

    @Test
    fun serializeRequestHeader() {
        val cookies = listOf(Cookie("a", "1"), Cookie("b", "2"))
        assertEquals("a=1; b=2", Cookie.toRequestHeader(cookies))
        assertEquals("a=1", Cookie("a", "1").toRequestHeader())
    }

    @Test
    fun bindHost_bindsHostOnlyCookie() {
        val hostOnly = Cookie("k", "v")
        val bound = Cookie.bindHost(hostOnly, "m.bilibili.com")
        assertEquals("m.bilibili.com", bound.domain)
        assertTrue(bound.hostOnly)
        // 非 host-only 不受 bindHost 影响
        val domainCookie = Cookie.parseSetCookie("k=v; Domain=.bilibili.com")!!
        assertEquals(domainCookie, Cookie.bindHost(domainCookie, "m.bilibili.com"))
    }

    // ---- domain-match ----

    @Test
    fun domainMatches_boundary() {
        assertTrue(Cookie.domainMatches("bilibili.com", "bilibili.com"))
        assertTrue(Cookie.domainMatches("bilibili.com", "www.bilibili.com"))
        assertTrue(Cookie.domainMatches("bilibili.com", "api.vc.bilibili.com"))
        assertFalse(Cookie.domainMatches("bilibili.com", "abcbilibili.com"))
        assertFalse(Cookie.domainMatches("bilibili.com", "bilibili.com.evil.com"))
        assertTrue(Cookie.domainMatches("BILIBILI.COM", "www.bilibili.com")) // 大小写不敏感
    }

    // ---- path-match ----

    @Test
    fun pathMatches_boundary() {
        assertTrue(Cookie.pathMatches("/", "/"))
        assertTrue(Cookie.pathMatches("/", "/anything/deep"))
        assertTrue(Cookie.pathMatches("/sub", "/sub"))
        assertTrue(Cookie.pathMatches("/sub", "/sub/page"))
        assertTrue(Cookie.pathMatches("/sub/", "/sub/x")) // 以 / 结尾则前缀匹配
        assertFalse(Cookie.pathMatches("/sub", "/subsidiary"))
        assertFalse(Cookie.pathMatches("/sub", "/xsub"))
    }

    // ---- 综合发送判定 ----

    @Test
    fun canSendTo_secureOnlyOnHttps() {
        // Secure cookie 先绑定来源 host（RFC 6265 存储步骤），再验证传输层约束
        val secure = Cookie.bindHost(Cookie.parseSetCookie("k=v; Secure")!!, "bilibili.com")
        assertTrue(Cookie.canSendTo(secure, "bilibili.com", "/", true))
        assertFalse(Cookie.canSendTo(secure, "bilibili.com", "/", false))
    }

    @Test
    fun canSendTo_hostOnlyMatchesExactBoundHost() {
        val unbound = Cookie("k", "v")
        assertFalse(Cookie.canSendTo(unbound, "m.bilibili.com", "/", true)) // 未绑定无法匹配

        val bound = Cookie.bindHost(unbound, "m.bilibili.com")
        assertTrue(Cookie.canSendTo(bound, "m.bilibili.com", "/x", true))
        assertFalse(Cookie.canSendTo(bound, "www.bilibili.com", "/x", true))
    }

    @Test
    fun canSendTo_domainCookieMatchesSubdomainAndPath() {
        val c = Cookie.parseSetCookie("k=v; Domain=.bilibili.com; Path=/api")!!
        assertTrue(Cookie.canSendTo(c, "api.bilibili.com", "/api/x", true))
        assertFalse(Cookie.canSendTo(c, "api.bilibili.com", "/other", true))
        assertFalse(Cookie.canSendTo(c, "example.com", "/api/x", true))
    }

    @Test
    fun isDomainAllowed() {
        assertTrue(Cookie.isDomainAllowed("bilibili.com", "bilibili.com"))
        assertTrue(Cookie.isDomainAllowed("passport.bilibili.com", "bilibili.com"))
        assertFalse(Cookie.isDomainAllowed("evil.com", "bilibili.com"))
        assertFalse(Cookie.isDomainAllowed("notbilibili.com", "bilibili.com"))
    }
}
