package com.qx.orbit.ohos.pages

import com.qx.orbit.ohos.data.BiliClient
import com.qx.orbit.ohos.data.RecommendCard
import com.qx.orbit.ohos.platform.OrbitCredentialModule
import com.qx.orbit.ohos.theme.OrbitColors
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.CircularProgressIndicator
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
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.RouterModule
import kotlinx.coroutines.launch

/**
 * 首页：推荐流（对齐 Android Home 主界面）。
 */
@Page("home")
internal class HomePage : ComposeContainer() {

    private val credential = OrbitCredentialModule()
    private val client = BiliClient(credential)

    override fun willInit() {
        super.willInit()
        setContent {
            HomeContent(client, credential)
        }
    }
}

@Composable
internal fun HomeContent(client: BiliClient, credential: OrbitCredentialModule) {
    var cards by remember { mutableStateOf<List<RecommendCard>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var loggedIn by remember { mutableStateOf(false) }
    var uname by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val nav = client.nav()
        loggedIn = nav?.isLogin == true
        uname = nav?.uname ?: ""
        credential.setLoggedIn(loggedIn)
        loading = true
        error = ""
        try {
            cards = client.recommend()
        } catch (e: Throwable) {
            error = e.message ?: "网络错误"
        }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize().background(OrbitColors.Background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (loggedIn && uname.isNotEmpty()) uname else "未登录",
                fontSize = 11.sp,
                color = OrbitColors.TextSecondary,
            )
            Button(onClick = { RouterModule().openPage("login", null) }) {
                Text(text = "登录", fontSize = 11.sp)
            }
            Button(onClick = { RouterModule().openPage("search", null) }) {
                Text(text = "搜索", fontSize = 11.sp)
            }
        }

        if (error.isNotEmpty()) {
            Text(
                text = error,
                fontSize = 12.sp,
                color = OrbitColors.ErrorRed,
                modifier = Modifier.padding(8.dp),
            )
        }

        when {
            loading && cards.isEmpty() -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            else -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                ) {
                    for (card in cards) {
                        RecommendCardRow(card = card, onClick = {
                            PageArgs.set("bvid", card.bvid)
                            PageArgs.set("cid", card.cid)
                            RouterModule().openPage("videoDetail", null)
                        })
                    }
                }
            }
        }
    }
}

@Composable
internal fun RecommendCardRow(card: RecommendCard, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrbitColors.CardBackground)
            .padding(10.dp)
            .clickable(onClick = onClick),
    ) {
        Text(
            text = card.title,
            fontSize = 13.sp,
            color = OrbitColors.OnSurface,
            maxLines = 2,
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(
                text = card.ownerName,
                fontSize = 11.sp,
                color = OrbitColors.TextSecondary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatCount(card.view),
                fontSize = 11.sp,
                color = OrbitColors.TextSecondary,
            )
        }
    }
}

internal fun formatCount(n: Long): String {
    if (n >= 10000) {
        val v = n / 10000.0
        val s = ((v * 10).toLong() + if (v * 10 - (v * 10).toLong() >= 0.5) 1 else 0)
        return "${s / 10}.${s % 10}万"
    }
    return n.toString()
}
