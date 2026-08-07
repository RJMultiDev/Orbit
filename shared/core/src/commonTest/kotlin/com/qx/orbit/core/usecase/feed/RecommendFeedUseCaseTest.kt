package com.qx.orbit.core.usecase.feed

import com.qx.orbit.core.error.BizError
import com.qx.orbit.core.error.BizErrors
import com.qx.orbit.core.error.BizResult
import com.qx.orbit.core.model.content.SearchResultGroup
import com.qx.orbit.core.model.content.VideoCard
import com.qx.orbit.core.repository.FeedRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * RecommendFeedUseCase 测试：注入 fake FeedRepository，覆盖分页、去重、错误重试
 * 与“请求参数注入”（page 作为 fresh_idx 注入 recommend 调用，Android adapter 会对其做 WBI 签名）。
 *
 * 所有 fake 数据均脱敏（bvid 等均为 fake 值）。
 */
class RecommendFeedUseCaseTest {

    // ---- 分页 ----

    @Test
    fun `refresh loads first page`() = runTest {
        val repo = fakeRepo(pages = mapOf(1 to success(cards("a", "b"))))
        val useCase = RecommendFeedUseCase(repo)

        val state = useCase.refresh()

        assertEquals(listOf("a", "b"), state.items.map { it.bvid })
        assertEquals(1, state.page)
        assertFalse(state.loading)
        assertNull(state.lastError)
        assertEquals(listOf(1), repo.requestedPages)
    }

    @Test
    fun `refresh then loadMore injects sequential fresh_idx pages`() = runTest {
        val repo = fakeRepo(
            pages = mapOf(
                1 to success(cards("a", "b")),
                2 to success(cards("c", "d")),
                3 to success(cards("e")),
            )
        )
        val useCase = RecommendFeedUseCase(repo)

        useCase.refresh()
        useCase.loadMore()
        useCase.loadMore()

        // 签名注入断言：注入到 recommend(page) 的页码序列（Android adapter 将 page 作为
        // fresh_idx 做 WBI 签名后构造请求）。
        assertEquals(listOf(1, 2, 3), repo.requestedPages)
        assertEquals(listOf("a", "b", "c", "d", "e"), useCase.items().map { it.bvid })
        assertEquals(3, useCase.state().page)
    }

    @Test
    fun `loadMore when no more pages does not request again`() = runTest {
        val repo = fakeRepo(
            pages = mapOf(
                1 to success(cards("a", "b")),
                2 to success(emptyList()),
            )
        )
        val useCase = RecommendFeedUseCase(repo)

        useCase.refresh()
        val afterLoadMore = useCase.loadMore()
        assertFalse(afterLoadMore.hasMore)

        val requestedBefore = repo.requestedPages.size
        useCase.loadMore()
        assertEquals(requestedBefore, repo.requestedPages.size)
    }

    // ---- 去重 ----

    @Test
    fun `loadMore dedups by bvid across pages`() = runTest {
        val repo = fakeRepo(
            pages = mapOf(
                1 to success(cards("a", "b", "c")),
                2 to success(cards("b", "d")), // b 与第 1 页重复
            )
        )
        val useCase = RecommendFeedUseCase(repo)

        useCase.refresh()
        val state = useCase.loadMore()

        assertEquals(listOf("a", "b", "c", "d"), state.items.map { it.bvid })
        assertEquals(4, state.items.size)
    }

    // ---- 错误重试 ----

    @Test
    fun `retry after loadMore failure reloads the same failed page`() = runTest {
        val repo = fakeRepo(
            pages = mapOf(
                1 to success(cards("a", "b")),
                2 to failure(BizErrors.network("load failed")),
            )
        )
        val useCase = RecommendFeedUseCase(repo)

        useCase.refresh()
        val failed = useCase.loadMore()

        assertNotNull(failed.lastError)
        assertEquals(1, failed.page) // 失败页不前进
        assertEquals(listOf("a", "b"), failed.items.map { it.bvid })

        // 恢复第 2 页后重试，应重新加载第 2 页而不是跳到第 3 页
        repo.pages[2] = success(cards("c"))
        val retried = useCase.retry()

        assertNull(retried.lastError)
        assertEquals(listOf(1, 2, 2), repo.requestedPages)
        assertEquals(listOf("a", "b", "c"), retried.items.map { it.bvid })
        assertEquals(2, retried.page)
    }

    @Test
    fun `retry after refresh failure retries first page`() = runTest {
        val repo = fakeRepo(
            pages = mapOf(1 to failure(BizErrors.network("offline")))
        )
        val useCase = RecommendFeedUseCase(repo)

        val failed = useCase.refresh()
        assertTrue(failed.items.isEmpty())
        assertNotNull(failed.lastError)

        repo.pages[1] = success(cards("a", "b"))
        val retried = useCase.retry()

        assertNull(retried.lastError)
        assertEquals(listOf(1, 1), repo.requestedPages)
        assertEquals(listOf("a", "b"), retried.items.map { it.bvid })
    }

    @Test
    fun `retry without error returns current state without requesting`() = runTest {
        val repo = fakeRepo(pages = mapOf(1 to success(cards("a"))))
        val useCase = RecommendFeedUseCase(repo)

        useCase.refresh()
        val before = repo.requestedPages.size
        val retried = useCase.retry()

        assertEquals(before, repo.requestedPages.size)
        assertEquals(useCase.state(), retried)
    }

    // ---- 刷新重置 ----

    @Test
    fun `refresh replaces previously loaded list`() = runTest {
        val repo = fakeRepo(pages = mapOf(1 to success(cards("a", "b"))))
        val useCase = RecommendFeedUseCase(repo)

        useCase.refresh()
        assertEquals(listOf("a", "b"), useCase.items().map { it.bvid })

        repo.pages[1] = success(cards("x"))
        val refreshed = useCase.refresh()

        assertEquals(listOf("x"), refreshed.items.map { it.bvid })
        assertEquals(1, refreshed.page)
        assertEquals(listOf(1, 1), repo.requestedPages)
    }

    // ---- 纯 reducer ----

    @Test
    fun `reducer page 1 replaces and later pages append with dedup`() {
        var state = RecommendFeedReducer.reduce(RecommendFeedState(), RecommendFeedEvent.RefreshStart)
        assertTrue(state.loading)

        state = RecommendFeedReducer.reduce(state, RecommendFeedEvent.PageLoaded(1, cards("a", "b")))
        assertEquals(listOf("a", "b"), state.items.map { it.bvid })
        assertTrue(state.hasMore)

        state = RecommendFeedReducer.reduce(state, RecommendFeedEvent.PageLoaded(2, cards("b", "c")))
        assertEquals(listOf("a", "b", "c"), state.items.map { it.bvid })
        assertEquals(2, state.page)
        assertFalse(state.loading)
    }

    @Test
    fun `reducer records failure and keeps previous items`() {
        val base = RecommendFeedReducer.reduce(
            RecommendFeedState(),
            RecommendFeedEvent.PageLoaded(1, cards("a", "b"))
        )
        val failed = RecommendFeedReducer.reduce(
            base,
            RecommendFeedEvent.PageFailed(2, BizErrors.api(-404, "not found"))
        )

        assertEquals(listOf("a", "b"), failed.items.map { it.bvid })
        assertNotNull(failed.lastError)
        assertEquals(-404, failed.lastError?.code)
        assertFalse(failed.loading)
    }

    // ---- fakes ----

    private fun fakeRepo(
        pages: Map<Int, BizResult<List<VideoCard>>> = emptyMap(),
    ): FakeFeedRepository = FakeFeedRepository().also { it.pages.putAll(pages) }

    private fun success(items: List<VideoCard>): BizResult<List<VideoCard>> = BizResult.success(items)

    private fun failure(error: BizError): BizResult<List<VideoCard>> = BizResult.failure(error)

    /** 构造脱敏的测试卡片；bvid 即唯一键。 */
    private fun cards(vararg bvids: String): List<VideoCard> =
        bvids.map { bvid ->
            VideoCard(
                title = "fake-title-$bvid",
                upName = "fake-up-$bvid",
                view = "1万",
                cover = "https://fake-cover.example/$bvid.png",
                aid = bvid.hashCode().toLong(),
                bvid = bvid,
                cid = 1L,
                mid = 0L,
            )
        }

    private open class StubFeedRepository : FeedRepository {
        protected fun unsupported(): Nothing = error("not used in this test")

        override suspend fun recommend(page: Int): BizResult<List<VideoCard>> = unsupported()
        override suspend fun popular(page: Int): BizResult<List<VideoCard>> = unsupported()
        override suspend fun precious(page: Int): BizResult<List<VideoCard>> = unsupported()
        override suspend fun related(aid: Long): BizResult<List<VideoCard>> = unsupported()
        override suspend fun ranking(rid: Int, type: String): BizResult<List<VideoCard>> = unsupported()
        override suspend fun dislike(aid: Long): BizResult<Unit> = unsupported()
        override suspend fun search(keyword: String, page: Int): BizResult<List<SearchResultGroup>> = unsupported()
        override suspend fun searchByType(keyword: String, page: Int, type: String): BizResult<List<Map<String, Any?>>> = unsupported()
        override suspend fun searchSuggestions(keyword: String): BizResult<List<String>> = unsupported()
        override suspend fun defaultSearchKeyword(): BizResult<String> = unsupported()
    }

    /**
     * 记录注入的 page（Android adapter 将其作为 fresh_idx 做 WBI 签名），
     * 并按页返回脚本化结果；未配置的页返回空列表。
     */
    private class FakeFeedRepository : StubFeedRepository() {
        val pages = mutableMapOf<Int, BizResult<List<VideoCard>>>()
        val requestedPages = mutableListOf<Int>()

        override suspend fun recommend(page: Int): BizResult<List<VideoCard>> {
            requestedPages += page
            return pages[page] ?: BizResult.success(emptyList())
        }
    }
}
