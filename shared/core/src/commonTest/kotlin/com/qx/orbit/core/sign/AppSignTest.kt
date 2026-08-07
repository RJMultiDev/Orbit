package com.qx.orbit.core.sign

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * AppSign golden 测试。固定 clock（ts=1700000000）、appKey/appSecret
 * （对齐 Android `AppSignUtil.APP_KEY_HD` / `APP_SEC_HD`）。
 *
 * golden 的 sign 由已知 MD5 向量自检实现正确后，对固定输入独立计算：
 * raw = "appkey=dfca71928277209b&baz=1&foo=bar&ts=1700000000"，
 * MD5(raw + secret) = 005173547179059a2763e51e7b5eb180。
 * 若此用例失败，说明算法或编码与 Android `data/sign/AppSignUtil.kt` 不一致。
 */
class AppSignTest {

    private val clock = Clock { 1700000000L }
    private val appKey = "dfca71928277209b"
    private val appSecret = "b5475a8825547a4fc26c7d518eaaa02e"

    private fun signer(md5: Md5 = defaultMd5()) =
        AppSign(appKey, appSecret, clock, md5)

    @Test
    fun sign_golden() {
        val out = signer().sign(mapOf("foo" to "bar", "baz" to "1"))
        assertEquals(appKey, out["appkey"])
        assertEquals("1700000000", out["ts"])
        assertEquals("005173547179059a2763e51e7b5eb180", out["sign"])
        // 输入参数原样保留
        assertEquals(mapOf("foo" to "bar", "baz" to "1"), out - "appkey" - "ts" - "sign")
    }

    @Test
    fun sign_utf8Value_golden() {
        val out = signer().sign(mapOf("q" to "你好"))
        assertEquals("79f3735e35afe8637995bd5aece6b944", out["sign"])
    }

    @Test
    fun sign_formEncoding_spaceBecomesPlus() {
        var raw: String? = null
        val fake = Md5 { input ->
            raw = input
            "x"
        }
        AppSign(appKey, appSecret, clock, fake).sign(mapOf("a b" to "x y"))
        // AppSign 用 URLEncoder 语义：空格 → '+'；排序按原始 key（"a b" < "appkey"，空格 0x20 < 'p'）；
        // fake 捕获的是 `raw + appSecret`（md5.hex(raw + appSecret) 的入参）。
        assertEquals("a+b=x+y&appkey=dfca71928277209b&ts=1700000000$appSecret", raw)
    }

    @Test
    fun sign_formEncoding_tildeAndBangEncoded_starUnencoded() {
        var raw: String? = null
        val fake = Md5 { input ->
            raw = input
            "x"
        }
        AppSign(appKey, appSecret, clock, fake).sign(mapOf("k" to "a~b!c*d"))
        // ~ → %7E，! → %21，* 不转义（与 WBI 的 encodeUriComponent 语义不同）；
        // fake 捕获的是 `raw + appSecret`。
        assertEquals("appkey=dfca71928277209b&k=a%7Eb%21c*d&ts=1700000000$appSecret", raw)
    }

    @Test
    fun sign_feedsRawPlusSecretIntoMd5() {
        var canonical: String? = null
        val fake = Md5 { input ->
            canonical = input
            "x"
        }
        AppSign(appKey, appSecret, clock, fake).sign(mapOf("foo" to "bar", "baz" to "1"))
        // 排序按原始 key；appSecret 直接拼接，无分隔符
        assertEquals(
            "appkey=dfca71928277209b&baz=1&foo=bar&ts=1700000000$appSecret",
            canonical,
        )
    }
}
