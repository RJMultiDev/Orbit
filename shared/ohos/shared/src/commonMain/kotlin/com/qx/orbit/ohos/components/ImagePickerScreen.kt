package com.qx.orbit.ohos.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.qx.orbit.ohos.theme.OrbitColors
import com.tencent.kuikly.compose.animation.AnimatedVisibility
import com.tencent.kuikly.compose.animation.fadeIn
import com.tencent.kuikly.compose.animation.fadeOut
import com.tencent.kuikly.compose.animation.slideInVertically
import com.tencent.kuikly.compose.animation.slideOutVertically
import com.tencent.kuikly.compose.animation.core.spring
import com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.aspectRatio
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.ButtonDefaults
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.layout.ContentScale
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray

/**
 * 媒体项（对应 Android components/ImagePickerScreen.kt 的 MediaItem）。
 *
 * 差异：Android 用 `android.net.Uri` + `java.io.File`；这里统一用字符串——
 * [uri] 为图片加载地址（content:// 或 http(s) URL），[path] 为本地文件路径（发送时用）。
 */
data class MediaItem(
    val uri: String,
    val path: String,
)

/**
 * 相册桥接模块：枚举设备相册图片。
 *
 * 对应 Android 的 MediaStore + contentResolver 查询；Kuikly 侧经
 * `asyncToNativeMethod("queryImages")` 桥到 ArkTS（需在 harmony entry 提供同名
 * OrbitMediaModule.ets，内部用 `@ohos.file.photoAccessHelper` 的 PhotoAccessHelper
 * 枚举图片，回调 `[{uri, path}, ...]`）。
 *
 * TODO：ArkTS 侧 OrbitMediaModule.ets 尚未接入，真机联调前本模块恒返回空列表；
 * 回调数据形态（JSONArray / 序列化 String / List）也待真机确认。
 */
class OrbitMediaModule : Module() {
    companion object {
        const val MODULE_NAME = "OrbitMediaModule"
    }

    override fun moduleName(): String = MODULE_NAME

    fun queryImages(cb: (List<MediaItem>) -> Unit) {
        asyncToNativeMethod("queryImages", arrayOf<Any>()) { result ->
            val items = mutableListOf<MediaItem>()
            try {
                val arr = when (result) {
                    is JSONArray -> result
                    is String -> JSONArray(result)
                    else -> null
                }
                arr?.let { json ->
                    for (i in 0 until json.length()) {
                        val obj = json.optJSONObject(i) ?: continue
                        val uri = obj.optString("uri", "")
                        val path = obj.optString("path", "")
                        if (uri.isNotEmpty()) items.add(MediaItem(uri, path))
                    }
                }
            } catch (_: Throwable) {
                // TODO: 真机确认 OrbitMediaModule 回调数据形态后移除兜底
            }
            cb(items)
        }
    }
}

/**
 * 图片选择器：3 列宫格浏览相册 + 多选 + 底部弹跳发送按钮。
 *
 * 对应 Android components/ImagePickerScreen.kt。差异：
 * - Android 用 Wear TransformingLazyColumn + ListHeader + FilledIconButton（滚轮缩放）；
 *   Kuikly 统一用 LazyColumn + 普通 Button，滚轮缩放行为无对应 API（TODO）；
 * - 「发送」按钮的 Check 图标用文本「✓」替代（Kuikly 无 Icon 组件）；
 * - 选中格子的 CheckCircle 角标同样用「✓」文本替代；
 * - 返回操作 Android 走系统返回手势/ScreenScaffold，此处 [onBack] 默认调
 *   `RouterModule.closePage()`，页面顶部提供「←」按钮兜底；
 * - 选中结果回调由 Android 的 `List<File>` 改为 `List<String>`（本地文件路径）。
 */
@Composable
fun ImagePickerScreen(
    onImagesSelected: (List<String>) -> Unit,
    onBack: () -> Unit = { RouterModule().closePage() },
) {
    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var selectedUris by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(Unit) {
        OrbitMediaModule().queryImages { items ->
            mediaItems = items
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(OrbitColors.Background)) {
        // 顶部：返回按钮 + 标题（替代 Android ListHeader / ScreenScaffold 系统返回）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onBack,
                modifier = Modifier.height(36.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrbitColors.SurfaceContainer,
                    contentColor = OrbitColors.OnSurface,
                ),
            ) {
                Text(text = "←", fontSize = 14.sp)
            }
            Text(
                text = "选取图片",
                color = OrbitColors.Primary,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 10.dp),
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(top = 44.dp),
        ) {
            // 3 列宫格（对齐 Android `mediaItems.chunked(3)`）
            val rows = mediaItems.chunked(3)
            items(rows) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    row.forEach { item ->
                        MediaTile(
                            item = item,
                            isSelected = selectedUris.contains(item.uri),
                            onToggle = {
                                selectedUris = if (selectedUris.contains(item.uri)) {
                                    selectedUris - item.uri
                                } else {
                                    selectedUris + item.uri
                                }
                            },
                            modifier = Modifier.weight(1f).aspectRatio(1f),
                        )
                    }
                    repeat(3 - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // 底部弹跳发送按钮（0→1 弹入，其余弹入/滑出，对齐 Android AnimatedVisibility）
        AnimatedVisibility(
            visible = selectedUris.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            enter = slideInVertically(spring(0.6f, 400f)) { it } + fadeIn(),
            exit = slideOutVertically(spring(1.0f, 300f)) { it } + fadeOut(),
        ) {
            val count = selectedUris.size
            Button(
                onClick = {
                    val files = mediaItems.filter { selectedUris.contains(it.uri) }.map { it.path }
                    onImagesSelected(files)
                },
                modifier = Modifier
                    .height(48.dp)
                    .fillMaxWidth(if (count > 1) 0.6f else 0.5f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrbitColors.Primary,
                    contentColor = OrbitColors.OnPrimary,
                ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    Text(text = "✓", fontSize = 14.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(text = if (count > 1) "发送($count)" else "发送", fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * 单个图片格：圆角卡片 + 选中主色描边 + 右上角「✓」角标（对齐 Android 的 MediaTile）。
 */
@Composable
private fun MediaTile(
    item: MediaItem,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(OrbitColors.SurfaceContainer)
            .clickable { onToggle() }
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, OrbitColors.Primary, RoundedCornerShape(8.dp))
                } else {
                    Modifier
                }
            ),
    ) {
        if (item.uri.isNotEmpty()) {
            Image(
                painter = rememberAsyncImagePainter(item.uri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // 选中遮罩 + 角标（替代 material-icons CheckCircle）
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(OrbitColors.Primary.copy(alpha = 0.3f)),
                contentAlignment = Alignment.TopEnd,
            ) {
                Text(
                    text = "✓",
                    color = OrbitColors.Primary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(2.dp),
                )
            }
        }
    }
}
