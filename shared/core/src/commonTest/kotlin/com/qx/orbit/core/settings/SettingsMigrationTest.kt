package com.qx.orbit.core.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsMigrationTest {

    @Test
    fun migrateOnceRenamesAndKeepsValues() {
        val input = mapOf(
            "isTextureViewOk" to "1",
            "try_look_enable" to "false",
            "player_loop" to "true",
            "play_qn" to "32",
        )
        val out = SettingsMigration.migrateOnce(input)
        assertEquals("1", out["player_texture_view_probe"])
        assertEquals("false", out["play_try_look"])
        assertEquals("true", out["player_loop"])
        assertEquals("32", out["play_qn"])
        // old names gone
        assertNull(out["isTextureViewOk"])
        assertNull(out["try_look_enable"])
        assertFalse("use_hd_qr_code" in out)
    }

    @Test
    fun backfillDefaultsFillsMissingKeysAndKeepsExisting() {
        val out = SettingsMigration.backfillDefaults(mapOf("player_loop" to "false"))
        assertEquals("16", out["play_qn"])
        assertEquals("dfm", out["danmaku_engine"])
        assertEquals("false", out["player_loop"])
        assertEquals("internal", out["cache_location"])
    }

    @Test
    fun currentVersionParsesRawMap() {
        assertEquals(0, SettingsMigration.currentVersion(emptyMap()))
        assertEquals(1, SettingsMigration.currentVersion(mapOf("settings_schema_version" to "1")))
    }

    @Test
    fun applyMigrationsRenamesBackfillsAndKeepsExisting() {
        val store = InMemorySettingsStore()
        // legacy keys written by the old app
        store.set(SettingValue(legacyKey("try_look_enable", SettingType.BOOLEAN, true), false))
        store.set(SettingValue(legacyKey("isTextureViewOk", SettingType.INT, 0), 1))
        store.set(SettingKeys.PLAYER_LOOP, false)

        val version = SettingsMigration.applyMigrations(store)

        assertEquals(SettingsMigration.VERSION, version)
        assertEquals(false, store.get<Boolean>(SettingKeys.PLAY_TRY_LOOK))
        assertEquals(1, store.get<Int>(SettingKeys.PLAYER_TEXTURE_VIEW_PROBE))
        assertEquals(false, store.get<Boolean>(SettingKeys.PLAYER_LOOP))
        // legacy names must not linger
        assertFalse(store.has(legacyKey("try_look_enable", SettingType.BOOLEAN, true)))
        assertFalse(store.has(legacyKey("isTextureViewOk", SettingType.INT, 0)))
        // defaults backfilled for absent keys
        assertEquals(16, store.get<Int>(SettingKeys.PLAY_QUALITY))
        assertTrue(store.has(SettingKeys.PLAY_QUALITY))
        // version stamped
        assertEquals(SettingsMigration.VERSION, store.get<Int>(SettingsMigration.versionKey))
    }

    @Test
    fun migrationHookRunsBeforeRules() {
        val store = InMemorySettingsStore()
        var hooked: Pair<Int, Int>? = null
        store.onMigrate = { old, new -> hooked = old to new }

        SettingsMigration.applyMigrations(store)

        assertEquals(0 to SettingsMigration.VERSION, hooked)
    }

    @Test
    fun applyMigrationsIsIdempotent() {
        val store = InMemorySettingsStore()
        SettingsMigration.applyMigrations(store)
        assertEquals(SettingsMigration.VERSION, SettingsMigration.applyMigrations(store))
        assertEquals(SettingsMigration.VERSION, SettingsMigration.applyMigrations(store))
    }

    private fun legacyKey(name: String, type: SettingType, default: Any) =
        SettingKey(name, type, default, Platform.ANDROID, "legacy test key")
}
