package com.qx.orbit.bili.data.remote

import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * JVM unit tests for the pure URL/query/header logic inside [HttpClient]'s
 * private interceptors (redirect policy, default headers, cookie filtering).
 *
 * No real network and no device: the private nested interceptors are invoked
 * via reflection, [Interceptor.Chain] is faked with a dynamic Proxy, and
 * [CookieManager]'s SharedPreferences is stubbed with a Proxy so the cookie
 * filter logic runs without Android storage.
 */
class HttpClientTest {

    // --- Reflection helpers ---

    private fun interceptor(simpleName: String): Any {
        val clazz = Class.forName("com.qx.orbit.bili.data.remote.HttpClient\$$simpleName")
        val constructor = clazz.declaredConstructors.single()
        constructor.isAccessible = true
        return constructor.newInstance()
    }

    private fun intercept(instance: Any, chain: Interceptor.Chain): Response {
        val method = instance.javaClass.getDeclaredMethod("intercept", Interceptor.Chain::class.java)
        method.isAccessible = true
        return method.invoke(instance, chain) as Response
    }

    /** Dynamic Proxy over [Interceptor.Chain]; only `request()`/`proceed()` are used by HttpClient. */
    private fun fakeChain(request: Request, onProceed: (Request) -> Response): Interceptor.Chain =
        Proxy.newProxyInstance(
            Interceptor.Chain::class.java.classLoader,
            arrayOf(Interceptor.Chain::class.java),
        ) { _, method, args ->
            when (method.name) {
                "request" -> request
                "proceed" -> onProceed(args[0] as Request)
                else -> null
            }
        } as Interceptor.Chain

    private fun responseFor(request: Request, code: Int, headers: Map<String, String> = emptyMap()): Response {
        val builder = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code in 300..399) "Redirect" else "OK")
        headers.forEach { (name, value) -> builder.header(name, value) }
        return builder.build()
    }

    private fun stubCookie(cookie: String) {
        val prefsProxy = Proxy.newProxyInstance(
            android.content.SharedPreferences::class.java.classLoader,
            arrayOf(android.content.SharedPreferences::class.java),
        ) { _, method, _ ->
            if (method.name == "getString") cookie else null
        }
        val field = CookieManager.javaClass.getDeclaredField("prefs")
        field.isAccessible = true
        field.set(CookieManager, prefsProxy)
    }

    // --- RedirectInterceptor: URL/redirect policy ---

    @Test
    fun `redirect interceptor follows http-to-https redirect on the same host`() {
        val initial = Request.Builder().url("http://api.bilibili.com/x/v1").build()
        val calls = mutableListOf<Request>()
        val chain = fakeChain(initial) { request ->
            calls += request
            if (calls.size == 1) {
                responseFor(request, 302, mapOf("Location" to "https://api.bilibili.com/x/v1"))
            } else {
                responseFor(request, 200)
            }
        }

        val result = intercept(interceptor("RedirectInterceptor"), chain)

        assertEquals(2, calls.size)
        assertEquals("https://api.bilibili.com/x/v1", calls[1].url.toString())
        assertEquals(200, result.code)
    }

    @Test
    fun `redirect interceptor does not follow non-https b23tv redirect`() {
        val initial = Request.Builder().url("http://b23.tv/abc123").build()
        var proceedCalls = 0
        val chain = fakeChain(initial) { request ->
            proceedCalls++
            responseFor(request, 302, mapOf("Location" to "http://b23.tv/other"))
        }

        val result = intercept(interceptor("RedirectInterceptor"), chain)

        assertEquals(1, proceedCalls) // only the initial probe, no second hop
        assertEquals(302, result.code)
    }

    @Test
    fun `redirect interceptor returns non-redirect responses untouched`() {
        val initial = Request.Builder().url("https://api.bilibili.com/x/v1").build()
        val chain = fakeChain(initial) { responseFor(it, 200) }

        val result = intercept(interceptor("RedirectInterceptor"), chain)

        assertEquals(200, result.code)
    }

    // --- DefaultHeadersInterceptor: UA / Referer ---

    @Test
    fun `default headers interceptor adds UA and Referer when missing`() {
        val initial = Request.Builder().url("https://api.bilibili.com/x/v1").build()
        var captured: Request? = null
        val chain = fakeChain(initial) { request ->
            captured = request
            responseFor(request, 200)
        }

        intercept(interceptor("DefaultHeadersInterceptor"), chain)

        assertEquals(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.95 Safari/537.36",
            captured!!.header("User-Agent"),
        )
        assertEquals("https://www.bilibili.com/", captured!!.header("Referer"))
    }

    @Test
    fun `default headers interceptor keeps existing UA and Referer`() {
        val initial = Request.Builder()
            .url("https://api.bilibili.com/x/v1")
            .header("User-Agent", "custom-ua")
            .header("Referer", "https://example.com/")
            .build()
        var captured: Request? = null
        val chain = fakeChain(initial) { request ->
            captured = request
            responseFor(request, 200)
        }

        intercept(interceptor("DefaultHeadersInterceptor"), chain)

        assertEquals("custom-ua", captured!!.header("User-Agent"))
        assertEquals("https://example.com/", captured!!.header("Referer"))
    }

    // --- CookieAddInterceptor: cookie filtering logic ---

    @Test
    fun `cookie add interceptor filters sensitive keys from stored cookie`() {
        stubCookie("SESSDATA=abc123; bili_jct=xyz; refresh_token=secret; access_token=at; expires_in=123; mid=12345")
        val initial = Request.Builder().url("https://api.bilibili.com/x/v1").build()
        var captured: Request? = null
        val chain = fakeChain(initial) { request ->
            captured = request
            responseFor(request, 200)
        }

        intercept(interceptor("CookieAddInterceptor"), chain)

        assertEquals("SESSDATA=abc123; bili_jct=xyz", captured!!.header("Cookie"))
    }

    @Test
    fun `cookie add interceptor keeps an existing Cookie header untouched`() {
        stubCookie("SESSDATA=abc123; refresh_token=secret")
        val initial = Request.Builder()
            .url("https://api.bilibili.com/x/v1")
            .header("Cookie", "custom=1")
            .build()
        var captured: Request? = null
        val chain = fakeChain(initial) { request ->
            captured = request
            responseFor(request, 200)
        }

        intercept(interceptor("CookieAddInterceptor"), chain)

        assertEquals("custom=1", captured!!.header("Cookie"))
    }

    @Test
    fun `cookie add interceptor passes through when there is no stored cookie`() {
        stubCookie("")
        val initial = Request.Builder().url("https://api.bilibili.com/x/v1").build()
        var captured: Request? = null
        val chain = fakeChain(initial) { request ->
            captured = request
            responseFor(request, 200)
        }

        intercept(interceptor("CookieAddInterceptor"), chain)

        assertEquals(null, captured!!.header("Cookie"))
    }
}
