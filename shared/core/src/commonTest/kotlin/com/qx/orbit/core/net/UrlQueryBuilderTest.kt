package com.qx.orbit.core.net

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UrlQueryBuilderTest {

    @Test
    fun encodeUriComponent_leavesUnreservedUntouched() {
        val unreserved = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.~!*'()"
        assertEquals(unreserved, UrlQueryBuilder.encodeUriComponent(unreserved))
    }

    @Test
    fun encodeUriComponent_encodesReservedAndSpace() {
        assertEquals("%20", UrlQueryBuilder.encodeUriComponent(" "))
        assertEquals("%2B", UrlQueryBuilder.encodeUriComponent("+"))
        assertEquals("%26%3D%3F%2F%23%25", UrlQueryBuilder.encodeUriComponent("&=?/#%"))
    }

    @Test
    fun encodeUriComponent_utf8MultiByte() {
        assertEquals("%E4%B8%AD%E6%96%87", UrlQueryBuilder.encodeUriComponent("中文"))
        assertEquals("%E2%82%AC", UrlQueryBuilder.encodeUriComponent("€"))
    }

    @Test
    fun build_preservesInsertionOrderWithoutSort() {
        val params = mapOf("b" to "2", "a" to "1")
        assertEquals("b=2&a=1", UrlQueryBuilder.build(params))
    }

    @Test
    fun build_sortsEncodedEntriesWhenRequested() {
        val params = mapOf("b" to "2", "a" to "1")
        assertEquals("a=1&b=2", UrlQueryBuilder.build(params, sort = true))

        // 排序作用于编码后的整条 k=v
        val mixed = mapOf("wts" to "1700000000", "foo" to "x y")
        assertEquals("foo=x%20y&wts=1700000000", UrlQueryBuilder.build(mixed, sort = true))
    }

    @Test
    fun build_encodesValuesWithSpecialChars() {
        val params = mapOf("q" to "你好 world", "from" to "search&nav")
        assertEquals(
            "q=%E4%BD%A0%E5%A5%BD%20world&from=search%26nav",
            UrlQueryBuilder.build(params)
        )
    }

    @Test
    fun appendQuery_usesCorrectSeparator() {
        assertEquals("https://x/y?a=1", UrlQueryBuilder.appendQuery("https://x/y", "a=1"))
        assertEquals("https://x/y?a=1&b=2", UrlQueryBuilder.appendQuery("https://x/y?a=1", "b=2"))
        assertEquals("https://x/y", UrlQueryBuilder.appendQuery("https://x/y", ""))
    }

    @Test
    fun encodeFormComponent_spaceBecomesPlus() {
        assertEquals("a+b+c", UrlQueryBuilder.encodeFormComponent("a b c"))
        assertEquals("a%26b%3Dc", UrlQueryBuilder.encodeFormComponent("a&b=c"))
        assertEquals("abc", UrlQueryBuilder.encodeFormComponent("abc"))
    }
}
