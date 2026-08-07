package com.qx.orbit.ohos.platform

import com.tencent.kuikly.core.module.Module

/**
 * 凭证模块：Cookie 的读写/清理、登录态标记。
 * ArkTS 侧由 OrbitCredentialModule.ets 实现（基于 preferences 持久化）。
 */
class OrbitCredentialModule : Module() {
    companion object {
        const val MODULE_NAME = "OrbitCredentialModule"
    }

    override fun moduleName(): String = MODULE_NAME

    fun getCookie(cb: (String) -> Unit) {
        asyncToNativeMethod("getCookie", arrayOf<Any>()) { result ->
            cb(result as? String ?: "")
        }
    }

    fun setCookie(cookie: String) {
        asyncToNativeMethod("setCookie", arrayOf<Any>(cookie)) {}
    }

    fun clear() {
        asyncToNativeMethod("clear", arrayOf<Any>()) {}
    }

    fun isLoggedIn(cb: (Boolean) -> Unit) {
        asyncToNativeMethod("isLoggedIn", arrayOf<Any>()) { result ->
            cb(result == true)
        }
    }

    fun setLoggedIn(v: Boolean) {
        asyncToNativeMethod("setLoggedIn", arrayOf<Any>(v)) {}
    }
}
