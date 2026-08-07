package com.qx.orbit.core.error

/**
 * 平台无关错误类型。
 *
 * 对应 Android 端 `Result.Error`/`BilibiliApiException`（app/src/main/java/com/qx/orbit/bili/data/remote/Result.kt），
 * 由各 transport/repository adapter 在边界处将平台异常归一化为本类型。
 */
enum class ErrorKind {
    /** 网络不可达 / 超时 / DNS 失败等传输层错误（Android ResultCall onFailure，code=-2）。 */
    NETWORK,

    /** 未登录 / 登录态失效 / 风控（如 B 站 code=-101、-352、-401、62002 等）。 */
    AUTH,

    /** B 站业务错误码（code != 0，且不属于 AUTH）。 */
    BIZ,

    /** HTTP 层错误（非 2xx，Android 侧 code=-1 且携带 httpCode）。 */
    HTTP,

    /** 响应体解析 / 结构不符合预期。 */
    PARSE,

    /** 请求被取消（协程取消或用户中断）。 */
    CANCELED,
}

/**
 * 平台无关错误模型。
 *
 * @param code     B 站业务 code；传输层错误使用内部约定负码（见 [BizErrors]）。
 * @param message  人类可读信息。
 * @param kind     错误分类。
 * @param httpCode HTTP 状态码，仅 [ErrorKind.HTTP] 时有意义，其余为 0。
 * @param cause    底层异常（平台 adapter 可选填充），common 逻辑不应依赖其具体类型。
 */
data class BizError(
    val code: Int,
    val message: String,
    val kind: ErrorKind,
    val httpCode: Int = 0,
    val cause: Throwable? = null,
) {
    /** 便捷判断：是否认证类错误。 */
    fun isAuth(): Boolean = kind == ErrorKind.AUTH
}

/**
 * 错误工厂与内部约定负码。
 *
 * 负码与 Android 端 `ResultCall.kt` 对齐：
 *  - -2 网络错误（onFailure）
 *  - -1 HTTP 错误 / 空 body
 * 其余负码为本模块内部约定。
 */
object BizErrors {
    const val CODE_NETWORK: Int = -2
    const val CODE_HTTP: Int = -1
    const val CODE_PARSE: Int = -3
    const val CODE_CANCELED: Int = -4

    /** 网络错误（对应 Android ResultCall onFailure code=-2）。 */
    fun network(message: String = "Network error"): BizError =
        BizError(CODE_NETWORK, message, ErrorKind.NETWORK)

    /** HTTP 非 2xx 错误（对应 Android ResultCall 非成功响应 code=-1 + httpCode）。 */
    fun http(httpCode: Int, message: String = "HTTP $httpCode"): BizError =
        BizError(CODE_HTTP, message, ErrorKind.HTTP, httpCode = httpCode)

    /** B 站业务 code != 0；自动按 [classifyBiliCode] 归入 AUTH 或 BIZ。 */
    fun api(code: Int, message: String = "API error $code"): BizError =
        classifyBiliCode(code, message)

    /** 强制按 AUTH 分类的业务错误。 */
    fun auth(code: Int = -101, message: String = "Not logged in"): BizError =
        BizError(code, message, ErrorKind.AUTH)

    /** 解析错误。 */
    fun parse(message: String = "Parse error"): BizError =
        BizError(CODE_PARSE, message, ErrorKind.PARSE)

    /** 取消错误。 */
    fun canceled(message: String = "Canceled"): BizError =
        BizError(CODE_CANCELED, message, ErrorKind.CANCELED)

    /** 空白 body / 无响应体（对应 Android ResultCall “Empty response body”）。 */
    fun emptyBody(message: String = "Empty response body"): BizError =
        BizError(CODE_HTTP, message, ErrorKind.PARSE)
}

/** 已知的“登录态失效 / 风控” B 站业务码。 */
val AUTH_ERROR_CODES: Set<Int> = setOf(-101, -102, -352, -400, -401, -412, 62002, 62004)

/** 纯函数：判断 B 站业务码是否为认证类错误。 */
fun isAuthCode(code: Int): Boolean = code in AUTH_ERROR_CODES

/** 纯函数：将 B 站业务 code 分类为 AUTH 或 BIZ（恒为非网络/HTTP 错误）。 */
fun classifyBiliCode(code: Int, message: String = "API error $code"): BizError =
    if (isAuthCode(code)) BizError(code, message, ErrorKind.AUTH)
    else BizError(code, message, ErrorKind.BIZ)

/** 纯函数：按 HTTP 状态码分类。401/403 视为认证类（需刷新登录态）。 */
fun classifyHttp(httpCode: Int, message: String = "HTTP $httpCode"): BizError =
    when (httpCode) {
        401, 403 -> BizError(BizErrors.CODE_HTTP, message, ErrorKind.AUTH, httpCode = httpCode)
        else -> BizError(BizErrors.CODE_HTTP, message, ErrorKind.HTTP, httpCode = httpCode)
    }

/** 纯函数：将任意异常归一为 BizError（取消 → CANCELED，其余 → NETWORK）。 */
fun classifyThrowable(t: Throwable, message: String = t.message ?: "Unknown error"): BizError =
    if (t is kotlinx.coroutines.CancellationException) BizError(BizErrors.CODE_CANCELED, message, ErrorKind.CANCELED)
    else BizError(BizErrors.CODE_NETWORK, message, ErrorKind.NETWORK, cause = t)
