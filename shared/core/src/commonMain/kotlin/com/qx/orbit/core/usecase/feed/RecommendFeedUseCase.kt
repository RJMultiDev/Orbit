package com.qx.orbit.core.usecase.feed

import com.qx.orbit.core.error.BizError
import com.qx.orbit.core.error.BizResult
import com.qx.orbit.core.model.content.VideoCard
import com.qx.orbit.core.repository.FeedRepository

/**
 * 推荐流状态。
 *
 * @param items     当前已加载的视频列表（已按 bvid 去重）。
 * @param page      当前已成功加载的最大页码（对应 Android 端推荐接口的 fresh_idx）。
 * @param loading   是否有请求在途。
 * @param hasMore   是否还有下一页（上一页返回非空列表时为 true）。
 * @param lastError 最近一次加载失败的错误；无失败为 null。
 */
data class RecommendFeedState(
    val items: List<VideoCard> = emptyList(),
    val page: Int = 0,
    val loading: Boolean = false,
    val hasMore: Boolean = true,
    val lastError: BizError? = null,
)

/** 驱动推荐流状态的事件。 */
sealed interface RecommendFeedEvent {
    /** 刷新开始：清空列表并进入加载态。 */
    data object RefreshStart : RecommendFeedEvent

    /** 开始加载下一页（保留已有列表）。 */
    data object LoadMoreStart : RecommendFeedEvent

    /** 第 [page] 页加载成功。 */
    data class PageLoaded(val page: Int, val items: List<VideoCard>) : RecommendFeedEvent

    /** 第 [page] 页加载失败。 */
    data class PageFailed(val page: Int, val error: BizError) : RecommendFeedEvent
}

/**
 * 推荐流纯 reducer：按事件推进 [RecommendFeedState]。
 *
 * 分页：第 1 页替换列表，其余页追加；去重：按 bvid 去重（对齐 Android
 * `MainViewModel.loadMore` 的 `distinctBy { it.bvid }`）；错误：保留已加载数据并记录 [RecommendFeedState.lastError]。
 */
object RecommendFeedReducer {

    fun reduce(state: RecommendFeedState, event: RecommendFeedEvent): RecommendFeedState = when (event) {
        is RecommendFeedEvent.RefreshStart -> RecommendFeedState(
            loading = true,
            hasMore = true,
        )
        is RecommendFeedEvent.LoadMoreStart -> state.copy(
            loading = true,
            lastError = null,
        )
        is RecommendFeedEvent.PageLoaded -> {
            val merged = if (event.page == 1) event.items else state.items + event.items
            RecommendFeedState(
                items = merged.distinctBy { it.bvid },
                page = event.page,
                loading = false,
                hasMore = event.items.isNotEmpty(),
                lastError = null,
            )
        }
        is RecommendFeedEvent.PageFailed -> state.copy(
            loading = false,
            lastError = event.error,
        )
    }
}

/**
 * 推荐分页加载 use case。
 *
 * 编排 [FeedRepository.recommend] 并维护分页状态：刷新（[refresh]）/ 加载下一页（[loadMore]）/
 * 错误重试（[retry]）/ 清理（[clear]）。所有状态变更经由 [RecommendFeedReducer] 纯函数产生。
 *
 * 依赖的 port（A18 契约）：[FeedRepository.recommend]，page 对应推荐接口的 fresh_idx
 * （Android adapter 会对其做 WBI 签名后注入请求）。
 *
 * 并发安全：loading 标志阻止 [loadMore] 在途时重复发起请求。
 */
class RecommendFeedUseCase(
    private val feedRepository: FeedRepository,
) {

    private var state = RecommendFeedState()

    /** 当前推荐流状态快照。 */
    fun state(): RecommendFeedState = state

    /** 当前已加载的视频列表。 */
    fun items(): List<VideoCard> = state.items

    /** 刷新：清空并重新从第 1 页加载。 */
    suspend fun refresh(): RecommendFeedState {
        state = RecommendFeedReducer.reduce(state, RecommendFeedEvent.RefreshStart)
        return loadPage(1)
    }

    /** 加载下一页；无更多页或已有请求在途时返回当前状态。 */
    suspend fun loadMore(): RecommendFeedState {
        if (state.loading || !state.hasMore) return state
        state = RecommendFeedReducer.reduce(state, RecommendFeedEvent.LoadMoreStart)
        return loadPage(state.page + 1)
    }

    /**
     * 错误重试：
     *  - 无错误：原样返回；
     *  - 首页加载失败（列表为空）：重试 [refresh]；
     *  - 翻页失败（列表非空）：重新加载失败的页码，不跳页。
     */
    suspend fun retry(): RecommendFeedState {
        if (state.lastError == null) return state
        return if (state.items.isEmpty()) {
            refresh()
        } else {
            loadPage(state.page + 1)
        }
    }

    /** 清理全部状态（如切换 tab）。 */
    fun clear() {
        state = RecommendFeedState()
    }

    private suspend fun loadPage(page: Int): RecommendFeedState {
        return when (val result = feedRepository.recommend(page)) {
            is BizResult.Success -> {
                state = RecommendFeedReducer.reduce(state, RecommendFeedEvent.PageLoaded(page, result.data))
                state
            }
            is BizResult.Failure -> {
                state = RecommendFeedReducer.reduce(state, RecommendFeedEvent.PageFailed(page, result.error))
                state
            }
        }
    }
}
