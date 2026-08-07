package com.qx.orbit.ohos.theme

import com.tencent.kuikly.compose.ui.graphics.Color

/**
 * Orbit 主题色（对齐 Android presentation/theme/Color.kt 的 wearColorScheme）。
 */
object OrbitColors {
    val Primary = Color(0xFFFFB1C6)
    val OnPrimary = Color(0xFF551D31)
    val PrimaryContainer = Color(0xFF703347)
    val OnPrimaryContainer = Color(0xFFFFD9E1)
    val Secondary = Color(0xFFE3BDC6)
    val OnSecondary = Color(0xFF422930)
    val SecondaryContainer = Color(0xFF5B3F47)
    val OnSecondaryContainer = Color(0xFFFFD9E1)
    val Tertiary = Color(0xFFEEBD93)
    val OnTertiary = Color(0xFF47290B)
    val TertiaryContainer = Color(0xFF613F1F)
    val OnTertiaryContainer = Color(0xFFFFDCC0)
    val Error = Color(0xFFFFB4AB)
    val OnError = Color(0xFF690005)
    val ErrorContainer = Color(0xFF93000A)
    val OnErrorContainer = Color(0xFFFFDAD6)
    val Background = Color(0xFF000000)
    val OnBackground = Color(0xFFEFDFE1)
    val OnSurface = Color(0xFFEFDFE1)
    val OnSurfaceVariant = Color(0xFFD6C2C5)
    val Outline = Color(0xFF9E8C90)
    val OutlineVariant = Color(0xFF514346)
    val SurfaceContainerLow = Color(0xFF22191B)
    val SurfaceContainer = Color(0xFF261D1F)
    val SurfaceContainerHigh = Color(0xFF31282A)

    val BiliPink = Color(0xFFFB7299)
    val TextSecondary = Color(0xFF888888)
    val ErrorRed = Color(0xFFE06565)
    val SuccessGreen = Color(0xFF3EAF5A)
    val CardBackground = Color(0xFF1A1A1A)
}

/** 页面背景统一使用纯黑（对齐 Android 手表主题）。 */
object PageDefaults {
    const val BACKGROUND = 0xFF000000
}
