package com.qx.orbit.core.danmaku

/**
 * Render-side configuration, platform-independent.
 *
 * Value-object projection of the Android `DanmakuConfig` knobs
 * (maximum lines / transparency / text scale / duplicate merging / overlap
 * prevention / special visibility) plus scroll speed, consumed by both the
 * DFM/DFMNext Android adapters and the HarmonyOS Canvas/XComponent renderer.
 */
data class DanmakuRenderConfig(
    /** Max concurrent lines per [DanmakuMode]; absent keys use [maxLinesDefault]. */
    val maxLines: Map<DanmakuMode, Int> = emptyMap(),
    /** Fallback line cap when a mode is not present in [maxLines]. */
    val maxLinesDefault: Int = 5,
    /** Scroll speed multiplier (1.0 = normal). */
    val scrollSpeed: Float = 1.0f,
    /** Global transparency; 1.0 = opaque, 0.0 = invisible. */
    val alpha: Float = 1.0f,
    /** Font size scale factor. */
    val textScale: Float = 1.0f,
    /** Merge identical consecutive danmaku. */
    val duplicateMerging: Boolean = false,
    /** Render special/advanced danmaku. */
    val specialDanmakuVisibility: Boolean = true,
    /** Whether overlap is prevented, per mode. */
    val preventOverlapping: Map<DanmakuMode, Boolean> = emptyMap(),
) {
    init {
        require(alpha in 0f..1f) { "alpha must be in 0..1, got $alpha" }
        require(scrollSpeed > 0f) { "scrollSpeed must be positive, got $scrollSpeed" }
        require(textScale > 0f) { "textScale must be positive, got $textScale" }
        require(maxLinesDefault > 0) { "maxLinesDefault must be positive, got $maxLinesDefault" }
    }

    /** Line cap for [mode]. */
    fun maxLinesFor(mode: DanmakuMode): Int = maxLines[mode] ?: maxLinesDefault
}
