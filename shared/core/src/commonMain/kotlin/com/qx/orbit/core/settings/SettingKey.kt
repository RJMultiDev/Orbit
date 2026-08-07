package com.qx.orbit.core.settings

/**
 * Supported storage types for typed settings.
 */
enum class SettingType { BOOLEAN, INT, LONG, FLOAT, STRING }

/**
 * Platform ownership of a setting.
 *
 * - [COMMON] — shared business setting; both Android and Harmony must honor the
 *   same user-facing semantics (quality, danmaku display, loop, background, …).
 * - [ANDROID] — tied to an Android-only mechanism: engine/renderer choice
 *   (IJK/Media3, DFM/DFMNext), full-storage access, texture view, rotation &
 *   touch gestures, Shizuku, display density. Harmony may need a native
 *   equivalent or may deliberately drop the option.
 * - [HARMONY] — Harmony-only setting with no Android counterpart. Currently
 *   none are declared; the value is reserved for the Harmony adapter.
 */
enum class Platform { COMMON, ANDROID, HARMONY }

/**
 * A single typed setting definition: stable canonical [name], [type], [defaultValue],
 * platform ownership and a human-readable [description].
 */
data class SettingKey(
    val name: String,
    val type: SettingType,
    val defaultValue: Any,
    val platform: Platform,
    val description: String,
)

/** Strongly-typed accessor for a key's default value. */
inline fun <reified T : Any> SettingKey.defaultValueAs(): T = defaultValue as T

/**
 * Registry of every setting key declared by the app.
 *
 * The canonical names are the *post-migration* names; legacy names are mapped in
 * [SettingsMigration.RULES].
 */
object SettingKeys {

    // ---- Playback & content (COMMON) ----
    val PLAY_QUALITY = SettingKey("play_qn", SettingType.INT, 16, Platform.COMMON, "默认视频清晰度 (qn)")
    val PLAY_AUDIO_QUALITY = SettingKey("play_audio_qn", SettingType.INT, 30280, Platform.COMMON, "默认音频清晰度 (qn)")
    val PLAY_TRY_LOOK = SettingKey("play_try_look", SettingType.BOOLEAN, true, Platform.COMMON, "播放链接附加 try_look 试看参数")
    val PLAY_REQUEST_DASH = SettingKey("player_request_dash", SettingType.BOOLEAN, false, Platform.COMMON, "优先请求 DASH 流")
    val PLAYER_BACKGROUND = SettingKey("player_background", SettingType.BOOLEAN, false, Platform.COMMON, "退到后台继续播放")
    val PLAYER_BACKGROUND_AUDIO_ONLY = SettingKey("player_background_audio_only", SettingType.BOOLEAN, false, Platform.COMMON, "后台播放时仅保留音频")
    val PLAYER_LOOP = SettingKey("player_loop", SettingType.BOOLEAN, false, Platform.COMMON, "循环播放")
    val PLAYER_SUBTITLE_DEFAULT_SHOW = SettingKey("player_subtitle_default_show", SettingType.BOOLEAN, false, Platform.COMMON, "有字幕时默认显示字幕")
    val PLAYER_LONGCLICK = SettingKey("player_longclick", SettingType.BOOLEAN, true, Platform.COMMON, "长按画面倍速播放")

    // ---- Danmaku (COMMON) ----
    val DANMAKU_DEFAULT_SHOW = SettingKey("player_danmaku_default_show", SettingType.BOOLEAN, true, Platform.COMMON, "播放时默认显示弹幕")
    val DANMAKU_ADVANCED_ENABLE = SettingKey("player_danmaku_advanced_enable", SettingType.BOOLEAN, true, Platform.COMMON, "启用弹幕高级功能")
    val DANMAKU_TEXT_SCALE = SettingKey("player_danmaku_textsize", SettingType.FLOAT, 1.0f, Platform.COMMON, "弹幕字号缩放")
    val DANMAKU_MERGE_DUPLICATE = SettingKey("player_danmaku_mergeduplicate", SettingType.BOOLEAN, false, Platform.COMMON, "合并重复弹幕")
    val DANMAKU_ALLOW_OVERLAP = SettingKey("player_danmaku_allowoverlap", SettingType.BOOLEAN, true, Platform.COMMON, "允许弹幕重叠")
    val DANMAKU_MAX_LINES = SettingKey("player_danmaku_maxline", SettingType.INT, 0, Platform.COMMON, "弹幕最大轨道数 (0=自动)")
    val DANMAKU_SHOW_SENDER = SettingKey("player_danmaku_showsender", SettingType.BOOLEAN, true, Platform.COMMON, "显示弹幕发送者昵称")

    // ---- Login / interaction (COMMON) ----
    val LOGIN_HD_QR_CODE = SettingKey("login_use_hd_qr", SettingType.BOOLEAN, true, Platform.COMMON, "登录使用高清二维码")
    val CONFIRM_DISLIKE = SettingKey("confirm_dislike", SettingType.BOOLEAN, true, Platform.COMMON, "不喜欢前二次确认")
    val UI_IMMERSIVE_TIME = SettingKey("ui_immersive_time", SettingType.BOOLEAN, true, Platform.COMMON, "状态栏沉浸式时间显示")
    val SEARCH_HISTORY = SettingKey("search_history", SettingType.STRING, "", Platform.COMMON, "搜索历史 (|| 分隔)，业务数据而非设置")

    // ---- Player engine / renderer (ANDROID) ----
    val PLAYER_ENGINE = SettingKey("player_engine", SettingType.STRING, "ijk", Platform.ANDROID, "播放内核: ijk | media3")
    val PLAYER_APP = SettingKey("player", SettingType.STRING, "apsisPlayer", Platform.ANDROID, "默认播放器: apsisPlayer | pureAudioPlayer | aliangPlayer")
    val PLAYER_TEXTURE_VIEW = SettingKey("player_texture_view", SettingType.BOOLEAN, true, Platform.ANDROID, "Media3 使用 TextureView (否则 SurfaceView)")
    val PLAYER_TEXTURE_VIEW_PROBE = SettingKey("player_texture_view_probe", SettingType.INT, 0, Platform.ANDROID, "TextureView 可用性探测结果 (-1/0/1)，内部运行时状态")
    val PLAYER_AUTO_LANDSCAPE = SettingKey("player_autolandscape", SettingType.BOOLEAN, false, Platform.ANDROID, "自动横屏")
    val PLAYER_SOFT_ROTATE = SettingKey("player_softrotate", SettingType.BOOLEAN, false, Platform.ANDROID, "手动旋转")
    val PLAYER_SOFT_ROTATE_STEPS = SettingKey("player_softrotate_steps", SettingType.STRING, "0,90,180,270", Platform.ANDROID, "旋转角度列表 (逗号分隔)")
    val PLAYER_SOFT_ROTATE_DEG = SettingKey("player_softrotate_deg", SettingType.FLOAT, 0f, Platform.ANDROID, "当前旋转角度，运行时状态")
    val PLAYER_SCALE = SettingKey("player_scale", SettingType.BOOLEAN, true, Platform.ANDROID, "双指缩放视频")
    val PLAYER_DOUBLE_MOVE = SettingKey("player_doublemove", SettingType.BOOLEAN, true, Platform.ANDROID, "双指移动视频")
    val PLAYER_ONE_FINGER_ZOOM = SettingKey("player_one_finger_zoom", SettingType.BOOLEAN, false, Platform.ANDROID, "单指缩放视频")
    val PLAYER_CUSTOM_BTN_LEFT_TOP = SettingKey("player_custom_btn_left", SettingType.INT, 6, Platform.ANDROID, "播放器左上按钮动作 (PLAYER_ACTION_*)")
    val PLAYER_CUSTOM_BTN_LEFT_BOTTOM = SettingKey("player_custom_btn_left_bottom", SettingType.INT, 5, Platform.ANDROID, "播放器左下按钮动作 (PLAYER_ACTION_*)")
    val PLAYER_CUSTOM_BTN_RIGHT_TOP = SettingKey("player_custom_btn_right", SettingType.INT, 2, Platform.ANDROID, "播放器右上按钮动作 (PLAYER_ACTION_*)")
    val PLAYER_CUSTOM_BTN_RIGHT_BOTTOM = SettingKey("player_custom_btn_right_bottom", SettingType.INT, 1, Platform.ANDROID, "播放器右下按钮动作 (PLAYER_ACTION_*)")

    // ---- Danmaku engine (ANDROID) ----
    val DANMAKU_ENGINE = SettingKey("danmaku_engine", SettingType.STRING, "dfm", Platform.ANDROID, "弹幕引擎: dfm | dfmnext")

    // ---- Storage / system (ANDROID) ----
    val CACHE_LOCATION = SettingKey("cache_location", SettingType.STRING, "internal", Platform.ANDROID, "缓存位置: internal | external (全盘存储)")
    val SHIZUKU_DECLINED = SettingKey("shizuku_declined", SettingType.BOOLEAN, false, Platform.ANDROID, "用户已拒绝 Shizuku 授权")

    // ---- Display (ANDROID) ----
    val SCREEN_MODE = SettingKey("screen_mode", SettingType.INT, 0, Platform.ANDROID, "屏幕模式: 0=自动 1=圆屏 2=方屏")
    val DPI_SCALE = SettingKey("dpi_scale", SettingType.FLOAT, 1.0f, Platform.ANDROID, "界面 DPI 缩放")
}
