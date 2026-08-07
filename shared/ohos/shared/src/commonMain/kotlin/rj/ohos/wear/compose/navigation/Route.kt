package rj.ohos.wear.compose.navigation

import com.qx.orbit.ohos.pages.AppRouter

/**
 * 路由构造辅助：页面名常量（对齐 [AppRouter]）与跨页参数键（对齐 [AppRouter.KEY_*]）。
 *
 * 与 androidx 的字符串路由不同，Kuikly 路由就是 `@Page("route")` 注册的页面名。
 * 直接引用 [AppRouter] 的 const，保证与现有页面（`com.qx.orbit.ohos.pages`）零漂移；
 * 所有页面/代理统一经本 object 或 [AppRouter] 引用，禁止散落字面量。
 */
object Route {

    // ---------------------------------------------------------------------
    // 页面名（= @Page 注册名 = AppRouter.PAGE_*）
    // ---------------------------------------------------------------------

    const val HOME = AppRouter.PAGE_HOME
    const val FEED = AppRouter.PAGE_FEED
    const val VIDEO_DETAIL = AppRouter.PAGE_VIDEO_DETAIL
    const val BANGUMI = AppRouter.PAGE_BANGUMI
    const val LIVE = AppRouter.PAGE_LIVE
    const val ARTICLE = AppRouter.PAGE_ARTICLE
    const val OPUS = AppRouter.PAGE_OPUS
    const val USER_SPACE = AppRouter.PAGE_USER_SPACE
    const val FOLLOW = AppRouter.PAGE_FOLLOW
    const val FAVORITES = AppRouter.PAGE_FAVORITES
    const val FAVORITE_DETAIL = AppRouter.PAGE_FAVORITE_DETAIL
    const val HISTORY = AppRouter.PAGE_HISTORY
    const val WATCH_LATER = AppRouter.PAGE_WATCH_LATER
    const val SEARCH = AppRouter.PAGE_SEARCH
    const val DOWNLOAD = AppRouter.PAGE_DOWNLOAD
    const val REPLY = AppRouter.PAGE_REPLY
    const val WRITE_REPLY = AppRouter.PAGE_WRITE_REPLY
    const val DYNAMIC_DETAIL = AppRouter.PAGE_DYNAMIC_DETAIL
    const val SETTINGS = AppRouter.PAGE_SETTINGS
    const val PLAYER_CUSTOMIZATION = AppRouter.PAGE_PLAYER_CUSTOMIZATION
    const val TUTORIAL = AppRouter.PAGE_TUTORIAL
    const val ABOUT = AppRouter.PAGE_ABOUT
    const val PLAYER = AppRouter.PAGE_PLAYER
    const val AUDIO_PLAYER = AppRouter.PAGE_AUDIO_PLAYER
    const val LOGIN = AppRouter.PAGE_LOGIN

    // ---------------------------------------------------------------------
    // 跨页参数键（= AppRouter.KEY_*，供 navigate(route, params) 使用）
    // ---------------------------------------------------------------------

    const val KEY_BVID = AppRouter.KEY_BVID
    const val KEY_AID = AppRouter.KEY_AID
    const val KEY_CID = AppRouter.KEY_CID
    const val KEY_MEDIA_ID = AppRouter.KEY_MEDIA_ID
    const val KEY_ROOM_ID = AppRouter.KEY_ROOM_ID
    const val KEY_ID = AppRouter.KEY_ID
    const val KEY_MID = AppRouter.KEY_MID
    const val KEY_FID = AppRouter.KEY_FID
    const val KEY_QUERY = AppRouter.KEY_QUERY
    const val KEY_DOWNLOAD_ID = AppRouter.KEY_DOWNLOAD_ID
    const val KEY_REPLY = AppRouter.KEY_REPLY
    const val KEY_PLAYER_DATA = AppRouter.KEY_PLAYER_DATA
    const val KEY_EPID = AppRouter.KEY_EPID
    const val KEY_COMMENT_TYPE = AppRouter.KEY_COMMENT_TYPE
    const val KEY_TARGET_NAME = AppRouter.KEY_TARGET_NAME

    /** 校验 route 是否在已知路由表中（声明表调试用）。 */
    fun isKnown(route: String): Boolean = AppRouter.pageSpec(route) != null
}
