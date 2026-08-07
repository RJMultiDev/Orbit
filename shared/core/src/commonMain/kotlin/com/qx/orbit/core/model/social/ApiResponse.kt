package com.qx.orbit.core.model.social

import com.qx.orbit.core.model.content.Wire

/**
 * 通用接口响应包装（组 2）。
 *
 * 对应 Android `data/model/ApiResponse.kt`。区别于 `model.content.ApiResult`
 * （分页状态包装），此处为通用 `code/message/data` 信封，供具型解析复用，
 * 例如 `ApiResponse.fromWire(fields, ::Reply.fromWire)`。
 */
data class ApiResponse<T>(
    val code: Int = -1,
    val message: String? = null,
    val data: T? = null
) {
    val isSuccess: Boolean get() = code == 0

    companion object {
        /** data 保持原始 wire 值（Map / List / 标量）。 */
        fun fromWire(fields: Map<String, Any?>): ApiResponse<Any?> = ApiResponse(
            code = Wire.int(fields, "code"),
            message = Wire.strOrNull(fields, "message", "msg"),
            data = fields["data"] ?: fields["result"]
        )

        /** data 为对象时经 [dataParser] 映射为 T（如 `ApiResponse.fromWire(f, ::Reply.fromWire)`）。 */
        fun <T> fromWire(fields: Map<String, Any?>, dataParser: (Map<String, Any?>) -> T?): ApiResponse<T> =
            ApiResponse(
                code = Wire.int(fields, "code"),
                message = Wire.strOrNull(fields, "message", "msg"),
                data = Wire.map(fields, "data", "result")?.let(dataParser)
            )
    }
}
