package com.qx.orbit.core.error

import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BizErrorTest {

    @Test
    fun `bili auth codes are classified as AUTH`() {
        assertTrue(isAuthCode(-101))
        assertTrue(isAuthCode(-352))
        assertTrue(isAuthCode(-401))
        assertTrue(isAuthCode(62002))
        assertTrue(isAuthCode(62004))
    }

    @Test
    fun `bili non-auth codes are classified as BIZ`() {
        assertFalse(isAuthCode(-404))
        assertFalse(isAuthCode(0))
        assertFalse(isAuthCode(1009))
    }

    @Test
    fun `classifyBiliCode maps auth and biz kinds`() {
        val auth = classifyBiliCode(-101, "未登录")
        assertEquals(ErrorKind.AUTH, auth.kind)
        assertEquals(-101, auth.code)
        assertEquals("未登录", auth.message)

        val biz = classifyBiliCode(-404, "资源不存在")
        assertEquals(ErrorKind.BIZ, biz.kind)
        assertEquals(-404, biz.code)
    }

    @Test
    fun `classifyHttp maps 401 and 403 to AUTH with httpCode`() {
        for (status in listOf(401, 403)) {
            val err = classifyHttp(status)
            assertEquals(ErrorKind.AUTH, err.kind)
            assertEquals(status, err.httpCode)
        }
        val server = classifyHttp(500)
        assertEquals(ErrorKind.HTTP, server.kind)
        assertEquals(500, server.httpCode)
        val notFound = classifyHttp(404)
        assertEquals(ErrorKind.HTTP, notFound.kind)
        assertTrue(notFound.isAuth().not())
    }

    @Test
    fun `factories produce expected codes and kinds`() {
        assertEquals(BizError(BizErrors.CODE_NETWORK, "x", ErrorKind.NETWORK), BizErrors.network("x"))
        val http = BizErrors.http(403)
        assertEquals(ErrorKind.HTTP, http.kind)
        assertEquals(403, http.httpCode)
        assertEquals(ErrorKind.AUTH, BizErrors.api(-101).kind)
        assertEquals(ErrorKind.CANCELED, BizErrors.canceled().kind)
        assertEquals(ErrorKind.PARSE, BizErrors.parse().kind)
        assertEquals(ErrorKind.PARSE, BizErrors.emptyBody().kind)
        assertEquals(BizErrors.CODE_NETWORK, BizErrors.network().code)
        assertEquals(BizErrors.CODE_CANCELED, BizErrors.canceled().code)
    }

    @Test
    fun `classifyThrowable maps cancellation and generic exceptions`() {
        val canceled = classifyThrowable(CancellationException("user canceled"))
        assertEquals(ErrorKind.CANCELED, canceled.kind)

        val network = classifyThrowable(RuntimeException("connect timeout"))
        assertEquals(ErrorKind.NETWORK, network.kind)
        assertEquals(BizErrors.CODE_NETWORK, network.code)
        assertTrue(network.cause is RuntimeException)
    }

    @Test
    fun `bizError isAuth helper`() {
        assertTrue(BizErrors.auth().isAuth())
        assertTrue(classifyHttp(401).isAuth())
        assertFalse(BizErrors.network().isAuth())
    }

    @Test
    fun `bizResult success and failure semantics`() {
        val ok: BizResult<Int> = BizResult.success(42)
        assertIs<BizResult.Success<Int>>(ok)
        assertEquals(42, ok.getOrNull())
        assertEquals(42, ok.getOrDefault(0))

        val fail: BizResult<Int> = BizResult.failure(BizErrors.api(-101))
        assertIs<BizResult.Failure>(fail)
        assertNull(fail.getOrNull())
        assertEquals(0, fail.getOrDefault(0))
        assertEquals(ErrorKind.AUTH, fail.onFailure { }.let { (it as BizResult.Failure).error.kind })
    }

    @Test
    fun `bizResult map and mapCatching`() {
        val ok: BizResult<Int> = BizResult.success(21)
        assertEquals(BizResult.success(42), ok.map { it * 2 })
        assertEquals(BizResult.success("21"), ok.map { it.toString() })

        val failing: BizResult<Int> = BizResult.failure(BizErrors.parse())
        assertIs<BizResult.Failure>(failing.map { it * 2 })

        val caught: BizResult<Int> = ok.mapCatching { error("boom") }
        assertIs<BizResult.Failure>(caught)
        assertEquals(ErrorKind.NETWORK, (caught as BizResult.Failure).error.kind)
    }

    @Test
    fun `bizResult catching catches throwable`() {
        val ok = BizResult.catching { "value" }
        assertEquals(BizResult.success("value"), ok)

        val fail = BizResult.catching<Int> { throw CancellationException("cancel") }
        assertIs<BizResult.Failure>(fail)
        assertEquals(ErrorKind.CANCELED, (fail as BizResult.Failure).error.kind)
    }
}
