package com.qx.orbit.ohos.pages

/**
 * 跨页参数透传（进程内共享）：RouterModule.openPage 的 params 在 ComposeContainer
 * 上的读取 API 尚待真机确认，先用内存 map 保证可用性。
 */
object PageArgs {
    private val map = mutableMapOf<String, Any?>()

    fun set(key: String, value: Any?) {
        map[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? = map[key] as? T

    fun getString(key: String): String = map[key] as? String ?: ""

    fun getLong(key: String): Long = (map[key] as? Number)?.toLong() ?: 0L
}
