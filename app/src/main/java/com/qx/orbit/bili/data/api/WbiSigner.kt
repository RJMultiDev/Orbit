package com.qx.orbit.bili.data.api

import com.qx.orbit.bili.data.remote.CookieManager
import com.qx.orbit.bili.data.remote.GsonConfig
import com.qx.orbit.bili.data.remote.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.security.MessageDigest

object WbiSigner {
    private var mixinKey: String? = null
    private val MIXIN_KEY_ENC_TAB = intArrayOf(
        46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
        27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13,
        37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4,
        22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52
    )

    suspend fun signUrl(url: String): String = withContext(Dispatchers.IO) {
        val key = getMixinKey()
        val separator = if (url.contains("?")) "&" else "?"
        val ts = System.currentTimeMillis() / 1000
        val params = Regex("[?&]([^&]+)").findAll(url).map { it.groupValues[1] }.toList()
        val sortedParams = (params + "wts=$ts").sorted().joinToString("&")
        val wrid = md5(sortedParams + key)
        "$url${separator}w_rid=$wrid&wts=$ts"
    }

    private suspend fun getMixinKey(): String {
        mixinKey?.let { return it }
        val request = Request.Builder()
            .url("https://api.bilibili.com/x/web-interface/nav")
            .addHeader("Cookie", CookieManager.getCookie())
            .addHeader("User-Agent", USER_AGENT)
            .build()
        val response = HttpClient.client.newCall(request).execute()
        val body = response.body?.string() ?: return ""
        @Suppress("UNCHECKED_CAST")
        val json = GsonConfig.gson.fromJson(body, Map::class.java) as? Map<String, Any?> ?: return ""
        @Suppress("UNCHECKED_CAST")
        val data = json["data"] as? Map<String, Any?> ?: return ""
        @Suppress("UNCHECKED_CAST")
        val wbiImg = data["wbi_img"] as? Map<String, Any?> ?: return ""
        val imgUrl = wbiImg["img_url"] as? String ?: return ""
        val subUrl = wbiImg["sub_url"] as? String ?: return ""
        val rawKey = imgUrl.substringAfterLast("/").substringBefore(".") +
                subUrl.substringAfterLast("/").substringBefore(".")
        val key = MIXIN_KEY_ENC_TAB.map { index -> if (index < rawKey.length) rawKey[index].toString() else "" }.joinToString("").take(32)
        mixinKey = key
        return key
    }

    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.95 Safari/537.36"
}
