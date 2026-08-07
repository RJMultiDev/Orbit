package com.qx.orbit.core.model.social

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TvLoginFromWireTest {

    @Test
    fun `TvQrCodeAuth maps auth code`() {
        val auth = TvQrCodeAuth.fromWire(
            mapOf(
                "auth_code" to "TESTAUTH123",
                "url" to "https://example.com/qr.png",
                "expires_in" to 180L
            )
        )
        assertEquals("TESTAUTH123", auth.authCode)
        assertEquals("https://example.com/qr.png", auth.url)
        assertEquals(180L, auth.expiresIn)
    }

    @Test
    fun `TvQrCodePoll maps token and cookie info`() {
        val poll = TvQrCodePoll.fromWire(
            mapOf(
                "auth_code" to "TESTAUTH123",
                "status" to true,
                "token_info" to mapOf(
                    "mid" to 9999L,
                    "access_token" to "access-token-dummy",
                    "refresh_token" to "refresh-token-dummy",
                    "expires_in" to 2592000L
                ),
                "cookie_info" to mapOf(
                    "cookies" to listOf(
                        mapOf(
                            "name" to "SESSDATA",
                            "value" to "dummy-value",
                            "http_only" to 1,
                            "expires" to 1700000000L
                        )
                    )
                )
            )
        )

        assertEquals("TESTAUTH123", poll.authCode)
        assertTrue(poll.status)
        assertEquals(9999L, poll.tokenInfo?.mid)
        assertEquals("access-token-dummy", poll.tokenInfo?.accessToken)
        assertEquals("refresh-token-dummy", poll.tokenInfo?.refreshToken)
        assertEquals(2592000L, poll.tokenInfo?.expiresIn)

        val cookie = poll.cookieInfo?.cookies?.single()
        assertEquals("SESSDATA", cookie?.name)
        assertEquals("dummy-value", cookie?.value)
        assertEquals(1, cookie?.httpOnly)
        assertEquals(1700000000L, cookie?.expires)
    }

    @Test
    fun `TvRefreshData maps token and cookie`() {
        val refresh = TvRefreshData.fromWire(
            mapOf(
                "token_info" to mapOf("mid" to 9999L, "access_token" to "access-token-dummy-2"),
                "cookie_info" to mapOf("cookies" to listOf(mapOf("name" to "bili_jct", "value" to "dummy-jct")))
            )
        )
        assertEquals("access-token-dummy-2", refresh.tokenInfo?.accessToken)
        assertEquals("bili_jct", refresh.cookieInfo?.cookies?.single()?.name)
    }
}
