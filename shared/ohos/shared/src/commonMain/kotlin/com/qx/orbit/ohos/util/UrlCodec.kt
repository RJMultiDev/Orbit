package com.qx.orbit.ohos.util

/**
 * URL 编码（对齐 shared/core PercentEncoding）：
 *  - encodeUriComponent：WBI 用（空格→%20，~!'( ) 不转义）
 *  - encodeFormComponent：AppSign 用（空格→+）
 *  - percentDecode：+→空格，%XX UTF-8 解码
 */
object UrlCodec {

    fun encodeUriComponent(s: String): String = encode(s, isWbiUnreserved = true)

    fun encodeFormComponent(s: String): String = encode(s, isWbiUnreserved = false)

    private fun isAlphaNum(cp: Int): Boolean =
        (cp in 0x41..0x5a) || (cp in 0x61..0x7a) || (cp in 0x30..0x39)

    private fun encode(s: String, isWbiUnreserved: Boolean): String {
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i].code
            var cp = c
            var charCount = 1
            if (c in 0xd800..0xdbff && i + 1 < s.length) {
                val low = s[i + 1].code
                if (low in 0xdc00..0xdfff) {
                    cp = 0x10000 + ((c - 0xd800) shl 10) + (low - 0xdc00)
                    charCount = 2
                }
            }
            if (cp == 0x20) {
                sb.append(if (isWbiUnreserved) "%20" else "+")
            } else if (isAlphaNum(cp) || cp == '-'.code || cp == '_'.code || cp == '.'.code || cp == '*'.code) {
                appendCodePoint(sb, cp)
            } else if (isWbiUnreserved && (cp == '~'.code || cp == '!'.code ||
                    cp == '\''.code || cp == '('.code || cp == ')'.code)) {
                appendCodePoint(sb, cp)
            } else {
                val bytes = utf8Bytes(cp)
                for (b in bytes) {
                    val v = b.toInt() and 0xff
                    sb.append('%')
                    sb.append("0123456789ABCDEF"[v ushr 4 and 0xf])
                    sb.append("0123456789ABCDEF"[v and 0xf])
                }
            }
            i += charCount
        }
        return sb.toString()
    }

    private fun appendCodePoint(sb: StringBuilder, cp: Int) {
        if (cp < 0x10000) {
            sb.append(cp.toChar())
        } else {
            val v = cp - 0x10000
            sb.append((0xd800 + (v ushr 10)).toChar())
            sb.append((0xdc00 + (v and 0x3ff)).toChar())
        }
    }

    private fun utf8Bytes(cp: Int): ByteArray = when {
        cp < 0x80 -> byteArrayOf(cp.toByte())
        cp < 0x800 -> byteArrayOf(
            (0xc0 or (cp shr 6)).toByte(),
            (0x80 or (cp and 0x3f)).toByte(),
        )
        cp < 0x10000 -> byteArrayOf(
            (0xe0 or (cp shr 12)).toByte(),
            (0x80 or ((cp shr 6) and 0x3f)).toByte(),
            (0x80 or (cp and 0x3f)).toByte(),
        )
        else -> byteArrayOf(
            (0xf0 or (cp shr 18)).toByte(),
            (0x80 or ((cp shr 12) and 0x3f)).toByte(),
            (0x80 or ((cp shr 6) and 0x3f)).toByte(),
            (0x80 or (cp and 0x3f)).toByte(),
        )
    }

    /** 百分号解码：+→空格，%XX 按 UTF-8 解码；非法序列原样保留。 */
    fun percentDecode(s: String): String {
        val out = StringBuilder(s.length)
        val bytes = ArrayList<Byte>()
        var i = 0
        fun flush() {
            if (bytes.isNotEmpty()) {
                out.append(bytesToUtf8(bytes))
                bytes.clear()
            }
        }
        while (i < s.length) {
            val c = s[i]
            when {
                c == '+' -> {
                    flush()
                    out.append(' ')
                    i++
                }
                c == '%' && i + 2 < s.length -> {
                    val hi = hexVal(s[i + 1])
                    val lo = hexVal(s[i + 2])
                    if (hi >= 0 && lo >= 0) {
                        bytes.add((hi shl 4 or lo).toByte())
                        i += 3
                    } else {
                        flush()
                        out.append(c)
                        i++
                    }
                }
                else -> {
                    flush()
                    out.append(c)
                    i++
                }
            }
        }
        flush()
        return out.toString()
    }

    private fun bytesToUtf8(bytes: List<Byte>): String {
        val sb = StringBuilder(bytes.size)
        var i = 0
        val arr = bytes
        while (i < arr.size) {
            val b = arr[i].toInt() and 0xff
            when {
                b < 0x80 -> {
                    sb.append(b.toChar())
                    i++
                }
                b and 0xe0 == 0xc0 && i + 1 < arr.size -> {
                    sb.append(((b and 0x1f) shl 6 or (arr[i + 1].toInt() and 0x3f)).toChar())
                    i += 2
                }
                b and 0xf0 == 0xe0 && i + 2 < arr.size -> {
                    sb.append(((b and 0x0f) shl 12 or
                        ((arr[i + 1].toInt() and 0x3f) shl 6) or
                        (arr[i + 2].toInt() and 0x3f)).toChar())
                    i += 3
                }
                b and 0xf8 == 0xf0 && i + 3 < arr.size -> {
                    val cp = (b and 0x07) shl 18 or
                        ((arr[i + 1].toInt() and 0x3f) shl 12) or
                        ((arr[i + 2].toInt() and 0x3f) shl 6) or
                        (arr[i + 3].toInt() and 0x3f)
                    if (cp < 0x10000) {
                        sb.append(cp.toChar())
                    } else {
                        val v = cp - 0x10000
                        sb.append((0xd800 + (v ushr 10)).toChar())
                        sb.append((0xdc00 + (v and 0x3ff)).toChar())
                    }
                    i += 4
                }
                else -> {
                    sb.append(b.toChar())
                    i++
                }
            }
        }
        return sb.toString()
    }

    private fun hexVal(c: Char): Int = when (c) {
        in '0'..'9' -> c - '0'
        in 'a'..'f' -> c - 'a' + 10
        in 'A'..'F' -> c - 'A' + 10
        else -> -1
    }
}
