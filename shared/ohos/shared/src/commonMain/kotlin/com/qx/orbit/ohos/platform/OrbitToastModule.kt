package com.qx.orbit.ohos.platform

import com.tencent.kuikly.core.module.Module

/**
 * Toast 提示模块：桥接 ArkTS 侧 OrbitToastModule.ets（promptAction.showToast）。
 */
class OrbitToastModule : Module() {
    companion object {
        const val MODULE_NAME = "OrbitToastModule"
    }

    override fun moduleName(): String = MODULE_NAME

    fun show(text: String) {
        asyncToNativeMethod("show", arrayOf<Any>(text)) {}
    }
}
