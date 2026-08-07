package com.qx.orbit.core.repository

import com.qx.orbit.core.error.BizResult
import com.qx.orbit.core.model.content.NavInfo

/** 扫码登录二维码信息（Web 端）。 */
data class QrLoginInfo(
    val url: String = "",
    val qrcodeKey: String = "",
)

/** 扫码登录轮询状态。code 对齐 B 站 passport qrcode/poll。 */
enum class QrPollCode {
    SUCCESS,          // 0：登录成功
    SCANNED_PENDING,  // 86090：已扫码待确认
    EXPIRED,          // 86038：二维码已失效
    PENDING,          // 86101：未扫码
    ERROR,
}

data class QrLoginStatus(
    val code: QrPollCode,
    val message: String = "",
    val refreshToken: String? = null,
    val timestamp: Long = 0,
)

/** TV/HD 扫码登录。 */
data class TvQrLoginInfo(
    val authCode: String = "",
)

data class TvQrLoginStatus(
    val code: Int = -1,
    val message: String = "",
    val accessToken: String? = null,
    val refreshToken: String? = null,
)

/**
 * 认证领域 repository：扫码登录 / 刷新 / 退出 / 登录态查询。
 *
 * Android 映射（app/src/main/java/com/qx/orbit/bili/data/）：
 *  - 扫码登录：`LoginApi.getLoginQR()`、`LoginApi.getLoginState()`（GET passport /x/passport-login/web/qrcode/generate、/qrcode/poll）
 *  - TV/HD 登录：`LoginApi.getTvAuthCode()`、`LoginApi.pollTvQrCode()`（BiliApiService.getTvAuthCode/pollTvQrCode，@AppSign）
 *  - Cookie 激活：`CookiesApi.activeCookieInfo()`（BiliApiService.activeCookie）
 *  - 刷新：`CookieRefreshApi.doCookieRefresh()`（cookieInfo/refreshCookie/confirmRefreshCookie）、`BiliApiService.refreshTvToken()`
 *  - 退出：`UserInfoApi.exitLogin()`（POST passport /login/exit/v2）
 *  - 登录态：`UserInfoApi.getNavInfo()`/`getCurrentUserInfo()`（BiliApiService.getNav/getMyInfo）、`CookieManager.isLoggedIn`
 */
interface AuthRepository {

    /** 生成 Web 端扫码登录二维码。返回 (url, qrcodeKey)。 */
    suspend fun createQrLogin(): BizResult<QrLoginInfo>

    /** 轮询扫码登录状态。调用方按 [QrPollCode] 驱动 UI 状态机。 */
    suspend fun pollQrLogin(qrcodeKey: String): BizResult<QrLoginStatus>

    /** 生成 TV/HD 端 authCode。 */
    suspend fun createTvQrLogin(): BizResult<TvQrLoginInfo>

    /** 轮询 TV/HD 扫码状态。成功后可调用 [activateCookie]。 */
    suspend fun pollTvQrLogin(authCode: String): BizResult<TvQrLoginStatus>

    /** 激活 Cookie（HD/TV 登录后调用，否则 /nav 判未登录）。 */
    suspend fun activateCookie(): BizResult<Unit>

    /**
     * 刷新登录态（Cookie 或 Token）。
     * @param force true 时忽略“无需刷新”结果强制执行（自动刷新用 false）。
     * @return true 表示已刷新/无需刷新，false 表示刷新失败。
     */
    suspend fun refreshLogin(force: Boolean = false): BizResult<Boolean>

    /** 退出登录（调服务端退出接口并清除本地凭据）。 */
    suspend fun logout(): BizResult<Unit>

    /** 登录态查询。 */
    suspend fun isLoggedIn(): BizResult<Boolean>

    /** 当前用户导航信息（未登录时 [NavInfo.isLogin] == false）。 */
    suspend fun currentUser(): BizResult<NavInfo>
}
