package com.qx.orbit.core.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * In-memory [SettingsStore] used to exercise the port contract and migration
 * without any platform dependency.
 */
class InMemorySettingsStore(
    var onMigrate: (Int, Int) -> Unit = { _, _ -> },
) : SettingsStore {

    private val map = LinkedHashMap<String, String>()

    override fun get(key: SettingKey): SettingValue<*>? = fromPrefString(key, map[key.name])

    override fun set(value: SettingValue<*>) {
        map[value.key.name] = value.toPrefString()
    }

    override fun has(key: SettingKey): Boolean = map.containsKey(key.name)

    override fun remove(key: SettingKey) {
        map.remove(key.name)
    }

    override fun all(): Map<String, String> = LinkedHashMap(map)

    override fun clear() {
        map.clear()
    }

    override fun migrationHook(oldVersion: Int, newVersion: Int) {
        onMigrate(oldVersion, newVersion)
    }
}

class SettingsStoreTest {

    @Test
    fun typedGetSetRoundTrip() {
        val store = InMemorySettingsStore()
        store.set(SettingKeys.PLAY_QUALITY, 64)
        store.set(SettingKeys.PLAYER_LOOP, false)
        store.set(SettingKeys.DANMAKU_TEXT_SCALE, 1.25f)
        store.set(SettingKeys.PLAYER_ENGINE, "media3")

        assertEquals(64, store.get<Int>(SettingKeys.PLAY_QUALITY))
        assertEquals(false, store.get<Boolean>(SettingKeys.PLAYER_LOOP))
        assertEquals(1.25f, store.get<Float>(SettingKeys.DANMAKU_TEXT_SCALE))
        assertEquals("media3", store.get<String>(SettingKeys.PLAYER_ENGINE))
    }

    @Test
    fun getWithWrongTypeReturnsNull() {
        val store = InMemorySettingsStore()
        store.set(SettingKeys.PLAY_QUALITY, 64)
        assertNull(store.get<Boolean>(SettingKeys.PLAY_QUALITY))
        assertNull(store.get<String>(SettingKeys.PLAY_QUALITY))
    }

    @Test
    fun getOrDefaultFallsBackToSchemaDefault() {
        val store = InMemorySettingsStore()
        assertEquals(16, store.getOrDefault<Int>(SettingKeys.PLAY_QUALITY))
        assertEquals(false, store.getOrDefault<Boolean>(SettingKeys.PLAYER_LOOP))
        assertEquals(1.0f, store.getOrDefault<Float>(SettingKeys.DANMAKU_TEXT_SCALE))

        store.set(SettingKeys.PLAY_QUALITY, 32)
        assertEquals(32, store.getOrDefault<Int>(SettingKeys.PLAY_QUALITY))
    }

    @Test
    fun hasAndRemoveTrackExplicitValues() {
        val store = InMemorySettingsStore()
        assertFalse(store.has(SettingKeys.PLAYER_LOOP))
        store.set(SettingKeys.PLAYER_LOOP, true)
        assertTrue(store.has(SettingKeys.PLAYER_LOOP))
        store.remove(SettingKeys.PLAYER_LOOP)
        assertFalse(store.has(SettingKeys.PLAYER_LOOP))
    }
}
