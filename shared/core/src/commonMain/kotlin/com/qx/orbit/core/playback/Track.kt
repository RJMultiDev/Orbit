package com.qx.orbit.core.playback

/** Whether a [Track] carries video or audio. */
enum class TrackKind { VIDEO, AUDIO }

/**
 * Description of a selectable audio/video track, mapped from DASH streams
 * (`DashVideoStream`/`DashAudioStream`), IJK info, or HarmonyOS track APIs.
 *
 * [width]/[height]/[frameRate] are only meaningful for [TrackKind.VIDEO];
 * [sampleRate]/[channelCount] only for [TrackKind.AUDIO]. Unused fields are `0`.
 */
data class Track(
    /** Stable identifier used with [PlaybackController.switchTrack]. */
    val id: String,
    val kind: TrackKind,
    /** Quality level index (e.g. DASH `qn`); higher is better. */
    val quality: Int = 0,
    /** Bitrate in bits per second. */
    val bandwidth: Long = 0,
    /** Codec string, e.g. `avc1.640032` or `mp4a.40.2`. */
    val codec: String = "",
    /** MIME type, e.g. `video/mp4` or `audio/mp4`. */
    val mimeType: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val frameRate: Float = 0f,
    val sampleRate: Int = 0,
    val channelCount: Int = 0,
    val language: String = "",
    val label: String = "",
    /** Adapter-specific extras; must not alter playback semantics. */
    val extensions: Map<String, String> = emptyMap(),
)
