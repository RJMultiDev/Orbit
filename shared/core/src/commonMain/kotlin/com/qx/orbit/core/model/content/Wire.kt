package com.qx.orbit.core.model.content

import kotlin.math.absoluteValue
import kotlin.math.round

/**
 * Type-safe readers over raw wire (Bilibili JSON) field maps.
 *
 * These are shared by every `fromWire(fields: Map<String, Any?>)` factory in this
 * package. Values coming off the wire may be `Int`/`Long`/`Double`, `String`, `Boolean`
 * or nested `Map`/`List`, depending on the endpoint, so coercion is lenient and
 * falls back to the domain default when a key is missing or malformed.
 */
internal object Wire {

    /** Converts an arbitrary `Map<*, *>` (e.g. from a JSON object) into a `Map<String, Any?>`.
     *  Returns null if the value is not a map. */
    fun toStrMap(value: Any?): Map<String, Any?>? {
        if (value !is Map<*, *>) return null
        val out = LinkedHashMap<String, Any?>()
        for ((k, v) in value) {
            val key = k?.toString() ?: continue
            out[key] = v
        }
        return out
    }

    fun str(fields: Map<String, Any?>, vararg keys: String): String {
        for (k in keys) {
            val v = fields[k] ?: continue
            when (v) {
                is String -> return v
                is Number -> return v.toString()
                else -> return v.toString()
            }
        }
        return ""
    }

    /** Like [str] but returns null when the key is absent (keeps nullable domain fields nullable). */
    fun strOrNull(fields: Map<String, Any?>, vararg keys: String): String? {
        for (k in keys) {
            val v = fields[k] ?: continue
            if (v is String && v.isNotEmpty()) return v
            if (v is Number) return v.toString()
        }
        return null
    }

    fun long(fields: Map<String, Any?>, vararg keys: String): Long {
        for (k in keys) {
            val v = fields[k] ?: continue
            when (v) {
                is Number -> return v.toLong()
                is String -> v.toLongOrNull()?.let { return it }
                else -> {}
            }
        }
        return 0L
    }

    fun int(fields: Map<String, Any?>, vararg keys: String): Int {
        for (k in keys) {
            val v = fields[k] ?: continue
            when (v) {
                is Number -> return v.toInt()
                is String -> v.toIntOrNull()?.let { return it }
                else -> {}
            }
        }
        return 0
    }

    fun float(fields: Map<String, Any?>, vararg keys: String): Float {
        for (k in keys) {
            val v = fields[k] ?: continue
            when (v) {
                is Number -> return v.toFloat()
                is String -> v.toFloatOrNull()?.let { return it }
                else -> {}
            }
        }
        return 0f
    }

    fun floatOrNull(fields: Map<String, Any?>, vararg keys: String): Float? {
        for (k in keys) {
            val v = fields[k] ?: continue
            when (v) {
                is Number -> return v.toFloat()
                is String -> v.toFloatOrNull()?.let { return it }
                else -> {}
            }
        }
        return null
    }

    fun bool(fields: Map<String, Any?>, vararg keys: String): Boolean {
        for (k in keys) {
            val v = fields[k] ?: continue
            when (v) {
                is Boolean -> return v
                is Number -> return v.toInt() != 0
                is String -> return when (v.lowercase()) {
                    "true", "1" -> true
                    else -> false
                }
                else -> {}
            }
        }
        return false
    }

    fun map(fields: Map<String, Any?>, vararg keys: String): Map<String, Any?>? {
        for (k in keys) {
            toStrMap(fields[k])?.let { return it }
        }
        return null
    }

    fun strList(fields: Map<String, Any?>, vararg keys: String): List<String> {
        for (k in keys) {
            val v = fields[k]
            if (v is List<*>) {
                return v.mapNotNull { item ->
                    when (item) {
                        is String -> item
                        is Number -> item.toString()
                        else -> null
                    }
                }
            }
        }
        return emptyList()
    }

    fun longList(fields: Map<String, Any?>, vararg keys: String): List<Long> {
        for (k in keys) {
            val v = fields[k]
            if (v is List<*>) {
                return v.mapNotNull { item ->
                    when (item) {
                        is Number -> item.toLong()
                        is String -> item.toLongOrNull()
                        else -> null
                    }
                }
            }
        }
        return emptyList()
    }

    fun intList(fields: Map<String, Any?>, vararg keys: String): List<Int> {
        for (k in keys) {
            val v = fields[k]
            if (v is List<*>) {
                return v.mapNotNull { item ->
                    when (item) {
                        is Number -> item.toInt()
                        is String -> item.toIntOrNull()
                        else -> null
                    }
                }
            }
        }
        return emptyList()
    }

    fun mapList(fields: Map<String, Any?>, vararg keys: String): List<Map<String, Any?>> {
        for (k in keys) {
            val v = fields[k]
            if (v is List<*>) {
                return v.mapNotNull { toStrMap(it) }
            }
        }
        return emptyList()
    }

    fun longMap(fields: Map<String, Any?>, vararg keys: String): Map<String, Long> {
        for (k in keys) {
            val v = fields[k]
            if (v is Map<*, *>) {
                return v.entries.mapNotNull { (kk, vv) ->
                    val key = kk?.toString() ?: return@mapNotNull null
                    val value = when (vv) {
                        is Number -> vv.toLong()
                        is String -> vv.toLongOrNull() ?: return@mapNotNull null
                        else -> return@mapNotNull null
                    }
                    key to value
                }.toMap()
            }
        }
        return emptyMap()
    }

    /** Matches Android `StringUtil.toTime`: 62 -> "01:02", 3725 -> "1:02:05". */
    fun formatDuration(totalSeconds: Int): String {
        if (totalSeconds <= 0) return "00:00"
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) "$h:${m.twoDigits()}:${s.twoDigits()}" else "${m.twoDigits()}:${s.twoDigits()}"
    }

    /** Matches Android `StringUtil.toWan` formatting (one decimal). */
    fun toWan(num: Long): String = when {
        num >= 100_000_000 -> "${(num / 100_000_000.0).oneDecimal()}亿"
        num >= 10_000 -> "${(num / 10_000.0).oneDecimal()}万"
        else -> num.toString()
    }

    private fun Int.twoDigits(): String = if (this < 10) "0$this" else toString()

    private fun Double.oneDecimal(): String {
        val scaled = round(this * 10).toLong()
        val whole = scaled / 10
        val frac = (scaled % 10).absoluteValue
        return "$whole.$frac"
    }
}
