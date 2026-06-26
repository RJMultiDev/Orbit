package com.qx.orbit.bili.presentation.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import com.qx.orbit.bili.presentation.component.WysTimeText
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.compose.ui.graphics.graphicsLayer
import androidx.wear.compose.material3.lazy.transformedHeight
import com.qx.orbit.bili.util.SharedPreferencesUtil

@Composable
fun SettingsScreen(navController: NavController) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        timeText = { WysTimeText() },
        scrollState = listState
    ) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .graphicsLayer {
                            with(transformationSpec) {
                                applyContainerTransformation(scrollProgress)
                            }
                        }
                ) {
                    Text(text = "设置")
                }
            }

            item {
                Button(
                    onClick = { navController.navigate("settings_terminal_player") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .graphicsLayer {
                            with(transformationSpec) {
                                applyContainerTransformation(scrollProgress)
                            }
                        }
                ) {
                    Text(text = "内置播放器设置", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            
            item {
                Button(
                    onClick = { navController.navigate("settings_ui") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .graphicsLayer {
                            with(transformationSpec) {
                                applyContainerTransformation(scrollProgress)
                            }
                        }
                ) {
                    Text(text = "界面设置", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            
            item {
                Button(
                    onClick = { /* TODO */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .graphicsLayer {
                            with(transformationSpec) {
                                applyContainerTransformation(scrollProgress)
                            }
                        }
                ) {
                    Text(text = "偏好设置", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            
            item {
                Button(
                    onClick = { /* TODO */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .graphicsLayer {
                            with(transformationSpec) {
                                applyContainerTransformation(scrollProgress)
                            }
                        }
                ) {
                    Text(text = "关于", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun SettingTerminalPlayerScreen(navController: NavController) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        timeText = { WysTimeText() },
        scrollState = listState
    ) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .graphicsLayer {
                            with(transformationSpec) {
                                applyContainerTransformation(scrollProgress)
                            }
                        }
                ) {
                    Text(text = "内置播放器设置")
                }
            }

            // Using SharedPreferencesUtil to track states with correct default values based on BiliClient
            val settings = listOf(
                Triple("player_longclick", "长按倍速", true),
                Triple("player_loop", "洗脑循环", false),
                Triple("player_background", "熄屏继续播放", false),
                Triple("player_autolandscape", "默认横屏", false),
                Triple("player_scale", "视频可缩放", true),
                Triple("player_doublemove", "缩放时可移动", true),
                Triple("player_danmaku_default_show", "弹幕默认开启", true),
                Triple("player_danmaku_allowoverlap", "弹幕允许重叠", true),
                Triple("player_danmaku_mergeduplicate", "合并重复弹幕", false),
                Triple("player_danmaku_showsender", "显示直播弹幕发送者", true),
                Triple("player_ui_showDanmakuBtn", "显示弹幕按钮", true)
            )

            settings.forEach { (key, label, defaultValue) ->
                item {
                    var checked by remember { mutableStateOf(SharedPreferencesUtil.getBoolean(key, defaultValue)) }
                    SwitchButton(
                        checked = checked,
                        onCheckedChange = { isChecked ->
                            checked = isChecked
                            SharedPreferencesUtil.putBoolean(key, isChecked)
                        },
                        label = {
                            Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .graphicsLayer {
                                with(transformationSpec) {
                                    applyContainerTransformation(scrollProgress)
                                }
                            }
                    )
                }
            }
            
            item {
                var maxLines by remember { mutableStateOf(SharedPreferencesUtil.getInt("player_danmaku_maxline", 0)) }
                Button(
                    onClick = {
                        val nextLines = when (maxLines) {
                            0 -> 5
                            5 -> 10
                            10 -> 20
                            else -> 0
                        }
                        maxLines = nextLines
                        SharedPreferencesUtil.putInt("player_danmaku_maxline", nextLines)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .graphicsLayer {
                            with(transformationSpec) {
                                applyContainerTransformation(scrollProgress)
                            }
                        }
                ) {
                    val label = if (maxLines == 0) "不限" else "${maxLines}行"
                    Text(text = "弹幕密度: $label", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun SettingUIScreen(navController: NavController) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = listState,
        timeText = { com.qx.orbit.bili.presentation.component.WysTimeText() }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .graphicsLayer {
                            with(transformationSpec) {
                                applyContainerTransformation(scrollProgress)
                            }
                        }
                ) {
                    Text(text = "界面设置")
                }
            }

            item {
                var checked by remember { mutableStateOf(SharedPreferencesUtil.getBoolean("ui_immersive_time", true)) }
                SwitchButton(
                    checked = checked,
                    onCheckedChange = { isChecked ->
                        checked = isChecked
                        SharedPreferencesUtil.putBoolean("ui_immersive_time", isChecked)
                    },
                    label = {
                        Text(text = "沉浸式时间显示", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .graphicsLayer {
                            with(transformationSpec) {
                                applyContainerTransformation(scrollProgress)
                            }
                        }
                )
            }
        }
    }
}
