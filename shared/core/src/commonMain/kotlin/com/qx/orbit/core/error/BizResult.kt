package com.qx.orbit.core.error

/**
 * 平台无关结果类型：成功携带 [T]，失败携带 [BizError]。
 *
 * 对应 Android 端 `Result<T>`（com.qx.orbit.bili.data.remote.Result），
 * 所有 repository port 方法统一返回本类型，禁止在 common 层抛平台异常。
 *
 * 变换类辅助函数（map/mapCatching/onSuccess/onFailure/getOrNull/getOrDefault）
 * 以顶层扩展形式提供（类声明为协变，故接受 T 入参的方法不能是成员）。
 */
sealed class BizResult<out T> {
    data class Success<T>(val data: T) : BizResult<T>()
    data class Failure(val error: BizError) : BizResult<Nothing>()

    companion object {
        fun <T> success(data: T): BizResult<T> = Success(data)

        fun <T> failure(error: BizError): BizResult<T> = Failure(error)

        /**
         * 执行 [block] 并把抛出的异常归一化为 [BizResult.Failure]。
         * 取消（CancellationException）会被 [classifyThrowable] 归为 CANCELED。
         */
        inline fun <T> catching(block: () -> T): BizResult<T> =
            try {
                success(block())
            } catch (t: Throwable) {
                failure(classifyThrowable(t))
            }
    }
}

/** 成功值或 null。 */
fun <T> BizResult<T>.getOrNull(): T? = when (this) {
    is BizResult.Success -> data
    is BizResult.Failure -> null
}

/** 成功值或默认值。 */
fun <T> BizResult<T>.getOrDefault(default: T): T = when (this) {
    is BizResult.Success -> data
    is BizResult.Failure -> default
}

inline fun <T, R> BizResult<T>.map(transform: (T) -> R): BizResult<R> = when (this) {
    is BizResult.Success -> BizResult.success(transform(data))
    is BizResult.Failure -> BizResult.failure(error)
}

inline fun <T, R> BizResult<T>.mapCatching(transform: (T) -> R): BizResult<R> = when (this) {
    is BizResult.Success -> BizResult.catching { transform(data) }
    is BizResult.Failure -> BizResult.failure(error)
}

fun <T> BizResult<T>.onSuccess(action: (T) -> Unit): BizResult<T> {
    if (this is BizResult.Success) action(data)
    return this
}

fun <T> BizResult<T>.onFailure(action: (BizError) -> Unit): BizResult<T> {
    if (this is BizResult.Failure) action(error)
    return this
}
