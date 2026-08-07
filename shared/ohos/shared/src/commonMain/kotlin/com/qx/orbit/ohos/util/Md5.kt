package com.qx.orbit.ohos.util

import kotlin.math.abs
import kotlin.math.sin

/**
 * 纯 Kotlin MD5（Kotlin/Native 无 java.security，逐字节对齐 Android MessageDigest）。
 */
object Md5 {
    private val K: IntArray = IntArray(64) { i ->
        (abs(sin((i + 1).toDouble())) * 4294967296.0).toLong().toInt()
    }

    private val S = intArrayOf(
        7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
        5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
        4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
        6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21,
    )

    private fun rotl(x: Int, c: Int): Int = (x shl c) or (x ushr (32 - c))

    private fun add(x: Int, y: Int): Int {
        val lsw = (x and 0xffff) + (y and 0xffff)
        val msw = (x ushr 16) + (y ushr 16) + (lsw ushr 16)
        return (msw shl 16) or (lsw and 0xffff)
    }

    /** 字符串 → UTF-8 字节（手动处理代理对）。 */
    private fun utf8Bytes(s: String): ByteArray {
        val out = ArrayList<Byte>(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i].code
            var cp = c
            if (c >= 0xd800 && c <= 0xdbff && i + 1 < s.length) {
                val low = s[i + 1].code
                if (low >= 0xdc00 && low <= 0xdfff) {
                    cp = 0x10000 + ((c - 0xd800) shl 10) + (low - 0xdc00)
                    i++
                }
            }
            when {
                cp < 0x80 -> out.add(cp.toByte())
                cp < 0x800 -> {
                    out.add((0xc0 or (cp shr 6)).toByte())
                    out.add((0x80 or (cp and 0x3f)).toByte())
                }
                cp < 0x10000 -> {
                    out.add((0xe0 or (cp shr 12)).toByte())
                    out.add((0x80 or ((cp shr 6) and 0x3f)).toByte())
                    out.add((0x80 or (cp and 0x3f)).toByte())
                }
                else -> {
                    out.add((0xf0 or (cp shr 18)).toByte())
                    out.add((0x80 or ((cp shr 12) and 0x3f)).toByte())
                    out.add((0x80 or ((cp shr 6) and 0x3f)).toByte())
                    out.add((0x80 or (cp and 0x3f)).toByte())
                }
            }
            i++
        }
        return out.toByteArray()
    }

    /** 计算 MD5，返回 32 位小写十六进制。 */
    fun hex(input: String): String {
        val bytes = utf8Bytes(input)
        val bitLen = bytes.size.toLong() * 8
        val padded = bytes.copyOf(bytes.size + 1)
        padded[bytes.size] = 0x80.toByte()
        var len = padded.size
        while (len % 64 != 56) {
            len++
        }
        val buf = padded.copyOf(len + 8)
        buf[len] = (bitLen and 0xff).toByte()
        buf[len + 1] = ((bitLen ushr 8) and 0xff).toByte()
        buf[len + 2] = ((bitLen ushr 16) and 0xff).toByte()
        buf[len + 3] = ((bitLen ushr 24) and 0xff).toByte()
        buf[len + 4] = 0
        buf[len + 5] = 0
        buf[len + 6] = 0
        buf[len + 7] = 0

        var a0 = 0x67452301
        var b0 = -0x10325477 // 0xefcdab89
        var c0 = -0x67452302 // 0x98badcfe
        var d0 = 0x10325476

        var off = 0
        while (off < buf.size) {
            val m = IntArray(16)
            for (j in 0 until 16) {
                val idx = off + j * 4
                m[j] = (buf[idx].toInt() and 0xff) or
                    ((buf[idx + 1].toInt() and 0xff) shl 8) or
                    ((buf[idx + 2].toInt() and 0xff) shl 16) or
                    ((buf[idx + 3].toInt() and 0xff) shl 24)
            }
            var a = a0
            var b = b0
            var c = c0
            var d = d0
            for (i in 0 until 64) {
                var f: Int
                var g: Int
                when {
                    i < 16 -> {
                        f = (b and c) or (b.inv() and d)
                        g = i
                    }
                    i < 32 -> {
                        f = (d and b) or (d.inv() and c)
                        g = (5 * i + 1) % 16
                    }
                    i < 48 -> {
                        f = b xor c xor d
                        g = (3 * i + 5) % 16
                    }
                    else -> {
                        f = c xor (b or d.inv())
                        g = (7 * i) % 16
                    }
                }
                val temp = d
                d = c
                c = b
                b = add(b, rotl(add(a, add(f, add(K[i], m[g]))), S[i]))
                a = temp
            }
            a0 = add(a0, a)
            b0 = add(b0, b)
            c0 = add(c0, c)
            d0 = add(d0, d)
            off += 64
        }

        val words = intArrayOf(a0, b0, c0, d0)
        val sb = StringBuilder(32)
        for (w in words) {
            for (j in 0 until 4) {
                val b = (w ushr (j * 8)) and 0xff
                if (b < 16) sb.append('0')
                sb.append(b.toString(16))
            }
        }
        return sb.toString()
    }
}
