package com.qx.orbit.core.download

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class RangeRuleTest {

    // --- ByteRange construction ---

    @Test
    fun `open-ended range header`() {
        assertEquals("bytes=0-", ByteRange(0).toHeader())
        assertEquals("bytes=100-", ByteRange(100).toHeader())
    }

    @Test
    fun `bounded range header`() {
        assertEquals("bytes=100-199", ByteRange(100, 199).toHeader())
    }

    @Test
    fun `range length`() {
        assertNull(ByteRange(0).length)
        assertEquals(100L, ByteRange(100, 199).length)
    }

    @Test
    fun `range requires non-negative start`() {
        assertFailsWith<IllegalArgumentException> { ByteRange(-1) }
    }

    @Test
    fun `range requires end not before start`() {
        assertFailsWith<IllegalArgumentException> { ByteRange(5, 4) }
    }

    @Test
    fun `isResume reflects positive start`() {
        assertFalse(ByteRange(0).isResume)
        assertTrue(ByteRange(1).isResume)
    }

    // --- resume offset ---

    @Test
    fun `resumeOffset clamps negative`() {
        assertEquals(0L, RangeRule.resumeOffset(-5))
        assertEquals(0L, RangeRule.resumeOffset(0))
        assertEquals(1234L, RangeRule.resumeOffset(1234))
    }

    // --- buildRange / buildHeader ---

    @Test
    fun `fresh download requests bytes 0-`() {
        assertEquals("bytes=0-", RangeRule.buildHeader(existingBytes = 0L))
    }

    @Test
    fun `existing partial file resumes at its length`() {
        assertEquals("bytes=1024-", RangeRule.buildHeader(existingBytes = 1024L))
    }

    @Test
    fun `buildRange start equals existing bytes`() {
        val range = RangeRule.buildRange(existingBytes = 2048L)
        assertEquals(2048L, range.start)
        assertNull(range.endInclusive)
        assertEquals("bytes=2048-", range.toHeader())
    }

    // --- status handling ---

    @Test
    fun `206 is partial content`() {
        assertTrue(RangeRule.isPartialContent(206))
        assertFalse(RangeRule.isPartialContent(200))
        assertFalse(RangeRule.isPartialContent(403))
    }

    @Test
    fun `total after resume 206 adds existing bytes`() {
        assertEquals(1024L + 900L, RangeRule.totalBytesAfterResponse(existingBytes = 1024L, statusCode = 206, contentLength = 900L))
    }

    @Test
    fun `total after fresh 200 is content length`() {
        assertEquals(900L, RangeRule.totalBytesAfterResponse(existingBytes = 1024L, statusCode = 200, contentLength = 900L))
    }

    @Test
    fun `total clamps negative content length`() {
        assertEquals(1024L, RangeRule.totalBytesAfterResponse(existingBytes = 1024L, statusCode = 206, contentLength = -1L))
    }

    @Test
    fun `write offset after resume starts at existing bytes`() {
        assertEquals(1024L, RangeRule.writeOffset(existingBytes = 1024L, statusCode = 206))
    }

    @Test
    fun `write offset after fresh 200 starts at zero`() {
        assertEquals(0L, RangeRule.writeOffset(existingBytes = 1024L, statusCode = 200))
    }

    // --- already complete ---

    @Test
    fun `isAlreadyComplete boundaries`() {
        assertTrue(RangeRule.isAlreadyComplete(existingBytes = 100L, totalBytes = 100L))
        assertTrue(RangeRule.isAlreadyComplete(existingBytes = 101L, totalBytes = 100L))
        assertFalse(RangeRule.isAlreadyComplete(existingBytes = 99L, totalBytes = 100L))
        assertFalse(RangeRule.isAlreadyComplete(existingBytes = 0L, totalBytes = 100L))
        // unknown total is never complete
        assertFalse(RangeRule.isAlreadyComplete(existingBytes = 0L, totalBytes = 0L))
    }
}
