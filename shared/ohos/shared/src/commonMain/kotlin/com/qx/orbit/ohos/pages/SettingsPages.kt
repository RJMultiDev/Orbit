package com.qx.orbit.ohos.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.qx.orbit.ohos.components.RoundToast
import com.qx.orbit.ohos.components.RoundToastHost
import com.qx.orbit.ohos.components.WysAlertDialog
import com.qx.orbit.ohos.theme.OrbitColors
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.rememberLazyListState
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.ButtonDefaults
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.RouterModule

// ---------------------------------------------------------------------------
// OrbitSettings：设置持久化（内存态）
// ---------------------------------------------------------------------------

/**
 * 设置存储（key → value，内存 Map）。
 *
 * 现状：仅保证「会话内」页面间一致；进程重启后恢复默认。
 * 后续持久化二选一接入：
 *  1. ArkTS preferences：经 Kuikly Module 桥（参照 OrbitCredentialModule / NetworkModule 模式）；
 *  2. shared/core 的 SettingsSchema typed store（SettingKey / SettingValue / SettingsStore）。
 *
 * 键名优先对齐 shared/core SettingKeys 规范名（如 play_qn、player_danmaku_textsize、
 * player_danmaku_maxline、player_danmaku_default_show、player_loop 等）；
 * 以下为 Harmony 新增、Android 无对应项的键：
 *  - play_speed：全局默认播放倍速（Android 无全局倍速设置，仅播放器内切换）
 *  - player_danmaku_opacity：弹幕透明度（Android 弹幕透明度固定）
 *  - download_qn：下载画质（Android 无全局下载设置，见 DownloadManagerScreen）
 */
internal object OrbitSettings {
    private val values = mutableMapOf<String, Any>()

    fun getBoolean(key: String, default: Boolean): Boolean = values[key] as? Boolean ?: default
    fun setBoolean(key: String, value: Boolean) {
        values[key] = value
    }

    fun getInt(key: String, default: Int): Int = values[key] as? Int ?: default
    fun setInt(key: String, value: Int) {
        values[key] = value
    }

    fun getFloat(key: String, default: Float): Float = values[key] as? Float ?: default
    fun setFloat(key: String, value: Float) {
        values[key] = value
    }

    fun getString(key: String, default: String): String = values[key] as? String ?: default
    fun setString(key: String, value: String) {
        values[key] = value
    }
}

// ---------------------------------------------------------------------------
// 设置键（优先复用 shared/core SettingKeys 规范名）
// ---------------------------------------------------------------------------

private const val KEY_PLAY_SPEED = "play_speed"
private const val KEY_PLAY_QN = "play_qn"
private const val KEY_DANMAKU_TEXT_SIZE = "player_danmaku_textsize"
private const val KEY_DANMAKU_OPACITY = "player_danmaku_opacity"
private const val KEY_DANMAKU_MAX_LINE = "player_danmaku_maxline"
private const val KEY_DOWNLOAD_QN = "download_qn"
private const val KEY_DANMAKU_DEFAULT_SHOW = "player_danmaku_default_show"
private const val KEY_PLAYER_LOOP = "player_loop"
private const val KEY_PLAYER_LONG_CLICK = "player_longclick"
private const val KEY_PLAYER_ONE_FINGER_ZOOM = "player_one_finger_zoom"
private const val KEY_DANMAKU_ALLOW_OVERLAP = "player_danmaku_allowoverlap"
private const val KEY_DANMAKU_MERGE_DUPLICATE = "player_danmaku_mergeduplicate"

// ---------------------------------------------------------------------------
// 可循环选项
// ---------------------------------------------------------------------------

private val SPEED_VALUES = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
private val QN_VALUES = listOf(16, 32, 64, 80)
private val QN_LABELS = mapOf(16 to "360p", 32 to "480p", 64 to "720p", 80 to "1080p")
private val DANMAKU_TEXT_SIZE_VALUES = listOf(0.8f, 1.0f, 1.2f, 1.5f)
private val DANMAKU_OPACITY_VALUES = listOf(1.0f, 0.8f, 0.6f, 0.4f)
private val DANMAKU_MAX_LINE_VALUES = listOf(0, 5, 10, 20)

/** 循环取下一个选项；当前值不在列表时取第一个。 */
private fun <T> cycleValue(options: List<T>, current: T): T {
    val i = options.indexOf(current)
    return if (i < 0) options.first() else options[(i + 1) % options.size]
}

/** "1.0"→"1x"、"1.25"→"1.25x"（避免 String.format）。 */
private fun scaleXLabel(v: Float): String = "${v.toString().removeSuffix(".0")}x"

private fun qnLabel(qn: Int): String = QN_LABELS[qn] ?: "${qn}p"

private fun maxLineLabel(lines: Int): String = if (lines == 0) "不限" else "${lines}行"

// ---------------------------------------------------------------------------
// 通用行组件
// ---------------------------------------------------------------------------

@Composable
private fun PageHeaderText(text: String) {
    Text(
        text = text,
        fontSize = 16.sp,
        color = OrbitColors.Primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
    )
}

@Composable
private fun SectionHeaderText(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        color = OrbitColors.TextSecondary,
        modifier = Modifier.fillMaxWidth().padding(start = 10.dp, top = 10.dp, bottom = 2.dp),
    )
}

@Composable
private fun SettingButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = OrbitColors.SurfaceContainer,
            contentColor = OrbitColors.OnSurface,
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(text = text, fontSize = 12.sp)
    }
}

/** 开关行：选中态主色容器 + ✓，未选中态深色容器 + ✕。 */
@Composable
private fun ToggleRow(label: String, checked: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (checked) OrbitColors.PrimaryContainer else OrbitColors.SurfaceContainer,
            contentColor = if (checked) OrbitColors.OnPrimaryContainer else OrbitColors.OnSurface,
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(text = (if (checked) "✓ " else "✕ ") + label, fontSize = 12.sp)
    }
}

// ---------------------------------------------------------------------------
// 设置页（@Page("settings")，对齐 Android SettingsScreen 核心入口 + 设置项）
// ---------------------------------------------------------------------------

@Page("settings")
internal class SettingsPage : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            SettingsContent()
        }
    }
}

@Composable
internal fun SettingsContent() {
    var playSpeed by remember { mutableFloatStateOf(OrbitSettings.getFloat(KEY_PLAY_SPEED, 1.0f)) }
    var playQn by remember { mutableIntStateOf(OrbitSettings.getInt(KEY_PLAY_QN, 64)) }
    var danmakuTextSize by remember { mutableFloatStateOf(OrbitSettings.getFloat(KEY_DANMAKU_TEXT_SIZE, 1.0f)) }
    var danmakuOpacity by remember { mutableFloatStateOf(OrbitSettings.getFloat(KEY_DANMAKU_OPACITY, 1.0f)) }
    var danmakuMaxLine by remember { mutableIntStateOf(OrbitSettings.getInt(KEY_DANMAKU_MAX_LINE, 0)) }
    var downloadQn by remember { mutableIntStateOf(OrbitSettings.getInt(KEY_DOWNLOAD_QN, 64)) }
    var showAbout by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize().background(OrbitColors.Background)) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item { PageHeaderText("设置") }

            item { SectionHeaderText("播放器") }
            item {
                SettingButton(text = "播放器界面自定义", onClick = {
                    RouterModule().openPage("playerCustomization", null)
                })
            }
            item {
                SettingButton(text = "播放倍速: ${scaleXLabel(playSpeed)}", onClick = {
                    playSpeed = cycleValue(SPEED_VALUES, playSpeed)
                    OrbitSettings.setFloat(KEY_PLAY_SPEED, playSpeed)
                })
            }
            item {
                SettingButton(text = "视频画质: ${qnLabel(playQn)}", onClick = {
                    playQn = cycleValue(QN_VALUES, playQn)
                    OrbitSettings.setInt(KEY_PLAY_QN, playQn)
                })
            }
            item { EngineInfoRow() }

            item { SectionHeaderText("弹幕") }
            item {
                SettingButton(text = "弹幕字号: ${scaleXLabel(danmakuTextSize)}", onClick = {
                    danmakuTextSize = cycleValue(DANMAKU_TEXT_SIZE_VALUES, danmakuTextSize)
                    OrbitSettings.setFloat(KEY_DANMAKU_TEXT_SIZE, danmakuTextSize)
                })
            }
            item {
                SettingButton(text = "弹幕透明度: ${(danmakuOpacity * 100).toInt()}%", onClick = {
                    danmakuOpacity = cycleValue(DANMAKU_OPACITY_VALUES, danmakuOpacity)
                    OrbitSettings.setFloat(KEY_DANMAKU_OPACITY, danmakuOpacity)
                })
            }
            item {
                SettingButton(text = "弹幕行数: ${maxLineLabel(danmakuMaxLine)}", onClick = {
                    danmakuMaxLine = cycleValue(DANMAKU_MAX_LINE_VALUES, danmakuMaxLine)
                    OrbitSettings.setInt(KEY_DANMAKU_MAX_LINE, danmakuMaxLine)
                })
            }

            item { SectionHeaderText("下载") }
            item {
                SettingButton(text = "下载画质: ${qnLabel(downloadQn)}", onClick = {
                    downloadQn = cycleValue(QN_VALUES, downloadQn)
                    OrbitSettings.setInt(KEY_DOWNLOAD_QN, downloadQn)
                })
            }
            item {
                SettingButton(text = "下载管理", onClick = {
                    RoundToast.show("下载管理待接入 Harmony 下载桥接（TODO）")
                })
            }

            item { SectionHeaderText("关于") }
            item {
                SettingButton(text = "关于软件", onClick = { showAbout = true })
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
        RoundToastHost()
    }

    WysAlertDialog(
        show = showAbout,
        onDismissRequest = { showAbout = false },
        title = "关于 Orbit",
        content = {
            Text(
                text = "Orbit · Wear OS B 站客户端（HarmonyOS 移植）\n" +
                    "播放引擎：系统 AVPlayer（单一引擎）\n\n" +
                    "设置当前保存在内存中，退出后恢复默认；\n后续接入 ArkTS preferences 持久化。",
                fontSize = 12.sp,
                color = OrbitColors.OnSurface,
                textAlign = TextAlign.Center,
            )
        },
        onConfirm = { showAbout = false },
    )
}

/**
 * 播放引擎选择行：Harmony 为单一引擎（系统 AVPlayer），
 * 与 Android 的 IJK / Media3 双引擎选择存在差异，此处仅展示说明、不可选择。
 */
@Composable
private fun EngineInfoRow() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .background(OrbitColors.SurfaceContainer)
            .padding(10.dp),
    ) {
        Text(text = "播放引擎", fontSize = 12.sp, color = OrbitColors.OnSurface)
        Text(
            text = "AVPlayer（HarmonyOS 单一引擎）",
            fontSize = 11.sp,
            color = OrbitColors.Primary,
            modifier = Modifier.padding(top = 2.dp),
        )
        Text(
            text = "注：Android 端可选 IJK / Media3；Harmony 端由系统 AVPlayer 承接，无引擎切换。",
            fontSize = 10.sp,
            color = OrbitColors.TextSecondary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// 播放器界面自定义页（@Page("playerCustomization")）
// 对齐 Android PlayerCustomizationScreen 的播放控制项；Kuikly 侧收敛为
// 画质/倍速/弹幕开关等设置项（Android 的按钮位置布局需手势/布局 API，暂不移植）。
// ---------------------------------------------------------------------------

@Page("playerCustomization")
internal class PlayerCustomizationPage : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            PlayerCustomizationContent()
        }
    }
}

@Composable
internal fun PlayerCustomizationContent() {
    var playSpeed by remember { mutableFloatStateOf(OrbitSettings.getFloat(KEY_PLAY_SPEED, 1.0f)) }
    var playQn by remember { mutableIntStateOf(OrbitSettings.getInt(KEY_PLAY_QN, 64)) }
    var danmakuShow by remember { mutableStateOf(OrbitSettings.getBoolean(KEY_DANMAKU_DEFAULT_SHOW, true)) }
    var loop by remember { mutableStateOf(OrbitSettings.getBoolean(KEY_PLAYER_LOOP, false)) }
    var longClick by remember { mutableStateOf(OrbitSettings.getBoolean(KEY_PLAYER_LONG_CLICK, true)) }
    var oneFingerZoom by remember { mutableStateOf(OrbitSettings.getBoolean(KEY_PLAYER_ONE_FINGER_ZOOM, false)) }
    var allowOverlap by remember { mutableStateOf(OrbitSettings.getBoolean(KEY_DANMAKU_ALLOW_OVERLAP, true)) }
    var mergeDuplicate by remember { mutableStateOf(OrbitSettings.getBoolean(KEY_DANMAKU_MERGE_DUPLICATE, false)) }
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize().background(OrbitColors.Background)) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item { PageHeaderText("播放器界面自定义") }

            item { SectionHeaderText("播放控制") }
            item {
                ToggleRow(label = "弹幕开关", checked = danmakuShow, onClick = {
                    danmakuShow = !danmakuShow
                    OrbitSettings.setBoolean(KEY_DANMAKU_DEFAULT_SHOW, danmakuShow)
                })
            }
            item {
                SettingButton(text = "播放倍速: ${scaleXLabel(playSpeed)}", onClick = {
                    playSpeed = cycleValue(SPEED_VALUES, playSpeed)
                    OrbitSettings.setFloat(KEY_PLAY_SPEED, playSpeed)
                })
            }
            item {
                SettingButton(text = "视频画质: ${qnLabel(playQn)}", onClick = {
                    playQn = cycleValue(QN_VALUES, playQn)
                    OrbitSettings.setInt(KEY_PLAY_QN, playQn)
                })
            }

            item { SectionHeaderText("播放行为") }
            item {
                ToggleRow(label = "循环播放", checked = loop, onClick = {
                    loop = !loop
                    OrbitSettings.setBoolean(KEY_PLAYER_LOOP, loop)
                })
            }
            item {
                ToggleRow(label = "长按倍速", checked = longClick, onClick = {
                    longClick = !longClick
                    OrbitSettings.setBoolean(KEY_PLAYER_LONG_CLICK, longClick)
                })
            }
            item {
                ToggleRow(label = "单指缩放", checked = oneFingerZoom, onClick = {
                    oneFingerZoom = !oneFingerZoom
                    OrbitSettings.setBoolean(KEY_PLAYER_ONE_FINGER_ZOOM, oneFingerZoom)
                    if (oneFingerZoom) {
                        RouterModule().openPage("tutorial", null)
                    }
                })
            }

            item { SectionHeaderText("弹幕") }
            item {
                ToggleRow(label = "弹幕允许重叠", checked = allowOverlap, onClick = {
                    allowOverlap = !allowOverlap
                    OrbitSettings.setBoolean(KEY_DANMAKU_ALLOW_OVERLAP, allowOverlap)
                })
            }
            item {
                ToggleRow(label = "合并重复弹幕", checked = mergeDuplicate, onClick = {
                    mergeDuplicate = !mergeDuplicate
                    OrbitSettings.setBoolean(KEY_DANMAKU_MERGE_DUPLICATE, mergeDuplicate)
                })
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
            item {
                SettingButton(text = "返回", onClick = {
                    RouterModule().openPage("settings", null)
                })
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

// ---------------------------------------------------------------------------
// 单指缩放教程页（@Page("tutorial")）
// 对齐 Android OneFingerZoomTutorialScreen 的 TheoryPage 文案（纯文本说明）。
// ---------------------------------------------------------------------------

@Page("tutorial")
internal class TutorialPage : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            TutorialContent()
        }
    }
}

@Composable
internal fun TutorialContent() {
    Column(
        modifier = Modifier.fillMaxSize().background(OrbitColors.Background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "单指缩放教程",
            fontSize = 16.sp,
            color = OrbitColors.Primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 14.dp),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "开启后，只需一根手指即可缩放视频，适合只有单点触控的手表。\n\n" +
                "操作方法：\n先单击屏幕，紧接着按住屏幕快速上下滑动即可缩放。",
            fontSize = 12.sp,
            color = OrbitColors.OnSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "练习步骤：\n" +
                "1. 单指放大：双击并按住，向下滑动\n" +
                "2. 单指平移：单指按住画面拖动\n" +
                "3. 单指缩小：双击并按住，向上滑动",
            fontSize = 11.sp,
            color = OrbitColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        Button(
            onClick = { RouterModule().openPage("playerCustomization", null) },
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text(text = "返回", fontSize = 12.sp)
        }
    }
}
