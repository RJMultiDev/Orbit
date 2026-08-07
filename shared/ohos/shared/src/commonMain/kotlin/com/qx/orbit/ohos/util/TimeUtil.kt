package com.qx.orbit.ohos.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ptr
import platform.posix.gettimeofday
import platform.posix.timeval

/**
 * 时间工具（对应 Android util/TimeUtil.kt 的 formatBiliTime）。
 * 纯 Kotlin：wall-clock 用 posix gettimeofday，日历字段用公历算法推导，无 java.time。
 */
object TimeUtil {

    /** 本地时区相对 UTC 的偏移（分钟）。默认 0 = UTC；可由 ArkTS 时区桥注入。 */
    var timezoneOffsetMinutes: Int = 0

    /** 当前系统时间戳（毫秒）。 */
    @OptIn(ExperimentalForeignApi::class)
    fun nowMillis(): Long {
        val tv = timeval()
        gettimeofday(tv.ptr, null)
        return tv.tv_sec * 1000L + tv.tv_usec / 1000L
    }

    /**
     * B 站时间戳相对格式化（对应 Android formatBiliTime）：
     * 同一天：刚刚 / N分钟前 / N小时前；昨天：昨天 HH:mm；
     * 2~7 天前：N天前；同年更早：M月d日；跨年：yyyy年M月d日。
     */
    fun formatBiliTime(timestampSeconds: Long): String {
        val targetMillis = timestampSeconds * 1000L
        val now = nowMillis()
        val target = epochToFields(targetMillis)
        val current = epochToFields(now)

        val diffMillis = now - targetMillis
        val diffMinutes = diffMillis / (60 * 1000)
        val diffHours = diffMillis / (60 * 60 * 1000)

        val dayDiff = (current.dayOfYear - target.dayOfYear) +
            (current.year - target.year) * 365

        return if (current.year == target.year) {
            if (current.dayOfYear == target.dayOfYear) {
                when {
                    diffMinutes < 1L -> "刚刚"
                    diffMinutes < 60L -> "${diffMinutes}分钟前"
                    else -> "${diffHours}小时前"
                }
            } else if (dayDiff == 1) {
                "昨天 ${pad(target.hour)}:${pad(target.minute)}"
            } else if (dayDiff in 2..7) {
                "${dayDiff}天前"
            } else {
                "${target.month}月${target.day}日"
            }
        } else {
            "${target.year}年${target.month}月${target.day}日"
        }
    }

    private class DateFields(
        val year: Int,
        val month: Int,     // 1..12
        val day: Int,       // 1..31
        val dayOfYear: Int, // 1..366
        val hour: Int,
        val minute: Int,
    )

    private fun epochToFields(epochMillis: Long): DateFields {
        val localMillis = epochMillis + timezoneOffsetMinutes.toLong() * 60_000L
        val days = floorDiv(localMillis, 86_400_000L)
        val daySeconds = floorMod(localMillis, 86_400_000L)
        val hour = (daySeconds / 3_600_000L).toInt()
        val minute = ((daySeconds % 3_600_000L) / 60_000L).toInt()

        val (year, month, day, dayOfYear) = civilFromDays(days)
        return DateFields(year, month, day, dayOfYear, hour, minute)
    }

    /** Howard Hinnant `civil_from_days`：天数 → (year, month, day, dayOfYear)。 */
    private fun civilFromDays(days: Long): FourTuple {
        val z = days + 719468
        val era = (if (z >= 0) z else z - 146096) / 146097
        val doe = z - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = if (mp < 10) mp + 3 else mp - 9
        return FourTuple(if (m <= 2) y + 1 else y, m.toInt(), d.toInt(), doy.toInt() + 1)
    }

    private data class FourTuple(val year: Int, val month: Int, val day: Int, val dayOfYear: Int)

    private fun pad(v: Int): String = v.toString().padStart(2, '0')

    private fun floorDiv(a: Long, b: Long): Long {
        var q = a / b
        if ((a xor b) < 0 && a % b != 0L) q--
        return q
    }

    private fun floorMod(a: Long, b: Long): Long {
        var r = a % b
        if (r != 0L && (a xor b) < 0) r += b
        return r
    }
}
