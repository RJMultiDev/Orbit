package com.qx.orbit.core.repository

import com.qx.orbit.core.error.BizResult
import com.qx.orbit.core.model.content.FollowTag
import com.qx.orbit.core.model.content.UserInfo
import com.qx.orbit.core.model.content.VideoCard

/** 评论（对应 Android `Reply`）。 */
data class Reply(
    val rpid: Long = 0,
    val oid: Long = 0,
    val root: Long = 0,
    val parent: Long = 0,
    val pubTime: String = "",
    val sender: UserInfo = UserInfo(),
    val message: String = "",
    val pictureList: List<String> = emptyList(),
    val likeCount: Int = 0,
    val upLiked: Boolean = false,
    val upReplied: Boolean = false,
    val liked: Boolean = false,
    val childCount: Int = 0,
    val childMsgList: List<Reply> = emptyList(),
    val isTop: Boolean = false,
    val isUp: Boolean = false,
)

/** 评论分页（对应 Android `ReplyApi.getReplies()` 返回）。 */
data class ReplyPage(
    val items: List<Reply>,
    val page: Int = 1,
    /** 是否还有更多（懒加载用 nextOffset 续拉）。 */
    val hasMore: Boolean = false,
)

/** 懒加载评论页（对应 Android `ReplyApi.getRepliesLazy()` 的 Triple(count, nextOffset, list)）。 */
data class LazyReplyPage(
    val count: Int = 0,
    val nextOffset: String? = null,
    val items: List<Reply> = emptyList(),
)

/** 收藏夹（对应 Android `FavoriteFolder`/`FavFolderUI`）。 */
data class FavoriteFolder(
    val id: Long = 0,
    val mediaId: Long = 0,
    val name: String = "",
    val cover: String = "",
    val videoCount: Int = 0,
    val isDefault: Boolean = false,
    /** 当前 aid 是否已收藏（getFavoriteState 用）。 */
    val isFav: Boolean = false,
)

/** 收藏夹内视频页。 */
data class FavoriteFolderPage(
    val totalCount: Int = 0,
    val hasMore: Boolean = false,
    val items: List<VideoCard> = emptyList(),
)

/** 历史记录页（对应 Android `HistoryApi.getHistory()` 的游标信息 + 列表）。 */
data class HistoryPage(
    val items: List<VideoCard> = emptyList(),
    val max: Long = 0,
    val viewAt: Long = 0,
    val business: String = "",
    val isEnd: Boolean = false,
)

/** 稍后再看项（对应 Android `WatchLaterApi.getWatchLater()` 的 VideoCard）。 */
data class WatchLaterItem(
    val aid: Long = 0,
    val bvid: String = "",
    val cid: Long = 0,
    val title: String = "",
    val upName: String = "",
    val cover: String = "",
    val view: String = "",
    val progressPercent: Float? = null,
)

/**
 * 互动领域 repository：评论 / 点赞 / 投币 / 收藏 / 关注 / 历史 / 稍后再看。
 *
 * Android 映射（app/src/main/java/com/qx/orbit/bili/data/api/）：
 *  - 评论：`ReplyApi.getReplies()`/`getRepliesLazy()`/`sendReply()`/`likeReply()`/`deleteReply()`/`getReplyCount()`
 *    （BiliApiService.getReplies/getSubReplies/getRepliesLazy/sendReply/likeReply/getReplyCount/deleteReply）
 *  - 点赞/投币/收藏/三连：`LikeCoinFavApi.like()`/`coin()`/`favorite()`/`triple()`（BiliApiService.like/coin/favorite/triple）
 *  - 收藏夹：`FavoriteApi.getFavoriteFolders()`/`getFolderVideosV3()`/`getFavoriteState()`/`addFavorite()`/`deleteFavorite()`
 *  - 关注：`FollowApi.getFollowingList()`/`getFollowerList()`/`getFollowTags()`/`getFollowTagUsers()`、`UserInfoApi.followUser()`（BiliApiService.followUser）
 *  - 历史：`HistoryApi.getHistory()`/`reportHistory()`/`deleteHistory()`（BiliApiService.getHistory/reportHistory/deleteHistory）
 *  - 稍后看：`WatchLaterApi.getWatchLater()`/`addWatchLater()`/`deleteWatchLater()`（BiliApiService.getWatchLater/addWatchLater/deleteWatchLater）
 */
interface InteractionRepository {

    // ===== 评论 =====

    /** 评论列表（rpid>0 为楼中楼）。 */
    suspend fun replies(
        oid: Long,
        rpid: Long = 0,
        page: Int = 1,
        type: Int = 1,
        sort: Int = 1,
    ): BizResult<ReplyPage>

    /** 懒加载评论列表（游标分页）。 */
    suspend fun repliesLazy(
        oid: Long,
        rpid: Long = 0,
        pagination: String? = null,
        type: Int = 1,
        sort: Int = 1,
    ): BizResult<LazyReplyPage>

    /** 发表评论。图片需先经上传接口得到引用串。 */
    suspend fun sendReply(
        oid: Long,
        root: Long = 0,
        parent: Long = 0,
        text: String,
        type: Int = 1,
        pictureRefs: List<String>? = null,
    ): BizResult<Reply>

    /** 点赞评论（action：1 赞 / 2 取消）。 */
    suspend fun likeReply(oid: Long, rpid: Long, action: Int, type: Int = 1): BizResult<Unit>

    /** 删除评论。 */
    suspend fun deleteReply(oid: Long, rpid: Long, type: Int = 1): BizResult<Unit>

    /** 评论数。 */
    suspend fun replyCount(oid: Long, type: Int = 1): BizResult<Long>

    // ===== 点赞 / 投币 / 收藏 / 三连 =====

    /** 点赞/取消点赞（like=1 赞、2 取消，对应 BiliApiService.like）。 */
    suspend fun like(aid: Long, liked: Boolean): BizResult<Unit>

    /** 投币。 */
    suspend fun coin(aid: Long, multiply: Int = 1): BizResult<Unit>

    /** 三连（点赞+投币+收藏）。 */
    suspend fun triple(aid: Long): BizResult<Unit>

    /** 加入收藏夹（对应 LikeCoinFavApi.favorite / FavoriteApi.addFavorite）。 */
    suspend fun favorite(aid: Long, mediaId: Long): BizResult<Unit>

    // ===== 收藏夹 =====

    /** 收藏夹列表（当前用户/指定用户）。 */
    suspend fun favoriteFolders(mid: Long): BizResult<List<FavoriteFolder>>

    /** 指定收藏夹的视频列表。 */
    suspend fun favoriteFolderVideos(mediaId: Long, page: Int, keyword: String = ""): BizResult<FavoriteFolderPage>

    /** 查询 aid 在各收藏夹中的状态。 */
    suspend fun favoriteState(aid: Long): BizResult<List<FavoriteFolder>>

    // ===== 关注 =====

    /** 关注列表（分页）。 */
    suspend fun followings(mid: Long, page: Int): BizResult<Page<UserInfo>>

    /** 粉丝列表（分页）。 */
    suspend fun followers(mid: Long, page: Int): BizResult<Page<UserInfo>>

    /** 关注分组标签。 */
    suspend fun followTags(): BizResult<List<FollowTag>>

    /** 分组内用户列表。 */
    suspend fun followTagUsers(tagid: Int, page: Int): BizResult<Page<UserInfo>>

    /** 关注 / 取关用户（对应 UserInfoApi.followUser / BiliApiService.followUser）。 */
    suspend fun followUser(mid: Long, follow: Boolean): BizResult<Unit>

    // ===== 历史 =====

    /** 历史记录（游标分页）。 */
    suspend fun history(
        viewAt: Long = 0,
        business: String = "",
        max: Long = 0,
    ): BizResult<HistoryPage>

    /** 上报播放进度。privacy 为隐身时调用方应跳过。 */
    suspend fun reportHistory(aid: Long, cid: Long, progress: Long): BizResult<Unit>

    /** 删除单条历史。 */
    suspend fun deleteHistory(kid: String): BizResult<Unit>

    // ===== 稍后再看 =====

    /** 稍后再看列表。 */
    suspend fun watchLater(): BizResult<List<WatchLaterItem>>

    /** 添加稍后再看。 */
    suspend fun addWatchLater(aid: Long): BizResult<Unit>

    /** 删除稍后再看。 */
    suspend fun deleteWatchLater(aid: Long): BizResult<Unit>
}
