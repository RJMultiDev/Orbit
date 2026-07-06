package com.qx.orbit.bili.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material.dialog.Dialog
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.lazy.transformedHeight
import com.qx.orbit.bili.R
import com.qx.orbit.bili.util.SharedPreferencesUtil

@Composable
fun PlayerCustomizationScreen(
    onBack: () -> Unit
) {
    var leftBtnAction by remember { mutableIntStateOf(SharedPreferencesUtil.getInt("player_custom_btn_left", 2)) }
    var rightBtnAction by remember { mutableIntStateOf(SharedPreferencesUtil.getInt("player_custom_btn_right", 1)) }
    
    var showActionDialog by remember { mutableStateOf(false) }
    var configuringSide by remember { mutableIntStateOf(0) } // 0 for left, 1 for right

    val actionNames = listOf("无操作", "弹幕开关", "倍速播放", "音量调节")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Mock Player UI
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "正在播放：自定义预览",
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp, start = 24.dp, end = 24.dp)
                    .clickable { onBack() }
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Button
                Box(
                    modifier = Modifier.size(48.dp).offset(x = (-16).dp).clickable {
                        configuringSide = 0
                        showActionDialog = true
                    },
                    contentAlignment = Alignment.Center
                ) {
                    PlayerActionIcon(action = leftBtnAction)
                    if (leftBtnAction == 0) {
                        Text(text = "左", color = Color.Gray, fontSize = 12.sp)
                    }
                }
                
                // Play Button (Center)
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(42.dp)
                )
                
                // Right Button
                Box(
                    modifier = Modifier.size(48.dp).offset(x = 16.dp).clickable {
                        configuringSide = 1
                        showActionDialog = true
                    },
                    contentAlignment = Alignment.Center
                ) {
                    PlayerActionIcon(action = rightBtnAction)
                    if (rightBtnAction == 0) {
                        Text(text = "右", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            // Mock Progress Bar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .fillMaxHeight()
                            .background(Color.White)
                    )
                }
                Text(
                    text = "点击两侧按钮配置",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
        
        // Action Selection Dialog
        Dialog(
            showDialog = showActionDialog,
            onDismissRequest = { showActionDialog = false }
        ) {
            TransformingLazyColumn(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    ListHeader {
                        Text(text = if (configuringSide == 0) "设置左侧按钮" else "设置右侧按钮", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
                itemsIndexed(actionNames) { index, name ->
                    val otherBtnAction = if (configuringSide == 0) rightBtnAction else leftBtnAction
                    val isSelected = index == if (configuringSide == 0) leftBtnAction else rightBtnAction
                    val isDisabled = index != 0 && index == otherBtnAction
                    
                    Button(
                        onClick = {
                            if (!isDisabled) {
                                if (configuringSide == 0) {
                                    leftBtnAction = index
                                    SharedPreferencesUtil.putInt("player_custom_btn_left", index)
                                } else {
                                    rightBtnAction = index
                                    SharedPreferencesUtil.putInt("player_custom_btn_right", index)
                                }
                                showActionDialog = false
                            }
                        },
                        colors = if (isSelected) {
                            ButtonDefaults.buttonColors()
                        } else {
                            ButtonDefaults.filledTonalButtonColors()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isDisabled
                    ) {
                        Text(text = name)
                    }
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
fun PlayerActionIcon(action: Int) {
    when (action) {
        1 -> {
            Icon(
                painter = painterResource(R.drawable.ic_danmaku_inline_switch_v2_on),
                contentDescription = "Danmaku",
                tint = Color.Unspecified,
                modifier = Modifier.size(36.dp)
            )
        }
        2 -> {
            Icon(
                painter = painterResource(R.drawable.speed_1x),
                contentDescription = "Playback Speed",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(28.dp)
            )
        }
        3 -> {
            Icon(
                imageVector = Icons.AutoMirrored.Default.VolumeUp,
                contentDescription = "Volume",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(24.dp)
            )
        }
        else -> {
            // Nothing for 0
        }
    }
}
