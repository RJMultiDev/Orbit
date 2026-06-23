package com.qx.orbit.bili.data.api

import com.qx.orbit.bili.data.remote.CookieManager
import com.qx.orbit.bili.data.remote.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request

object HeartbeatApi {

    suspend fun reportHeartbeat(
        aid: Long,
        bvid: String,
        cid: Long,
        playedTime: Long,
        startTs: Long = System.currentTimeMillis() / 1000,
        realtime: Long = playedTime
    ) = withContext(Dispatchers.IO) {
        val mid = CookieManager.getMid()
        val csrf = CookieManager.getCsrf()
        if (csrf.isBlank()) return@withContext // Not logged in

        val body = FormBody.Builder()
            .add("aid", aid.toString())
            .add("bvid", bvid)
            .add("cid", cid.toString())
            .add("mid", mid.toString())
            .add("csrf", csrf)
            .add("played_time", playedTime.toString())
            .add("realtime", realtime.toString())
            .add("start_ts", startTs.toString())
            .add("type", "3")
            .add("dt", "2")
            .add("play_type", "0")
            .build()
            
        val request = Request.Builder()
            .url("https://api.bilibili.com/x/click-interface/web/heartbeat")
            .post(body)
            .build()
            
        try {
            HttpClient.client.newCall(request).execute().close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
