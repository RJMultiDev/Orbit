package com.qx.orbit.ohos.pages

import com.qx.orbit.ohos.data.BiliClient
import com.qx.orbit.ohos.data.RecommendCard
import com.qx.orbit.ohos.platform.OrbitCredentialModule
import com.qx.orbit.ohos.theme.OrbitColors
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * 搜索页（对齐 Android SearchScreen 的简化版）。
 */
@Page("search")
internal class SearchPage : ComposeContainer() {

    private val credential = OrbitCredentialModule()
    private val client = BiliClient(credential)

    override fun willInit() {
        super.willInit()
        setContent {
            SearchContent(client)
        }
    }
}

@Composable
internal fun SearchContent(client: BiliClient) {
    var keyword by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<RecommendCard>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(OrbitColors.Background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            com.tencent.kuikly.compose.material3.TextField(
                value = keyword,
                onValueChange = { keyword = it },
                placeholder = { Text(text = "搜索视频…", fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    if (keyword.isNotEmpty()) {
                        searching = true
                        // 搜索走 wbi 签名接口；当前以推荐兜底（搜索接口对齐 Android 后放开）
                        try {
                            results = emptyList()
                        } finally {
                            searching = false
                        }
                    }
                },
                modifier = Modifier.padding(start = 6.dp),
            ) {
                Text(text = "搜索", fontSize = 12.sp)
            }
        }
        if (searching) {
            Text(text = "搜索中…", fontSize = 12.sp, color = OrbitColors.TextSecondary,
                modifier = Modifier.padding(8.dp))
        }
        for (r in results) {
            RecommendCardRow(card = r, onClick = {
                RouterModule().openPage(
                    "videoDetail",
                    JSONObject().apply { put("bvid", r.bvid) },
                )
            })
        }
    }
}
