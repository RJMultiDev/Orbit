package com.qx.orbit.core.sign

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WBI 签名 golden 测试。固定 clock（wts=1700000000）与 mixinKey（"abcdef0123456789"）。
 *
 * golden 的 w_rid 先由已知 MD5 向量自检实现正确，再对固定输入独立计算：
 * 输入 `baz=1&foo=bar&wts=1700000000` + key `abcdef0123456789`，
 * MD5("baz=1&foo=bar&wts=1700000000abcdef0123456789") = 9f94ebffe341c931171698686ca652b4。
 * 若此用例失败，说明算法或编码与 Android `data/api/WbiSigner.kt` 不一致。
 */
class WbiSignerTest {

    private val clock = Clock { 1700000000L }
    private val key = WbiKeyProvider { "abcdef0123456789" }

    private fun signer(md5: Md5 = defaultMd5()) =
        WbiSigner(SignerConfig(clock, key, md5))

    // ---- 先验证 MD5 实现正确，再推 w_rid ----

    @Test
    fun defaultMd5_matchesKnownVectors() {
        val md5 = defaultMd5()
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", md5.hex(""))
        assertEquals("900150983cd24fb0d6963f7d28e17f72", md5.hex("abc"))
    }

    @Test
    fun signParams_golden() {
        val out = signer().signParams(mapOf("foo" to "bar", "baz" to "1"))
        assertEquals("1700000000", out["wts"])
        assertEquals("9f94ebffe341c931171698686ca652b4", out["w_rid"])
        assertEquals(mapOf("foo" to "bar", "baz" to "1"), out - "w_rid" - "wts")
    }

    @Test
    fun signParams_encodingAndSort_golden() {
        // "a b" → "a%20b"（+→%20 语义），编码后整条排序：a%20b=x%20y < wts=...
        val out = signer().signParams(mapOf("a b" to "x y"))
        assertEquals("1700000000", out["wts"])
        assertEquals("e8ddfb61965f7d3a55c713c3203b34c6", out["w_rid"])
    }

    @Test
    fun signParams_outputOverridesPreexistingWts() {
        // Android 行为：旧 wts 仍参与签名（编码后排序），但输出表覆盖为新 wts
        val out = signer().signParams(mapOf("wts" to "999"))
        assertEquals("1700000000", out["wts"])
        // 编码后整条排序：wts=1700000000 < wts=999 → "wts=1700000000&wts=999" + key
        assertEquals("77d806fcc56043b3d4c6e950ccc245b8", out["w_rid"])
    }

    @Test
    fun signParams_feedsCanonicalStringWithKeyIntoMd5() {
        var canonical: String? = null
        val fake = Md5 { input ->
            canonical = input
            "deadbeef"
        }
        val out = signer(fake).signParams(mapOf("foo" to "bar", "baz" to "1"))
        // 编码 → 追加 wts → 整条排序 → 直接拼接 mixinKey（无分隔符）后交给 MD5
        assertEquals("baz=1&foo=bar&wts=1700000000abcdef0123456789", canonical)
        assertEquals("deadbeef", out["w_rid"])
    }

    @Test
    fun signUrl_golden_preencodedUtf8() {
        val out = signer().signUrl("https://api.bilibili.com/x/web-interface/search?keyword=%E4%BD%A0%E5%A5%BD")
        // query 解码为 "你好" 再重新编码，结果与原编码一致
        assertEquals(
            "https://api.bilibili.com/x/web-interface/search?keyword=%E4%BD%A0%E5%A5%BD" +
                "&w_rid=83a8e9aad78238fcdd90e8f3364ffe88&wts=1700000000",
            out,
        )
    }

    @Test
    fun signUrl_plusInQueryDecodesToSpace() {
        val out = signer().signUrl("https://x/y?a=b+c")
        // a=b+c → 解码 "a=b c" → 重编码参与签名计算；输出 URL 保留原始 query
        assertEquals(
            "https://x/y?a=b+c&w_rid=62a7ed28bdcce161bdd3031b13a84ec8&wts=1700000000",
            out,
        )
    }

    @Test
    fun signUrl_noQuery_usesQuestionSeparator() {
        val out = signer().signUrl("https://x/y")
        assertEquals(
            "https://x/y?w_rid=6ce02f0444f9718b0b2c983438a6306b&wts=1700000000",
            out,
        )
    }

    @Test
    fun signUrl_withQuery_usesAmpSeparator() {
        val out = signer().signUrl("https://x/y?fid=2")
        assertEquals(
            "https://x/y?fid=2&w_rid=33aa848c986aec5bb73c2c2083a869df&wts=1700000000",
            out,
        )
    }
}
