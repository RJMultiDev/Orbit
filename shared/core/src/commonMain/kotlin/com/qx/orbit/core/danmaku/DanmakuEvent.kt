package com.qx.orbit.core.danmaku

/**
 * B 站弹幕模式（类型）。
 *
 * 语义与 app 端 DanmakuItem 传给弹幕引擎的 type 对齐，`raw` 保留 B 站原始 mode 值
 * （与 XML `<d p>` 第 2 位、protobuf `DanmakuElem.mode`、直播 WS `info[0][1]` 一致）。
 */
enum class DanmakuMode(val raw: Int, val scrolling: Boolean, val fixed: Boolean) {
    /** 滚动弹幕（右→左），B 站 mode=1 */
    SCROLL(1, true, false),
    /** 底部固定弹幕，B 站 mode=4 */
    FIX_BOTTOM(4, false, true),
    /** 顶部固定弹幕，B 站 mode=5 */
    FIX_TOP(5, false, true),
    /** 逆向滚动弹幕（左→右），B 站 mode=6 */
    REVERSE(6, true, false),
    /** 高级/特殊弹幕，B 站 mode=7 */
    SPECIAL(7, false, true),
    /** 代码弹幕，B 站 mode=8 */
    CODE(8, false, true),
    /** BAS 弹幕，B 站 mode=9 */
    BAS(9, false, true),
    /** 未知/非法模式 */
    UNKNOWN(0, false, false);

    companion object {
        /**
         * 按 B 站原始 mode 值映射。历史 mode 2/3 与 1 同为滚动，一并归入 [SCROLL]。
         * 其余未知值返回 [UNKNOWN]（渲染端可视为普通滚动）。
         */
        fun fromRaw(raw: Int): DanmakuMode = when (raw) {
            1, 2, 3 -> SCROLL
            4 -> FIX_BOTTOM
            5 -> FIX_TOP
            6 -> REVERSE
            7 -> SPECIAL
            8 -> CODE
            9 -> BAS
            else -> UNKNOWN
        }
    }
}

/**
 * 弹幕事件模型（平台无关）。
 *
 * 字段语义与 app 端 [DanmakuItem] 对齐：
 * - [timeMs] 对应 `time`（毫秒）
 * - [color] 对应 `textColor`（ARGB，含 alpha）
 * - [fontSize] 对应 `textSize`（B 站字号单位）
 * - [userHash] 对应 `userHash`
 * - [mode] 对应引擎接收的 type（保留 B 站原始值）
 *
 * 刻意不包含任何渲染/平台字段（padding、borderColor、obj 等留在渲染层）。
 */
data class DanmakuEvent(
    /** 弹幕出现时间，毫秒。 */
    val timeMs: Long,
    /** 弹幕类型。 */
    val mode: DanmakuMode,
    /** 弹幕文本（XML/JSON 实体已解码）。 */
    val text: String,
    /** ARGB 颜色（含 alpha）。用 Long 避免平台 Int 溢出（0xFFFFFFFF）。 */
    val color: Long,
    /** 字号（B 站单位，默认 25）。 */
    val fontSize: Float = 25f,
    /** 发送时间，Unix 秒（仅信息用途）。 */
    val sentAtSec: Long = 0L,
    /** 弹幕池：0 普通 / 1 字幕 / 2 特殊。 */
    val pool: Int = 0,
    /** 用户 ID hash。 */
    val userHash: String = "",
    /** dmid / 行 ID。 */
    val rowId: Long = 0L,
    /** 输入顺序索引，用于保证稳定排序与调度确定性。 */
    val sourceIndex: Int = 0,
)
