package com.qx.orbit.core.sign

import com.qx.orbit.core.net.UrlQueryBuilder
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * encodeUriComponent 转义矩阵 —— 逐字节对齐 Android `WbiSigner.encodeUriComponent`。
 *
 * 若此用例失败，说明编码语义与 Android 不一致。Android 实现为：
 * `URLEncoder.encode(value)` 后再替换 `+`→`%20`、`%21`→`!`、`%27`→`'`、
 * `%28`→`(`、`%29`→`)`、`%7E`→`~`、`%2A`→`*`。
 *
 * 注意：Java URLEncoder 本就不转义 `*`，故 Android 最终 `*` 未转义
 * （与 bilibili-API-collect 参考实现 `*`→`%2A` 不同，本模块以 Android 逐字节输出为 golden）。
 */
class EncodingTest {

    @Test
    fun encode_emptyString_isEmpty() {
        assertEquals("", UrlQueryBuilder.encodeUriComponent(""))
    }

    @Test
    fun encode_plainAscii_unchanged() {
        assertEquals("abcXYZ0129", UrlQueryBuilder.encodeUriComponent("abcXYZ0129"))
    }

    @Test
    fun encode_space_becomesPercent20() {
        assertEquals("%20", UrlQueryBuilder.encodeUriComponent(" "))
        assertEquals("a%20b", UrlQueryBuilder.encodeUriComponent("a b"))
    }

    @Test
    fun encode_unreserved_unchanged() {
        // - _ . ~ ! * ' ( ) 均不转义
        assertEquals("-_.~!*'()", UrlQueryBuilder.encodeUriComponent("-_.~!*'()"))
    }

    @Test
    fun encode_plus_isPercent2B() {
        assertEquals("%2B", UrlQueryBuilder.encodeUriComponent("+"))
    }

    @Test
    fun encode_reserved_encoded() {
        assertEquals("%26%3D%3F%2F%23%25", UrlQueryBuilder.encodeUriComponent("&=?/#%"))
    }

    @Test
    fun encode_utf8MultiByte() {
        assertEquals("%E4%B8%AD%E6%96%87", UrlQueryBuilder.encodeUriComponent("中文"))
        assertEquals("%E4%BD%A0%E5%A5%BD", UrlQueryBuilder.encodeUriComponent("你好"))
    }

    @Test
    fun encode_surrogatePairEmoji() {
        assertEquals("%F0%9F%98%80", UrlQueryBuilder.encodeUriComponent("😀"))
    }
}
