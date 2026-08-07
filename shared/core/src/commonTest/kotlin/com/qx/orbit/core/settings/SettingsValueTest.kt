package com.qx.orbit.core.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SettingsValueTest {

    private val longKey = SettingKey("test_long", SettingType.LONG, 0L, Platform.COMMON, "t")

    @Test
    fun booleanRoundTrip() {
        for (v in listOf(true, false)) {
            val value = SettingValue(SettingKeys.PLAYER_LOOP, v)
            assertEquals(v.toString(), value.toPrefString())
            assertEquals(
                BoolValue(SettingKeys.PLAYER_LOOP, v),
                fromPrefString(SettingKeys.PLAYER_LOOP, value.toPrefString()),
            )
        }
    }

    @Test
    fun intRoundTrip() {
        val value = SettingValue(SettingKeys.PLAY_QUALITY, 1080)
        assertEquals("1080", value.toPrefString())
        assertEquals(
            IntValue(SettingKeys.PLAY_QUALITY, 1080),
            fromPrefString(SettingKeys.PLAY_QUALITY, "1080"),
        )
    }

    @Test
    fun longRoundTrip() {
        val value = SettingValue(longKey, 1234567890123L)
        assertEquals("1234567890123", value.toPrefString())
        assertEquals(
            LongValue(longKey, 1234567890123L),
            fromPrefString(longKey, value.toPrefString()),
        )
    }

    @Test
    fun floatRoundTrip() {
        for (v in listOf(0f, 1f, 1.5f, 0.1f, -2.25f)) {
            val value = SettingValue(SettingKeys.DANMAKU_TEXT_SCALE, v)
            assertEquals(
                FloatValue(SettingKeys.DANMAKU_TEXT_SCALE, v),
                fromPrefString(SettingKeys.DANMAKU_TEXT_SCALE, value.toPrefString()),
            )
        }
    }

    @Test
    fun stringRoundTrip() {
        val value = SettingValue(SettingKeys.SEARCH_HISTORY, "a||b")
        assertEquals("a||b", value.toPrefString())
        assertEquals(
            StringValue(SettingKeys.SEARCH_HISTORY, "a||b"),
            fromPrefString(SettingKeys.SEARCH_HISTORY, value.toPrefString()),
        )
        // empty string is a valid STRING value
        assertEquals(
            StringValue(SettingKeys.SEARCH_HISTORY, ""),
            fromPrefString(SettingKeys.SEARCH_HISTORY, ""),
        )
    }

    @Test
    fun missingOrUnparseableInputReturnsNull() {
        assertNull(fromPrefString(SettingKeys.PLAY_QUALITY, null))
        assertNull(fromPrefString(SettingKeys.PLAY_QUALITY, "not-an-int"))
        assertNull(fromPrefString(SettingKeys.DANMAKU_TEXT_SCALE, "abc"))
        assertNull(fromPrefString(SettingKeys.PLAYER_LOOP, "yes"))
        assertNull(fromPrefString(longKey, "1.5"))
    }

    @Test
    fun reifiedFactoryBuildsTypedValues() {
        assertIs<BoolValue>(SettingValue(SettingKeys.PLAYER_LOOP, true))
        assertIs<IntValue>(SettingValue(SettingKeys.PLAY_QUALITY, 16))
        assertIs<LongValue>(SettingValue(longKey, 1L))
        assertIs<FloatValue>(SettingValue(SettingKeys.DPI_SCALE, 1.0f))
        assertIs<StringValue>(SettingValue(SettingKeys.PLAYER_ENGINE, "ijk"))
    }

    @Test
    fun anyFactoryDispatchesOnKeyType() {
        assertEquals(
            BoolValue(SettingKeys.SHIZUKU_DECLINED, false),
            settingValueOf(SettingKeys.SHIZUKU_DECLINED, SettingKeys.SHIZUKU_DECLINED.defaultValue),
        )
        assertEquals(
            StringValue(SettingKeys.PLAYER_ENGINE, "ijk"),
            settingValueOf(SettingKeys.PLAYER_ENGINE, SettingKeys.PLAYER_ENGINE.defaultValue),
        )
    }
}
