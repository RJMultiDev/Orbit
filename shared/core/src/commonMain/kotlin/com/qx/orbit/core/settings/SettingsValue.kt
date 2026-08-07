package com.qx.orbit.core.settings

/**
 * Strongly-typed wrapper of a stored setting value: a [key] plus a typed [value]
 * matching the key's declared [SettingType].
 */
sealed interface SettingValue<out T> {
    val key: SettingKey
    val value: T
}

data class BoolValue(override val key: SettingKey, override val value: Boolean) : SettingValue<Boolean>
data class IntValue(override val key: SettingKey, override val value: Int) : SettingValue<Int>
data class LongValue(override val key: SettingKey, override val value: Long) : SettingValue<Long>
data class FloatValue(override val key: SettingKey, override val value: Float) : SettingValue<Float>
data class StringValue(override val key: SettingKey, override val value: String) : SettingValue<String>

/**
 * Reified factory: builds the wrapper matching the compile-time type of [value].
 * Throws for unsupported types (e.g. passing an [Any]).
 */
inline fun <reified T : Any> SettingValue(key: SettingKey, value: T): SettingValue<T> = when (T::class) {
    Boolean::class -> BoolValue(key, value as Boolean) as SettingValue<T>
    Int::class -> IntValue(key, value as Int) as SettingValue<T>
    Long::class -> LongValue(key, value as Long) as SettingValue<T>
    Float::class -> FloatValue(key, value as Float) as SettingValue<T>
    String::class -> StringValue(key, value as String) as SettingValue<T>
    else -> error("Unsupported setting type: ${T::class}")
}

/**
 * Non-reified factory that dispatches on the key's declared type. Used when the
 * value only exists as [Any] (e.g. schema defaults during migration).
 */
fun settingValueOf(key: SettingKey, value: Any): SettingValue<*> = when (key.type) {
    SettingType.BOOLEAN -> BoolValue(key, value as Boolean)
    SettingType.INT -> IntValue(key, value as Int)
    SettingType.LONG -> LongValue(key, value as Long)
    SettingType.FLOAT -> FloatValue(key, value as Float)
    SettingType.STRING -> StringValue(key, value as String)
}

/**
 * Serialize a typed value to its canonical pref string. Pure; no platform I/O.
 */
fun SettingValue<*>.toPrefString(): String = when (this) {
    is BoolValue -> value.toString()
    is IntValue -> value.toString()
    is LongValue -> value.toString()
    is FloatValue -> value.toString()
    is StringValue -> value
}

/**
 * Parse a pref string back into a typed value for [key]. Returns null when the
 * raw string is absent or does not match the key's type. Pure; no platform I/O.
 */
fun fromPrefString(key: SettingKey, raw: String?): SettingValue<*>? {
    if (raw == null) return null
    return when (key.type) {
        SettingType.BOOLEAN -> raw.toBooleanStrictOrNull()?.let { BoolValue(key, it) }
        SettingType.INT -> raw.toIntOrNull()?.let { IntValue(key, it) }
        SettingType.LONG -> raw.toLongOrNull()?.let { LongValue(key, it) }
        SettingType.FLOAT -> raw.toFloatOrNull()?.let { FloatValue(key, it) }
        SettingType.STRING -> StringValue(key, raw)
    }
}
