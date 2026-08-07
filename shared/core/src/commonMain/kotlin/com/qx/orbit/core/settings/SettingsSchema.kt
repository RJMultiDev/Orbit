package com.qx.orbit.core.settings

/**
 * The complete declaration list of every typed setting.
 *
 * This is the single source of truth consumed by the settings UI, the platform
 * storage adapters and the migration rules. Adding a new setting means:
 * 1. declare the key in [SettingKeys],
 * 2. add it to [all],
 * 3. bump [SettingsMigration.VERSION] if the change affects existing stores.
 */
object SettingsSchema {

    val all: List<SettingKey> = listOf(
        // ---- COMMON: playback & content ----
        SettingKeys.PLAY_QUALITY,
        SettingKeys.PLAY_AUDIO_QUALITY,
        SettingKeys.PLAY_TRY_LOOK,
        SettingKeys.PLAY_REQUEST_DASH,
        SettingKeys.PLAYER_BACKGROUND,
        SettingKeys.PLAYER_BACKGROUND_AUDIO_ONLY,
        SettingKeys.PLAYER_LOOP,
        SettingKeys.PLAYER_SUBTITLE_DEFAULT_SHOW,
        SettingKeys.PLAYER_LONGCLICK,
        // ---- COMMON: danmaku ----
        SettingKeys.DANMAKU_DEFAULT_SHOW,
        SettingKeys.DANMAKU_ADVANCED_ENABLE,
        SettingKeys.DANMAKU_TEXT_SCALE,
        SettingKeys.DANMAKU_MERGE_DUPLICATE,
        SettingKeys.DANMAKU_ALLOW_OVERLAP,
        SettingKeys.DANMAKU_MAX_LINES,
        SettingKeys.DANMAKU_SHOW_SENDER,
        // ---- COMMON: login / interaction ----
        SettingKeys.LOGIN_HD_QR_CODE,
        SettingKeys.CONFIRM_DISLIKE,
        SettingKeys.UI_IMMERSIVE_TIME,
        SettingKeys.SEARCH_HISTORY,
        // ---- ANDROID: player engine / renderer ----
        SettingKeys.PLAYER_ENGINE,
        SettingKeys.PLAYER_APP,
        SettingKeys.PLAYER_TEXTURE_VIEW,
        SettingKeys.PLAYER_TEXTURE_VIEW_PROBE,
        SettingKeys.PLAYER_AUTO_LANDSCAPE,
        SettingKeys.PLAYER_SOFT_ROTATE,
        SettingKeys.PLAYER_SOFT_ROTATE_STEPS,
        SettingKeys.PLAYER_SOFT_ROTATE_DEG,
        SettingKeys.PLAYER_SCALE,
        SettingKeys.PLAYER_DOUBLE_MOVE,
        SettingKeys.PLAYER_ONE_FINGER_ZOOM,
        SettingKeys.PLAYER_CUSTOM_BTN_LEFT_TOP,
        SettingKeys.PLAYER_CUSTOM_BTN_LEFT_BOTTOM,
        SettingKeys.PLAYER_CUSTOM_BTN_RIGHT_TOP,
        SettingKeys.PLAYER_CUSTOM_BTN_RIGHT_BOTTOM,
        // ---- ANDROID: danmaku engine ----
        SettingKeys.DANMAKU_ENGINE,
        // ---- ANDROID: storage / system ----
        SettingKeys.CACHE_LOCATION,
        SettingKeys.SHIZUKU_DECLINED,
        // ---- ANDROID: display ----
        SettingKeys.SCREEN_MODE,
        SettingKeys.DPI_SCALE,
    )

    private val byName: Map<String, SettingKey> = all.associateBy { it.name }

    /** Look a setting up by its canonical name, or null. */
    fun byName(name: String): SettingKey? = byName[name]

    /** All settings owned by the given platform. */
    fun forPlatform(platform: Platform): List<SettingKey> = all.filter { it.platform == platform }

    /**
     * Integrity checks. Returns a list of problems (empty = schema is consistent).
     * Checks: unique names and default value matching the declared type.
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        for (key in all) {
            if (!seen.add(key.name)) {
                errors += "duplicate key: ${key.name}"
            }
            val defaultMatches = when (key.type) {
                SettingType.BOOLEAN -> key.defaultValue is Boolean
                SettingType.INT -> key.defaultValue is Int
                SettingType.LONG -> key.defaultValue is Long
                SettingType.FLOAT -> key.defaultValue is Float
                SettingType.STRING -> key.defaultValue is String
            }
            if (!defaultMatches) {
                errors += "default type mismatch for ${key.name}: " +
                    "expected ${key.type}, got ${key.defaultValue::class.simpleName}"
            }
        }
        return errors
    }
}
