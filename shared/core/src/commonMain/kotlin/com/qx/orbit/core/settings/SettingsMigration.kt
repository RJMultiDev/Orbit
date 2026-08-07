package com.qx.orbit.core.settings

/**
 * Migration rules for the settings schema.
 *
 * Legacy Android builds wrote raw string keys straight from call sites. These
 * rules canonicalize key names and backfill defaults so a platform adapter can
 * start from a clean, typed store. The rules themselves are pure functions over
 * raw string maps, so they are unit-testable on any target.
 */
object SettingsMigration {

    /**
     * Current schema version. Bump whenever keys are renamed/removed, types change,
     * or defaults change in a way existing stores should see.
     */
    const val VERSION = 1

    /** Internal key recording the schema version the store was last migrated to. */
    val versionKey = SettingKey(
        name = "settings_schema_version",
        type = SettingType.INT,
        defaultValue = 0,
        platform = Platform.COMMON,
        description = "内部：设置 schema 迁移版本号",
    )

    /** One rename/backfill rule: legacy [oldKey] → canonical [newKey]. */
    data class Rule(
        val oldKey: String,
        val newKey: String,
        val defaultValue: Any? = null,
    )

    /**
     * v0 → v1 canonicalizations. Only keys whose legacy name differs from the
     * canonical schema name appear here.
     */
    val RULES: List<Rule> = listOf(
        Rule(oldKey = "isTextureViewOk", newKey = "player_texture_view_probe"),
        Rule(oldKey = "try_look_enable", newKey = "play_try_look", defaultValue = true),
        Rule(oldKey = "use_hd_qr_code", newKey = "login_use_hd_qr", defaultValue = true),
    )

    /**
     * Pure rename pass over a raw string map. Old keys are renamed, values are kept
     * untouched. Unknown keys pass through.
     */
    fun migrateOnce(values: Map<String, String>): Map<String, String> {
        val ruleByOld = RULES.associateBy { it.oldKey }
        val out = LinkedHashMap<String, String>()
        for ((old, raw) in values) {
            out[ruleByOld[old]?.newKey ?: old] = raw
        }
        return out
    }

    /**
     * Pure default backfill: every schema key missing from [values] gets its
     * serialized default; existing entries are never overwritten.
     */
    fun backfillDefaults(
        values: Map<String, String>,
        schema: List<SettingKey> = SettingsSchema.all,
    ): Map<String, String> {
        val out = LinkedHashMap(values)
        for (key in schema) {
            if (!out.containsKey(key.name)) {
                out[key.name] = settingValueOf(key, key.defaultValue).toPrefString()
            }
        }
        return out
    }

    /** Current schema version recorded in a raw value map (0 when absent). */
    fun currentVersion(values: Map<String, String>): Int = values[versionKey.name]?.toIntOrNull() ?: 0

    /**
     * Apply all migrations to a live store: run [migrationHook], rename legacy keys,
     * backfill defaults, rewrite the store and stamp [versionKey]. Returns the new
     * version. Idempotent.
     */
    fun applyMigrations(store: SettingsStore): Int {
        val from = store.get<Int>(versionKey) ?: 0
        if (from >= VERSION) return from
        store.migrationHook(from, VERSION)

        val backfilled = backfillDefaults(migrateOnce(store.all()))

        store.clear()
        for ((name, raw) in backfilled) {
            val key = SettingsSchema.byName(name) ?: continue
            val value = fromPrefString(key, raw) ?: continue
            store.set(value)
        }
        store.set(versionKey, VERSION)
        return VERSION
    }
}
