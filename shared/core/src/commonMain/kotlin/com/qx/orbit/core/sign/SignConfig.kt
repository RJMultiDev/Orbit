package com.qx.orbit.core.sign

/**
 * 签名纯算法（WBI / AppSign）所需的注入点定义，全部平台无关。
 * 时钟、WBI mixin key 与 MD5 摘要由调用方（Android / Harmony adapter）注入。
 */

/** 返回当前 epoch 秒（对齐 Android 的 `System.currentTimeMillis() / 1000`）。 */
fun interface Clock {
    fun epochSecond(): Long
}

/** 提供 WBI mixin key（由 img_key + sub_key 打乱重排后取前 32 位得到）。 */
fun interface WbiKeyProvider {
    fun mixinKey(): String
}

/**
 * MD5 摘要接口：输入任意字符串（按 UTF-8 编码），输出小写十六进制。
 * 与 Android `MessageDigest.getInstance("MD5")` 后逐字节 `%02x` 拼接的输出完全一致。
 */
fun interface Md5 {
    fun hex(input: String): String
}

/**
 * [WbiSigner] 的注入配置。
 *
 * @param clock 时钟，签名时取当前 epoch 秒作为 wts。
 * @param wbiKeyProvider mixin key 提供者。
 * @param md5 MD5 摘要实现（jvm 平台默认见 [defaultMd5]）。
 */
data class SignerConfig(
    val clock: Clock,
    val wbiKeyProvider: WbiKeyProvider,
    val md5: Md5,
)

/**
 * 平台默认 MD5 实现。jvm 实际使用 `java.security.MessageDigest`
 * （见 jvmMain 的 Md5.jvm.kt）；commonTest 可注入 fake 或直接使用本默认实现。
 */
expect fun defaultMd5(): Md5
