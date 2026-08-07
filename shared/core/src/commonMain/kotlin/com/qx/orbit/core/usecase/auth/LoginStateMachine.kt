package com.qx.orbit.core.usecase.auth

/**
 * 登录态阶段。
 *
 * 对应 Android 端 `LoginViewModel`/`MainViewModel` 的登录相关状态与
 * `CookieRefreshApi.RefreshResult` 的失效判定；Harmony 端可直接复用。
 */
enum class LoginPhase {
    /** 未登录（初始态 / 退出后 / 扫码失败回退）。 */
    ANONYMOUS,

    /** 扫码登录进行中（二维码已生成，等待扫码 / 确认）。 */
    SCANNING,

    /** 已登录（凭据有效）。 */
    LOGGED_IN,

    /** 登录态失效（服务端判定过期或刷新失败），需要重新登录。 */
    EXPIRED,
}

/** 驱动登录态迁移的事件。 */
sealed interface LoginEvent {
    /** 开始扫码登录。 */
    data object StartLogin : LoginEvent

    /** 登录成功（凭据已保存）。 */
    data object LoginSuccess : LoginEvent

    /** 登录失败 / 用户取消扫码。 */
    data object LoginFailed : LoginEvent

    /** 检测到登录态失效（如 -101/-352 或 Cookie 刷新失败）。 */
    data object SessionExpired : LoginEvent

    /** 主动退出。 */
    data object Logout : LoginEvent
}

/**
 * 登录态机（纯函数，无副作用）。
 *
 * [transition] 返回迁移后的状态；非法事件-状态组合返回原状态不变（调用方应忽略），
 * 与 `com.qx.orbit.core.download.DownloadStateMachine` 的约定保持一致。
 *
 * 合法迁移：
 * ```
 * ANONYMOUS --StartLogin--> SCANNING
 * SCANNING  --LoginSuccess--> LOGGED_IN
 * SCANNING  --LoginFailed --> ANONYMOUS
 * SCANNING  --SessionExpired--> EXPIRED
 * LOGGED_IN --SessionExpired--> EXPIRED
 * LOGGED_IN --Logout--> ANONYMOUS
 * EXPIRED   --StartLogin--> SCANNING
 * EXPIRED   --Logout--> ANONYMOUS
 * ```
 */
object LoginStateMachine {

    fun transition(phase: LoginPhase, event: LoginEvent): LoginPhase = when (phase) {
        LoginPhase.ANONYMOUS -> when (event) {
            LoginEvent.StartLogin -> LoginPhase.SCANNING
            else -> phase
        }
        LoginPhase.SCANNING -> when (event) {
            LoginEvent.LoginSuccess -> LoginPhase.LOGGED_IN
            LoginEvent.LoginFailed -> LoginPhase.ANONYMOUS
            LoginEvent.SessionExpired -> LoginPhase.EXPIRED
            else -> phase
        }
        LoginPhase.LOGGED_IN -> when (event) {
            LoginEvent.SessionExpired -> LoginPhase.EXPIRED
            LoginEvent.Logout -> LoginPhase.ANONYMOUS
            else -> phase
        }
        LoginPhase.EXPIRED -> when (event) {
            LoginEvent.StartLogin -> LoginPhase.SCANNING
            LoginEvent.Logout -> LoginPhase.ANONYMOUS
            else -> phase
        }
    }
}
