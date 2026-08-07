# Orbit · shared/ohos —— HarmonyOS (OHOS) 独立 Kuikly 构建

面向 HarmonyOS NEXT 6.1 / API 22 手表的 Kuikly/KMP 业务模块骨架（Phase 1, A06），
与根工程（Android/Wear OS 单体）完全隔离。

## 为什么独立构建

- 根工程是 AGP 9.x / Gradle 9.6.1 / Kotlin 2.4 的 Android 工程（`settings.gradle.kts`、
  `gradle/libs.versions.toml` 均为其服务）。
- OHOS 目标依赖 Kuikly / Kotlin-OHOS 定制编译链（KBA fork + Kuikly 编译插件
  `com.tencent.kuikly-open.kuikly` + KNOI），官方 OHOS 构建脚本锁定
  Kotlin `2.0.21-KBA-010`、AGP 7.4.2、**Gradle 8.0**，与根工程 wrapper/plugin resolution
  不兼容。
- 把上述插件塞进根工程会牵动 Kotlin/Native 工具链、依赖解析与版本冲突，破坏稳定的
  Android 构建。因此本目录自持 `settings.gradle.kts` + `build.gradle.kts` +
  `gradle/libs.versions.toml`，复用根工程 `gradlew`（wrapper jar 不复制），
  仅通过 `-c` 指定本 settings 完成隔离构建。

## 目录结构

```
shared/ohos/
├── settings.gradle.kts              # 独立构建入口（pluginManagement / repos / include(":shared")）
├── build.gradle.kts                 # 根：仅声明插件版本（apply false）
├── gradle.properties                # 本构建独立 JVM 配置
├── gradle/libs.versions.toml        # 本构建独立版本锁定表（根工程不受影响）
├── shared/
│   ├── build.gradle.kts             # KMP：ohosArm64 target + Kuikly 依赖 + KSP
│   └── src/commonMain/kotlin/
│       └── com/qx/orbit/ohos/
│           ├── AppEntry.kt          # 根包导出入口 fun initKuikly(): Int
│           ├── Platform.kt          # 纯 Kotlin 占位（AppInfo 等）
│           └── pages/TestPage.kt    # @Page("test") 示例页面
└── README.md
```

## 构建

前置条件（Kuikly 官方环境要求）：

- JDK 17（Android Studio 2024.2.1+ 默认 Gradle JDK 21，需切回 17）。
- DevEco Studio 5.1.0+（HarmonyOS SDK，API >= 18；本项目目标 API 22）。
- OHOS SDK 环境变量（Windows 需手动设置，参考 Kuikly 官方 README）：
  `OHOS_SDK_HOME`、`TOOL_HOME`。
- Gradle 8.0：Kuikly 官方 OHOS 构建脚本（`2.0_ohos_demo_build.sh`）会临时把
  `distributionUrl` 切到 `gradle-8.0-bin.zip`。根 wrapper 为 9.6.1，可能与 KBA fork
  工具链不兼容；TODO(A04) 在 `docs/harmony-toolchain-lock.md` 锁定 Gradle 版本后，
  必要时为本目录单独提供 `gradle/wrapper/gradle-wrapper.properties`（指向 8.0，
  复用根 wrapper jar 与 gradlew 脚本）。

构建命令（仓库根目录执行，复用根 gradlew，`-c` 指定本 settings）：

```bash
./gradlew -c shared/ohos/settings.gradle.kts :shared:linkOhosArm64
```

- `-c` 使根工程目录变为 `shared/ohos/`（即 settings 所在目录），
  `include(":shared")` 解析为 `shared/ohos/shared`。
- 若 `shared/build.gradle.kts` 中 `sharedLib` 的 baseName 导致完整任务名不同
  （例如 `:shared:linkSharedDebugSharedOhosArm64` / `:shared:linkSharedReleaseSharedOhosArm64`），
  以 `./gradlew -c shared/ohos/settings.gradle.kts :shared:tasks --all` 列出的为准；
  TODO(A04) 校正为 lock 文件内的规范任务名。

产物（Debug）：

- `shared/build/bin/ohosArm64/sharedDebugShared/libshared.so`
- `shared/build/bin/ohosArm64/sharedDebugShared/libshared_api.h`

## 拷贝到 Harmony HAP

将产物拷入 `harmony/` 工程（本阶段 `harmony/` 尚不存在，路径为规划；A04/harmony 任务确认）：

```
libshared.so     -> harmony/entry/src/main/cpp/libs/${OHOS_ARCH}/libshared.so
libshared_api.h  -> harmony/entry/src/main/cpp/    （或 CMakeLists.txt 引用的头文件目录）
```

参考：Kuikly 官方 demo 的拷贝约定为
`ohosApp/entry/libs/arm64-v8a/libshared.so` 与
`ohosApp/entry/src/main/cpp/thirdparty/biz_entry/libshared_api.h`，
CMakeLists.txt 中通过 `libs/${OHOS_ARCH}/libshared.so` 引用，并 `target_link_libraries(... kuikly_shared kuikly_render)`。
N-API 胶水调用 `libshared_symbols()->kotlin.root.initKuikly()`，对应本骨架的
`AppEntry.kt`（根包，无 package 声明）。ArkTS 宿主以 `pageName: 'test'` 加载
`TestPage.kt` 的 `@Page("test")` 页面。

## 版本锁定

- 所有版本集中在 `gradle/libs.versions.toml`（本目录独立，不影响根工程）。
- 权威版本来源：`docs/harmony-toolchain-lock.md`（A04 产出，当前不存在）。
- 当前值取自 Kuikly 官方（2026-06-02 release 2.21.0）与官方 OHOS 构建脚本：
  - Kotlin（KBA fork）`2.0.21-KBA-010`；
  - Kuikly `2.21.0`（**2.5.0 起 Maven 源切换**到
    `https://mirrors.tencent.com/nexus/repository/maven-tencent/`，settings 已加入）；
  - KNOI 插件 `com.tencent.kuiklybase.knoi.plugin` `0.0.4`；
  - KSP `2.0.21-1.0.27`；
  - Gradle 8.0（wrapper 处理见上）。
- 所有 `TODO(A04)` 项在 lock 文件产出后逐一核对（KBA 版本、Kuikly 源上的版本号规则、
  目标名/任务名、KSP 的 OHOS 接线、编译/链接参数、KNOI 是否必需）。

## 参考

- Kuikly 快速开始（KMP 侧）：https://kuikly.tds.qq.com/QuickStart/common.html
- Kuikly 鸿蒙侧接入（N-API 胶水 / CMakeLists / 拷贝约定）：https://kuikly.tds.qq.com/QuickStart/harmony.html
- KuiklyUI 源码：https://github.com/Tencent-TDS/KuiklyUI

## 构建状态（2026-08-07 实测：已打通 ✅）

- **libshared.so 已成功产出**：`./build.sh`（或 `./build.sh install` 拷入 harmony/entry）。
- 关键步骤/踩坑：
  1. 独立 **Gradle 8.0** wrapper（本目录 `gradle/wrapper`），KBA 官方锁定 Gradle 8.0；
  2. **JDK 17**：`gradle.properties` 设 `org.gradle.java.home=/usr/lib/jvm/java-17-openjdk`；
  3. **OHOS_SDK_HOME** env 指向 OpenHarmony SDK（`~/.local/sdk/default/openharmony`），
     否则链接阶段报 "OHOS SDK is not found in /usr/local/lib/DevEco-Studio/..."；
  4. 模块**不应用** `com.tencent.kuikly-open.kuikly` 插件（那是 JS/H5 打包插件），
     OHOS 用 KNOI（`com.tencent.kuiklybase.knoi.plugin` 0.0.4）+ `binaries.sharedLib("shared")`；
  5. 版本对齐：KBA Kotlin `2.0.21-KBA-010`、Kuikly `2.24.0-2.0.21(-ohos)`、
     KSP `2.0.21-1.0.27`（Maven Central）；
  6. 产物任务：`linkSharedDebugSharedOhosArm64`（官方命名）。
- 产物：`shared/build/bin/ohosArm64/sharedDebugShared/{libshared.so, libshared_api.h}`，
  头文件结构 `libshared_symbols()->kotlin.root.initKuikly()` 与 harmony N-API 胶水匹配。
- HAP 已包含 libshared.so；`EntryAbility` 默认仍走原生 ArkUI（Home），
  Home 页「Kuikly」按钮可进入 Kuikly 容器页（pageName=test，渲染 TestPage）。
