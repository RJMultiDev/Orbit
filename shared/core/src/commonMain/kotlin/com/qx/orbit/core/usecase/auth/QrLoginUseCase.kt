package com.qx.orbit.core.usecase.auth

import com.qx.orbit.core.error.BizError
import com.qx.orbit.core.error.BizResult
import com.qx.orbit.core.error.ErrorKind
import com.qx.orbit.core.error.getOrNull
import com.qx.orbit.core.repository.AuthRepository
import com.qx.orbit.core.repository.QrLoginInfo
import com.qx.orbit.core.repository.QrPollCode
import com.qx.orbit.core.transport.CredentialStore
import com.qx.orbit.core.transport.TokenRefresher
import kotlin.time.DurationUnit
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * 扫码登录流程阶段（状态机结果）。
 *
 * 对应 Android 端 `LoginViewModel.startLogin()`/`startPolling()` 的各阶段映射：
 * 86101→[QR_WAIT]、86090→[QR_SCANNED]、86038→[EXPIRED]、0→确认登录。
 */
enum class QrLoginPhase {
    /** 二维码已生成，等待扫码。 */
    QR_WAIT,

    /** 已扫码，等待用户在手机上确认。 */
    QR_SCANNED,

    /** 用户已确认、refresh_token 已保存，等待 Cookie 刷新收尾。 */
    QR_CONFIRMED,

    /** 二维码失效或轮询超时。 */
    EXPIRED,

    /** 轮询失败（网络 / 业务错误）。 */
    FAILED,

    /** 登录成功（终态）。 */
    SUCCESS,
}

/**
 * Web 端扫码登录 use case。
 *
 * 编排：生成二维码参数（[createQr]）→ 轮询扫码状态（[poll]）→
 * 超时 / 取消 → 成功保存 refresh_token 到 [CredentialStore] → 尽力刷新 Cookie（[TokenRefresher]）。
 *
 * 轮询间隔 [pollIntervalMillis] 与超时 [timeoutMillis] 可注入；[nowMillis] 与 [delayFn]
 * 仅用于测试替身（默认使用单调时钟与真实 delay），生产调用方无需关心。
 *
 * 依赖的 port（A18 契约）：
 *  - [AuthRepository.createQrLogin] / [AuthRepository.pollQrLogin]
 *  - [CredentialStore.setRefreshToken]
 *  - [TokenRefresher.refresh]（可选）
 *
 * 取消语义：轮询循环为协程取消友好的——外部取消（CancellationException）会自然传播，
 * [AuthRepository] 返回 CANCELED 失败时也会重抛而非吞掉。
 */
class QrLoginUseCase(
    private val authRepository: AuthRepository,
    private val credentialStore: CredentialStore,
    private val pollIntervalMillis: Long = 1_000L,
    private val timeoutMillis: Long = 180_000L,
    private val cookieRefresher: TokenRefresher? = null,
    private val nowMillis: () -> Long =
        { TimeSource.Monotonic.markNow().elapsedNow().toLong(DurationUnit.MILLISECONDS) },
    private val delayFn: suspend (Long) -> Unit = { delay(it) },
) {

    /**
     * 生成二维码参数（url + qrcodeKey）。
     *
     * UI 拿到 [QrLoginInfo.url] 渲染二维码后，调用 [run] 传入 `qrcodeKey` 开始轮询；
     * 失败（[BizResult.Failure]）由 UI 提示重试。
     */
    suspend fun createQr(): BizResult<QrLoginInfo> = authRepository.createQrLogin()

    /**
     * 单次轮询扫码状态。
     *
     * 返回 [QrLoginPhase.QR_CONFIRMED] 表示已确认且 refresh_token 已保存，
     * 调用方应继续执行 [run]（或自行触发 Cookie 刷新）以到达终态 [QrLoginPhase.SUCCESS]。
     *
     * 网络/业务失败映射为 [QrLoginPhase.FAILED]；取消类失败会重抛 [CancellationException]。
     */
    suspend fun poll(qrcodeKey: String): QrLoginPhase {
        val result = authRepository.pollQrLogin(qrcodeKey)
        return when (result) {
            is BizResult.Failure -> {
                result.error.throwIfCanceled()
                QrLoginPhase.FAILED
            }
            is BizResult.Success -> when (result.data.code) {
                QrPollCode.PENDING -> QrLoginPhase.QR_WAIT
                QrPollCode.SCANNED_PENDING -> QrLoginPhase.QR_SCANNED
                QrPollCode.SUCCESS -> {
                    result.data.refreshToken?.takeIf { it.isNotEmpty() }
                        ?.let { credentialStore.setRefreshToken(it) }
                    QrLoginPhase.QR_CONFIRMED
                }
                QrPollCode.EXPIRED -> QrLoginPhase.EXPIRED
                QrPollCode.ERROR -> QrLoginPhase.FAILED
            }
        }
    }

    /**
     * 完整扫码登录流程。
     *
     * [qrcodeKey] 为空时先调用 [createQr] 生成二维码；随后按 [pollIntervalMillis] 间隔
     * 轮询直到终态：确认后触发 Cookie 刷新并返回 [QrLoginPhase.SUCCESS]；
     * 二维码失效 / 轮询失败 / 超过 [timeoutMillis] 未完成时返回对应终态。
     */
    suspend fun run(qrcodeKey: String? = null): QrLoginPhase {
        val key = qrcodeKey?.takeIf { it.isNotEmpty() }
            ?: createQr().getOrNull()?.qrcodeKey
        if (key.isNullOrEmpty()) return QrLoginPhase.FAILED

        val deadline = nowMillis() + timeoutMillis
        while (true) {
            if (nowMillis() >= deadline) return QrLoginPhase.EXPIRED
            val phase = poll(key)
            when (phase) {
                QrLoginPhase.QR_CONFIRMED -> {
                    refreshCookieIfNeeded()
                    return QrLoginPhase.SUCCESS
                }
                QrLoginPhase.EXPIRED, QrLoginPhase.FAILED -> return phase
                else -> Unit // 继续轮询
            }
            delayFn(pollIntervalMillis)
        }
    }

    /**
     * 登录成功后尽力刷新 Cookie。刷新失败不阻断登录成功（登录态已由扫码确认建立）。
     */
    private suspend fun refreshCookieIfNeeded() {
        val refresher = cookieRefresher ?: return
        try {
            refresher.refresh(force = false)
        } catch (_: Throwable) {
            // 尽力而为：忽略刷新异常，返回 SUCCESS
        }
    }

    private fun BizError.throwIfCanceled() {
        val cause = this.cause
        if (cause is CancellationException) throw cause
        if (kind == ErrorKind.CANCELED) throw CancellationException(message)
    }
}
