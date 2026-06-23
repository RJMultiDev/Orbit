package com.qx.orbit.bili.data.api

import com.qx.orbit.bili.data.model.*
import com.qx.orbit.bili.data.remote.CookieManager
import com.qx.orbit.bili.data.remote.GsonConfig
import com.qx.orbit.bili.data.remote.HttpClient
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

object InteractionVideoApi {

    internal data class EdgeInfoData(
        @SerializedName("title") val title: String? = null,
        @SerializedName("edge_id") val edge_id: Long = 0,
        @SerializedName("story_list") val story_list: List<StoryNodeData>? = null,
        @SerializedName("edges") val edges: EdgesData? = null,
        @SerializedName("is_leaf") val is_leaf: Int = 0
    )

    internal data class StoryNodeData(
        @SerializedName("node_id") val node_id: Long = 0,
        @SerializedName("edge_id") val edge_id: Long = 0,
        @SerializedName("title") val title: String? = null,
        @SerializedName("cid") val cid: Long = 0,
        @SerializedName("start_pos") val start_pos: Long = 0,
        @SerializedName("cover") val cover: String? = null,
        @SerializedName("is_current") val is_current: Int = 0
    )

    internal data class EdgesData(
        @SerializedName("questions") val questions: List<QuestionData>? = null
    )

    internal data class QuestionData(
        @SerializedName("id") val id: Long = 0,
        @SerializedName("type") val type: Int = 0,
        @SerializedName("start_time_r") val start_time_r: Long = 0,
        @SerializedName("duration") val duration: Long = 0,
        @SerializedName("pause_video") val pause_video: Int = 0,
        @SerializedName("title") val title: String? = null,
        @SerializedName("choices") val choices: List<ChoiceData>? = null
    )

    internal data class ChoiceData(
        @SerializedName("id") val id: Long = 0,
        @SerializedName("cid") val cid: Long = 0,
        @SerializedName("option") val option: String? = null,
        @SerializedName("is_default") val is_default: Int = 0,
        @SerializedName("is_hidden") val is_hidden: Int = 0
    )

    suspend fun getEdgeInfo(aid: Long, bvid: String, graphVersion: Long, edgeId: Long): InteractionVideoData? = withContext(Dispatchers.IO) {
        val url = "https://api.bilibili.com/x/stein/edgeinfo_v2?aid=$aid&bvid=$bvid&graph_version=$graphVersion&edge_id=$edgeId"
        val json = httpGet(url)
        val type = object : TypeToken<ApiResponse<EdgeInfoData>>() {}.type
        val resp: ApiResponse<EdgeInfoData>? = GsonConfig.gson.fromJson(json, type)
        if (resp == null || !resp.isSuccess || resp.data == null) return@withContext null
        val data = resp.data
        InteractionVideoData(
            title = data.title ?: "",
            edgeId = data.edge_id,
            storyList = data.story_list?.map { node ->
                InteractionVideoData.StoryNode(
                    nodeId = node.node_id,
                    edgeId = node.edge_id,
                    title = node.title ?: "",
                    cid = node.cid,
                    startPos = node.start_pos,
                    cover = node.cover ?: "",
                    isCurrent = node.is_current
                )
            } ?: emptyList(),
            edges = data.edges?.let { edgesData ->
                InteractionVideoData.InteractionEdge(
                    questions = edgesData.questions?.map { q ->
                        InteractionVideoData.Question(
                            id = q.id,
                            type = q.type,
                            startTimeR = q.start_time_r,
                            duration = q.duration,
                            pauseVideo = q.pause_video,
                            title = q.title ?: "",
                            choices = q.choices?.map { c ->
                                InteractionVideoData.Choice(
                                    id = c.id,
                                    cid = c.cid,
                                    option = c.option ?: "",
                                    isDefault = c.is_default,
                                    isHidden = c.is_hidden
                                )
                            } ?: emptyList()
                        )
                    } ?: emptyList()
                )
            },
            isLeaf = data.is_leaf
        )
    }

    private fun httpGet(url: String): String {
        val request = Request.Builder().url(url)
            .addHeader("Cookie", CookieManager.getCookie())
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Referer", "https://www.bilibili.com/")
            .build()
        return HttpClient.client.newCall(request).execute().body?.string() ?: ""
    }

    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.95 Safari/537.36"
}
