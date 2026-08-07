package com.qx.orbit.core.transport

import com.qx.orbit.core.error.BizResult

/**
 * 登录态凭据（Cookie / Token / 用户信息）读写 port。
 *
 * Android 由 `CookieManager`（com.qx.orbit.bili.data.remote.CookieManager，SharedPreferences 实现）适配；
 * Harmony 使用安全存储，不复制明文 Cookie 方案。
 */
interface CredentialStore {
    /** 是否已登录（存在有效 Cookie/Token）。 */
    val isLoggedIn: Boolean

    /** 完整 Cookie 串（形如 "SESSDATA=xxx; bili_jct=yyy; ..."）。 */
    fun getCookie(): String

    /** 替换整个 Cookie 串。 */
    fun setCookie(cookie: String)

    /** 从 Cookie 串取单个值（如 "bili_jct"、["DedeUserID"]）。 */
    fun getCookieValue(name: String): String

    /** 写入单个 Cookie 值（合并进现有 Cookie 串）。 */
    fun putCookie(name: String, value: String)

    /** CSRF 令牌（bili_jct）。 */
    fun csrf(): String

    /** 当前用户 mid。 */
    fun mid(): Long

    fun setMid(mid: Long)

    /** 刷新口令（refresh_token / ac_time_value）。 */
    fun refreshToken(): String

    /** 持久化新的刷新口令。 */
    fun setRefreshToken(token: String)

    /** 清除全部凭据（退出登录）。 */
    fun clear()

    /** Token 刷新回调：检测到登录态失效时由上层调用以续期。 */
    var tokenRefresher: TokenRefresher?
}

/**
 * Token/Cookie 刷新 port。对应 Android 端 `CookieRefreshApi.doCookieRefresh()`、
 * `BiliApiService.refreshTvToken()`/`refreshCookie()`/`confirmRefreshCookie()`。
 */
fun interface TokenRefresher {
    /** @return true 表示刷新成功，false 表示无需刷新或刷新未完成（force 为 true 时强制执行）。 */
    suspend fun refresh(force: Boolean): BizResult<Boolean>
}
