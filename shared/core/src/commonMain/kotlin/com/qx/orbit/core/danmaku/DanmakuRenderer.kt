package com.qx.orbit.core.danmaku

/**
 * Port contract for the danmaku renderer.
 *
 * Implemented on Android by the DFM/DFMNext wrappers (backing the existing
 * `DanmakuPlayer` view) and on HarmonyOS by a Canvas/XComponent/native
 * renderer. The interface is pure — no `View`/`Surface` types.
 *
 * Timeline vs live mode: VOD events carry an absolute [DanmakuEvent.timeMs];
 * live events arrive through [submit] with an anchor time provided by
 * [DanmakuLifecycle] so they render immediately.
 */
interface DanmakuRenderer {

    /** Configure the renderer before the first [submit]. Idempotent. */
    fun prepare(config: DanmakuRenderConfig)

    /** Apply a new configuration at runtime. */
    fun setConfig(config: DanmakuRenderConfig)

    /** Submit a single danmaku for rendering. */
    fun submit(event: DanmakuEvent)

    /** Submit a batch of danmaku (e.g. a parsed timeline segment). */
    fun submitAll(events: List<DanmakuEvent>)

    /** Remove all danmaku, optionally keeping currently visible ones. */
    fun clear(clearOnScreen: Boolean = false)

    /** Freeze the animation clock (buffering, app background, pause). */
    fun pause()

    /** Resume the animation clock. */
    fun resume()

    /** Jump the animation clock to [positionMs]. */
    fun seekTo(positionMs: Long)

    /** Change scroll speed factor. */
    fun setSpeed(speed: Float)

    /** Install a filter; events rejected by it are dropped. */
    fun setFilter(filter: DanmakuFilter)

    /** Advance the frame clock by hand (used by timer-driven renderers). */
    fun onFrame(timestampMs: Long)

    /** Whether the renderer finished loading and is ready to [submit]. */
    fun isReady(): Boolean

    /** Invoked once when the renderer becomes ready. */
    fun setOnReadyListener(listener: (() -> Unit)?)

    /** Release native resources. */
    fun release()
}

/** Pure predicate deciding whether an event is accepted for rendering. */
fun interface DanmakuFilter {
    fun accept(event: DanmakuEvent): Boolean

    companion object {
        val ACCEPT_ALL: DanmakuFilter = DanmakuFilter { true }
    }
}
