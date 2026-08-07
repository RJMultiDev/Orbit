/**
 * Orbit · shared/ohos · :shared —— KMP 业务模块（OHOS 目标，Compose DSL）
 *
 * 对齐 Kuikly 官方 OHOS 构建（Tencent-TDS/KuiklyUI `demo/build.2.0.ohos.gradle.kts`）：
 *  - 不应用 com.tencent.kuikly-open.kuikly（该插件用于 JS/H5/小程序打包，OHOS 不需要）；
 *  - OHOS 目标由 KNOI（com.tencent.kuiklybase.knoi.plugin）提供；
 *  - UI 采用 Compose Multiplatform DSL（org.jetbrains.compose + kotlin plugin.compose +
 *    Kuikly compose fork），页面用 @Page + @Composable。
 *  - 产物：libshared.so + libshared_api.h（`binaries.sharedLib("shared")`）。
 *
 * 构建命令：cd shared/ohos && ./build.sh
 */
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.knoi)
}

knoi {
    // KNOI 生成的 ArkTS 类型声明输出目录（对接 harmony/entry 的 ts-api）。
    tsGenDir = rootProject.projectDir.absolutePath + "/../harmony/entry/src/main/ets/ts-api/"
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
    ohosArm64 {
        binaries.sharedLib("shared") {
            freeCompilerArgs += "-Xadd-light-debug=enable"
            linkerOpts += "--build-id=sha1"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Kuikly UI 核心库（OHOS 变体）
                implementation(libs.kuikly.core)
                // @Page 等业务注解
                implementation(libs.kuikly.annotations)
                // Kuikly Compose-DSL（Compose Multiplatform 跨端 UI）
                implementation(libs.kuikly.compose)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

// @Page 注解处理（KSP，配置名 kspOhosArm64）
dependencies {
    add("kspOhosArm64", libs.kuikly.ksp)
}

ksp {
    arg("catchException", "false")
}
