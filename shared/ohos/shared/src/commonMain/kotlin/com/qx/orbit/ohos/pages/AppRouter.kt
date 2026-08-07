package com.qx.orbit.ohos.pages

import com.tencent.kuikly.core.module.RouterModule

/**
 * Orbit · 全局路由表（Kuikly @Page 路由清单）。
 *
 * 对齐 Android `MainActivity` 的 `SwipeDismissableNavHost` 全部 composable route
 * （home / detail / bangumi_detail / live_room / article_detail / opus_detail /
 *  user_space / follow_list / favorite_folders / favorite_detail / history /
 *  watch_later / search / download_manager / reply_detail / dynamic_detail /
 *  settings_* / player / ...）。Kuikly 侧页面用 @Page 注册同名路由。
 *
 * 路由常量命名稳定，所有页面/代理统一引用本 object，禁止散落字面量。
 * 跨页参数统一经 [PageArgs]（进程内共享 map），参数键见 KEY_* 常量。
 */
object AppRouter {

    // ---------------------------------------------------------------------
    // 路由常量（稳定命名，勿改）
    // ---------------------------------------------------------------------

    /** 首页（推荐/热门/动态 tab + 侧滑菜单）。对应 Android `home`。 */
    const val PAGE_HOME = "home"

    /** 动态流（首页 DYNAMIC tab 内容，Kuikly 独立页）。对应 Android HomeScreen 内嵌 DynamicFeedScreen。 */
    const val PAGE_FEED = "feed"

    /** 视频详情。对应 Android `detail/{bvid}/{aid}`。 */
    const val PAGE_VIDEO_DETAIL = "videoDetail"

    /** 番剧详情。对应 Android `bangumi_detail/{media_id}`。 */
    const val PAGE_BANGUMI = "bangumi"

    /** 直播间。对应 Android `live_room/{roomId}`。 */
    const val PAGE_LIVE = "live"

    /** 专栏文章详情。对应 Android `article_detail/{id}`。 */
    const val PAGE_ARTICLE = "article"

    /** 图文动态详情。对应 Android `opus_detail/{id}`。 */
    const val PAGE_OPUS = "opus"

    /** 用户空间。对应 Android `user_space/{mid}`。 */
    const val PAGE_USER_SPACE = "userSpace"

    /** 追番列表。对应 Android `follow_list`。 */
    const val PAGE_FOLLOW = "follow"

    /** 收藏夹列表。对应 Android `favorite_folders`。 */
    const val PAGE_FAVORITES = "favorites"

    /** 收藏夹详情。对应 Android `favorite_detail/{fid}/{mid}`。 */
    const val PAGE_FAVORITE_DETAIL = "favoriteDetail"

    /** 历史记录。对应 Android `history`。 */
    const val PAGE_HISTORY = "history"

    /** 稍后再看。对应 Android `watch_later`。 */
    const val PAGE_WATCH_LATER = "watchLater"

    /** 搜索（输入页；结果页可带 query 复用本页）。对应 Android `search` + `search_result/{query}`。 */
    const val PAGE_SEARCH = "search"

    /** 缓存管理（含下载进度子页）。对应 Android `download_manager` + `video_download_progress/{downloadId}`。 */
    const val PAGE_DOWNLOAD = "download"

    /** 评论详情。对应 Android `reply_detail`。 */
    const val PAGE_REPLY = "reply"

    /** 写评论（Kuikly 独立页；Android 为详情页内嵌 Dialog）。 */
    const val PAGE_WRITE_REPLY = "writeReply"

    /** 动态详情。对应 Android `dynamic_detail/{id}`。 */
    const val PAGE_DYNAMIC_DETAIL = "dynamicDetail"

    /** 设置（含各设置子页）。对应 Android `settings_main` 与 settings_* 系列。 */
    const val PAGE_SETTINGS = "settings"

    /** 播放器定制。对应 Android `settings_player_customization`。 */
    const val PAGE_PLAYER_CUSTOMIZATION = "playerCustomization"

    /** 单指缩放教程。对应 Android `one_finger_zoom_tutorial`。 */
    const val PAGE_TUTORIAL = "tutorial"

    /** 关于。对应 Android `about`。 */
    const val PAGE_ABOUT = "about"

    /** 视频播放。对应 Android `player/{playerDataJson}`。 */
    const val PAGE_PLAYER = "player"

    /** 音频播放（Android 为 player 路由内嵌 AudioPlayerScreen，Kuikly 独立页）。 */
    const val PAGE_AUDIO_PLAYER = "audioPlayer"

    /** 登录（辅助路由，Android 导航图真实存在，不在 24 个稳定命名内）。 */
    const val PAGE_LOGIN = "login"

    // ---------------------------------------------------------------------
    // PageArgs 参数键（跨页传参统一 key）
    // ---------------------------------------------------------------------

    const val KEY_BVID = "bvid"
    const val KEY_AID = "aid"
    const val KEY_CID = "cid"
    const val KEY_MEDIA_ID = "mediaId"
    const val KEY_ROOM_ID = "roomId"
    const val KEY_ID = "id"
    const val KEY_MID = "mid"
    const val KEY_FID = "fid"
    const val KEY_QUERY = "query"
    const val KEY_DOWNLOAD_ID = "downloadId"
    const val KEY_REPLY = "reply"
    const val KEY_PLAYER_DATA = "playerDataJson"
    const val KEY_EPID = "epid"
    const val KEY_COMMENT_TYPE = "type"       // 评论区类型：1=视频 2=动态 3=文章 4=专栏
    const val KEY_TARGET_NAME = "targetName"  // 写评论的回复对象名

    // ---------------------------------------------------------------------
    // 页面描述（中文名 / 需要参数 / 登录要求）
    // ---------------------------------------------------------------------

    data class PageSpec(
        val page: String,
        val title: String,
        val params: List<String> = emptyList(),
        val requiresLogin: Boolean = false,
        val androidRoute: String = "",
    )

    val PAGES: List<PageSpec> = listOf(
        PageSpec(PAGE_HOME, "首页", androidRoute = "home"),
        PageSpec(PAGE_FEED, "动态流", androidRoute = "home（DynamicFeedScreen 内嵌）"),
        PageSpec(PAGE_VIDEO_DETAIL, "视频详情", listOf(KEY_BVID, KEY_CID), androidRoute = "detail/{bvid}/{aid}"),
        PageSpec(PAGE_BANGUMI, "番剧详情", listOf(KEY_MEDIA_ID), androidRoute = "bangumi_detail/{media_id}"),
        PageSpec(PAGE_LIVE, "直播间", listOf(KEY_ROOM_ID), androidRoute = "live_room/{roomId}"),
        PageSpec(PAGE_ARTICLE, "文章详情", listOf(KEY_ID), androidRoute = "article_detail/{id}"),
        PageSpec(PAGE_OPUS, "图文动态", listOf(KEY_ID), androidRoute = "opus_detail/{id}"),
        PageSpec(PAGE_USER_SPACE, "用户空间", listOf(KEY_MID), androidRoute = "user_space/{mid}"),
        PageSpec(PAGE_FOLLOW, "追番列表", requiresLogin = true, androidRoute = "follow_list"),
        PageSpec(PAGE_FAVORITES, "收藏夹列表", requiresLogin = true, androidRoute = "favorite_folders"),
        PageSpec(PAGE_FAVORITE_DETAIL, "收藏夹详情", listOf(KEY_FID, KEY_MID), requiresLogin = true, androidRoute = "favorite_detail/{fid}/{mid}"),
        PageSpec(PAGE_HISTORY, "历史记录", requiresLogin = true, androidRoute = "history"),
        PageSpec(PAGE_WATCH_LATER, "稍后再看", requiresLogin = true, androidRoute = "watch_later"),
        PageSpec(PAGE_SEARCH, "搜索", listOf(KEY_QUERY), androidRoute = "search / search_result/{query}"),
        PageSpec(PAGE_DOWNLOAD, "缓存管理", listOf(KEY_DOWNLOAD_ID), androidRoute = "download_manager / video_download_progress/{downloadId}"),
        PageSpec(PAGE_REPLY, "评论详情", listOf(KEY_REPLY), androidRoute = "reply_detail"),
        PageSpec(PAGE_WRITE_REPLY, "写评论", listOf(KEY_ID, KEY_COMMENT_TYPE, KEY_TARGET_NAME), requiresLogin = true, androidRoute = "各详情页内嵌 WriteReplyScreen"),
        PageSpec(PAGE_DYNAMIC_DETAIL, "动态详情", listOf(KEY_ID), androidRoute = "dynamic_detail/{id}"),
        PageSpec(PAGE_SETTINGS, "设置", androidRoute = "settings_main + settings_*"),
        PageSpec(PAGE_PLAYER_CUSTOMIZATION, "播放器定制", androidRoute = "settings_player_customization"),
        PageSpec(PAGE_TUTORIAL, "单指缩放教程", androidRoute = "one_finger_zoom_tutorial"),
        PageSpec(PAGE_ABOUT, "关于", androidRoute = "about"),
        PageSpec(PAGE_PLAYER, "视频播放", listOf(KEY_PLAYER_DATA), androidRoute = "player/{playerDataJson}"),
        PageSpec(PAGE_AUDIO_PLAYER, "音频播放", listOf(KEY_PLAYER_DATA), androidRoute = "player（AudioPlayerScreen 分支）"),
        PageSpec(PAGE_LOGIN, "登录", androidRoute = "login"),
    )

    private val pageMap: Map<String, PageSpec> = PAGES.associateBy { it.page }

    /** 查询页面描述；未知路由返回 null。 */
    fun pageSpec(page: String): PageSpec? = pageMap[page]

    // ---------------------------------------------------------------------
    // 便捷导航（统一走 PageArgs 传参）
    // ---------------------------------------------------------------------

    fun open(page: String) {
        RouterModule().openPage(page, null)
    }

    fun open(page: String, args: Map<String, Any?>) {
        args.forEach { (k, v) -> PageArgs.set(k, v) }
        RouterModule().openPage(page, null)
    }
}
