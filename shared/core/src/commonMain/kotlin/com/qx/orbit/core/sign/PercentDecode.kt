package com.qx.orbit.core.sign

/**
 * 百分号解码，语义对齐 Android `java.net.URLDecoder.decode(value, "UTF-8")`：
 * `+` 解码为空格，`%XX` 按字节序列解码为 UTF-8 字符；非法 `%` 序列原样保留。
 *
 * 用于 `WbiSigner.signUrl` 解析 query 后再重新编码的规范化步骤。
 * jvm actual 直接用 `URLDecoder` 保证与 Android 逐字节一致。
 */
expect fun percentDecode(value: String): String
