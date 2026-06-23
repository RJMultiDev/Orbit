package com.qx.orbit.bili.data.remote

import android.content.Context
import android.content.SharedPreferences

object CookieManager {
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("orbit_cookies", Context.MODE_PRIVATE)
    }

    fun getCookie(): String = prefs.getString("cookie", "") ?: ""
    fun setCookie(cookie: String) = prefs.edit().putString("cookie", cookie).apply()
    fun getCsrf(): String = getInfoFromCookie("bili_jct")
    fun getMid(): Long = prefs.getLong("mid", 0)
    fun setMid(mid: Long) = prefs.edit().putLong("mid", mid).apply()

    fun getInfoFromCookie(name: String): String {
        val cookie = getCookie()
        cookie.split("; ").forEach { part ->
            if (part.startsWith("$name=")) {
                return part.substring(name.length + 1)
            }
        }
        return ""
    }

    fun putCookie(key: String, value: String) {
        val cookies = mutableMapOf<String, String>()
        getCookie().split("; ").filter { it.contains("=") }.forEach { part ->
            val eqIdx = part.indexOf("=")
            cookies[part.substring(0, eqIdx)] = part.substring(eqIdx + 1)
        }
        cookies[key] = value
        setCookie(cookies.entries.joinToString("; ") { "${it.key}=${it.value}" })
    }
}
