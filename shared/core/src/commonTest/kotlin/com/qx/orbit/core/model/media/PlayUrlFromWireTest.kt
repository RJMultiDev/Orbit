package com.qx.orbit.core.model.media

import kotlin.test.Test
import kotlin.test.assertEquals

class PlayUrlFromWireTest {

    @Test
    fun `Playurl parses live playurl tree`() {
        val playurl = Playurl.fromWire(
            mapOf(
                "cid" to 999L,
                "g_qn_desc" to listOf(
                    mapOf("qn" to 10000, "desc" to "原画", "hdr_desc" to "HDR")
                ),
                "stream" to listOf(
                    mapOf(
                        "protocol_name" to "http_stream",
                        "format" to listOf(
                            mapOf(
                                "format_name" to "fmp4",
                                "master_url" to "https://example.com/master.m3u8",
                                "codec" to listOf(
                                    mapOf(
                                        "codec_name" to "avc",
                                        "current_qn" to 10000,
                                        "accept_qn" to listOf(10000, 400, 250),
                                        "base_url" to "live_1.flv",
                                        "url_info" to listOf(
                                            mapOf("host" to "https://example.com", "extra" to "?sign=dummy", "stream_ttl" to 60)
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        assertEquals(999L, playurl.cid)
        assertEquals("原画", playurl.qnDesc.single().desc)
        assertEquals("HDR", playurl.qnDesc.single().hdr_desc)

        val protocol = playurl.stream.single()
        assertEquals("http_stream", protocol.protocol_name)
        val format = protocol.format.single()
        assertEquals("fmp4", format.format_name)
        assertEquals("https://example.com/master.m3u8", format.master_url)
        val codec = format.codec.single()
        assertEquals("avc", codec.codec_name)
        assertEquals(10000, codec.current_qn)
        assertEquals(listOf(10000, 400, 250), codec.acceptQn)
        assertEquals("live_1.flv", codec.base_url)
        val urlInfo = codec.urlInfo.single()
        assertEquals("https://example.com", urlInfo.host)
        assertEquals("?sign=dummy", urlInfo.extra)
        assertEquals(60, urlInfo.stream_ttl)
    }
}
