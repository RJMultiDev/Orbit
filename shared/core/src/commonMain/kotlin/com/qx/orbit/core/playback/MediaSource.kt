package com.qx.orbit.core.playback

/**
 * A media item to play: a primary URI plus optional secondary streams (e.g. the
 * separated DASH audio track), request headers, cookie injection and DRM config.
 *
 * The model is intentionally Bilibili-agnostic: B-site specifics (WBI query,
 * `qn`, `referer`) are carried through [extensions] and mapped by the platform
 * adapter or repository layer.
 */
data class MediaSource(
    /** Primary media URI (http/https/file). */
    val uri: String,
    /** Extra request headers, e.g. `Referer`/`User-Agent`. */
    val headers: Map<String, String> = emptyMap(),
    /** Cookies to inject into media requests. */
    val cookies: Map<String, String> = emptyMap(),
    /** DRM configuration, or `null` for clear content. */
    val drm: DrmConfig? = null,
    /** Secondary streams merged with the primary one (typically audio for DASH). */
    val secondaryUris: List<SecondaryUri> = emptyList(),
    /** Optional MIME hint (e.g. `video/mp4`) when the URI is not self-describing. */
    val mimeType: String? = null,
    /** Bilibili/platform specific metadata; must not alter playback semantics. */
    val extensions: Map<String, String> = emptyMap(),
) {

    /** A secondary stream merged with the primary [uri] (DASH audio/video split). */
    data class SecondaryUri(
        val uri: String,
        val role: Role = Role.AUDIO,
        val headers: Map<String, String> = emptyMap(),
    )

    enum class Role { AUDIO, VIDEO, SUBTITLE, CAPTION }

    data class DrmConfig(
        val scheme: DrmScheme = DrmScheme.WIDEVINE,
        /** License server URL, when licensing is performed on this device. */
        val licenseUri: String? = null,
        val licenseHeaders: Map<String, String> = emptyMap(),
        /** Existing license key set identifier, or `null`. */
        val keySetId: ByteArray? = null,
    )

    enum class DrmScheme { WIDEVINE, PLAYREADY, CLEARKEY, FAIRPLAY }
}
