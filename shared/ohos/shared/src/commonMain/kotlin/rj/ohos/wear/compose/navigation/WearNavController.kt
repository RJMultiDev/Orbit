package rj.ohos.wear.compose.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * [WearNavController] 对齐 `androidx.navigation.NavHostController` 的最小表面，
 * 实际导航行为委托给内部持有的 [WearNavigator]。
 *
 * 与 [WearNavigator] 一样，[currentBackStackEntry] / [previousBackStackEntry] 是
 * 名义值：Kuikly 不维护真实回栈，跨容器页面切换由 @Page + KSP + `RouterModule().openPage`
 * 完成。本类型的存在意义是让迁移代码保留 `navController.navigate(...)` 的调用形态。
 */
class WearNavController(
    initialRoute: String,
) {
    /** 内部导航器（可经 [WearNavHost] 的 scope 暴露给页面）。 */
    val navigator: WearNavigator = WearNavigator(currentBackStackEntry = initialRoute)

    val currentBackStackEntry: String?
        get() = navigator.currentBackStackEntry

    val previousBackStackEntry: String?
        get() = navigator.previousBackStackEntry

    fun navigate(route: String, params: Map<String, Any?> = emptyMap()) = navigator.navigate(route, params)

    fun navigateTo(route: String) = navigator.navigateTo(route)

    fun popBackStack() = navigator.popBackStack()
}

/** 记住一个以 [initialRoute] 为起点的 [WearNavController]。 */
@Composable
fun rememberWearNavController(initialRoute: String): WearNavController {
    return remember(initialRoute) { WearNavController(initialRoute) }
}
