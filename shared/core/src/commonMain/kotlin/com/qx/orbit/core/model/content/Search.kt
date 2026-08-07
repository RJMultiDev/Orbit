package com.qx.orbit.core.model.content

/**
 * 搜索结果（对应 Android `data/model/Search.kt`）。
 *
 * Android 侧 `SearchResultGroup.data` 为 `List<JsonElement>`；领域层改为
 * `List<Map<String, Any?>>`（原始 wire 对象），避免依赖 Gson/JsonElement。
 */
data class SearchData(
    val seid: String = "",
    val result: List<SearchResultGroup> = emptyList()
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): SearchData = SearchData(
            seid = Wire.str(fields, "seid"),
            result = Wire.mapList(fields, "result").map { SearchResultGroup.fromWire(it) }
        )
    }
}

data class SearchResultGroup(
    val result_type: String = "",
    val data: List<Map<String, Any?>> = emptyList()
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): SearchResultGroup = SearchResultGroup(
            result_type = Wire.str(fields, "result_type"),
            data = Wire.mapList(fields, "data")
        )
    }
}
