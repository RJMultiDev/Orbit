package com.qx.orbit.core.transport

/**
 * 平台无关 HTTP 方法。对应 Retrofit 的 @GET/@POST/@FormUrlEncoded 等标注。
 */
enum class HttpMethod {
    GET, POST, PUT, DELETE, PATCH, HEAD,
}

/**
 * 平台无关请求。
 *
 * @param method        HTTP 方法。
 * @param url           完整 URL（已含 query 或由 [queryParams] 显式携带均可）。
 * @param headers       请求头（如 Cookie、User-Agent、Referer、Range）。
 * @param queryParams   显式 query 参数（顺序敏感，便于签名与测试）。
 * @param body          请求体字节；GET/HEAD 应为 null。
 */
data class TransportRequest(
    val method: HttpMethod,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val queryParams: List<Pair<String, String>> = emptyList(),
    val body: ByteArray? = null,
) {
    /** 便捷构造：以 form 字段构造 body（application/x-www-form-urlencoded）。 */
    fun withFormBody(form: Map<String, String>): TransportRequest = copy(
        headers = headers + ("Content-Type" to "application/x-www-form-urlencoded"),
        body = form.entries.joinToString("&") { (k, v) -> "${k.encodeUrl()}=${v.encodeUrl()}" }.encodeToByteArray(),
    )

    /** 便捷构造：以 JSON 字符串构造 body。 */
    fun withJsonBody(json: String): TransportRequest = copy(
        headers = headers + ("Content-Type" to "application/json"),
        body = json.encodeToByteArray(),
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TransportRequest) return false
        if (method != other.method) return false
        if (url != other.url) return false
        if (headers != other.headers) return false
        if (queryParams != other.queryParams) return false
        return body.contentEquals(other.body)
    }

    override fun hashCode(): Int {
        var result = method.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + headers.hashCode()
        result = 31 * result + queryParams.hashCode()
        result = 31 * result + (body?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * 平台无关响应。
 *
 * @param status   HTTP 状态码。
 * @param headers  响应头（保留顺序与重复项，如多条 Set-Cookie）。
 * @param body     响应体字节。
 */
data class TransportResponse(
    val status: Int,
    val headers: List<Pair<String, String>> = emptyList(),
    val body: ByteArray = ByteArray(0),
) {
    /** 取首个同名头（大小写不敏感）。 */
    fun header(name: String): String? =
        headers.firstOrNull { it.first.equals(name, ignoreCase = true) }?.second

    /** 取全部同名头（如 Set-Cookie）。 */
    fun headers(name: String): List<String> =
        headers.filter { it.first.equals(name, ignoreCase = true) }.map { it.second }

    val isSuccessful: Boolean get() = status in 200..299

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TransportResponse) return false
        if (status != other.status) return false
        if (headers != other.headers) return false
        return body.contentEquals(other.body)
    }

    override fun hashCode(): Int {
        var result = status
        result = 31 * result + headers.hashCode()
        result = 31 * result + body.contentHashCode()
        return result
    }
}

/**
 * Transport port：Android 由 OkHttp/Retrofit 网络层实现（复用 HttpClient 的 UA/Referer/IPv4/重定向/Cookie 语义），
 * Harmony 由原生网络栈实现。所有字节/URL/Header 均为平台无关结构，common 层不接触 okhttp/retrofit。
 */
interface TransportClient {
    suspend fun execute(request: TransportRequest): TransportResponse
}

private fun String.encodeUrl(): String = encodeToByteArray().joinToString("") { b ->
    val c = b.toInt() and 0xFF
    when (c) {
        in 'a'.code..'z'.code, in 'A'.code..'Z'.code, in '0'.code..'9'.code,
        '-'.code, '_'.code, '.'.code, '~'.code -> c.toChar().toString()
        else -> "%" + c.toString(16).uppercase().padStart(2, '0')
    }
}
