package com.qx.orbit.core.playback

/**
 * Platform-agnostic playback error model.
 *
 * Adapters normalize their native errors into this type: Media3
 * `PlaybackException.errorCode`, IJK `what`/`extra`, and the HarmonyOS media
 * error codes all land in [code]/[what]/[extra] while [source] names the adapter.
 *
 * This is the media/player-layer error; transport/API-layer failures (network,
 * auth, Bili business codes) use `com.qx.orbit.core.error.BizError` instead.
 */
data class PlaybackError(
    /** Adapter or platform error code (see [source]). */
    val code: Int,
    /** Coarse classification for UI/reporting. */
    val category: Category = Category.UNKNOWN,
    /** Human-readable message (may be empty). */
    val message: String = "",
    /** Adapter identity, e.g. `media3`, `ijk`, `ohos-media`. */
    val source: String? = null,
    /** Raw IJK/Media3 `what` passthrough (0 when unused). */
    val what: Int = 0,
    /** Raw IJK/Media3 `extra` passthrough (0 when unused). */
    val extra: Int = 0,
    /** Underlying cause when available. */
    val cause: Throwable? = null,
) {

    enum class Category {
        UNKNOWN,
        /** Source/format not supported or malformed. */
        SOURCE,
        /** DRM/license failure. */
        DRM,
        /** Network or HTTP failure. */
        NETWORK,
        /** Decode/render failure. */
        DECODE,
        /** Timed out while loading or rendering. */
        TIMEOUT,
        /** I/O failure (local files). */
        IO,
        /** Controller used in an invalid state. */
        ILLEGAL_STATE,
    }

    companion object {
        /** Generic/unknown error code used by adapters without a native code. */
        const val CODE_GENERIC = -1
    }
}
