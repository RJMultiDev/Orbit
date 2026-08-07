package com.qx.orbit.core.model.social

import com.qx.orbit.core.model.content.Wire

/**
 * TV/HD 扫码登录领域模型（组 2）。
 *
 * 对应 Android `data/model/TvLogin.kt`，wire 键与 `@SerializedName`
 * 指定的字段一致（`auth_code`/`expires_in`/`token_info` 等）。
 */
/** TV/HD 扫码登录 - 获取 auth_code 响应。 */
data class TvQrCodeAuth(
    val authCode: String = "",
    val url: String = "",
    val expiresIn: Long = 0
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): TvQrCodeAuth = TvQrCodeAuth(
            authCode = Wire.str(fields, "auth_code", "authCode"),
            url = Wire.str(fields, "url"),
            expiresIn = Wire.long(fields, "expires_in", "expiresIn")
        )
    }
}

/** TV/HD 扫码登录 - 轮询结果。 */
data class TvQrCodePoll(
    val authCode: String = "",
    val status: Boolean = false,
    val tokenInfo: TvTokenInfo? = null,
    val cookieInfo: TvCookieInfo? = null
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): TvQrCodePoll = TvQrCodePoll(
            authCode = Wire.str(fields, "auth_code", "authCode"),
            status = Wire.bool(fields, "status"),
            tokenInfo = Wire.map(fields, "token_info", "tokenInfo").let { it?.let(TvTokenInfo::fromWire) },
            cookieInfo = Wire.map(fields, "cookie_info", "cookieInfo").let { it?.let(TvCookieInfo::fromWire) }
        )
    }
}

data class TvTokenInfo(
    val mid: Long = 0,
    val accessToken: String = "",
    val refreshToken: String = "",
    val expiresIn: Long = 0
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): TvTokenInfo = TvTokenInfo(
            mid = Wire.long(fields, "mid"),
            accessToken = Wire.str(fields, "access_token", "accessToken"),
            refreshToken = Wire.str(fields, "refresh_token", "refreshToken"),
            expiresIn = Wire.long(fields, "expires_in", "expiresIn")
        )
    }
}

data class TvCookieInfo(
    val cookies: List<TvCookie> = emptyList()
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): TvCookieInfo = TvCookieInfo(
            cookies = Wire.mapList(fields, "cookies").map { TvCookie.fromWire(it) }
        )
    }
}

data class TvCookie(
    val name: String = "",
    val value: String = "",
    val httpOnly: Int = 0,
    val expires: Long = 0
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): TvCookie = TvCookie(
            name = Wire.str(fields, "name"),
            value = Wire.str(fields, "value"),
            httpOnly = Wire.int(fields, "http_only", "httpOnly"),
            expires = Wire.long(fields, "expires")
        )
    }
}

/** 刷新 token 的返回（对应 Android `TvRefreshData`）。 */
data class TvRefreshData(
    val tokenInfo: TvTokenInfo? = null,
    val cookieInfo: TvCookieInfo? = null
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): TvRefreshData = TvRefreshData(
            tokenInfo = Wire.map(fields, "token_info", "tokenInfo").let { it?.let(TvTokenInfo::fromWire) },
            cookieInfo = Wire.map(fields, "cookie_info", "cookieInfo").let { it?.let(TvCookieInfo::fromWire) }
        )
    }
}
