package com.qx.orbit.ohos.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.qx.orbit.ohos.theme.OrbitColors
import com.tencent.kuikly.compose.animation.AnimatedVisibility
import com.tencent.kuikly.compose.animation.core.tween
import com.tencent.kuikly.compose.animation.fadeIn
import com.tencent.kuikly.compose.animation.fadeOut
import com.tencent.kuikly.compose.animation.slideInVertically
import com.tencent.kuikly.compose.animation.slideOutVertically
import com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.gestures.detectTapGestures
import com.tencent.kuikly.compose.foundation.gestures.detectTransformGestures
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyRow
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.foundation.lazy.rememberLazyListState
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.ButtonDefaults
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.graphicsLayer
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.compose.ui.layout.ContentScale
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.compose.ui.window.Dialog
import com.tencent.kuikly.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// RoundToast（对应 Android components/RoundToast.kt）
// ---------------------------------------------------------------------------

/**
 * 全局轻提示（Toast）状态单例。
 *
 * 对应 Android components/RoundToast.kt（WindowManager 悬浮 Toast）的 Compose 等价实现：
 * Android 版在系统窗口层绘制弧形圆条，Kuikly 无法直接操作系统窗口，因此改为
 * 「状态 + 页面内挂载 RoundToastHost()」模式：调用方在根 Box 末尾挂一个
 * `RoundToastHost()`，[show] 之后自动淡入淡出。
 *
 * duration 语义与原版一致：1 = 长（3.5s），其余 = 短（2s）。
 */
object RoundToast {
    private var visible by mutableStateOf(false)
    private var message by mutableStateOf("")
    private var durationValue by mutableStateOf(0)

    val isVisible: Boolean get() = visible
    val text: String get() = message
    val duration: Int get() = durationValue

    fun show(text: CharSequence, duration: Int = 0) {
        if (text.isEmpty()) return
        message = text.toString()
        durationValue = duration
        visible = true
    }

    fun dismiss() {
        visible = false
    }
}

/**
 * Toast 渲染挂载点：深灰半透明圆角胶囊 + 细白边 + 白色粗体文字，置于底部居中。
 *
 * 需要在页面根布局中显式放置（建议作为最后一个子项），
 * 对应 Android 的 RoundToastView（圆弧形系统悬浮条，此处简化为胶囊条，TODO）。
 */
@Composable
fun RoundToastHost(modifier: Modifier = Modifier) {
    val text = RoundToast.text
    val visible = RoundToast.isVisible
    val duration = RoundToast.duration

    LaunchedEffect(visible, text) {
        if (visible) {
            delay(if (duration == 1) 3_500L else 2_000L)
            RoundToast.dismiss()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it },
            exit = fadeOut(tween(400)) + slideOutVertically(tween(400)) { it },
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .border(width = 0.8.dp, color = Color(0x33FFFFFF), shape = RoundedCornerShape(20.dp))
                    .background(Color(0xCC1A1A1A), RoundedCornerShape(20.dp))
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text(
                    text = text,
                    fontSize = 13.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// WysAlertDialog（对应 Android components/WysAlertDialog.kt）
// ---------------------------------------------------------------------------

/**
 * 确认/警告弹窗：居中卡片，标题 + 可选内容 + 「✕ 取消 / ✓ 确认」按钮组。
 *
 * 对应 Android components/WysAlertDialog.kt 的简化移植：
 * - Android 有「无触觉反馈」分支（TransformingLazyColumn + 滚轮缩放）与
 *   AlertDialog 分支；Kuikly 统一走居中 Dialog（Kuikly 无 AlertDialog / 旋转缩放 API）；
 * - 图标用文本「ⓘ」替代 material-icons Info；确认/取消用「✓ / ✕」替代 Check/Close。
 */
@Composable
fun WysAlertDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    content: @Composable (() -> Unit)? = null,
    onConfirm: () -> Unit,
) {
    if (!show) return

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(OrbitColors.SurfaceContainer, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "ⓘ",
                    fontSize = 24.sp,
                    color = OrbitColors.OnSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    color = OrbitColors.OnSurface,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (content != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        content()
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f).padding(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrbitColors.SurfaceContainer,
                            contentColor = OrbitColors.OnSurface,
                        ),
                    ) {
                        Text(text = "✕", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).padding(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrbitColors.Primary,
                            contentColor = OrbitColors.OnPrimary,
                        ),
                    ) {
                        Text(text = "✓", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// ImageViewerDialog（对应 Android components/ImageViewerDialog.kt）
// ---------------------------------------------------------------------------

/**
 * 图片查看器：横滑多图 + 双指缩放/拖动 + 长按呼出保存菜单 + 页码指示。
 *
 * 对应 Android components/ImageViewerDialog.kt 的移植。差异：
 * - 原版用 Wear HorizontalPager，Kuikly 无 pager，改用 LazyRow + 页码文本
 *   （"index/total"）替代 HorizontalPageIndicator；
 * - 关闭按钮用文本「✕」替代 material-icons Close；
 * - 保存功能依赖 HarmonyOS 相册/MediaLibrary 桥接，暂通过 [onSave] 回调交由
 *   调用方实现；默认回调只弹 RoundToast（TODO）。
 */
@Composable
fun ImageViewerDialog(
    imageUrls: List<String>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit = { url ->
        RoundToast.show("保存图片功能待接 HarmonyOS 相册桥接（TODO: $url）")
    },
) {
    var showMenuForUrl by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(),
    ) {
        val listState = rememberLazyListState(initialIndex.coerceIn(0, (imageUrls.size - 1).coerceAtLeast(0)))
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
        ) {
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(imageUrls) { url ->
                    val dialogUrl = if (url.contains("@")) url else "$url@1024w.webp"

                    var scale by remember { mutableStateOf(1f) }
                    var offsetX by remember { mutableStateOf(0f) }
                    var offsetY by remember { mutableStateOf(0f) }

                    Box(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = { showMenuForUrl = dialogUrl },
                                )
                            }
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 5f)
                                    if (scale > 1f) {
                                        offsetX += pan.x
                                        offsetY += pan.y
                                    } else {
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(dialogUrl),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY,
                                ),
                        )
                    }
                }
            }

            // 页码指示（替代 HorizontalPageIndicator）
            if (imageUrls.size > 1) {
                Text(
                    text = "${listState.firstVisibleItemIndex + 1}/${imageUrls.size}",
                    fontSize = 11.sp,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp),
                )
            }

            // 顶部关闭按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .align(Alignment.TopCenter),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.size(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        contentColor = Color.White,
                    ),
                ) {
                    Text(text = "✕", fontSize = 16.sp)
                }
            }
        }
    }

    // 长按后的保存菜单
    val menuUrl = showMenuForUrl
    if (menuUrl != null) {
        Dialog(
            onDismissRequest = { showMenuForUrl = null },
            properties = DialogProperties(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable { showMenuForUrl = null },
                contentAlignment = Alignment.Center,
            ) {
                Button(
                    onClick = {
                        showMenuForUrl = null
                        onSave(menuUrl)
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    Text(text = "保存图片", fontSize = 12.sp)
                }
            }
        }
    }
}
