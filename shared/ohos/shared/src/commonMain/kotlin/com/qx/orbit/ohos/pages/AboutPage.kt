package com.qx.orbit.ohos.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.qx.orbit.ohos.components.WysAlertDialog
import com.qx.orbit.ohos.theme.OrbitColors
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
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

/**
 * 关于页（对齐 Android presentation/about/AboutScreen.kt + DonationDialog.kt）。
 *
 * 差异：
 * - App 图标无本地资源，用 Box+背景+首字母「O」占位；
 * - 捐赠二维码依赖图片资源，Kuikly 侧用 WysAlertDialog 简单文案提示；
 * - 开发者头像用首字符 Box 占位。
 */
@Page("about")
internal class AboutPage : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            AboutContent()
        }
    }
}

@Composable
internal fun AboutContent() {
    var showDonation by remember { mutableStateOf(false) }
    var showLicense by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(OrbitColors.Background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "关于软件",
            fontSize = 14.sp,
            color = OrbitColors.Primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        )

        // App 图标占位 + 名称 + 版本号
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(OrbitColors.PrimaryContainer, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "O",
                fontSize = 36.sp,
                color = OrbitColors.OnPrimaryContainer,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = "Orbit",
            fontSize = 16.sp,
            color = OrbitColors.Primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "0.1.0",
            fontSize = 11.sp,
            color = OrbitColors.TextSecondary,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 开源许可说明
        Text(
            text = "开源许可",
            fontSize = 12.sp,
            color = OrbitColors.Primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
        )
        Text(
            text = "Orbit 是一个运行在 Wear OS 智能手表上的第三方哔哩哔哩客户端，" +
                "借鉴了哔哩终端的部分 API 接口调用和 UI 设计。\n" +
                "使用 Jetpack Compose + Wear Material 3 实现，网络层为 Retrofit + OkHttp。\n" +
                "项目仍在开发阶段，部分功能仍处于实验性，有 Bug 请到仓库 issue 反馈，欢迎提交 PR。\n" +
                "本项目使用 GPL-3.0 协议开源，仓库地址：https://github.com/PianoEthan/Orbit",
            fontSize = 11.sp,
            color = OrbitColors.OnSurface,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 致谢：开发人员名单
        Text(
            text = "开发人员名单",
            fontSize = 12.sp,
            color = OrbitColors.Primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))

        for (dev in developers) {
            DeveloperRow(
                name = dev.name,
                role = dev.role,
                onClick = { showDonation = true },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 捐助按钮
        Button(
            onClick = { showDonation = true },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = OrbitColors.PrimaryContainer,
                contentColor = OrbitColors.OnPrimaryContainer,
            ),
        ) {
            Text(text = "支持开发", fontSize = 12.sp)
        }

        // 开源项目按钮
        Button(
            onClick = { showLicense = true },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = OrbitColors.SurfaceContainer,
                contentColor = OrbitColors.Primary,
            ),
        ) {
            Text(text = "开源项目", fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    WysAlertDialog(
        show = showDonation,
        onDismissRequest = { showDonation = false },
        onConfirm = { showDonation = false },
        title = "支持开发",
        content = {
            Text(
                text = "开发 Orbit 投入了大量的时间和精力，如果您觉得好用，" +
                    "可以考虑请作者喝杯咖啡，您的支持是我们前进的最大动力！",
                fontSize = 12.sp,
                color = OrbitColors.OnSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )

    WysAlertDialog(
        show = showLicense,
        onDismissRequest = { showLicense = false },
        onConfirm = { showLicense = false },
        title = "开源项目",
        content = {
            Text(
                text = "本项目使用 GPL-3.0 协议开源，\n仓库地址：https://github.com/PianoEthan/Orbit",
                fontSize = 12.sp,
                color = OrbitColors.OnSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

private data class DeveloperEntry(val name: String, val role: String)

private val developers = listOf(
    DeveloperEntry("琴弦上的小医生", "开发者"),
    DeveloperEntry("iamRJ", "贡献者"),
)

@Composable
private fun DeveloperRow(name: String, role: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .background(OrbitColors.SurfaceContainer, RoundedCornerShape(12.dp))
            .padding(10.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 头像占位（首字符代替图片资源）
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(OrbitColors.SecondaryContainer, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.take(1),
                fontSize = 13.sp,
                color = OrbitColors.OnSecondaryContainer,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(modifier = Modifier.padding(start = 8.dp).weight(1f)) {
            Text(
                text = name,
                fontSize = 12.sp,
                color = OrbitColors.OnSurface,
                maxLines = 1,
            )
            Text(
                text = role,
                fontSize = 10.sp,
                color = OrbitColors.TextSecondary,
                maxLines = 1,
            )
        }
    }
}
