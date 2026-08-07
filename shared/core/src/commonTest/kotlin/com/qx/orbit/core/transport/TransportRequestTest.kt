package com.qx.orbit.core.transport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TransportRequestTest {

    @Test
    fun `get request keeps method url headers and queryParams`() {
        val request = TransportRequest(
            method = HttpMethod.GET,
            url = "https://api.bilibili.com/x/web-interface/nav",
            headers = mapOf("Cookie" to "SESSDATA=abc", "Referer" to "https://www.bilibili.com/"),
            queryParams = listOf("w_rid" to "deadbeef", "wts" to "1700000000"),
        )
        assertEquals(HttpMethod.GET, request.method)
        assertEquals("https://api.bilibili.com/x/web-interface/nav", request.url)
        assertEquals("SESSDATA=abc", request.headers["Cookie"])
        assertEquals(2, request.queryParams.size)
        assertEquals("wts" to "1700000000", request.queryParams[1])
        assertNull(request.body)
    }

    @Test
    fun `post withFormBody encodes urlencoded bytes and content type`() {
        val request = TransportRequest(
            method = HttpMethod.POST,
            url = "https://api.bilibili.com/x/v2/reply/add",
        ).withFormBody(
            mapOf(
                "oid" to "123",
                "message" to "你好 bilibili",
                "csrf" to "tok",
            )
        )
        assertEquals(HttpMethod.POST, request.method)
        assertEquals("application/x-www-form-urlencoded", request.headers["Content-Type"])
        val body = request.body!!.decodeToString()
        assertTrue("oid=123" in body)
        assertTrue("csrf=tok" in body)
        // UTF-8 字符必须被正确百分号编码
        assertTrue("message=%E4%BD%A0%E5%A5%BD%20bilibili" in body)
    }

    @Test
    fun `post withJsonBody sets json content type`() {
        val request = TransportRequest(
            method = HttpMethod.POST,
            url = "https://api.bilibili.com/x/dynamic/feed/dyn/thumb",
        ).withJsonBody("""{"dyn_id_str":"123","up":1}""")
        assertEquals("application/json", request.headers["Content-Type"])
        assertEquals("""{"dyn_id_str":"123","up":1}""", request.body!!.decodeToString())
    }

    @Test
    fun `requests with same content are equal`() {
        val a = TransportRequest(HttpMethod.GET, "https://example.com", body = "hi".encodeToByteArray())
        val b = TransportRequest(HttpMethod.GET, "https://example.com", body = "hi".encodeToByteArray())
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `requests with different bodies are not equal`() {
        val a = TransportRequest(HttpMethod.GET, "https://example.com", body = "hi".encodeToByteArray())
        val b = TransportRequest(HttpMethod.GET, "https://example.com", body = "hey".encodeToByteArray())
        assertNotEquals(a, b)
    }

    @Test
    fun `response header lookup is case-insensitive and keeps duplicates`() {
        val response = TransportResponse(
            status = 200,
            headers = listOf(
                "Content-Type" to "application/json",
                "Set-Cookie" to "SESSDATA=one",
                "set-cookie" to "bili_jct=two",
            ),
            body = "{}".encodeToByteArray(),
        )
        assertEquals(200, response.status)
        assertTrue(response.isSuccessful)
        assertEquals("application/json", response.header("CONTENT-TYPE"))
        assertEquals(listOf("SESSDATA=one", "bili_jct=two"), response.headers("Set-Cookie"))
        assertEquals("{}", response.body.decodeToString())
    }

    @Test
    fun `response equality compares body bytes`() {
        val a = TransportResponse(206, body = byteArrayOf(1, 2, 3))
        val b = TransportResponse(206, body = byteArrayOf(1, 2, 3))
        val c = TransportResponse(200, body = byteArrayOf(1, 2, 3))
        assertEquals(a, b)
        assertNotEquals(a, c)
    }
}
