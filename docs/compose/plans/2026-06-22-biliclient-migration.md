# BiliClient → Orbit Migration Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use compose:subagent (recommended) or compose:execute to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate all API, model, and utility code from BiliClient (Java) to Orbit (Kotlin + Retrofit), creating a clean Wear OS Bilibili client foundation.

**Architecture:** Three-layer structure under `com.qx.orbit.bili.data`: `api/` (Retrofit interfaces), `model/` (Kotlin data classes), `remote/` (OkHttp client, Gson config, cookie management). Models are simplified data classes without Parcelable (Wear OS doesn't need IPC). APIs become Retrofit suspend functions.

**Tech Stack:** Kotlin, Retrofit + OkHttp, Gson, Coroutines

---

## File Structure

```
app/src/main/java/com/qx/orbit/bili/data/
├── api/
│   ├── BiliApiService.kt          # Main Retrofit interface (all endpoints)
│   └── WbiSigner.kt               # WBI signing utility
├── model/
│   ├── ApiResponse.kt             # Generic response wrapper
│   ├── User.kt                    # UserInfo, VipInfo, OfficialInfo
│   ├── Video.kt                   # VideoInfo, VideoCard, Stats, PageData, StaffData
│   ├── Player.kt                  # PlayerData, DashData, DashVideoStream, DashAudioStream, Subtitle, ViewPoint, HighEnergyData
│   ├── Live.kt                    # LiveRoom, LivePlayInfo, LiveInfo
│   ├── Bangumi.kt                 # Bangumi, Timeline
│   ├── Dynamic.kt                 # Dynamic, Opus, OpusParagraph, OpusCard
│   ├── Reply.kt                   # Reply, ContentType
│   ├── Message.kt                 # MessageCard, PrivateMessage, PrivateMsgSession
│   ├── Search.kt                  # SearchData, SearchResult
│   ├── Favorite.kt                # FavoriteFolder, Collection, Series
│   ├── History.kt                 # (VideoCard reuse)
│   ├── Article.kt                 # ArticleInfo, ArticleCard, ArticleLine
│   ├── Emote.kt                   # Emote, EmotePackage
│   ├── Interaction.kt             # InteractionVideoData
│   ├── Danmaku.kt                 # DanmakuElem, DmSegMobileReply
│   ├── Electric.kt                # ElectricPanel, ElectricUser
│   ├── Account.kt                 # LoginRecord, ExpLog, CoinLog, MedalInfo, FollowTag, VipInfo
│   └── Misc.kt                    # Announcement, Tutorial, CustomText, SettingSection, LocalVideo, DownloadSection, PageInfo, At
├── remote/
│   ├── HttpClient.kt              # OkHttp singleton + interceptors
│   ├── GsonConfig.kt              # Gson with custom adapters
│   └── CookieManager.kt           # Cookie storage/generation
```

---

## Task 1: Core Infrastructure — HttpClient + GsonConfig + CookieManager

**Files:**
- Create: `data/remote/HttpClient.kt`
- Create: `data/remote/GsonConfig.kt`
- Create: `data/remote/CookieManager.kt`
- Create: `data/model/ApiResponse.kt`

- [ ] **Step 1: Create `data/remote/CookieManager.kt`**

```kotlin
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
```

- [ ] **Step 2: Create `data/remote/GsonConfig.kt`**

```kotlin
package com.qx.orbit.bili.data.remote

import com.google.gson.*
import java.lang.reflect.Type

object GsonConfig {
    val gson: Gson = GsonBuilder()
        .setLenient()
        .serializeNulls()
        .registerTypeAdapter(Int::class.java, IntOrStringAdapter())
        .registerTypeAdapter(Int::class.javaObjectType, IntOrStringAdapter())
        .registerTypeAdapter(Long::class.java, LongOrStringAdapter())
        .registerTypeAdapter(Long::class.javaObjectType, LongOrStringAdapter())
        .create()

    private class IntOrStringAdapter : JsonDeserializer<Int>, JsonSerializer<Int> {
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Int {
            if (json == null || json.isJsonNull) return 0
            val prim = json.asJsonPrimitive
            return when {
                prim.isNumber -> prim.asInt
                prim.isString -> prim.asString.toIntOrNull() ?: 0
                prim.asBoolean -> 1
                else -> 0
            }
        }

        override fun serialize(src: Int?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            return JsonPrimitive(src ?: 0)
        }
    }

    private class LongOrStringAdapter : JsonDeserializer<Long>, JsonSerializer<Long> {
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Long {
            if (json == null || json.isJsonNull) return 0L
            val prim = json.asJsonPrimitive
            return when {
                prim.isNumber -> prim.asLong
                prim.isString -> prim.asString.toLongOrNull() ?: 0L
                prim.asBoolean -> 1L
                else -> 0L
            }
        }

        override fun serialize(src: Long?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            return JsonPrimitive(src ?: 0L)
        }
    }
}
```

- [ ] **Step 3: Create `data/remote/HttpClient.kt`**

```kotlin
package com.qx.orbit.bili.data.remote

import okhttp3.*
import java.net.InetAddress
import java.net.URI
import java.util.concurrent.TimeUnit

object HttpClient {
    private val ipv4Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return Dns.SYSTEM.lookup(hostname).filter { it.address.size == 4 }
        }
    }

    val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(false)
        .addInterceptor(RedirectInterceptor())
        .addInterceptor(CookieSaveInterceptor())
        .dns(ipv4Dns)
        .pingInterval(8, TimeUnit.SECONDS)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(16, TimeUnit.SECONDS)
        .build()

    private class RedirectInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val response = chain.proceed(request)
            val location = response.header("Location") ?: return response

            val isSslRedirect = try {
                !request.isHttps && URI(location).scheme.equals("https", true)
                        && request.url.host.equals(URI(location).host, true)
            } catch (_: Exception) { false }

            if (response.isRedirect) {
                if (request.url.host == "b23.tv" && !isSslRedirect) {
                    return response // let caller handle b23.tv redirects
                }
                return chain.proceed(request.newBuilder().url(location).build())
            }
            return response
        }
    }

    private class CookieSaveInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val response = chain.proceed(chain.request())
            val newCookies = response.headers("Set-Cookie")
            if (newCookies.isEmpty()) return response

            val cookieMap = mutableMapOf<String, String>()
            CookieManager.getCookie().split("; ").filter { it.contains("=") }.forEach { part ->
                val eqIdx = part.indexOf("=")
                cookieMap[part.substring(0, eqIdx)] = part.substring(eqIdx + 1)
            }

            for (newCookie in newCookies) {
                val domain = Regex("Domain=([^;]+)").find(newCookie)?.groupValues?.get(1)
                if (domain != null && !domain.endsWith("bilibili.com")) continue

                val clean = newCookie.substringBefore("; ")
                val eqIdx = clean.indexOf("=")
                if (eqIdx <= 0) continue
                cookieMap[clean.substring(0, eqIdx)] = clean.substring(eqIdx + 1)
            }

            CookieManager.setCookie(cookieMap.entries.joinToString("; ") { "${it.key}=${it.value}" })
            return response
        }
    }
}
```

- [ ] **Step 4: Create `data/model/ApiResponse.kt`**

```kotlin
package com.qx.orbit.bili.data.model

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("code") val code: Int = -1,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: T? = null
) {
    val isSuccess: Boolean get() = code == 0
}
```

- [ ] **Step 5: Verify compilation**

Run: `cd /Users/yuchen/AndroidStudioProjects/Orbit && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

## Task 2: Model Layer — User + Video + Stats

**Files:**
- Create: `data/model/User.kt`
- Create: `data/model/Video.kt`

- [ ] **Step 1: Create `data/model/User.kt`**

```kotlin
package com.qx.orbit.bili.data.model

import com.google.gson.annotations.SerializedName

data class UserInfo(
    @SerializedName(value = "mid") val mid: Long = 0,
    @SerializedName(value = "name", alternate = ["uname"]) val name: String = "",
    @SerializedName(value = "face", alternate = ["avatar"]) val avatar: String = "",
    val sign: String = "",
    @SerializedName(value = "follower", alternate = ["fans"]) val fans: Int = 0,
    val level: Int = 0,
    val following: Int = 0,
    val followed: Boolean = false,
    val notice: String = "",
    val official: Int = 0,
    val officialDesc: String = "",
    val mtime: Long = 0,
    val vip_role: Int = 0,
    val vip_nickname_color: String = "",
    val current_exp: Long = 0,
    val next_exp: Long = 0,
    val medal_name: String = "",
    val medal_level: Int = 0,
    val sys_notice: String = "",
    val live_room: LiveRoom? = null,
    val is_senior_member: Int = 0,
    val is_follow_display: Boolean = false
)

data class VipInfo(
    @SerializedName("status") val status: Int = 0,
    @SerializedName("role") val role: Int = 0,
    @SerializedName("nickname_color") val nicknameColor: String = ""
)
```

- [ ] **Step 2: Create `data/model/Video.kt`**

```kotlin
package com.qx.orbit.bili.data.model

import com.google.gson.annotations.SerializedName

data class VideoInfo(
    val bvid: String = "",
    val aid: Long = 0,
    val title: String = "",
    val staff: List<UserInfo> = emptyList(),
    @SerializedName("pic") val cover: String = "",
    @SerializedName("desc") val description: String = "",
    val duration: String = "",
    val stats: Stats? = null,
    val timeDesc: String = "",
    val pagenames: List<String> = emptyList(),
    val cids: List<Long> = emptyList(),
    val descAts: List<At> = emptyList(),
    val upowerExclusive: Boolean = false,
    val argueMsg: String? = null,
    val isCooperation: Boolean = false,
    val isSteinGate: Boolean = false,
    val is360: Boolean = false,
    val epid: Long = -1,
    val copyright: Int = 0,
    val collection: Collection? = null
) {
    companion object {
        const val COPYRIGHT_SELF = 1
        const val COPYRIGHT_REPRINT = 2
    }

    fun toCard(): VideoCard = VideoCard(
        title = title,
        upName = staff.firstOrNull()?.name ?: "",
        view = stats?.view?.let { StringUtil.toWan(it.toLong()) } ?: "0",
        cover = cover,
        aid = aid,
        bvid = bvid
    )
}

data class VideoCard(
    val title: String = "",
    val upName: String = "",
    val view: String = "",
    @SerializedName("pic") val cover: String = "",
    val type: String = "video",
    val aid: Long = 0,
    val bvid: String = "",
    val cid: Long = 0
)

data class Stats(
    val view: Int = 0,
    val like: Int = 0,
    val reply: Int = 0,
    val coin: Int = 0,
    val share: Int = 0,
    val danmaku: Int = 0,
    val favorite: Int = 0,
    val followed: Boolean = false,
    val liked: Boolean = false,
    val disliked: Boolean = false,
    val favoured: Boolean = false,
    val coined: Int = 0,
    val like_disabled: Boolean = false,
    val coin_disabled: Boolean = false,
    val fav_disabled: Boolean = false,
    val reply_disabled: Boolean = false,
    val share_disabled: Boolean = false,
    val coin_limit: Int = 0
)

data class At(
    val id: Long,
    val start: Int = 0,
    val end: Int = 0,
    val name: String = ""
)
```

- [ ] **Step 3: Add `StringUtil` stub for compilation**

```kotlin
package com.qx.orbit.bili.data.model

object StringUtil {
    fun toWan(num: Long): String = when {
        num >= 100_000_000 -> String.format("%.1f亿", num / 100_000_000.0)
        num >= 10_000 -> String.format("%.1f万", num / 10_000.0)
        else -> num.toString()
    }

    fun toTime(seconds: Int): String {
        if (seconds <= 0) return "00:00"
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
        else String.format("%02d:%02d", m, s)
    }
}
```

- [ ] **Step 4: Verify compilation**

Run: `cd /Users/yuchen/AndroidStudioProjects/Orbit && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

## Task 3: Model Layer — Player + Dash + Subtitle

**Files:**
- Create: `data/model/Player.kt`

- [ ] **Step 1: Create `data/model/Player.kt`**

```kotlin
package com.qx.orbit.bili.data.model

import com.google.gson.annotations.SerializedName
import org.json.JSONObject

data class PlayerData(
    val title: String = "",
    val videoUrl: String = "",
    val danmakuUrl: String = "",
    val qn: Int = -1,
    val qnStrList: Array<String>? = null,
    val qnValueList: IntArray? = null,
    val aid: Long = 0,
    val cid: Long = 0,
    val mid: Long = 0,
    val progress: Int = 0,
    val cidHistory: Long = 0,
    val type: Int = TYPE_VIDEO,
    val timeStamp: Long = 0,
    val pagenames: List<String> = emptyList(),
    val cids: List<Long> = emptyList(),
    val currentPageIndex: Int = 0,
    val dashData: DashData? = null,
    val audioUrl: String = ""
) {
    companion object {
        const val TYPE_VIDEO = 0
        const val TYPE_BANGUMI = 1
        const val TYPE_LIVE = 2
        const val TYPE_LOCAL = 4
    }
}

data class DashData(
    val duration: Int = 0,
    val minBufferTime: Double = 0.0,
    val videoStreams: List<DashVideoStream> = emptyList(),
    val audioStreams: List<DashAudioStream> = emptyList(),
    val dolbyAudio: DashAudioStream? = null,
    val flacAudio: DashAudioStream? = null
) {
    companion object {
        fun fromJson(json: JSONObject): DashData {
            val video = mutableListOf<DashVideoStream>()
            json.optJSONArray("video")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { video.add(DashVideoStream.fromJson(it)) }
                }
            }
            val audio = mutableListOf<DashAudioStream>()
            json.optJSONArray("audio")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { audio.add(DashAudioStream.fromJson(it)) }
                }
            }
            return DashData(
                duration = json.optInt("duration"),
                minBufferTime = json.optDouble("minBufferTime"),
                videoStreams = video,
                audioStreams = audio
            )
        }
    }
}

data class DashVideoStream(
    val id: Int = 0,
    @SerializedName("baseUrl") val baseUrl: String = "",
    @SerializedName("backupUrl") val backupUrl: List<String> = emptyList(),
    val bandwidth: Long = 0,
    val mimeType: String = "",
    val codecs: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val frameRate: String = "",
    val codecid: Int = 0
) {
    companion object {
        fun fromJson(json: JSONObject) = DashVideoStream(
            id = json.optInt("id"),
            baseUrl = json.optString("baseUrl") ?: json.optString("base_url"),
            bandwidth = json.optLong("bandwidth"),
            mimeType = json.optString("mimeType"),
            codecs = json.optString("codecs"),
            width = json.optInt("width"),
            height = json.optInt("height"),
            frameRate = json.optString("frameRate"),
            codecid = json.optInt("codecid")
        )
    }
}

data class DashAudioStream(
    val id: Int = 0,
    @SerializedName("baseUrl") val baseUrl: String = "",
    @SerializedName("backupUrl") val backupUrl: List<String> = emptyList(),
    val bandwidth: Long = 0,
    val mimeType: String = "",
    val codecs: String = "",
    val codecid: Int = 0
) {
    companion object {
        fun fromJson(json: JSONObject) = DashAudioStream(
            id = json.optInt("id"),
            baseUrl = json.optString("baseUrl") ?: json.optString("base_url"),
            bandwidth = json.optLong("bandwidth"),
            mimeType = json.optString("mimeType"),
            codecs = json.optString("codecs"),
            codecid = json.optInt("codecid")
        )
    }
}

data class Subtitle(
    val content: String = "",
    val from: Double = 0.0,
    val to: Double = 0.0
)

data class SubtitleLink(
    val id: Long = 0,
    val isAI: Boolean = false,
    val lang: String = "",
    val url: String = ""
)

data class ViewPoint(
    val content: String = "",
    val from: Int = 0,
    val to: Int = 0,
    val type: Int = 0,
    val imgUrl: String = "",
    val logoUrl: String = ""
)

data class HighEnergyData(
    val stepSec: Int = 0,
    val tagStr: String = "",
    val events: FloatArray = floatArrayOf(),
    val debug: String = ""
)
```

- [ ] **Step 2: Verify compilation**

Run: `cd /Users/yuchen/AndroidStudioProjects/Orbit && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

## Task 4: Model Layer — Live + Bangumi + Timeline

**Files:**
- Create: `data/model/Live.kt`
- Create: `data/model/Bangumi.kt`

- [ ] **Step 1: Create `data/model/Live.kt`**

```kotlin
package com.qx.orbit.bili.data.model

import com.google.gson.annotations.SerializedName

data class LiveRoom(
    @SerializedName(value = "roomid", alternate = ["room_id"]) val roomid: Long = 0,
    val short_id: Long = 0,
    val uid: Long = 0,
    val title: String = "",
    val uname: String = "",
    val tags: String = "",
    val description: String = "",
    val online: Int = 0,
    val attention: Int = 0,
    val user_cover: String = "",
    val cover: String = "",
    val keyframe: String = "",
    val face: String = "",
    @SerializedName(value = "area_parent_id", alternate = ["area_v2_parent_id", "parent_area_id"]) val area_parent_id: Int = 0,
    @SerializedName(value = "area_parent_name", alternate = ["area_v2_parent_name", "parent_area_name"]) val area_parent_name: String = "",
    @SerializedName(value = "area_id", alternate = ["area_v2_id"]) val area_id: Int = 0,
    @SerializedName(value = "area_name", alternate = ["area_v2_name"]) val area_name: String = "",
    val live_status: Int = 0,
    val liveTime: String = "",
    val is_portrait: Boolean = false
)

data class LivePlayInfo(
    @SerializedName("room_id") val roomid: Long = 0,
    val short_id: Long = 0,
    val uid: Long = 0,
    @SerializedName("is_hidden") val isHidden: Boolean = false,
    @SerializedName("is_locked") val isLocked: Boolean = false,
    @SerializedName("is_portrait") val isPortrait: Boolean = false,
    val live_status: Int = 0,
    val encrypted: Boolean = false,
    val pwd_verified: Boolean = false,
    val live_time: Long = 0,
    val playurl_info: PlayurlInfo? = null,
    val official_type: Int = 0,
    val official_room_id: Int = 0,
    val risk_with_delay: Int = 0
)

data class PlayurlInfo(
    val playurl: Playurl? = null
)

data class Playurl(
    val cid: Long = 0,
    @SerializedName("g_qn_desc") val qnDesc: List<QnDesc> = emptyList(),
    val stream: List<ProtocolInfo> = emptyList()
)

data class QnDesc(
    val qn: Int = 0,
    val desc: String = "",
    val hdr_desc: String = ""
)

data class ProtocolInfo(
    val protocol_name: String = "",
    val format: List<FormatInfo> = emptyList()
)

data class FormatInfo(
    val format_name: String = "",
    val codec: List<CodecInfo> = emptyList(),
    val master_url: String = ""
)

data class CodecInfo(
    val codec_name: String = "",
    val current_qn: Int = 0,
    @SerializedName("accept_qn") val acceptQn: List<Int> = emptyList(),
    val base_url: String = "",
    @SerializedName("url_info") val urlInfo: List<UrlInfo> = emptyList()
)

data class UrlInfo(
    val host: String = "",
    val extra: String = "",
    val stream_ttl: Int = 0
)

data class LiveInfo(
    val userInfo: UserInfo? = null,
    val liveRoom: LiveRoom? = null,
    val livePlayInfo: LivePlayInfo? = null
)
```

- [ ] **Step 2: Create `data/model/Bangumi.kt`**

```kotlin
package com.qx.orbit.bili.data.model

import com.google.gson.annotations.SerializedName

data class Bangumi(
    val info: Info? = null,
    val sectionList: List<Section> = emptyList()
) {
    data class Info(
        val media_id: Long = 0,
        val season_id: Long = 0,
        val type: Int = 0,
        val count: Int = 0,
        val score: Float = 0f,
        val title: String = "",
        val cover: String = "",
        @SerializedName("horizontal_picture") val cover_horizontal: String = "",
        val type_name: String = "",
        val area_name: String = "",
        @SerializedName("index_show") val indexShow: String = "",
        val evaluate: String = "",
        val staff: String = "",
        val record: String = "",
        val subtitle: String = "",
        val publish: Publish? = null,
        val styles: List<String> = emptyList(),
        val stat: Stat? = null,
        val up_info: UpInfo? = null,
        val series: Series? = null,
        val seasons: List<Season> = emptyList()
    )

    data class Publish(
        val is_finish: Int = 0,
        val is_started: Int = 0,
        val pub_time: String = "",
        val pub_time_show: String = ""
    )

    data class Stat(
        val favorites: Int = 0,
        val series_follow: Int = 0,
        val views: Int = 0,
        val vt: Int = 0
    )

    data class UpInfo(
        val mid: Long = 0,
        val name: String = "",
        val avatar: String = ""
    )

    data class Series(
        val series_id: Long = 0,
        val series_title: String = ""
    )

    data class Season(
        val media_id: Long = 0,
        val season_id: Long = 0,
        val season_title: String = "",
        val cover: String = "",
        val badge: String = ""
    )

    data class Section(
        val id: Long = 0,
        val type: Int = 0,
        val title: String = "",
        val episodes: List<Episode> = emptyList()
    )

    data class Episode(
        val id: Long = 0,
        val aid: Long = 0,
        val cid: Long = 0,
        val title: String = "",
        @SerializedName("long_title") val title_long: String = "",
        val cover: String = "",
        val badge: String = ""
    )
}

data class Timeline(
    val dayList: List<DayInfo> = emptyList()
) {
    data class DayInfo(
        val date: String = "",
        val date_ts: Long = 0,
        val day_of_week: Int = 0,
        val episodes: List<Episode> = emptyList(),
        val is_today: Int = 0
    )

    data class Episode(
        val cover: String = "",
        val delay: Int = 0,
        val episode_id: Long = 0,
        val pub_index: String = "",
        val pub_time: String = "",
        val pub_ts: Long = 0,
        val published: Int = 0,
        val follows: String = "",
        val plays: String = "",
        val season_id: Long = 0,
        val title: String = ""
    )
}
```

- [ ] **Step 3: Verify compilation**

Run: `cd /Users/yuchen/AndroidStudioProjects/Orbit && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

## Task 5: Model Layer — Dynamic + Opus + Reply

**Files:**
- Create: `data/model/Dynamic.kt`
- Create: `data/model/Reply.kt`

- [ ] **Step 1: Create `data/model/Dynamic.kt`**

```kotlin
package com.qx.orbit.bili.data.model

data class Dynamic(
    val dynamicId: Long = 0,
    val type: String = "",
    val comment_id: Long = 0,
    val comment_type: Int = 0,
    val title: String = "",
    val userInfo: UserInfo? = null,
    val content: String = "",
    val pubTime: String = "",
    val stats: Stats? = null,
    val major_type: String = "",
    val major_object: Any? = null,
    val dynamic_forward: Dynamic? = null,
    val canDelete: Boolean = false
) {
    companion object {
        const val DYNAMIC_TYPE_UGC_SEASON = "DYNAMIC_TYPE_UGC_SEASON"
    }
}

data class Opus(
    val id: Long = 0,
    val type: Int = TYPE_DYNAMIC,
    val commentId: Long = 0,
    val commentType: Int = 0,
    val title: String = "",
    val cover: String = "",
    val content: String = "",
    val pubTime: String = "",
    val upInfo: UserInfo? = null,
    val stats: Stats? = null,
    val topImages: List<String> = emptyList(),
    val paragraphs: Array<OpusParagraph>? = null,
    val parsedId: Long = 0
) {
    companion object {
        const val TYPE_DYNAMIC = 1
        const val TYPE_ARTICLE = 2
        const val TYPE_DYNAMIC_OLD_STYLE = 3
    }
}

data class OpusParagraph(
    val align: Int = 0,
    val type: Int = 0,
    val content: Any? = null
) {
    companion object {
        const val TYPE_TEXT = 1
        const val TYPE_PIC = 2
        const val TYPE_DIVIDER = 3
        const val TYPE_TEXT_BLOCKQUOTE = 4
        const val TYPE_LIST = 5
        const val TYPE_HEADING = 8
        const val TYPE_TEXT_OPUS = 99
        const val TYPE_VIDEO = 100
        const val TYPE_DYNAMIC = 101
        const val TYPE_ARTICLE = 102
    }
}

data class OpusCard(
    val title: String = "",
    val id: Long = 0,
    val cover: String = "",
    val upName: String = "",
    val view: String = ""
)
```

- [ ] **Step 2: Create `data/model/Reply.kt`**

```kotlin
package com.qx.orbit.bili.data.model

enum class ContentType(val typeCode: Int) {
    Video(1), Topic(2), Activity(4), ShortVideo(5), BlackRoom(6),
    Announcement(7), Live(8), ActivityContent(9), LiveAnnouncement(10),
    Photo(11), Article(12), Ticket(13), Audio(14), Judgement(15),
    Review(16), Dynamic(17), VideoPlaylist(18), AudioPlaylist(19),
    Manga_1(20), Manga_2(21), Manga_3(22), Opus(23), Course(33);

    companion object {
        fun fromCode(code: Int) = entries.firstOrNull { it.typeCode == code } ?: Video
    }
}

data class Reply(
    val rpid: Long = 0,
    val oid: Long = 0,
    val root: Long = 0,
    val parent: Long = 0,
    val forceDelete: Boolean = false,
    val ofBvid: String = "",
    val pubTime: String = "",
    val sender: UserInfo? = null,
    val message: String = "",
    val pictureList: List<String> = emptyList(),
    val likeCount: Int = 0,
    val upLiked: Boolean = false,
    val upReplied: Boolean = false,
    val liked: Boolean = false,
    val childCount: Int = 0,
    val isDynamic: Boolean = false,
    val childMsgList: List<Reply> = emptyList(),
    val isTop: Boolean = false
)
```

- [ ] **Step 3: Verify compilation**

Run: `cd /Users/yuchen/AndroidStudioProjects/Orbit && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

## Task 6: Model Layer — Message + Search + Favorite + Article + Emote + Misc

**Files:**
- Create: `data/model/Message.kt`
- Create: `data/model/Search.kt`
- Create: `data/model/Favorite.kt`
- Create: `data/model/Article.kt`
- Create: `data/model/Emote.kt`
- Create: `data/model/Interaction.kt`
- Create: `data/model/Danmaku.kt`
- Create: `data/model/Electric.kt`
- Create: `data/model/Account.kt`
- Create: `data/model/Misc.kt`
- Create: `data/model/Collection.kt`

- [ ] **Step 1: Create `data/model/Message.kt`**

```kotlin
package com.qx.orbit.bili.data.model

import com.google.gson.annotations.SerializedName
import org.json.JSONObject

data class MessageCard(
    val id: Long = 0,
    val user: List<UserInfo> = emptyList(),
    val timeStamp: Long = 0,
    val timeDesc: String = "",
    val content: String = "",
    val videoCard: VideoCard? = null,
    val replyInfo: Reply? = null,
    val subjectId: Long = 0,
    val businessId: Int = 0,
    val itemType: String = "",
    val getType: Int = 0,
    val sourceId: Long = 0,
    val rootId: Long = 0,
    val targetId: Long = 0
) {
    data class Cursor(
        val is_end: Boolean,
        val id: Long,
        val time: Long
    )

    companion object {
        const val GET_TYPE_REPLY = 0
        const val GET_TYPE_AT = 1
        const val GET_TYPE_LIKE = 2
    }
}

data class PrivateMessage(
    val content: JSONObject = JSONObject(),
    val type: Int = 0,
    val timestamp: Long = 0,
    val uid: Long = 0,
    val name: String = "",
    val msgId: Long = 0,
    val msgSeqno: Long = 0,
    val msg_source: Int = 0
) {
    companion object {
        const val TYPE_TEXT = 1
        const val TYPE_VIDEO = 7
        const val TYPE_PIC = 2
        const val TYPE_RETRACT = 5
        const val TYPE_FACE = 6
        const val TYPE_NOMAL_CARD = 10
        const val TYPE_PIC_CARD = 13
        const val TYPE_TEXT_WITH_VIDEO = 16
        const val TYPE_SYSTEM = 18
    }
}

data class PrivateMsgSession(
    val talkerUid: Long = 0,
    val unread: Int = 0,
    val contentType: Int = 0,
    val content: JSONObject? = null
)
```

- [ ] **Step 2: Create `data/model/Search.kt`**

```kotlin
package com.qx.orbit.bili.data.model

import com.google.gson.annotations.SerializedName
import com.google.gson.JsonElement

data class SearchData(
    val seid: String = "",
    val result: List<SearchResultGroup> = emptyList()
)

data class SearchResultGroup(
    val result_type: String = "",
    val data: List<JsonElement> = emptyList()
)
```

- [ ] **Step 3: Create `data/model/Favorite.kt`**

```kotlin
package com.qx.orbit.bili.data.model

import com.google.gson.annotations.SerializedName

data class FavoriteFolder(
    @SerializedName(value = "fav_box", alternate = ["id", "fid"]) val id: Long = 0,
    @SerializedName(value = "id", alternate = ["media_id"]) val mediaId: Long = 0,
    val name: String = "",
    val cover: String = "",
    @SerializedName("count") val videoCount: Int = 0,
    @SerializedName("max_count") val maxCount: Int = 0,
    val isDefault: Boolean = false
)

data class Collection(
    val id: Int = 0,
    val title: String = "",
    val cover: String = "",
    val intro: String = "",
    val mid: Long = 0,
    val sections: List<Section> = emptyList(),
    val cards: List<VideoCard> = emptyList(),
    val view: String = ""
) {
    data class Section(
        val season_id: Int = 0,
        val id: Int = 0,
        val title: String = "",
        val type: Int = 0,
        val episodes: List<Episode> = emptyList()
    )

    data class Episode(
        val season_id: Int = 0,
        val section_id: Int = 0,
        val id: Long = 0,
        val aid: Long = 0,
        val cid: Long = 0,
        val title: String = "",
        val bvid: String = "",
        val arc: VideoInfo? = null
    )
}

data class Series(
    val type: String = "series",
    val id: Int = 0,
    val title: String = "",
    val cover: String = "",
    val intro: String = "",
    val mid: Long = 0,
    val total: String = ""
)
```

- [ ] **Step 4: Create `data/model/Article.kt`**

```kotlin
package com.qx.orbit.bili.data.model

data class ArticleInfo(
    val id: Long = 0,
    val title: String = "",
    val summary: String = "",
    val banner: String = "",
    val upInfo: UserInfo? = null,
    val ctime: Long = 0,
    val stats: Stats? = null,
    val wordCount: Int = 0,
    val keywords: String = "",
    val content: String = ""
)

data class ArticleCard(
    val title: String = "",
    val id: Long = 0,
    val cover: String = "",
    val upName: String = "",
    val view: String = ""
)

data class ArticleLine(
    val type: Int = 0,
    val content: String = "",
    val extra: String = ""
)
```

- [ ] **Step 5: Create `data/model/Emote.kt`**

```kotlin
package com.qx.orbit.bili.data.model

data class Emote(
    val id: Int = 0,
    val packageId: Int = 0,
    val name: String = "",
    val alias: String = "",
    val url: String = "",
    val size: Int = 0
)

data class EmotePackage(
    val id: Int = 0,
    val text: String = "",
    val url: String = "",
    val type: Int = 0,
    val attr: Int = 0,
    val size: Int = 0,
    val item_id: Int = 0,
    val emotes: List<Emote> = emptyList(),
    val permanent: Boolean = false
)
```

- [ ] **Step 6: Create `data/model/Interaction.kt`**

```kotlin
package com.qx.orbit.bili.data.model

data class InteractionVideoData(
    val title: String = "",
    val edgeId: Long = 0,
    val storyList: List<StoryNode> = emptyList(),
    val edges: InteractionEdge? = null,
    val isLeaf: Int = 0
) {
    data class StoryNode(
        val nodeId: Long = 0, val edgeId: Long = 0, val title: String = "",
        val cid: Long = 0, val startPos: Long = 0, val cover: String = "",
        val isCurrent: Int = 0
    )

    data class InteractionEdge(
        val questions: List<Question> = emptyList()
    )

    data class Question(
        val id: Long = 0, val type: Int = 0, val startTimeR: Long = 0,
        val duration: Long = 0, val pauseVideo: Int = 0, val title: String = "",
        val choices: List<Choice> = emptyList()
    )

    data class Choice(
        val id: Long = 0, val cid: Long = 0, val option: String = "",
        val isDefault: Int = 0, val isHidden: Int = 0
    )
}
```

- [ ] **Step 7: Create `data/model/Danmaku.kt`**

```kotlin
package com.qx.orbit.bili.data.model

data class DanmakuElem(
    val id: Long = 0,
    val progress: Int = 0,
    val mode: Int = 1,
    val fontsize: Int = 25,
    val color: Int = 0,
    val midHash: String = "",
    val content: String = "",
    val ctime: Long = 0,
    val weight: Int = 0,
    val action: String = "",
    val pool: Int = 0,
    val idStr: String = "",
    val attr: Int = 0,
    val animation: String = ""
)

data class DmSegMobileReply(
    val elems: List<DanmakuElem> = emptyList()
)
```

- [ ] **Step 8: Create `data/model/Electric.kt`**

```kotlin
package com.qx.orbit.bili.data.model

data class ElectricPanel(
    val count: Int = 0,
    val total_count: Int = 0,
    val total: Int = 0,
    val special_day: Int = 0,
    val list: List<ElectricUser> = emptyList()
)

data class ElectricUser(
    val uname: String = "",
    val avatar: String = "",
    val mid: Long = 0,
    val pay_mid: Long = 0,
    val rank: Int = 0,
    val trend_type: Int = 0,
    val message: String = "",
    val msg_hidden: Int = 0,
    val vip_info: VipInfoData? = null
) {
    data class VipInfoData(
        val vipDueMsec: Long = 0,
        val vipStatus: Int = 0,
        val vipType: Int = 0
    )
}
```

- [ ] **Step 9: Create `data/model/Account.kt`**

```kotlin
package com.qx.orbit.bili.data.model

data class LoginRecord(
    val mid: Long = 0,
    val deviceName: String = "",
    val loginType: String = "",
    val loginTime: String = "",
    val location: String = "",
    val ip: String = ""
)

data class ExpLog(
    val delta: Int = 0,
    val time: String = "",
    val reason: String = ""
)

data class CoinLog(
    val time: String = "",
    val delta: Int = 0,
    val reason: String = ""
)

data class MedalInfo(
    val target_id: Long = 0,
    val level: Int = 0,
    val medal_name: String = "",
    val medal_color_start: Int = 0,
    val medal_color_end: Int = 0,
    val medal_color_border: Int = 0,
    val guard_level: Int = 0,
    val wearing_status: Int = 0,
    val medal_id: Long = 0,
    val target_name: String = "",
    val target_icon: String = ""
)

data class FollowTag(
    val tagid: Int = 0,
    val name: String = "",
    val count: Int = 0
)

data class VipDetailInfo(
    val isVip: Boolean = false,
    val level: Int = 0,
    val vipStatus: Int = 0,
    val vipType: Int = 0,
    val vipDueDate: Long = 0,
    val privilegeList: List<Privilege> = emptyList()
) {
    data class Privilege(
        val type: Int = 0,
        val state: Int = 0,
        val expireTime: Long = 0
    )
}
```

- [ ] **Step 10: Create `data/model/Misc.kt`**

```kotlin
package com.qx.orbit.bili.data.model

import com.google.gson.annotations.SerializedName

data class Announcement(
    val id: Int = 0,
    val ctime: String = "",
    val title: String = "",
    val content: String = ""
)

data class Tutorial(
    val name: String = "",
    val description: String = "",
    val imgid: String = "",
    val type: Int = 0,
    val content: List<CustomText> = emptyList()
)

data class CustomText(
    val type: Int = 0,
    val text: String = "",
    val style: String = "",
    val color: String = ""
)

data class SettingSection(
    val type: String,
    val id: String,
    val name: String,
    val desc: String,
    val defaultValue: String
)

data class LocalVideo(
    val cover: String = "",
    val title: String = "",
    val pageList: List<String> = emptyList(),
    val videoFileList: List<String> = emptyList(),
    val danmakuFileList: List<String> = emptyList(),
    val sizeList: List<Long> = emptyList(),
    val size: Long = 0
)

data class DownloadSection(
    val id: Long = 0,
    val type: String = "",
    val aid: Long = 0,
    val cid: Long = 0,
    val qn: Int = 0,
    val url_cover: String = "",
    val title: String = "",
    val child: String = "",
    val state: String = "",
    val downloadType: String = "",
    val audioUrl: String = ""
)

data class PageInfo(
    val page_num: Int = 0,
    val require_ps: Int = 0,
    val total: Int = 0,
    val return_ps: Int = 0
)

data class ApiResult(
    val code: Int = -1,
    val message: String = "",
    val offset: Long = 0,
    val timestamp: Long = 0,
    val business: String = "",
    val isBottom: Boolean = false
)
```

- [ ] **Step 11: Verify compilation**

Run: `cd /Users/yuchen/AndroidStudioProjects/Orbit && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

## Task 7: WBI Signer Utility

**Files:**
- Create: `data/api/WbiSigner.kt`

- [ ] **Step 1: Create `data/api/WbiSigner.kt`**

```kotlin
package com.qx.orbit.bili.data.api

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
            .addHeader("Cookie", com.qx.orbit.bili.data.remote.CookieManager.getCookie())
            .addHeader("User-Agent", USER_AGENT)
            .build()
        val response = HttpClient.client.newCall(request).execute()
        val body = response.body?.string() ?: return ""
        val json = GsonConfig.gson.fromJson(body, Map::class.java)
        val data = json["data"] as? Map<*, *> ?: return ""
        val wbiImg = data["wbi_img"] as? Map<*, *> ?: return ""
        val imgUrl = wbiImg["img_url"] as? String ?: return ""
        val subUrl = wbiImg["sub_url"] as? String ?: return ""
        val rawKey = imgUrl.substringAfterLast("/").substringBefore(".")
            .plus(subUrl.substringAfterLast("/").substringBefore("."))
        val key = rawKey.map { MIXIN_KEY_ENC_TAB[it.code] }.joinToString("").take(32)
        mixinKey = key
        return key
    }

    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.95 Safari/537.36"
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd /Users/yuchen/AndroidStudioProjects/Orbit && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

## Task 8: Retrofit API Service — Core Endpoints

**Files:**
- Create: `data/api/BiliApiService.kt`

- [ ] **Step 1: Create `data/api/BiliApiService.kt` with video + user endpoints**

```kotlin
package com.qx.orbit.bili.data.api

import com.qx.orbit.bili.data.model.*
import com.google.gson.JsonElement
import retrofit2.http.*

interface BiliApiService {

    // ===== Video =====

    @GET("https://api.bilibili.com/x/web-interface/view")
    suspend fun getVideoInfo(@Query("bvid") bvid: String? = null, @Query("aid") aid: Long? = null): ApiResponse<JsonElement>

    @GET("https://api.bilibili.com/x/tag/archive/tags")
    suspend fun getVideoTags(@Query("bvid") bvid: String? = null, @Query("aid") aid: Long? = null): ApiResponse<List<JsonElement>>

    @GET("https://api.bilibili.com/x/player/online/total")
    suspend fun getWatching(@Query("aid") aid: Long, @Query("cid") cid: Long): ApiResponse<JsonElement>

    @GET("https://api.bilibili.com/x/player/wbi/playurl")
    suspend fun getPlayUrl(@QueryMap params: Map<String, String>): ApiResponse<JsonElement>

    @GET("https://api.bilibili.com/x/player/wbi/v2")
    suspend fun getPlayerV2(@Query("aid") aid: Long, @Query("cid") cid: Long): ApiResponse<JsonElement>

    @GET("https://api.bilibili.com/x/web-interface/archive/relation")
    suspend fun getVideoStats(@Query("aid") aid: Long): ApiResponse<Stats>

    @GET("https://api.bilibili.com/x/web-interface/archive/related")
    suspend fun getRelated(@Query("aid") aid: Long): ApiResponse<List<JsonElement>>

    // ===== Recommend / Popular / Ranking =====

    @GET("https://api.bilibili.com/x/web-interface/popular")
    suspend fun getPopular(@Query("pn") page: Int, @Query("ps") pageSize: Int = 10): ApiResponse<JsonElement>

    @GET("https://api.bilibili.com/x/web-interface/ranking/v2")
    suspend fun getRanking(@Query("rid") rid: Int, @Query("type") type: String): ApiResponse<JsonElement>

    // ===== User =====

    @GET("https://api.bilibili.com/x/web-interface/card")
    suspend fun getUserCard(@Query("mid") mid: Long): ApiResponse<JsonElement>

    @GET("https://api.bilibili.com/x/space/myinfo")
    suspend fun getMyInfo(): ApiResponse<JsonElement>

    @GET("https://account.bilibili.com/site/getCoin")
    suspend fun getCoin(): ApiResponse<JsonElement>

    @GET("https://api.bilibili.com/x/space/wbi/arc/search")
    suspend fun getUserVideos(@QueryMap params: Map<String, String>): ApiResponse<JsonElement>

    @GET("https://api.bilibili.com/x/space/wbi/article")
    suspend fun getUserArticles(@QueryMap params: Map<String, String>): ApiResponse<JsonElement>

    @FormUrlEncoded
    @POST("https://api.bilibili.com/x/relation/modify")
    suspend fun followUser(@Field("fid") fid: Long, @Field("act") act: Int, @Field("csrf") csrf: String): ApiResponse<Unit>

    // ===== Like / Coin / Favorite =====

    @FormUrlEncoded
    @POST("https://api.bilibili.com/x/web-interface/archive/like")
    suspend fun like(@Field("aid") aid: Long, @Field("like") like: Int, @Field("csrf") csrf: String): ApiResponse<Unit>

    @FormUrlEncoded
    @POST("https://api.bilibili.com/x/web-interface/coin/add")
    suspend fun coin(@Field("aid") aid: Long, @Field("multiply") multiply: Int, @Field("csrf") csrf: String): ApiResponse<Unit>

    @FormUrlEncoded
    @POST("https://api.bilibili.com/medialist/gateway/coll/resource/deal")
    suspend fun favorite(@Field("rid") rid: Long, @Field("media_id") mediaId: Long, @Field("csrf") csrf: String): ApiResponse<Unit>

    @FormUrlEncoded
    @POST("https://api.bilibili.com/x/web-interface/archive/like/triple")
    suspend fun triple(@Field("aid") aid: Long, @Field("csrf") csrf: String): ApiResponse<Unit>

    // ===== Reply =====

    @GET("https://api.bilibili.com/x/v2/reply")
    suspend fun getReplies(@Query("oid") oid: Long, @Query("type") type: Int, @Query("pn") pn: Int, @Query("sort") sort: Int): ApiResponse<JsonElement>

    @GET("https://api.bilibili.com/x/v2/reply/wbi/main")
    suspend fun getRepliesLazy(@QueryMap params: Map<String, String>): ApiResponse<JsonElement>

    @FormUrlEncoded
    @POST("https://api.bilibili.com/x/v2/reply/add")
    suspend fun sendReply(@Field("oid") oid: Long, @Field("root") root: Long, @Field("parent") parent: Long, @Field("message") message: String, @Field("type") type: Int, @Field("csrf") csrf: String): ApiResponse<JsonElement>

    @FormUrlEncoded
    @POST("https://api.bilibili.com/x/v2/reply/action")
    suspend fun likeReply(@Field("oid") oid: Long, @Field("rpid") rpid: Long, @Field("action") action: Int, @Field("csrf") csrf: String): ApiResponse<Unit>

    @GET("https://api.bilibili.com/x/v2/reply/count")
    suspend fun getReplyCount(@Query("oid") oid: Long, @Query("type") type: Int): ApiResponse<JsonElement>

    // ===== Dynamic =====

    @GET("https://api.bilibili.com/x/polymer/web-dynamic/v1/feed/all")
    suspend fun getDynamicList(@Query("type") type: String, @Query("offset") offset: String): ApiResponse<JsonElement>

    @GET("https://api.bilibili.com/x/polymer/web-dynamic/v1/detail")
    suspend fun getDynamic(@Query("id") id: Long): ApiResponse<JsonElement>

    @FormUrlEncoded
    @POST("https://api.vc.bilibili.com/dynamic_like/v1/dynamic_like/thumb")
    suspend fun likeDynamic(@Field("dynamic_id") dynamicId: Long, @Field("up") up: Int, @Field("csrf") csrf: String): ApiResponse<Unit>

    // ===== History =====

    @GET("https://api.bilibili.com/x/web-interface/history/cursor")
    suspend fun getHistory(@Query("type") type: String, @Query("view_at") viewAt: Long, @Query("business") business: String, @Query("max") max: Long): ApiResponse<JsonElement>

    @FormUrlEncoded
    @POST("https://api.bilibili.com/x/v2/history/report")
    suspend fun reportHistory(@Field("aid") aid: Long, @Field("cid") cid: Long, @Field("progress") progress: Long, @Field("csrf") csrf: String): ApiResponse<Unit>

    // ===== Watch Later =====

    @GET("https://api.bilibili.com/x/v2/history/toview/web")
    suspend fun getWatchLater(): ApiResponse<JsonElement>

    @FormUrlEncoded
    @POST("https://api.bilibili.com/x/v2/history/toview/add")
    suspend fun addWatchLater(@Field("aid") aid: Long, @Field("csrf") csrf: String): ApiResponse<Unit>

    @FormUrlEncoded
    @POST("https://api.bilibili.com/x/v2/history/toview/del")
    suspend fun deleteWatchLater(@Field("aid") aid: Long, @Field("csrf") csrf: String): ApiResponse<Unit>

    // ===== Search =====

    @GET("https://api.bilibili.com/x/web-interface/wbi/search/all/v2")
    suspend fun searchAll(@QueryMap params: Map<String, String>): ApiResponse<JsonElement>

    @GET("https://api.bilibili.com/x/web-interface/wbi/search/type")
    suspend fun searchType(@QueryMap params: Map<String, String>): ApiResponse<JsonElement>

    @GET("https://s.search.bilibili.com/main/suggest")
    suspend fun getSearchSuggest(@Query("term") term: String): JsonElement

    @GET("https://api.bilibili.com/x/web-interface/wbi/search/default")
    suspend fun getDefaultSearch(): ApiResponse<JsonElement>

    // ===== Favorite =====

    @GET("https://api.bilibili.com/x/v3/fav/folder/created/list-all")
    suspend fun getFavFolders(@Query("type") type: Int, @Query("up_mid") upMid: Long): ApiResponse<JsonElement>

    @GET("https://api.bilibili.com/x/v3/fav/folder/collected/list")
    suspend fun getCollectedFolders(@Query("up_mid") upMid: Long, @Query("pn") pn: Int, @Query("ps") ps: Int = 10): ApiResponse<JsonElement>

    @GET("https://api.bilibili.com/x/space/fav/arc")
    suspend fun getFavVideos(@Query("vmid") vmid: Long, @Query("fid") fid: Long, @Query("pn") pn: Int, @Query("ps") ps: Int = 30): ApiResponse<JsonElement>

    // ===== Live =====

    @GET("https://api.live.bilibili.com/xlive/web-interface/v1/second/getUserRecommend")
    suspend fun getLiveRecommend(@Query("page") page: Int, @Query("page_size") pageSize: Int = 10): ApiResponse<JsonElement>

    @GET("https://api.live.bilibili.com/xlive/web-ucenter/v1/xfetter/GetWebList")
    suspend fun getLiveFollowed(@Query("page") page: Int, @Query("page_size") pageSize: Int = 10): ApiResponse<JsonElement>

    @GET("https://api.live.bilibili.com/room/v1/Room/get_info")
    suspend fun getLiveRoomInfo(@Query("room_id") roomId: Long): ApiResponse<JsonElement>

    @GET("https://api.live.bilibili.com/xlive/web-room/v2/index/getRoomPlayInfo")
    suspend fun getLivePlayInfo(@Query("room_id") roomId: Long, @Query("qn") qn: Int): ApiResponse<JsonElement>

    // ===== Bangumi =====

    @GET("https://api.bilibili.com/pgc/review/user")
    suspend fun getBangumiReview(@Query("media_id") mediaId: Long): ApiResponse<JsonElement>

    @GET("https://api.bilibili.com/pgc/view/web/season")
    suspend fun getSeasonInfo(@Query("season_id") seasonId: Long? = null, @Query("ep_id") epId: Long? = null): ApiResponse<JsonElement>

    @GET("https://api.bilibili.com/pgc/web/season/section")
    suspend fun getSeasonSection(@Query("season_id") seasonId: Long): ApiResponse<JsonElement>

    @GET("https://api.bilibili.com/pgc/web/timeline")
    suspend fun getTimeline(@Query("types") types: String, @Query("before") before: Int, @Query("after") after: Int): ApiResponse<JsonElement>

    // ===== Message =====

    @GET("https://api.bilibili.com/x/msgfeed/unread")
    suspend fun getUnread(): ApiResponse<JsonElement>

    @GET("https://api.vc.bilibili.com/session_svr/v1/session_svr/single_unread")
    suspend fun getPrivateMsgUnread(): ApiResponse<JsonElement>

    @GET("https://api.bilibili.com/x/msgfeed/like")
    suspend fun getLikeMsg(@Query("id") id: Long, @Query("reply_time") replyTime: Long): ApiResponse<JsonElement>

    @GET("https://api.bilibili.com/x/msgfeed/reply")
    suspend fun getReplyMsg(@Query("id") id: Long, @Query("reply_time") replyTime: Long): ApiResponse<JsonElement>

    @GET("https://api.bilibili.com/x/msgfeed/at")
    suspend fun getAtMsg(@Query("id") id: Long, @Query("at_time") atTime: Long): ApiResponse<JsonElement>

    // ===== Emote =====

    @GET("https://api.bilibili.com/x/emote/user/panel/web")
    suspend fun getEmotes(@Query("business") business: String): ApiResponse<JsonElement>

    // ===== Danmaku =====

    @FormUrlEncoded
    @POST("https://api.bilibili.com/x/v2/dm/post")
    suspend fun sendDanmaku(@Field("oid") oid: Long, @Field("msg") msg: String, @Field("bvid") bvid: String, @Field("progress") progress: Long, @Field("color") color: Int, @Field("mode") mode: Int, @Field("csrf") csrf: String): ApiResponse<Unit>

    // ===== Article =====

    @GET("https://api.bilibili.com/x/article/view")
    suspend fun getArticle(@Query("id") id: Long): ApiResponse<JsonElement>

    @FormUrlEncoded
    @POST("https://api.bilibili.com/x/article/like")
    suspend fun likeArticle(@Field("id") id: Long, @Field("type") type: Int, @Field("csrf") csrf: String): ApiResponse<Unit>

    // ===== Account =====

    @GET("https://api.bilibili.com/x/vip/privilege/my")
    suspend fun getVipInfo(): ApiResponse<JsonElement>

    @GET("https://api.bilibili.com/x/member/web/exp/log")
    suspend fun getExpLog(): ApiResponse<JsonElement>

    @GET("https://api.bilibili.com/x/member/web/coin/log")
    suspend fun getCoinLog(): ApiResponse<JsonElement>

    // ===== Series =====

    @GET("https://api.bilibili.com/x/polymer/web-space/seasons_series_list")
    suspend fun getUserSeries(@QueryMap params: Map<String, String>): ApiResponse<JsonElement>

    @GET("https://api.bilibili.com/x/series/archives")
    suspend fun getSeriesArchives(@QueryMap params: Map<String, String>): ApiResponse<JsonElement>

    // ===== Electric =====

    @GET("https://api.bilibili.com/x/ugcpay-rank/elec/month/up")
    suspend fun getElectricPanel(@Query("up_mid") upMid: Long): ApiResponse<JsonElement>

    // ===== Interaction =====

    @GET("https://api.bilibili.com/x/stein/edgeinfo_v2")
    suspend fun getEdgeInfo(@Query("aid") aid: Long, @Query("bvid") bvid: String, @Query("graph_version") graphVersion: Long, @Query("edge_id") edgeId: Long): ApiResponse<JsonElement>

    companion object {
        fun create(): BiliApiService {
            val retrofit = retrofit2.Retrofit.Builder()
                .baseUrl("https://api.bilibili.com/")
                .client(HttpClient.client)
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create(GsonConfig.gson))
                .build()
            return retrofit.create(BiliApiService::class.java)
        }
    }
}
```

- [ ] **Step 2: Add retrofit + gson-converter dependencies if missing**

Check `build.gradle.kts` — already has retrofit + gson-converter. Verify.

- [ ] **Step 3: Verify compilation**

Run: `cd /Users/yuchen/AndroidStudioProjects/Orbit && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

## Task 9: Application Init + Manifest Update

**Files:**
- Modify: `app/src/main/java/com/qx/orbit/bili/presentation/MainActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml` (if needed)

- [ ] **Step 1: Add internet permission check to AndroidManifest.xml**

Ensure `<uses-permission android:name="android.permission.INTERNET" />` exists.

- [ ] **Step 2: Initialize CookieManager in Application class or MainActivity**

Add `CookieManager.init(this)` in `onCreate`.

- [ ] **Step 3: Verify full build**

Run: `cd /Users/yuchen/AndroidStudioProjects/Orbit && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

## Task 10: Cleanup + Verify

- [ ] **Step 1: Run full build**
- [ ] **Step 2: Verify all model classes compile**
- [ ] **Step 3: Verify Retrofit service compiles with all endpoints**
