package com.qx.orbit.core.sign

import java.net.URLDecoder

/**
 * jvm 平台的百分号解码：直接复用 `java.net.URLDecoder.decode(value, "UTF-8")`，
 * 与 Android `data/api/WbiSigner.kt` 中的解码行为完全一致。
 */
actual fun percentDecode(value: String): String =
    URLDecoder.decode(value, "UTF-8")
