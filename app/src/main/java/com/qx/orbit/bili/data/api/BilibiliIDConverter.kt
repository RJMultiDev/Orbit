package com.qx.orbit.bili.data.api

object BilibiliIDConverter {
    private const val TABLE = "fZodR9XQDSUm21yCkr6zBqiveYah8bt4xsWpHnJE7jL5VG3guMTKNPAwcF"
    private val S = intArrayOf(11, 10, 3, 8, 4, 6)
    private const val XOR = 177451812L
    private const val ADD = 8728348608L
    private val TR = HashMap<Char, Int>().apply {
        for (i in 0 until 58) this[TABLE[i]] = i
    }

    fun bvToAid(bv: String): Long {
        var x = 0L
        for (i in 0 until 6) {
            TR[bv[S[i]]]?.let { x += (it.toLong() * Math.pow(58.0, i.toDouble())).toLong() }
        }
        return (x - ADD) xor XOR
    }

    fun aidToBv(aid: Long): String {
        var x = (aid xor XOR) + ADD
        val r = StringBuilder("BV1  4 1 7  ")
        for (i in 0 until 6) {
            r.setCharAt(S[i], TABLE[((x / Math.pow(58.0, i.toDouble())) % 58).toInt()])
        }
        return r.toString()
    }
}
