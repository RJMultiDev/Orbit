package com.qx.orbit.core.mediaselection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 典型 B 站 DASH 轨道清单（fixture 已脱敏：URL 使用 example 域名，带宽为示意值）。
 *
 * 视频 qn：120(4K) / 80(1080P) / 64(720P) / 32(480P) / 16(360P)，其中 80 同时有 AVC 与 HEVC。
 * 音频：30280 / 30232 / 30216 普通流 + FLAC(30251) + Dolby(30250)。
 */
class DashStreamSelectorTest {

    private val video120 = DashVideoTrack(
        id = 120, baseUrl = "https://upos-sz.example.com/upgcx/codec/avc/120",
        bandwidth = 12_000_000, codecs = "avc1.640034", width = 3840, height = 2160, codecId = 7
    )
    private val video80Avc = DashVideoTrack(
        id = 80, baseUrl = "https://upos-sz.example.com/upgcx/codec/avc/80",
        bandwidth = 6_000_000, codecs = "avc1.640032", width = 1920, height = 1080, codecId = 7
    )
    private val video80Hevc = DashVideoTrack(
        id = 80, baseUrl = "https://upos-sz.example.com/upgcx/codec/hev/80",
        bandwidth = 4_500_000, codecs = "hev1.1.6.L153.B0", width = 1920, height = 1080, codecId = 12
    )
    private val video64 = DashVideoTrack(
        id = 64, baseUrl = "https://upos-sz.example.com/upgcx/codec/avc/64",
        bandwidth = 3_000_000, codecs = "avc1.64002A", width = 1280, height = 720, codecId = 7
    )
    private val video32 = DashVideoTrack(
        id = 32, baseUrl = "https://upos-sz.example.com/upgcx/codec/avc/32",
        bandwidth = 1_500_000, codecs = "avc1.64001F", width = 852, height = 480, codecId = 7
    )
    private val video16 = DashVideoTrack(
        id = 16, baseUrl = "https://upos-sz.example.com/upgcx/codec/avc/16",
        bandwidth = 700_000, codecs = "avc1.640015", width = 640, height = 360, codecId = 7
    )

    private val audio192 = DashAudioTrack(id = 30280, baseUrl = "https://upos-sz.example.com/upgcx/audio/192", bandwidth = 320_000, codecs = "mp4a.40.2", codecId = 2)
    private val audio128 = DashAudioTrack(id = 30232, baseUrl = "https://upos-sz.example.com/upgcx/audio/128", bandwidth = 128_000, codecs = "mp4a.40.2", codecId = 2)
    private val audio64 = DashAudioTrack(id = 30216, baseUrl = "https://upos-sz.example.com/upgcx/audio/64", bandwidth = 64_000, codecs = "mp4a.40.2", codecId = 2)
    private val flac = DashAudioTrack(id = 30251, baseUrl = "https://upos-sz.example.com/upgcx/audio/flac", bandwidth = 1_500_000, codecs = "fLaC", codecId = 3)
    private val dolby = DashAudioTrack(id = 30250, baseUrl = "https://upos-sz.example.com/upgcx/audio/dolby", bandwidth = 640_000, codecs = "ec-3", codecId = 4)

    private val tracks = DashTrackList(
        videoTracks = listOf(video120, video80Avc, video80Hevc, video64, video32, video16),
        audioTracks = listOf(audio192, audio128, audio64),
        dolbyTrack = dolby,
        flacTrack = flac
    )

    // ---- 视频：按质量 ----

    @Test
    fun `exact quality match prefers AVC over HEVC`() {
        val result = DashStreamSelector.select(tracks, StreamSelectionRequest(preferredVideoQuality = 80))
        assertEquals(80, result.video?.id)
        assertEquals(7, result.video?.codecId)
        assertEquals(VideoSelectionStrategy.EXACT, result.videoStrategy)
    }

    @Test
    fun `exact match falls back to first track when no AVC variant`() {
        val list = tracks.copy(videoTracks = listOf(video80Hevc, video64, video32, video16))
        val pick = DashStreamSelector.selectVideo(list.videoTracks, StreamSelectionRequest(preferredVideoQuality = 80))
        assertEquals(80, pick.track?.id)
        assertEquals(12, pick.track?.codecId)
        assertEquals(VideoSelectionStrategy.EXACT, pick.strategy)
    }

    @Test
    fun `no exact quality downgrades to highest id below target`() {
        val list = tracks.copy(videoTracks = listOf(video80Avc, video80Hevc, video64, video32, video16))
        val pick = DashStreamSelector.selectVideo(list.videoTracks, StreamSelectionRequest(preferredVideoQuality = 116))
        assertEquals(80, pick.track?.id)
        assertEquals(VideoSelectionStrategy.DOWNGRADE, pick.strategy)
    }

    @Test
    fun `target below all qualities picks nearest`() {
        val pick = DashStreamSelector.selectVideo(tracks.videoTracks, StreamSelectionRequest(preferredVideoQuality = 6))
        assertEquals(16, pick.track?.id)
        assertEquals(VideoSelectionStrategy.NEAREST, pick.strategy)
    }

    // ---- 视频：编解码限制 ----

    @Test
    fun `codec hard limit restricts candidates`() {
        val pick = DashStreamSelector.selectVideo(
            tracks.videoTracks,
            StreamSelectionRequest(preferredVideoQuality = 80, allowedCodecIds = setOf(12))
        )
        assertEquals(12, pick.track?.codecId)
        assertEquals(80, pick.track?.id)
    }

    @Test
    fun `codec name limit selects only matching streams`() {
        val pick = DashStreamSelector.selectVideo(
            tracks.videoTracks,
            StreamSelectionRequest(preferredVideoQuality = 120, allowedCodecNames = setOf("avc1"))
        )
        assertEquals(7, pick.track?.codecId)
        assertEquals(120, pick.track?.id)
    }

    @Test
    fun `no track satisfies codec limit returns null`() {
        val pick = DashStreamSelector.selectVideo(
            tracks.videoTracks,
            StreamSelectionRequest(preferredVideoQuality = 80, allowedCodecIds = setOf(99))
        )
        assertNull(pick.track)
        assertNull(pick.strategy)
    }

    // ---- 视频：带宽预算 ----

    @Test
    fun `bandwidth budget downgrades quality`() {
        val pick = DashStreamSelector.selectVideo(
            tracks.videoTracks,
            StreamSelectionRequest(preferredVideoQuality = 80, bandwidthBudget = 4_000_000)
        )
        assertEquals(64, pick.track?.id)
        assertEquals(VideoSelectionStrategy.DOWNGRADE, pick.strategy)
    }

    @Test
    fun `budget too small falls back to full candidate list`() {
        val pick = DashStreamSelector.selectVideo(
            tracks.videoTracks,
            StreamSelectionRequest(preferredVideoQuality = 80, bandwidthBudget = 100)
        )
        assertEquals(80, pick.track?.id)
        assertEquals(VideoSelectionStrategy.EXACT, pick.strategy)
    }

    // ---- 视频：兜底 ----

    @Test
    fun `fallback video prefers first AVC track`() {
        val list = tracks.copy(videoTracks = listOf(video80Hevc, video32, video16))
        assertEquals(video32, DashStreamSelector.fallbackVideo(list.videoTracks))
    }

    @Test
    fun `fallback video picks first track when no AVC present`() {
        val list = tracks.copy(videoTracks = listOf(video80Hevc, video64))
        val noAvc = list.copy(videoTracks = listOf(video80Hevc.copy(codecId = 12), video64.copy(codecId = 12)))
        assertEquals(video80Hevc.copy(codecId = 12), DashStreamSelector.fallbackVideo(noAvc.videoTracks))
    }

    // ---- 音频 ----

    @Test
    fun `best audio prefers flac then dolby then max bandwidth`() {
        assertEquals(flac, DashStreamSelector.selectAudio(tracks, StreamSelectionRequest(preferredVideoQuality = 80)))
        val noFlac = tracks.copy(flacTrack = null)
        assertEquals(dolby, DashStreamSelector.selectAudio(noFlac, StreamSelectionRequest(preferredVideoQuality = 80)))
        val noFlacNoDolby = noFlac.copy(dolbyTrack = null)
        assertEquals(audio192, DashStreamSelector.selectAudio(noFlacNoDolby, StreamSelectionRequest(preferredVideoQuality = 80)))
    }

    @Test
    fun `audio quality 30251 requests flac when allowed`() {
        val selected = DashStreamSelector.selectAudio(tracks, StreamSelectionRequest(preferredVideoQuality = 80, preferredAudioQuality = 30251))
        assertEquals(flac, selected)
    }

    @Test
    fun `audio quality 30250 disabled falls back to best`() {
        val selected = DashStreamSelector.selectAudio(
            tracks,
            StreamSelectionRequest(preferredVideoQuality = 80, preferredAudioQuality = 30250, allowDolby = false)
        )
        assertEquals(flac, selected)
    }

    @Test
    fun `exact audio id match wins`() {
        val selected = DashStreamSelector.selectAudio(
            tracks,
            StreamSelectionRequest(preferredVideoQuality = 80, preferredAudioQuality = 30232)
        )
        assertEquals(audio128, selected)
    }

    @Test
    fun `audio bandwidth budget picks highest within budget`() {
        val noDolby = tracks.copy(flacTrack = null, dolbyTrack = null)
        val selected = DashStreamSelector.selectAudio(
            noDolby,
            StreamSelectionRequest(preferredVideoQuality = 80, preferredAudioQuality = -1, bandwidthBudget = 200_000)
        )
        assertEquals(audio128, selected)
    }

    // ---- 确定性 ----

    @Test
    fun `same input yields identical output`() {
        val req = StreamSelectionRequest(preferredVideoQuality = 80, bandwidthBudget = 4_000_000)
        val a = DashStreamSelector.select(tracks, req)
        val b = DashStreamSelector.select(tracks, req)
        assertEquals(a, b)
        assertTrue(a.video != null && a.audio != null)
    }
}
