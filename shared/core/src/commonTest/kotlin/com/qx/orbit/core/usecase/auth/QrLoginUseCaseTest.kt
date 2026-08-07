package com.qx.orbit.core.usecase.auth

import com.qx.orbit.core.error.BizError
import com.qx.orbit.core.error.BizErrors
import com.qx.orbit.core.error.BizResult
import com.qx.orbit.core.error.ErrorKind
import com.qx.orbit.core.model.content.NavInfo
import com.qx.orbit.core.repository.AuthRepository
import com.qx.orbit.core.repository.QrLoginInfo
import com.qx.orbit.core.repository.QrLoginStatus
import com.qx.orbit.core.repository.QrPollCode
import com.qx.orbit.core.repository.TvQrLoginInfo
import com.qx.orbit.core.repository.TvQrLoginStatus
import com.qx.orbit.core.transport.CredentialStore
import com.qx.orbit.core.transport.HttpMethod
import com.qx.orbit.core.transport.TokenRefresher
import com.qx.orbit.core.transport.TransportClient
import com.qx.orbit.core.transport.TransportRequest
import com.qx.orbit.core.transport.TransportResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest

/**
 * QrLoginUseCase 测试：注入 fake TransportClient 支撑的 fake AuthRepository 与
 * fake CredentialStore，模拟轮询各阶段。
 *
 * 所有 fake 数据均脱敏：无真实账号、Cookie、refresh_token、私有 URL 参数。
 */
class QrLoginUseCaseTest {

    // ---- 单次轮询：各阶段映射 ----

    @Test
    fun `poll pending maps to QR_WAIT`() = runTest {
        val repo = repoWith(script = listOf(status(QrPollCode.PENDING)))
        val store = FakeCredentialStore()
        val useCase = QrLoginUseCase(authRepository = repo, credentialStore = store)

        assertEquals(QrLoginPhase.QR_WAIT, useCase.poll("fake-qr-key"))
    }

    @Test
    fun `poll scanned maps to QR_SCANNED`() = runTest {
        val repo = repoWith(script = listOf(status(QrPollCode.SCANNED_PENDING, message = "已扫码")))
        val useCase = QrLoginUseCase(authRepository = repo, credentialStore = FakeCredentialStore())

        assertEquals(QrLoginPhase.QR_SCANNED, useCase.poll("fake-qr-key"))
    }

    @Test
    fun `poll expired maps to EXPIRED`() = runTest {
        val repo = repoWith(script = listOf(status(QrPollCode.EXPIRED, message = "二维码已失效")))
        val useCase = QrLoginUseCase(authRepository = repo, credentialStore = FakeCredentialStore())

        assertEquals(QrLoginPhase.EXPIRED, useCase.poll("fake-qr-key"))
    }

    @Test
    fun `poll error maps to FAILED`() = runTest {
        val repo = repoWith(script = listOf(status(QrPollCode.ERROR)))
        val useCase = QrLoginUseCase(authRepository = repo, credentialStore = FakeCredentialStore())

        assertEquals(QrLoginPhase.FAILED, useCase.poll("fake-qr-key"))
    }

    @Test
    fun `poll network failure maps to FAILED`() = runTest {
        val repo = repoWith(script = listOf(BizResult.failure(BizErrors.network("timeout"))))
        val useCase = QrLoginUseCase(authRepository = repo, credentialStore = FakeCredentialStore())

        assertEquals(QrLoginPhase.FAILED, useCase.poll("fake-qr-key"))
    }

    @Test
    fun `poll success saves refresh token and returns QR_CONFIRMED`() = runTest {
        val repo = repoWith(
            script = listOf(status(QrPollCode.SUCCESS, refreshToken = FAKE_REFRESH_TOKEN))
        )
        val store = FakeCredentialStore()
        val useCase = QrLoginUseCase(authRepository = repo, credentialStore = store)

        assertEquals(QrLoginPhase.QR_CONFIRMED, useCase.poll("fake-qr-key"))
        assertEquals(listOf(FAKE_REFRESH_TOKEN), store.savedRefreshTokens)
    }

    @Test
    fun `poll success without refresh token saves nothing`() = runTest {
        val repo = repoWith(script = listOf(status(QrPollCode.SUCCESS)))
        val store = FakeCredentialStore()
        val useCase = QrLoginUseCase(authRepository = repo, credentialStore = store)

        assertEquals(QrLoginPhase.QR_CONFIRMED, useCase.poll("fake-qr-key"))
        assertTrue(store.savedRefreshTokens.isEmpty())
    }

    @Test
    fun `poll routes qrcodeKey to the poll endpoint`() = runTest {
        val transport = FakeTransportClient()
        val repo = FakeAuthRepository(transport, script = listOf(status(QrPollCode.PENDING)))
        val useCase = QrLoginUseCase(authRepository = repo, credentialStore = FakeCredentialStore())

        useCase.poll("fake-qr-key")

        assertEquals(listOf("fake-qr-key"), repo.pollKeys)
        assertTrue(
            transport.executed.any {
                it.url.contains("/x/passport-login/web/qrcode/poll") &&
                    it.url.contains("qrcode_key=fake-qr-key")
            }
        )
    }

    // ---- 取消传播 ----

    @Test
    fun `poll propagates cancellation when repository reports canceled`() = runTest {
        val repo = repoWith(script = listOf(BizResult.failure(BizErrors.canceled("user canceled"))))
        val useCase = QrLoginUseCase(authRepository = repo, credentialStore = FakeCredentialStore())

        val thrown = try {
            useCase.poll("fake-qr-key")
            null
        } catch (e: CancellationException) {
            e
        }
        assertTrue(thrown is CancellationException)
    }

    @Test
    fun `poll propagates the original cancellation cause`() = runTest {
        val original = CancellationException("job canceled")
        val error = BizError(BizErrors.CODE_CANCELED, "canceled", ErrorKind.CANCELED, cause = original)
        val repo = repoWith(script = listOf(BizResult.failure(error)))
        val useCase = QrLoginUseCase(authRepository = repo, credentialStore = FakeCredentialStore())

        val thrown = try {
            useCase.poll("fake-qr-key")
            null
        } catch (e: CancellationException) {
            e
        }
        assertEquals("job canceled", thrown?.message)
    }

    // ---- 完整流程 ----

    @Test
    fun `run completes full flow saves token and refreshes cookie`() = runTest {
        val transport = FakeTransportClient()
        val repo = FakeAuthRepository(
            transport,
            script = listOf(
                status(QrPollCode.PENDING),
                status(QrPollCode.SCANNED_PENDING, message = "已扫码"),
                status(QrPollCode.SUCCESS, refreshToken = FAKE_REFRESH_TOKEN),
            ),
        )
        val store = FakeCredentialStore()
        val refresher = FakeTokenRefresher()
        val useCase = QrLoginUseCase(
            authRepository = repo,
            credentialStore = store,
            cookieRefresher = refresher,
            delayFn = { },
        )

        val phase = useCase.run()

        assertEquals(QrLoginPhase.SUCCESS, phase)
        assertEquals(1, repo.createQrCalls)
        assertEquals(listOf("fake-qr-key", "fake-qr-key", "fake-qr-key"), repo.pollKeys)
        assertEquals(listOf(FAKE_REFRESH_TOKEN), store.savedRefreshTokens)
        assertEquals(1, refresher.refreshCalls)
        assertEquals(false, refresher.lastForce)
        // 垂直切片接线：createQr 命中 generate 端点；poll 命中 poll 端点且携带 qrcode_key
        assertTrue(transport.executed.any { it.url.contains("/x/passport-login/web/qrcode/generate") })
        assertTrue(
            transport.executed.any {
                it.url.contains("/x/passport-login/web/qrcode/poll") &&
                    it.url.contains("qrcode_key=fake-qr-key")
            }
        )
    }

    @Test
    fun `run with explicit qrcodeKey skips qr generation`() = runTest {
        val repo = repoWith(
            script = listOf(status(QrPollCode.PENDING), status(QrPollCode.EXPIRED))
        )
        val useCase = QrLoginUseCase(authRepository = repo, credentialStore = FakeCredentialStore(), delayFn = { })

        assertEquals(QrLoginPhase.EXPIRED, useCase.run("fake-qr-key"))
        assertEquals(0, repo.createQrCalls)
    }

    @Test
    fun `run without cookie refresher still returns SUCCESS`() = runTest {
        val repo = repoWith(script = listOf(status(QrPollCode.SUCCESS, refreshToken = FAKE_REFRESH_TOKEN)))
        val store = FakeCredentialStore()
        val useCase = QrLoginUseCase(authRepository = repo, credentialStore = store, delayFn = { })

        assertEquals(QrLoginPhase.SUCCESS, useCase.run())
        assertEquals(listOf(FAKE_REFRESH_TOKEN), store.savedRefreshTokens)
    }

    @Test
    fun `run returns EXPIRED when the QR code expires during polling`() = runTest {
        val repo = repoWith(
            script = listOf(status(QrPollCode.PENDING), status(QrPollCode.EXPIRED, message = "二维码已失效"))
        )
        val useCase = QrLoginUseCase(authRepository = repo, credentialStore = FakeCredentialStore(), delayFn = { })

        assertEquals(QrLoginPhase.EXPIRED, useCase.run())
    }

    @Test
    fun `run returns FAILED when qr generation fails`() = runTest {
        val repo = FakeAuthRepository(
            FakeTransportClient(),
            createQrResult = BizResult.failure(BizErrors.network("generate failed")),
        )
        val useCase = QrLoginUseCase(authRepository = repo, credentialStore = FakeCredentialStore(), delayFn = { })

        assertEquals(QrLoginPhase.FAILED, useCase.run())
        assertTrue(repo.pollKeys.isEmpty())
    }

    @Test
    fun `run returns EXPIRED when poll exceeds timeout`() = runTest {
        var tick = 0L
        val clock = { tick++ }
        val useCase = QrLoginUseCase(
            authRepository = repoWith(), // fallback 恒为 PENDING，永不成功
            credentialStore = FakeCredentialStore(),
            timeoutMillis = 3L,
            pollIntervalMillis = 10L,
            nowMillis = clock,
            delayFn = { },
        )

        assertEquals(QrLoginPhase.EXPIRED, useCase.run())
    }

    // ---- fakes ----

    private fun repoWith(
        script: List<BizResult<QrLoginStatus>> = emptyList(),
        fallback: BizResult<QrLoginStatus> = BizResult.success(QrLoginStatus(QrPollCode.PENDING)),
    ): FakeAuthRepository =
        FakeAuthRepository(FakeTransportClient(), script = script, fallback = fallback)

    private fun status(
        code: QrPollCode,
        message: String = "",
        refreshToken: String? = null,
    ): BizResult<QrLoginStatus> =
        BizResult.success(QrLoginStatus(code = code, message = message, refreshToken = refreshToken))

    /** 记录实际发出的请求，供断言“轮询请求注入正确”。 */
    private class FakeTransportClient : TransportClient {
        val executed = mutableListOf<TransportRequest>()

        override suspend fun execute(request: TransportRequest): TransportResponse {
            executed += request
            return TransportResponse(status = 200, body = "{}".encodeToByteArray())
        }
    }

    /** 未使用的方法默认抛错；子类只需覆盖被测路径。 */
    private open class StubAuthRepository : AuthRepository {
        protected fun unsupported(): Nothing = error("not used in this test")

        override suspend fun createQrLogin(): BizResult<QrLoginInfo> = unsupported()
        override suspend fun pollQrLogin(qrcodeKey: String): BizResult<QrLoginStatus> = unsupported()
        override suspend fun createTvQrLogin(): BizResult<TvQrLoginInfo> = unsupported()
        override suspend fun pollTvQrLogin(authCode: String): BizResult<TvQrLoginStatus> = unsupported()
        override suspend fun activateCookie(): BizResult<Unit> = unsupported()
        override suspend fun refreshLogin(force: Boolean): BizResult<Boolean> = unsupported()
        override suspend fun logout(): BizResult<Unit> = unsupported()
        override suspend fun isLoggedIn(): BizResult<Boolean> = unsupported()
        override suspend fun currentUser(): BizResult<NavInfo> = unsupported()
    }

    /**
     * 模拟 Android `LoginApi` adapter：二维码/轮询请求经由 [TransportClient] 发出，
     * 轮询响应来自脚本队列（模拟各阶段），脚本耗尽时返回 [fallback]。
     */
    private class FakeAuthRepository(
        private val transport: TransportClient,
        private val script: List<BizResult<QrLoginStatus>> = emptyList(),
        private val fallback: BizResult<QrLoginStatus> = BizResult.success(QrLoginStatus(QrPollCode.PENDING)),
        private val createQrResult: BizResult<QrLoginInfo> = BizResult.success(
            QrLoginInfo(
                url = "https://www.bilibili.com/fake-login?channel=test",
                qrcodeKey = "fake-qr-key",
            )
        ),
    ) : StubAuthRepository() {

        private val queue = ArrayDeque(script)
        val pollKeys = mutableListOf<String>()
        var createQrCalls = 0

        override suspend fun createQrLogin(): BizResult<QrLoginInfo> {
            createQrCalls++
            transport.execute(
                TransportRequest(
                    method = HttpMethod.GET,
                    url = "https://passport.bilibili.com/x/passport-login/web/qrcode/generate",
                )
            )
            return createQrResult
        }

        override suspend fun pollQrLogin(qrcodeKey: String): BizResult<QrLoginStatus> {
            pollKeys += qrcodeKey
            transport.execute(
                TransportRequest(
                    method = HttpMethod.GET,
                    url = "https://passport.bilibili.com/x/passport-login/web/qrcode/poll?qrcode_key=$qrcodeKey",
                )
            )
            return queue.removeFirstOrNull() ?: fallback
        }
    }

    private class FakeCredentialStore : CredentialStore {
        private var cookie = "SESSDATA=fake-sessdata; bili_jct=fake-csrf"
        private var mid: Long = 0L
        private var refreshToken = ""

        val savedRefreshTokens = mutableListOf<String>()
        var clearCount = 0

        override var tokenRefresher: TokenRefresher? = null
        override val isLoggedIn: Boolean get() = cookie.contains("SESSDATA")

        override fun getCookie(): String = cookie
        override fun setCookie(cookie: String) {
            this.cookie = cookie
        }

        override fun getCookieValue(name: String): String =
            cookie.split(";").map { it.trim() }
                .firstOrNull { it.startsWith("$name=") }
                ?.substringAfter("=")
                ?: ""

        override fun putCookie(name: String, value: String) {
            val others = cookie.split(";").map { it.trim() }.filter { !it.startsWith("$name=") }
            cookie = (others + "$name=$value").joinToString("; ")
        }

        override fun csrf(): String = getCookieValue("bili_jct")
        override fun mid(): Long = mid
        override fun setMid(mid: Long) {
            this.mid = mid
        }

        override fun refreshToken(): String = refreshToken
        override fun setRefreshToken(token: String) {
            refreshToken = token
            savedRefreshTokens += token
        }

        override fun clear() {
            clearCount++
            cookie = ""
            refreshToken = ""
            mid = 0L
        }
    }

    private class FakeTokenRefresher(
        private val result: BizResult<Boolean> = BizResult.success(true),
    ) : TokenRefresher {
        var refreshCalls = 0
        var lastForce: Boolean? = null

        override suspend fun refresh(force: Boolean): BizResult<Boolean> {
            refreshCalls++
            lastForce = force
            return result
        }
    }

    private companion object {
        const val FAKE_REFRESH_TOKEN = "fake-refresh-token"
    }
}
