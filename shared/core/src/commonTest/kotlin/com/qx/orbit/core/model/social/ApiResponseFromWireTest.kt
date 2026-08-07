package com.qx.orbit.core.model.social

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiResponseFromWireTest {

    @Test
    fun `ApiResponse wraps raw data`() {
        val resp = ApiResponse.fromWire(
            mapOf("code" to 0, "message" to "0", "data" to mapOf("key" to "value"))
        )
        assertTrue(resp.isSuccess)
        assertEquals("value", (resp.data as? Map<*, *>)?.get("key"))
    }

    @Test
    fun `ApiResponse parses typed data with parser`() {
        val resp = ApiResponse.fromWire(
            mapOf("code" to 0, "data" to mapOf("rpid" to 1L, "content" to mapOf("message" to "解析评论（脱敏）"))),
            { Reply.fromWire(it) }
        )
        assertTrue(resp.isSuccess)
        assertEquals(1L, resp.data?.rpid)
        assertEquals("解析评论（脱敏）", resp.data?.message)
    }

    @Test
    fun `ApiResponse failure code is not success`() {
        val resp = ApiResponse.fromWire(mapOf("code" to -412, "message" to "请求被拦截"))
        assertTrue(!resp.isSuccess)
        assertEquals(-412, resp.code)
        assertEquals("请求被拦截", resp.message)
        assertEquals(null, resp.data)
    }

    @Test
    fun `ApiResponse reads result alias and msg alias`() {
        val resp = ApiResponse.fromWire(
            mapOf("code" to 0, "msg" to "ok", "result" to mapOf("total" to 7))
        )
        assertTrue(resp.isSuccess)
        assertEquals("ok", resp.message)
        assertEquals(7, (resp.data as? Map<*, *>)?.get("total"))
    }
}
