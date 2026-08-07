package com.qx.orbit.core.settings

/**
 * Platform-agnostic persistence port for typed settings.
 *
 * Implementations (Android SharedPreferences adapter, Harmony preferences adapter)
 * only deal with [SettingValue] wrappers; (de)serialization lives in
 * [toPrefString] / [fromPrefString]. The port deliberately exposes no Android
 * types, so it compiles on any KMP target.
 */
interface SettingsStore {

    /** Stored value for [key], or null when the key has no explicit value. */
    fun get(key: SettingKey): SettingValue<*>?

    /** Persist a typed value. */
    fun set(value: SettingValue<*>)

    /** True when the key has an explicitly stored value (schema default does not count). */
    fun has(key: SettingKey): Boolean

    /** Remove the stored value for [key]. */
    fun remove(key: SettingKey)

    /**
     * Raw snapshot of every stored entry (canonical *and* legacy names), name to
     * serialized string. Used by migration; must include keys not (yet) in the schema.
     */
    fun all(): Map<String, String>

    /** Remove everything. */
    fun clear()

    /**
     * Migration hook invoked by [SettingsMigration.applyMigrations] before rules
     * run. Defaults to no-op; adapters may use it to wrap writes in a transaction.
     */
    fun migrationHook(oldVersion: Int, newVersion: Int) = Unit
}

/** Strong-typed read: the stored value cast to [T], or null when absent or mismatched. */
inline fun <reified T : Any> SettingsStore.get(key: SettingKey): T? = (get(key)?.value) as? T

/** Strong-typed write from a raw Kotlin value. */
inline fun <reified T : Any> SettingsStore.set(key: SettingKey, value: T) = set(SettingValue(key, value))

/** Stored value, falling back to the schema default when absent. */
inline fun <reified T : Any> SettingsStore.getOrDefault(key: SettingKey): T =
    get<T>(key) ?: key.defaultValueAs()
