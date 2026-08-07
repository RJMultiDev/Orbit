package com.qx.orbit.ohos.platform

import com.tencent.kuikly.core.module.Module

/**
 * 播放模块：桥接 ArkTS 侧 AVPlayer 实现（OrbitPlaybackModule.ets）。
 *
 * 当前为简化实现，仅播放音频（B 站 playurl dash 的 audio m4s 流）。
 * 视频画面需要 XComponent 提供的 Surface（avPlayer.setVideoSurface），页面侧尚未接入，
 * 待后续在播放页 XComponent 回调中补 surfaceId 桥接方法。
 */
class OrbitPlaybackModule : Module() {
    companion object {
        const val MODULE_NAME = "OrbitPlaybackModule"
    }

    override fun moduleName(): String = MODULE_NAME

    /** 开始播放；若已有播放中实例则重置后切换源。 */
    fun play(url: String, title: String) {
        asyncToNativeMethod("play", arrayOf<Any>(url, title)) {}
    }

    fun pause() {
        asyncToNativeMethod("pause", arrayOf<Any>()) {}
    }

    fun resume() {
        asyncToNativeMethod("resume", arrayOf<Any>()) {}
    }

    /** 跳转播放位置（毫秒）。 */
    fun seek(ms: Long) {
        asyncToNativeMethod("seek", arrayOf<Any>(ms)) {}
    }

    /** 释放播放器实例。 */
    fun release() {
        asyncToNativeMethod("release", arrayOf<Any>()) {}
    }

    /** 当前播放位置（毫秒）。 */
    fun getPosition(cb: (Long) -> Unit) {
        asyncToNativeMethod("getPosition", arrayOf<Any>()) { result ->
            cb((result as? Number)?.toLong() ?: 0L)
        }
    }

    /** 是否处于播放中状态。 */
    fun isPlaying(cb: (Boolean) -> Unit) {
        asyncToNativeMethod("isPlaying", arrayOf<Any>()) { result ->
            cb(result == true)
        }
    }
}
