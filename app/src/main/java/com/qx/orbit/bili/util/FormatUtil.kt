package com.qx.orbit.bili.util

fun formatCount(count: Int): String {
    return if (count >= 10000) {
        String.format("%.1fw", count / 10000f)
    } else {
        count.toString()
    }
}
