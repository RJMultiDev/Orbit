package com.qx.orbit.core.repository

import com.qx.orbit.core.error.BizResult
import com.qx.orbit.core.model.content.SearchResultGroup
import com.qx.orbit.core.model.content.VideoCard

/**
 * Feed 领域 repository：推荐 / 排行 / 搜索。
 *
 * Android 映射（app/src/main/java/com/qx/orbit/bili/data/api/）：
 *  - 推荐：`RecommendApi.getRecommend()`（GET /x/web-interface/wbi/index/top/feed/rcmd，WBI 签名）、
 *    `RecommendApi.getPrecious()`（/x/web-interface/popular/precious）、`RecommendApi.getRelated()`（BiliApiService.getRelated）、`RecommendApi.dislike()`
 *  - 热门：`RecommendApi.getPopular()`（BiliApiService.getPopular，/x/web-interface/popular）
 *  - 排行：`RankingApi.getRanking()`（BiliApiService.getRanking，/x/web-interface/ranking/v2）
 *  - 搜索：`SearchApi.search()`（BiliApiService.searchAll，/x/web-interface/wbi/search/all/v2）、
 *    `SearchApi.searchType()`（searchType）、`SearchApi.getSearchSuggestions()`（/s.search.bilibili.com/main/suggest）、
 *    `SearchApi.getDefaultSearchContent()`（/x/web-interface/wbi/search/default）
 */
interface FeedRepository {

    /** 首页推荐流（WBI 签名）。page 对应 fresh_idx。 */
    suspend fun recommend(page: Int): BizResult<List<VideoCard>>

    /** 热门视频（分页）。 */
    suspend fun popular(page: Int): BizResult<List<VideoCard>>

    /** 每周必看（precious）。 */
    suspend fun precious(page: Int): BizResult<List<VideoCard>>

    /** 相关推荐（详情页“相关视频”）。 */
    suspend fun related(aid: Long): BizResult<List<VideoCard>>

    /** 排行榜。rid=0 全站，type=all/24h/...。 */
    suspend fun ranking(rid: Int, type: String): BizResult<List<VideoCard>>

    /** 对推荐流中的视频点“不感兴趣”。 */
    suspend fun dislike(aid: Long): BizResult<Unit>

    /** 综合搜索（search-all，按 [SearchResultGroup.result_type] 分组的混合结果）。 */
    suspend fun search(keyword: String, page: Int): BizResult<List<SearchResultGroup>>

    /** 按类型搜索（search/type），返回原始 wire 对象列表（每条为对应类型的字段 Map）。 */
    suspend fun searchByType(keyword: String, page: Int, type: String): BizResult<List<Map<String, Any?>>>

    /** 搜索联想词。 */
    suspend fun searchSuggestions(keyword: String): BizResult<List<String>>

    /** 默认搜索词。 */
    suspend fun defaultSearchKeyword(): BizResult<String>
}
