package com.qx.orbit.bili.data.api

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder
import java.security.MessageDigest

/**
 * JVM unit tests for [WbiSigner]'s pure signing/encoding logic.
 *
 * No network is touched: the private [ConfInfoApi.wbiKey] cache is stubbed via
 * reflection so [WbiSigner.signParams] / [WbiSigner.signUrl] never fall through
 * to `BiliApiService.getNav()`.
 */
class WbiSignerTest {

    /** 32-char stub key, same length/shape B站 returns from the nav `wbi_img` endpoints. */
    private val stubKey = "ea1db124af3c7062474693fa704f4ff8"

    // --- Test helpers ---

    private fun stubWbiKey(key: String) {
        val field = ConfInfoApi.javaClass.getDeclaredField("wbiKey")
        field.isAccessible = true
        field.set(ConfInfoApi, key)
    }

    private fun invokeEncodeUriComponent(str: String): String {
        val method = WbiSigner.javaClass.getDeclaredMethod("encodeUriComponent", String::class.java)
        method.isAccessible = true
        return method.invoke(WbiSigner, str) as String
    }

    /** Independent reference encoder (B站 WBI spec), duplicated here on purpose so
     *  a regression in the production encoder is caught instead of mirrored. */
    private fun referenceEncode(str: String): String =
        java.net.URLEncoder.encode(str, "UTF-8")
            .replace("+", "%20")
            .replace("%21", "!")
            .replace("%27", "'")
            .replace("%28", "(")
            .replace("%29", ")")
            .replace("%7E", "~")
            .replace("%2A", "*")

    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // --- encodeUriComponent (pure encoding rules) ---

    @Test
    fun `encodeUriComponent keeps RFC3986 unreserved characters literal`() {
        assertEquals("abcABC012-_.", invokeEncodeUriComponent("abcABC012-_."))
        assertEquals("!", invokeEncodeUriComponent("!"))
        assertEquals("'", invokeEncodeUriComponent("'"))
        assertEquals("(", invokeEncodeUriComponent("("))
        assertEquals(")", invokeEncodeUriComponent(")"))
        assertEquals("~", invokeEncodeUriComponent("~"))
        assertEquals("*", invokeEncodeUriComponent("*"))
    }

    @Test
    fun `encodeUriComponent percent-encodes reserved and non-ASCII characters`() {
        // URLEncoder turns ' ' into '+', which B站 WBI rewrites to %20
        assertEquals("%20", invokeEncodeUriComponent(" "))
        assertEquals("%2B", invokeEncodeUriComponent("+"))
        assertEquals("%2F", invokeEncodeUriComponent("/"))
        assertEquals("%2C", invokeEncodeUriComponent(","))
        assertEquals("%3A", invokeEncodeUriComponent(":"))
        // Multi-byte UTF-8: 中文
        assertEquals("%E4%B8%AD%E6%96%87", invokeEncodeUriComponent("中文"))
        assertEquals("%E4%B8%AD%E6%96%87%20%E5%8C%BA", invokeEncodeUriComponent("中文 区"))
    }

    // --- signParams (stubbed key, no network) ---

    @Test
    fun `signParams appends w_rid and wts and preserves original params`() = runTest {
        stubWbiKey(stubKey)
        val params = mapOf(
            "foo" to "a b",
            "keyword" to "中文",
            "special" to "!x(y)z",
        )

        val signed = WbiSigner.signParams(params)

        assertEquals("a b", signed["foo"])
        assertEquals("中文", signed["keyword"])
        assertEquals("!x(y)z", signed["special"])

        val wrid = signed["w_rid"]
        assertTrue("w_rid must be 32 lowercase hex chars, got: $wrid", wrid != null && wrid.matches(Regex("[0-9a-f]{32}")))
        val wts = signed["wts"]
        assertTrue("wts must be epoch seconds, got: $wts", wts != null && wts.toLongOrNull() != null)
    }

    @Test
    fun `signParams produces deterministic signature against reference encoder`() = runTest {
        stubWbiKey(stubKey)
        val params = mapOf("keyword" to "中文 空间", "page" to "1", "special" to "+-*/")

        val signed = WbiSigner.signParams(params)
        val wts = requireNotNull(signed["wts"])
        val wrid = requireNotNull(signed["w_rid"])

        // Recompute the expected w_rid from the observed wts using the reference encoder.
        val expected = md5(
            (params.map { (k, v) -> "${referenceEncode(k)}=${referenceEncode(v)}" } + "wts=$wts")
                .sorted()
                .joinToString("&") + stubKey
        )
        assertEquals(expected, wrid)
    }

    // --- signUrl (stubbed key, no network) ---

    @Test
    fun `signUrl uses ampersand separator when a query already exists`() = runTest {
        stubWbiKey(stubKey)
        val url = "https://api.bilibili.com/x/web-interface/view?bvid=BV1xx411c7mD&p=1"

        val signed = WbiSigner.signUrl(url)

        assertTrue("signed url must keep the original url", signed.startsWith(url + "&"))
        val wts = signed.substringAfter("&wts=")
        val wrid = signed.substringAfter("&w_rid=").substringBefore("&")
        assertTrue("w_rid must be 32 lowercase hex chars, got: $wrid", wrid.matches(Regex("[0-9a-f]{32}")))
        assertTrue("wts must be digits, got: $wts", wts.isNotEmpty() && wts.all { it.isDigit() })

        // Recompute expected signature from the observed wts.
        val params = url.substringAfter("?").split("&").map { part ->
            val parts = part.split("=", limit = 2)
            if (parts.size == 2) {
                "${referenceEncode(URLDecoder.decode(parts[0], "UTF-8"))}=${referenceEncode(URLDecoder.decode(parts[1], "UTF-8"))}"
            } else {
                part
            }
        }
        val expected = md5((params + "wts=$wts").sorted().joinToString("&") + stubKey)
        assertEquals(expected, wrid)
    }

    @Test
    fun `signUrl uses question mark separator when there is no query`() = runTest {
        stubWbiKey(stubKey)
        val signed = WbiSigner.signUrl("https://api.bilibili.com/x/web-interface/nav")
        assertTrue(signed.contains("?w_rid="))
        assertTrue(signed.contains("&wts="))
    }
}
