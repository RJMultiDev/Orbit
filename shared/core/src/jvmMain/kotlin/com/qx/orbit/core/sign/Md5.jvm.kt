package com.qx.orbit.core.sign

import java.security.MessageDigest

/** jvm 平台的默认 MD5：java.security.MessageDigest，输出小写十六进制。 */
actual fun defaultMd5(): Md5 = JvmMd5

private object JvmMd5 : Md5 {
    override fun hex(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
