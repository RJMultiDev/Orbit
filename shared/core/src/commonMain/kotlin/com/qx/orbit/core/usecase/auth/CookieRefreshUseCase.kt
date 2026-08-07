package com.qx.orbit.core.usecase.auth

import com.qx.orbit.core.error.BizResult
import com.qx.orbit.core.repository.AuthRepository
import com.qx.orbit.core.transport.CredentialStore
import com.qx.orbit.core.transport.TokenRefresher

/**
 * Cookie / 登录态刷新与退出清理 use case。
 *
 * 编排 [AuthRepository.refreshLogin]（对应 Android `CookieRefreshApi.doCookieRefresh`）、
 * 失效判定（[isExpired]）与退出清理（[logout]），并暴露 [tokenRefresher] 供
 * [CredentialStore.tokenRefresher] 挂接，实现“检测到登录态失效时自动续期”。
 *
 * 依赖的 port（A18 契约）：
 *  - [AuthRepository.refreshLogin] / [AuthRepository.isLoggedIn] / [AuthRepository.logout]
 *  - [CredentialStore.isLoggedIn] / [CredentialStore.clear]
 */
class CookieRefreshUseCase(
    private val authRepository: AuthRepository,
    private val credentialStore: CredentialStore,
) {

    /**
     * 判定登录态是否已失效。
     *
     * 本地无凭据或服务端明确判定未登录（含 AUTH 类错误，如 -101/-352）即视为失效；
     * 网络等临时错误不判定失效（避免弱网时误触发退出流程）。
     */
    suspend fun isExpired(): Boolean {
        if (!credentialStore.isLoggedIn) return true
        return when (val result = authRepository.isLoggedIn()) {
            is BizResult.Success -> !result.data
            is BizResult.Failure -> result.error.isAuth()
        }
    }

    /**
     * 刷新登录态。
     *
     * @param force true 时强制执行；false 时仅当服务端提示“需要刷新”时执行
     *              （对应 Android `doCookieRefresh(auto = true)` 的自动刷新路径）。
     * @return true 表示已刷新 / 无需刷新；false 表示刷新失败。
     */
    suspend fun refresh(force: Boolean = false): BizResult<Boolean> =
        authRepository.refreshLogin(force)

    /**
     * 退出登录：调用服务端退出接口，并保证本地凭据被清理（即使服务端调用失败）。
     */
    suspend fun logout(): BizResult<Unit> {
        val result = authRepository.logout()
        credentialStore.clear()
        return result
    }

    /**
     * 挂接到 [CredentialStore.tokenRefresher] 的刷新回调。
     *
     * 用法：`credentialStore.tokenRefresher = cookieRefreshUseCase.tokenRefresher`
     */
    val tokenRefresher: TokenRefresher = TokenRefresher { force -> refresh(force) }
}
