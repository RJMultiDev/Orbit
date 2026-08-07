package com.qx.orbit.core.danmaku

/**
 * 弹幕过滤纯逻辑：屏蔽词 / 类型 / 正则规则。
 *
 * 语义对齐 app 端：
 * - `advancedDanmakuEnabled` ↔ `player_danmaku_advanced_enable`（关闭时把 7/8 弹幕降级为滚动）
 * - `specialDanmakuVisible` ↔ `setSpecialDanmakuVisibility`
 * - `forceR2L` ↔ DFMNext `player_danmaku_forceR2L`
 * - `duplicateMergingEnabled` ↔ `setDuplicateMerging`
 * - 文本/用户/类型屏蔽对应 DFM 的 Text/UserHash/Type 过滤器
 *
 * 注意：`DanmakuFilter`（fun interface，渲染端口谓词）定义在 [DanmakuRenderer]，此处规则引擎
 * 命名为 [DanmakuFilterEngine]，并可通过 [DanmakuFilterConfig.asRenderFilter] 桥接到端口。
 */
object DanmakuFilterEngine {

    /** 过滤并转换一批事件。先做类型变换（advanced/forceR2L），再做屏蔽，最后按需去重。 */
    fun filter(events: List<DanmakuEvent>, config: DanmakuFilterConfig): List<DanmakuEvent> {
        val transformed = ArrayList<DanmakuEvent>(events.size)
        for (e in events) {
            val t = transformMode(e, config)
            if (!shouldBlock(t, config)) transformed.add(t)
        }
        if (!config.duplicateMergingEnabled) return transformed

        // 合并重复弹幕：按时间序，同一文本在窗口内只保留最早一条。
        val sorted = transformed.sortedWith(compareBy<DanmakuEvent> { it.timeMs }.thenBy { it.sourceIndex })
        val lastSeen = HashMap<String, Long>()
        val result = ArrayList<DanmakuEvent>(sorted.size)
        for (e in sorted) {
            val last = lastSeen[e.text]
            if (last == null || e.timeMs - last >= config.duplicateWindowMs) {
                result.add(e)
                lastSeen[e.text] = e.timeMs
            }
        }
        return result
    }

    /**
     * 类型变换：
     * - advanced 关闭：SPECIAL/CODE 降级为 SCROLL（与 app `player_danmaku_advanced_enable=false` 一致）
     * - forceR2L：非特殊弹幕全部转为 SCROLL
     */
    fun transformMode(event: DanmakuEvent, config: DanmakuFilterConfig): DanmakuEvent {
        var mode = event.mode
        if (!config.advancedDanmakuEnabled && (mode == DanmakuMode.SPECIAL || mode == DanmakuMode.CODE)) {
            mode = DanmakuMode.SCROLL
        }
        if (config.forceR2L && mode != DanmakuMode.SPECIAL && mode != DanmakuMode.CODE && mode != DanmakuMode.UNKNOWN) {
            mode = DanmakuMode.SCROLL
        }
        return if (mode == event.mode) event else event.copy(mode = mode)
    }

    /** 判断单条事件是否应被屏蔽（不改变类型）。 */
    fun shouldBlock(event: DanmakuEvent, config: DanmakuFilterConfig): Boolean {
        if (config.blockModes.isNotEmpty() && event.mode in config.blockModes) return true
        if (!config.specialDanmakuVisible && event.mode == DanmakuMode.SPECIAL) return true
        if (event.userHash.isNotEmpty() && event.userHash in config.blockUserHashes) return true
        for (rule in config.blockTextRules) {
            if (rule.matches(event.text)) return true
        }
        return false
    }
}

/** 文本屏蔽规则类型。 */
enum class TextRuleType { CONTAINS, EQUALS, REGEX }

/** 文本屏蔽规则。REGEX 非法时按不匹配处理，不抛异常。 */
data class TextRule(val type: TextRuleType, val pattern: String) {
    fun matches(text: String): Boolean = when (type) {
        TextRuleType.CONTAINS -> text.contains(pattern)
        TextRuleType.EQUALS -> text == pattern
        TextRuleType.REGEX -> runCatching { Regex(pattern).containsMatchIn(text) }.getOrDefault(false)
    }
}

/** 弹幕过滤配置（全部可空安全，默认不屏蔽任何内容）。 */
data class DanmakuFilterConfig(
    /** 文本屏蔽规则。 */
    val blockTextRules: List<TextRule> = emptyList(),
    /** 屏蔽用户（按 userHash 精确匹配）。 */
    val blockUserHashes: Set<String> = emptySet(),
    /** 屏蔽弹幕类型。 */
    val blockModes: Set<DanmakuMode> = emptySet(),
    /** 高级弹幕开关：false 时 SPECIAL/CODE 降级为 SCROLL。 */
    val advancedDanmakuEnabled: Boolean = true,
    /** 强制全部滚动：非特殊弹幕转为 SCROLL。 */
    val forceR2L: Boolean = false,
    /** 特殊弹幕可见性：false 时屏蔽 SPECIAL。 */
    val specialDanmakuVisible: Boolean = true,
    /** 合并重复弹幕。 */
    val duplicateMergingEnabled: Boolean = false,
    /** 去重窗口：同一文本在此毫秒窗口内只保留最早一条。 */
    val duplicateWindowMs: Long = 10_000L,
) {
    /**
     * 桥接到渲染端口的过滤谓词 [DanmakuFilter]（[DanmakuRenderer.setFilter]）。
     * 按 [DanmakuFilterEngine.shouldBlock] 拒绝被屏蔽的事件；不包含类型变换。
     */
    fun asRenderFilter(): DanmakuFilter = DanmakuFilter { event ->
        !DanmakuFilterEngine.shouldBlock(event, this)
    }
}
