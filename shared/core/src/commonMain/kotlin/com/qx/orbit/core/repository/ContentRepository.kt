package com.qx.orbit.core.repository

import com.qx.orbit.core.error.BizResult
import com.qx.orbit.core.model.content.ArticleCard
import com.qx.orbit.core.model.content.ArticleInfo
import com.qx.orbit.core.model.content.Bangumi
import com.qx.orbit.core.model.content.Dynamic
import com.qx.orbit.core.model.content.Opus
import com.qx.orbit.core.model.content.Stats
import com.qx.orbit.core.model.content.Timeline
import com.qx.orbit.core.model.content.UserInfo
import com.qx.orbit.core.model.content.VideoCard
import com.qx.orbit.core.model.content.VideoInfo

/** 通用分页结构（items 已按 page 拉取；游标分页另见 HistoryPage/DynamicFeedPage）。 */
data class Page<T>(
    val items: List<T>,
    val page: Int = 1,
    val hasMore: Boolean = false,
)

/** 动态分页（对应 Android `DynamicApi.getDynamicList()` 的 (offset, list)）。 */
data class DynamicFeedPage(
    val offset: Long = 0,
    val items: List<Dynamic> = emptyList(),
)

/**
 * 内容领域 repository：视频详情 / Bangumi / 文章 / 动态 / Opus / 用户空间。
 *
 * Android 映射（app/src/main/java/com/qx/orbit/bili/data/）：
 *  - 视频详情：`VideoInfoApi.getVideoInfo()`（BiliApiService.getVideoInfo，/x/web-interface/view）、
 *    `VideoInfoApi.getTags()`（getVideoTags）、`VideoInfoApi.getWatching()`（getWatching）、
 *    `VideoInfoApi`（BiliApiService.getVideoStats，/x/web-interface/archive/relation）
 *  - Bangumi：`BangumiApi.getBangumi()`/`getInfo()`（getSeasonInfo/getBangumiReview/getSeasonUserStatus）、
 *    `BangumiApi.getSections()`（getSeasonSection）、`BangumiApi.getMdidFromEpid()`、`followBangumi()`/`unfollowBangumi()`（addBangumiFollow/deleteBangumiFollow）
 *  - 文章：`ArticleApi.getArticle()`（BiliApiService.getArticle，/x/article/view）
 *  - 动态：`DynamicApi.getDynamicList()`/`getDynamic()`/`checkDynamicUpdate()`（/x/polymer/web-dynamic/v1/feed/all、/detail、/feed/all/update）、
 *    `OpusApi.getOpus()`（HTML 解析 /opus/{id}）
 *  - 用户空间：`UserInfoApi.getUserVideos()`/`getUserArticles()`（getUserVideos/getUserArticles）、
 *    `BiliApiService.getUserCard()`（/x/web-interface/card）、`getMyInfo()`（/x/space/myinfo）
 */
interface ContentRepository {

    // ===== 视频详情 =====

    /** 视频详情。bvid/aid 至少一个。 */
    suspend fun videoDetail(bvid: String? = null, aid: Long? = null): BizResult<VideoInfo>

    /** 视频标签（"tag1/tag2"）。 */
    suspend fun videoTags(bvid: String? = null, aid: Long? = null): BizResult<String>

    /** 正在观看人数。 */
    suspend fun watching(aid: Long, cid: Long): BizResult<String>

    /** 视频互动状态（是否点赞/投币/收藏/关注）。 */
    suspend fun videoStats(aid: Long): BizResult<Stats>

    // ===== Bangumi =====

    /** Bangumi 完整信息 + 分集（mediaId 或 seasonId 均可）。 */
    suspend fun bangumi(id: Long): BizResult<Bangumi>

    /** Bangumi 基础信息（含用户观看进度与 follow 状态）。 */
    suspend fun bangumiInfo(id: Long): BizResult<Bangumi.Info>

    /** Bangumi 分集列表。 */
    suspend fun bangumiSections(seasonId: Long): BizResult<List<Bangumi.Section>>

    /** 由 epid 反查 mediaId。 */
    suspend fun mediaIdFromEpId(epId: Long): BizResult<Long>

    /** 关注 / 取关番剧。 */
    suspend fun followBangumi(seasonId: Long, follow: Boolean): BizResult<Unit>

    /** 番剧时间表。 */
    suspend fun bangumiTimeline(types: String, before: Int, after: Int): BizResult<Timeline>

    // ===== 文章 =====

    /** 文章详情（含富文本内容）。 */
    suspend fun article(id: Long): BizResult<ArticleInfo>

    // ===== 动态 / Opus =====

    /** 动态流（offset 为游标；mid>0 查指定用户空间，type>0 过滤类型）。 */
    suspend fun dynamicFeed(offset: String = "", mid: Long = 0, type: Int = 0): BizResult<DynamicFeedPage>

    /** 动态详情。 */
    suspend fun dynamic(id: String): BizResult<Dynamic>

    /** 动态更新数（关注列表红点）。 */
    suspend fun dynamicUpdateCount(type: Int, baseline: String): BizResult<Int>

    /** Opus 详情（HTML 解析）。 */
    suspend fun opus(id: Long): BizResult<Opus>

    // ===== 用户空间 =====

    /** 用户主页卡片。 */
    suspend fun userCard(mid: Long): BizResult<UserInfo>

    /** 当前用户我的信息（/x/space/myinfo）。 */
    suspend fun myInfo(): BizResult<UserInfo>

    /** 用户投稿视频（分页）。 */
    suspend fun userVideos(mid: Long, page: Int, keyword: String = ""): BizResult<Page<VideoCard>>

    /** 用户投稿文章（分页）。 */
    suspend fun userArticles(mid: Long, page: Int): BizResult<Page<ArticleCard>>
}
