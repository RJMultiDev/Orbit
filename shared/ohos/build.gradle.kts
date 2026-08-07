/**
 * Orbit · shared/ohos 根构建脚本 —— 仅声明插件版本（apply false），
 * 实际由 :shared 模块应用。
 */
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.kuikly) apply false
    alias(libs.plugins.knoi) apply false
    alias(libs.plugins.ksp) apply false
}
