package com.qx.orbit.core.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsSchemaTest {

    @Test
    fun schemaIsConsistent() {
        assertEquals(emptyList(), SettingsSchema.validate())
    }

    @Test
    fun byNameFindsEveryDeclaredKey() {
        for (key in SettingsSchema.all) {
            assertEquals(key, SettingsSchema.byName(key.name))
        }
        assertNull(SettingsSchema.byName("no_such_key"))
    }

    @Test
    fun everyLegacyKeyIsCoveredBySchemaOrMigrationRule() {
        val schemaNames = SettingsSchema.all.map { it.name }.toSet()
        val ruleOldKeys = SettingsMigration.RULES.map { it.oldKey }.toSet()
        for (key in legacyKeys) {
            assertTrue(
                key in schemaNames || key in ruleOldKeys,
                "legacy key not covered: $key",
            )
        }
        // every rename target must exist in the schema and only rename legacy keys
        for (rule in SettingsMigration.RULES) {
            assertNotNull(SettingsSchema.byName(rule.newKey), "migration target missing from schema: ${rule.newKey}")
            assertFalse(rule.newKey in legacyKeys, "rule must not rename an already canonical key: ${rule.newKey}")
        }
    }

    @Test
    fun platformSplitIsExplicit() {
        assertTrue(SettingsSchema.forPlatform(Platform.COMMON).isNotEmpty())
        assertTrue(SettingsSchema.forPlatform(Platform.ANDROID).isNotEmpty())
        // HARMONY-only keys are reserved; none declared yet.
        assertEquals(emptyList(), SettingsSchema.forPlatform(Platform.HARMONY))
    }

    private val legacyKeys = setOf(
        "player_engine", "cache_location", "play_qn", "play_audio_qn", "danmaku_engine",
        "player", "player_danmaku_advanced_enable", "player_danmaku_default_show",
        "player_autolandscape", "player_softrotate", "player_softrotate_steps",
        "player_softrotate_deg", "player_background", "player_background_audio_only",
        "isTextureViewOk", "player_texture_view", "player_request_dash",
        "player_danmaku_textsize", "player_danmaku_mergeduplicate",
        "player_danmaku_allowoverlap", "player_danmaku_maxline",
        "player_subtitle_default_show", "player_loop", "player_danmaku_showsender",
        "player_longclick", "player_one_finger_zoom", "player_scale", "player_doublemove",
        "use_hd_qr_code", "shizuku_declined", "confirm_dislike", "ui_immersive_time",
        "try_look_enable", "player_custom_btn_left", "player_custom_btn_left_bottom",
        "player_custom_btn_right", "player_custom_btn_right_bottom", "search_history",
        "screen_mode", "dpi_scale",
    )
}
