package com.qx.orbit.core.mediaselection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StreamUrlResolverTest {

    // ---- URL 归一化 ----

    @Test
    fun `normalize handles all forms`() {
        assertEquals("", StreamUrlResolver.normalizeUrl(""))
        assertEquals(
            "https://upos-sz.example.com/upgcx/v.mp4",
            StreamUrlResolver.normalizeUrl("https://upos-sz.example.com/upgcx/v.mp4")
        )
        assertEquals(
            "https://upos-sz.example.com/upgcx/v.mp4",
            StreamUrlResolver.normalizeUrl("//upos-sz.example.com/upgcx/v.mp4")
        )
        assertEquals("file:///storage/sub.srt", StreamUrlResolver.normalizeUrl("/storage/sub.srt"))
        assertEquals(
            "https://upos-sz.example.com/upgcx/v.mp4",
            StreamUrlResolver.normalizeUrl("upos-sz.example.com/upgcx/v.mp4")
        )
    }

    // ---- 主备候选 ----

    @Test
    fun `candidate urls place base first and dedup backups`() {
        val candidates = StreamUrlResolver.candidateUrls(
            baseUrl = "https://upos.example.com/a",
            backupUrls = listOf("https://upos2.example.com/a", "https://upos.example.com/a", "")
        )
        assertEquals(
            listOf("https://upos.example.com/a", "https://upos2.example.com/a"),
            candidates
        )
    }

    @Test
    fun `empty candidates return empty`() {
        assertEquals(emptyList<String>(), StreamUrlResolver.candidateUrls("", emptyList()))
    }

    // ---- durl 主选 ----

    @Test
    fun `durl primary is first entry`() {
        val durls = listOf(
            DurlSource("https://durl1.example.com/v.mp4", listOf("https://durl2.example.com/v.mp4")),
            DurlSource("https://durl3.example.com/v.mp4")
        )
        assertEquals("https://durl1.example.com/v.mp4", StreamUrlResolver.selectDurlPrimary(durls))
    }

    @Test
    fun `durl empty returns empty string`() {
        assertEquals("", StreamUrlResolver.selectDurlPrimary(emptyList()))
    }

    // ---- 403/失败后候选轮换 ----

    @Test
    fun `failed url not in fresh candidates uses new primary`() {
        // 403 后重新取流，旧地址已过期（不在新列表）→ 用新主地址
        val fresh = listOf("https://upos-new.example.com/a", "https://upos-new2.example.com/a")
        assertEquals("https://upos-new.example.com/a", StreamUrlResolver.nextCandidate("https://old.example.com/a", fresh))
    }

    @Test
    fun `failed url rotates to next backup`() {
        val candidates = listOf("https://upos.example.com/a", "https://upos2.example.com/a")
        assertEquals("https://upos2.example.com/a", StreamUrlResolver.nextCandidate("https://upos.example.com/a", candidates))
    }

    @Test
    fun `last backup exhausted returns null`() {
        val candidates = listOf("https://upos.example.com/a", "https://upos2.example.com/a")
        assertNull(StreamUrlResolver.nextCandidate("https://upos2.example.com/a", candidates))
    }

    @Test
    fun `no candidates returns null`() {
        assertNull(StreamUrlResolver.nextCandidate("https://upos.example.com/a", emptyList()))
    }
}
