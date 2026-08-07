package com.qx.orbit.ohos.pages

import com.qx.orbit.ohos.data.BiliClient
import com.qx.orbit.ohos.platform.OrbitCredentialModule
import com.qx.orbit.ohos.theme.OrbitColors
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.RouterModule
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 扫码登录页（对齐 Android LoginScreen / HdQrCode 流程）。
 */
@Page("login")
internal class LoginPage : ComposeContainer() {

    private val credential = OrbitCredentialModule()
    private val client = BiliClient(credential)

    override fun willInit() {
        super.willInit()
        setContent {
            LoginContent(client, credential)
        }
    }
}

@Composable
internal fun LoginContent(client: BiliClient, credential: OrbitCredentialModule) {
    var qrKey by remember { mutableStateOf("") }
    var qrUrl by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("正在生成二维码…") }
    var success by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val qr = client.createQr()
        if (qr != null) {
            qrUrl = qr.url
            qrKey = qr.qrcodeKey
            status = "请用手机 B 站 App 扫码"
            var count = 0
            while (count < 180 && !success) {
                delay(1000)
                count++
                when (client.pollQr(qrKey)) {
                    0 -> {
                        success = true
                        credential.setLoggedIn(true)
                        status = "登录成功"
                        delay(800)
                        RouterModule().openPage("home", null)
                        return@LaunchedEffect
                    }
                    86038 -> {
                        status = "二维码已失效，请返回重试"
                        return@LaunchedEffect
                    }
                    86090 -> status = "已扫码，请在手机上确认"
                    else -> status = "等待扫码…"
                }
            }
        } else {
            status = "获取二维码失败"
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(OrbitColors.Background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (qrUrl.isEmpty()) {
            Text(text = "加载中…", fontSize = 14.sp, color = OrbitColors.TextSecondary,
                modifier = Modifier.padding(top = 60.dp))
        } else {
            // 二维码以文本形式展示（QR 组件在 Kuikly Compose 侧按需扩展）
            Text(
                text = qrUrl,
                fontSize = 10.sp,
                color = OrbitColors.OnSurface,
                maxLines = 4,
                modifier = Modifier.padding(top = 16.dp, start = 24.dp, end = 24.dp),
            )
        }
        Text(
            text = status,
            fontSize = 12.sp,
            color = if (success) OrbitColors.SuccessGreen else OrbitColors.TextSecondary,
            modifier = Modifier.padding(top = 16.dp),
        )
        Button(
            onClick = { RouterModule().openPage("home", null) },
            modifier = Modifier.padding(top = 20.dp),
        ) {
            Text(text = "返回", fontSize = 12.sp)
        }
    }
}
