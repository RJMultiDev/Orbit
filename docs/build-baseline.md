# Orbit Android 构建基线（Phase 0 / A01）

> 目的：固化 HarmonyOS 改造前 Android 客户端的可复现构建基线。
> 日期：2026-08-07

## 1. 环境版本

| 组件 | 版本 | 备注 |
| --- | --- | --- |
| JDK | OpenJDK 21.0.11 (build 21.0.11+10) | launcher JVM，`java -version` 实测 |
| Gradle | 9.6.1 | `gradle/wrapper/gradle-wrapper.properties`，wrapper distribution |
| AGP | 9.2.1 | `gradle/libs.versions.toml` `[versions] agp` |
| Kotlin | 2.4.0 | 同上 `kotlin`；Kotlin Gradle plugin = `org.jetbrains.kotlin.plugin.compose` 2.4.0 |
| Compose BOM | 2026.06.01 | `compose-bom` |
| Android SDK | sdk.dir=`/home/rj/android-sdk`（`local.properties`） | `ANDROID_HOME=/opt/android-sdk` 存在但未使用 |
| compileSdk | 37（minor 1，`release(37){ minorApiLevel = 1 }`） | app/build.gradle.kts:9 |
| minSdk / targetSdk | 23 / 36 | app/build.gradle.kts:17-18 |
| versionCode / versionName | 510 / 0.5.10-Alpha | app/build.gradle.kts:19-20 |

### 根工程模块（settings.gradle.kts:50-53）

`:app`、`:ijkplayer-java`、`:DFMNext`、`:DanmakuFlameMaster`

## 2. Flavor / Engine 配置

定义位置：`app/build.gradle.kts:44-57`

- `flavorDimensions = "player"`，共两个 flavor：
  - `full`：`HAS_IJK=true`，额外依赖 `implementation(project(":ijkplayer-java"))`（app/build.gradle.kts:107），`versionNameSuffix = "-full"`
  - `lite`：`HAS_IJK=false`，`versionNameSuffix = "-lite"`，无 IJK
- 播放引擎由 BuildConfig `HAS_IJK` 切换（Media3/ExoPlayer 为通用实现，`app/build.gradle.kts:146-147`）。
- ABI splits：`armeabi-v7a`、`arm64-v8a` + universal（app/build.gradle.kts:58-65）。
- 依赖：DFMNext 与 DanmakuFlameMaster 为两个 flavor 共用（app/build.gradle.kts:108-109）。

## 3. 构建命令与结果

```bash
./gradlew :app:assembleLiteDebug --console=plain -x lint
```

- **结果：BUILD SUCCESSFUL in 8m 28s**（76 actionable tasks: 58 executed, 18 up-to-date）
- 首次构建会自动安装缺失的 `Android SDK Platform 37.1`（license 已接受），并下载全部依赖；总墙钟约 15 分钟。
- 产物（`app/build/outputs/apk/lite/debug/`，各约 35MB）：
  - `Orbit-0.5.10-Alpha-lite-arm64-v8a-release.apk`
  - `Orbit-0.5.10-Alpha-lite-armeabi-v7a-release.apk`
  - `Orbit-0.5.10-Alpha-lite-universal-release.apk`
  - `output-metadata.json`
- 注意：debug 构建产物文件名仍带 `-release`，这是 `app/build.gradle.kts:76` 的既有命名逻辑（`outputFileName.set("Orbit-${ver}-${abi}-release.apk")`），与构建类型无关。

## 4. DFMNext submodule 锁定状态

- `.gitmodules`：`path = DFMNext`，`url = git@github.com:YNotBili/DFMNext.git`（SSH）
- **锁定 commit：`40dbae8da1214b8354e7f6aae66a5b9cc3fd9216`**（`git submodule status` 无前缀 `-`，已初始化并检出，HEAD = `40dbae8` "无关紧要的更改"）
- 本次操作：初始状态为未初始化（status 前缀 `-`），已执行 `git submodule update --init` 检出到锁定 commit。

## 5. 已知问题 / 警告（不影响构建）

- **AGP 与 compileSdk 37.1 不匹配**：AGP 9.2.1 官方测试到 compileSdk 37.0，项目使用 37.1 → 警告。可在 `gradle.properties` 加 `android.suppressUnsupportedCompileSdk=37.1` 抑制（本阶段未改）。
- **`android.defaults.buildfeatures.resvalues=true` 已弃用**（AGP 10.0 将移除），gradle.properties:20。
- **Deprecated Gradle features**：构建报告与 Gradle 10 不兼容。
- 大量 Kotlin deprecation 警告（`StaticLayout`、`LocalClipboardManager`、`ClickableText`、`GsonBuilder.setLenient`、`FLAG_FULLSCREEN`、`externalMediaDirs` 等），均为警告。
- SDK 布局告警：`cmdline-tools;latest` 实际位于 `/home/rj/android-sdk/cmdline-tools/cmdline-tools/latest`（嵌套多一层），非致命。
- 构建时无 lint（`-x lint` 跳过）。

## 6. 构建耗时参考

| 阶段 | 耗时 |
| --- | --- |
| Gradle 报告总时长 | 8m 28s |
| 首次端到端墙钟（含 SDK Platform 37.1 安装 + 依赖下载） | ~15m |
| 增量构建（configuration cache 生效后） | 待记录（后续构建可对比） |

## 7. 下一步（Plan 阶段 0 后续）

- 功能矩阵、关键流程基线录制、测试脚手架与 CI。
- 注意：根 `settings.gradle.kts` / `gradle/libs.versions.toml` 由其他代理负责，本基线不触碰。
