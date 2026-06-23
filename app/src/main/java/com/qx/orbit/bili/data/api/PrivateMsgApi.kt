package com.qx.orbit.bili.data.api

import com.qx.orbit.bili.data.model.*
import com.qx.orbit.bili.data.remote.CookieManager
import com.qx.orbit.bili.data.remote.GsonConfig
import com.qx.orbit.bili.data.remote.HttpClient
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONObject

object PrivateMsgApi {

    internal data class SessionListData(
        @SerializedName("session_list") val session_list: List<SessionItem>? = null
    )

    internal data class SessionItem(
        @SerializedName("talker_id") val talker_id: Long = 0,
        @SerializedName("unread_count") val unread_count: Int = 0,
        @SerializedName("last_msg") val last_msg: LastMsg? = null,
        @SerializedName("session_type") val session_type: Int = 0
    )

    internal data class LastMsg(
        @SerializedName("msg_type") val msg_type: Int = 0,
        @SerializedName("content") val content: String? = null,
        @SerializedName("timestamp") val timestamp: Long = 0,
        @SerializedName("sender_uid") val sender_uid: Long = 0,
        @SerializedName("msg_seqno") val msg_seqno: Long = 0
    )

    internal data class FetchMsgData(
        @SerializedName("messages") val messages: List<JSONObject>? = null
    )

    internal data class UserCardsData(
        @SerializedName("cards") val cards: List<UserCardItem>? = null
    )

    internal data class UserCardItem(
        @SerializedName("mid") val mid: Long = 0,
        @SerializedName("name") val name: String? = null,
        @SerializedName("face") val face: String? = null,
        @SerializedName("sign") val sign: String? = null,
        @SerializedName("fans") val fans: Int = 0,
        @SerializedName("attention") val attention: Int = 0
    )

    suspend fun getPrivateMsg(talkerId: Long, size: Int, beginSeqno: Long, endSeqno: Long): JSONObject = withContext(Dispatchers.IO) {
        val url = "https://api.vc.bilibili.com/svr_sync/v1/svr_sync/fetch_session_msgs?talker_id=$talkerId&session_type=1&size=$size&begin_seqno=$beginSeqno&end_seqno=$endSeqno"
        val json = httpGet(url)
        JSONObject(json)
    }

    suspend fun getPrivateMsgList(allMsgJson: JSONObject): List<PrivateMessage> = withContext(Dispatchers.IO) {
        val messages = allMsgJson.optJSONObject("data")?.optJSONArray("messages") ?: return@withContext emptyList()
        val list = mutableListOf<PrivateMessage>()
        for (i in 0 until messages.length()) {
            val msg = messages.optJSONObject(i) ?: continue
            val content = msg.optJSONObject("content") ?: JSONObject()
            val senderUid = msg.optLong("sender_uid", 0)
            val msgType = msg.optInt("msg_type", 0)
            val timestamp = msg.optLong("timestamp", 0)
            val msgId = msg.optLong("msg_seqno", 0)
            list.add(PrivateMessage(
                content = content,
                type = msgType,
                timestamp = timestamp,
                uid = senderUid,
                msgId = msgId,
                msgSeqno = msgId
            ))
        }
        list
    }

    suspend fun getUsersInfo(uidList: List<Long>): Map<Long, UserInfo> = withContext(Dispatchers.IO) {
        if (uidList.isEmpty()) return@withContext emptyMap()
        val uids = uidList.joinToString(",")
        val url = "https://api.bilibili.com/account/v1/user/cards?uids=$uids"
        val json = httpGet(url)
        val type = object : TypeToken<ApiResponse<UserCardsData>>() {}.type
        val resp: ApiResponse<UserCardsData>? = GsonConfig.gson.fromJson(json, type)
        if (resp == null || !resp.isSuccess || resp.data == null) return@withContext emptyMap()
        resp.data.cards?.associate { card ->
            card.mid to UserInfo(
                mid = card.mid,
                name = card.name ?: "",
                avatar = card.face ?: "",
                sign = card.sign ?: "",
                fans = card.fans,
                following = card.attention
            )
        } ?: emptyMap()
    }

    suspend fun getSessionsList(size: Int): List<PrivateMsgSession> = withContext(Dispatchers.IO) {
        val url = "https://api.vc.bilibili.com/session_svr/v1/session_svr/get_sessions?size=$size&session_type=1"
        val json = httpGet(url)
        val type = object : TypeToken<ApiResponse<SessionListData>>() {}.type
        val resp: ApiResponse<SessionListData>? = GsonConfig.gson.fromJson(json, type)
        if (resp == null || !resp.isSuccess || resp.data == null) return@withContext emptyList()
        resp.data.session_list?.map { session ->
            val contentJson = session.last_msg?.content?.let {
                try { JSONObject(it) } catch (_: Exception) { null }
            }
            PrivateMsgSession(
                talkerUid = session.talker_id,
                unread = session.unread_count,
                contentType = session.last_msg?.msg_type ?: 0,
                content = contentJson
            )
        } ?: emptyList()
    }

    suspend fun sendMsg(senderUid: Long, receiverUid: Long, msgType: Int, timestamp: Long, content: String): JSONObject = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("msg_type", msgType.toString())
            .add("msg_content", content)
            .add("sender_uid", senderUid.toString())
            .add("receiver_uid", receiverUid.toString())
            .add("timestamp", timestamp.toString())
            .add("csrf", CookieManager.getCsrf())
            .build()
        val request = Request.Builder()
            .url("https://api.vc.bilibili.com/web_im/v1/web_im/send_msg")
            .post(body)
            .addHeader("Cookie", CookieManager.getCookie())
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Referer", "https://message.bilibili.com/")
            .build()
        val json = HttpClient.client.newCall(request).execute().body?.string() ?: ""
        JSONObject(json)
    }

    suspend fun updateAck(talkerId: Long, sessionType: Int, ackSeqno: Long): JSONObject = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("talker_id", talkerId.toString())
            .add("session_type", sessionType.toString())
            .add("ack_seqno", ackSeqno.toString())
            .add("csrf", CookieManager.getCsrf())
            .build()
        val request = Request.Builder()
            .url("https://api.vc.bilibili.com/session_svr/v1/session_svr/update_ack")
            .post(body)
            .addHeader("Cookie", CookieManager.getCookie())
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Referer", "https://message.bilibili.com/")
            .build()
        val json = HttpClient.client.newCall(request).execute().body?.string() ?: ""
        JSONObject(json)
    }

    private fun httpGet(url: String): String {
        val request = Request.Builder().url(url)
            .addHeader("Cookie", CookieManager.getCookie())
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Referer", "https://www.bilibili.com/")
            .build()
        return HttpClient.client.newCall(request).execute().body?.string() ?: ""
    }

    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.95 Safari/537.36"
}
