/**
 * Orbit · shared/ohos —— HarmonyOS (OHOS) 独立构建入口
 *
 * 本工程与根工程（Android/Wear OS）完全隔离：
 *  - 不共享根 settings.gradle.kts / build.gradle.kts / gradle/libs.versions.toml
 *  - 不共享 Gradle plugin resolution（本文件独立声明 pluginManagement / dependencyResolutionManagement）
 *  - 复用根工程 gradlew + wrapper jar，仅通过 `-c` 指定本文件作为 settings
 *
 * 构建命令（仓库根目录执行，复用根 wrapper）：
 *   ./gradlew -c shared/ohos/settings.gradle.kts :shared:linkOhosArm64
 *
 * 版本锁定：所有版本集中在 gradle/libs.versions.toml，
 * 权威来源为 A04 的 docs/harmony-toolchain-lock.md（当前不存在，值为官方最新 + TODO）。
 */
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // Kuikly 2.5.0+ 官方 Maven 源：Kotlin OHOS 定制版（KBA fork）、Kuikly 编译插件、
        // KNOI 等均从此拉取（2.5.0 之前的版本在 Maven Central）。
        maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/") }
        // Kuikly 官方 Gradle 插件镜像（KBA kotlin 插件、kuikly 编译插件等）。
        maven { url = uri("https://mirrors.tencent.com/nexus/repository/gradle-plugins/") }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // Kuikly UI/核心库（com.tencent.kuikly-open:*，>= 2.5.0 从此源获取）。
        maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/") }
        // 如需消费本地发布制品（如对 Kuikly 自行 patch 后 mavenLocal 发布）可取消注释：
        // mavenLocal()
    }
}

rootProject.name = "shared-ohos"

include(":shared")
