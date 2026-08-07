package com.qx.orbit.ohos

/**
 * 平台占位信息 —— 保持纯净 Kotlin（无 Android/Harmony 依赖）。
 * 后续由各平台 adapter 提供真实实现（安全存储、网络、媒体、文件系统等）。
 */
data class AppInfo(
    val name: String = "Orbit",
    val versionName: String = "0.1.0",
    val buildType: String = "debug",
)

/** 平台能力占位。TODO(A04/后续阶段): 注入 credentialStore / httpClient / playback 等 platform ports。 */
object Platform {
    /** 当前应用的元信息占位。TODO(A04/后续): 从各平台宿主注入真实值。 */
    fun appInfo(): AppInfo = AppInfo()
}
