# HarmonyOS 工具链版本锁定（Phase 0 / A04）

> 用途：为 Kuikly + API 22 手表可行性门（Phase 1）锁定工具链版本。
> 调研日期：2026-08-07。所有版本均来自下方"来源"，未臆造；查不到的标 TODO。
> 本文只锁定版本与配置写法，不涉及代码改动。

---

## 0. 版本口径澄清（重要）

**"HarmonyOS NEXT 6.1 / API 22" 这个组合在官方版本表中不存在。** 实测对应对照：

| 版本 | API | DevEco Studio 线 |
|---|---|---|
| HarmonyOS 6.0.2 | **API 22** | 6.0.2.x（Release） |
| HarmonyOS NEXT 6.1.0 | API 23 | 6.1.0.x |
| HarmonyOS NEXT 6.1.1 | API 24 | 6.1.1.x |

API 22 = HarmonyOS 6.0.2。若目标"API 22 手表"为硬约束，本文件按 **API 22（HarmonyOS 6.0.2）** 锁定；若目标其实是"NEXT 6.1"，则对应 API 23，见 [风险与开放问题](#5-风险与开放问题) 第 1 条。

---

## 1. 版本锁定表

| 组件 | 锁定版本 | 来源（获取日期 2026-08-07） | 本机是否已安装 | 备注 |
|---|---|---|---|---|
| 目标 OS / API | HarmonyOS 6.0.2 (22)，手表 | 华为 Release 页 | 否（本机 OS 未知，需真机） | API 22 = 6.0.2；目标真机需 6.0.2+ |
| DevEco Studio | **6.0.2.660 (Release)**（6.0.2.640/642/650 亦可） | https://developer.huawei.com/consumer/cn/doc/harmonyos-releases/deveco-studio-new-features-602 | 未安装 | **无官方 Linux 版**（仅 Win/macOS），本机走 CLI 构建，IDE 只在 Win/Mac 使用 |
| HarmonyOS SDK | HarmonyOS 6.0.2 Release SDK（compileSdk 6.0.2(22)；targetSdk 4.0.0(10)~6.0.2(22)；最低兼容 4.0.0(10)） | 同上 | **未安装**（本机装的是 6.1.1/API 24，见 §2） | 需下载 API 22 SDK 或整套 command-line-tools 6.0.2.x |
| Hvigor | **6.22.7**（DevEco 6.0.2.660 配套；6.0.2.642/650 为 6.22.3/6.22.4） | 同上 | 已装 **6.24.4**（来自 6.1.1 套件，非 6.0.2 配套） | hvigor 随 IDE/CLI 工具包附带；版本与 DevEco 配套 |
| ohpm | 6.0.2.640 | 同上 | 已装 **6.1.2.285** | 与 DevEco 配套；本机版本来自 6.1.1 套件 |
| Node.js（内置） | 18.20.1 | 同上 | 已装 v20.20.2（系统）+ CLI 套件内置 node | CLI 构建用套件内置 node 最稳 |
| HarmonyOS 模拟器 | 6.0.2.210 | 同上 | 未安装 | **X86 Mac 不支持鸿蒙模拟器**（需 Apple Silicon）；Linux 无模拟器，冒烟靠真表 |
| OHOS NDK / 编译链 | 随 SDK 附带，无独立公开版本号；路径 `sdk/default/openharmony/native`（llvm clang、cmake、ninja、sysroot、libc++） | https://device.harmonyos.com/cn/docs/apiref/harmonyos-guides/ndk-development-overview | API 24 套件内实测：**clang 15.0.4、cmake 3.28.2、ninja 1.12.0** | API 22 SDK 附带的 NDK 精确版本 → **TODO**：装完 API 22 SDK 后查 `native/llvm/bin/clang-XX` |
| Kuikly（KMP 侧） | **2.24.0**（2026-07-31） | https://github.com/Tencent-TDS/KuiklyUI/releases | 未安装（依赖未拉取） | OHOS target = **ohosArm64**；任务 `:demo:linkSharedDebugSharedOhosArm64`；产物 `libshared.so` + `libshared_api.h` |
| @kuikly-open/render（ohpm） | **2.24.0**（min 兼容 API 12） | https://ohpm.openharmony.cn/#/cn/detail/@kuikly-open%2Frender | 未安装 | **必须与 KMP 侧 Kuikly 版本一致**；`ohpm i @kuikly-open/render` |
| Kuikly maven 源（2.5.0+） | `maven("https://mirrors.tencent.com/nexus/repository/maven-tencent/")` | https://kuikly.tds.qq.com/ChangeLog/changelog.html | 源可达（curl HTTP 200 实测） | 2.5.0 起切换该源；Maven Central 仅存 ≤2.4.2（如 core 2.4.2-2.0.21-ohos） |
| Kotlin（KMP 默认） | 2.1.21（Kuikly Compose 默认线；2.7.0 起从 2.0.21 升级到 2.1.21） | GitHub README / changelog 2.7.0 | 项目侧待定（本机有 JDK21） | OHOS 构建实际使用 **Kotlin 2.0.21-KBA-010**（腾讯 K/N 分支，README 注明支持 Windows/Linux） |
| Kotlin-OHOS（社区） | prebuilt `kotlin-native-prebuilt-macos-aarch64-2.1.255-SNAPSHOT`；target = **harmonyOSArm64**（任务 `linkHarmonyOSArm64`） | https://github.com/zxystd/Kotlin-OHOS | 未安装 | **2024-09-19 后停更**；仅 M 系 Mac；需 aarch64 Azul Zulu JDK8；只作参考，不作主路径 |
| JDK | CLI 构建：CLI 工具 ≥26.0.0 Beta2 用 **JDK21**，以下用 JDK17；Kuikly Gradle 侧要求 JDK17 | https://device.harmonyos.com/cn/docs/apiref/harmonyos-guides/ide-command-line-building-app | 已装 OpenJDK 21.0.11 | 满足 CLI 侧；Kuikly 源码构建需 JDK17（Android Studio 里切 Gradle JDK） |
| 宿主 OS（Linux CLI 构建） | Linux 64 位，GLIBC 2.28+，内存 ≥8GB（推荐 16GB），磁盘 ≥100GB | 同上 | GLIBC/内存 → **TODO** 验证 | 官方文档明确支持 Linux 命令行构建 HAP |
| 签名 | `hap-sign-tool.jar`（CLI 套件内置）；.p12/.cer/.p7b 来自 AppGallery Connect | 同上 | 工具在套件内（待确认路径） | 本地签名需申请 Debug/Release Profile |

---

## 2. 本机工具链实测状态（2026-08-07）

> 与计划前提（"本机只有 JDK21 + Android SDK，无 DevEco/Harmony SDK/hvigor"）**不一致**：实测 `~/.local` 下已存在 **Command Line Tools 6.1.1.300**。

- `~/.local/version.txt`：Command Line Tools (linux-x64) 6.1.1.300，releaseMr2
  - hvigor **6.24.4**、ohpm **6.1.2.285**、codelinter 6.0.240、hstack 5.1.0
  - HarmonyOS SDK：**HarmonyOS 6.1.1 Release（Ohos_sdk_public 6.1.1.125，API 24 Release）**
- SDK 位置：`~/.local/sdk/default/`（`sdk-pkg.json` → apiVersion 24 / 6.1.1.125）
- NDK：`sdk/default/openharmony/native` → **clang 15.0.4**（OHOS dev）、**cmake 3.28.2**、**ninja 1.12.0**、sysroot 存在
- hdc：`sdk/default/openharmony/toolchains/hdc`（未加入 PATH）
- `ANDROID_HOME=/opt/android-sdk`；JDK 21.0.11；系统 node v20.20.2

**结论**：本机缺少的是 **API 22（6.0.2）SDK**，而非整套 Harmony 工具链。需要决定：下载 API 22 SDK/CLI 套件，或改用本机 API 24 套件（见 §5 第 2、3 条）。

---

## 3. 建议的 Min-supported API 22 配置写法

### 3.1 工程级 `build-profile.json5`（API 22 编译，最低兼容 API 22 手表）

```json5
{
  "app": {
    "signingConfigs": [
      {
        "name": "default",
        "type": "HarmonyOS",
        "material": {
          "certpath": "./SigningConfig/debug_hos.cer",
          "storePassword": "****",
          "keyAlias": "debugKey",
          "keyPassword": "****",
          "profile": "./SigningConfig/debug_hos.p7b",
          "signAlg": "SHA256withECDSA",
          "storeFile": "./SigningConfig/debug_hos.p12"
        }
      }
    ],
    "products": [
      {
        "name": "default",
        "signingConfig": "default",
        "bundleName": "com.qx.orbit.watch",
        "compileSdkVersion": "6.0.2(22)",
        "targetSdkVersion": "6.0.2(22)",
        "compatibleSdkVersion": "6.0.2(22)",
        "runtimeOS": "HarmonyOS",
        "arkTSVersion": "1.1",
        "bundleType": "app"
      }
    ],
    "buildModeSet": [
      { "name": "debug", "buildOption": { "debuggable": true } },
      { "name": "release", "buildOption": { "debuggable": false } }
    ]
  },
  "modules": [
    {
      "name": "entry",
      "srcPath": "./entry",
      "targets": [
        { "name": "default", "applyToProducts": ["default"] }
      ]
    }
  ]
}
```

要点：
- HarmonyOS 环境 SDK 版本写法为 `"6.0.2(22)"`（OpenHarmony 用数值 `22`；API 26.0.0 后改为 `"26.0.0"` 纯字符串）。
- `compatibleSdkVersion` 必填且为"最低兼容版本"；`targetSdkVersion` 建议与 compileSdk 一致。
- 若只允许在 6.0.2 及以上设备安装：`compatibleSdkVersion` = `"6.0.2(22)"`；若希望兼容更老设备可下调（受已装 SDK 限制，见 §5）。

### 3.2 `entry/src/main/module.json5`（wearable 手表）

```json5
{
  "module": {
    "name": "entry",
    "type": "entry",
    "description": "$string:module_desc",
    "mainElement": "EntryAbility",
    "deviceTypes": ["wearable"],
    "deliveryWithInstall": true,
    "pages": "$profile:main_pages",
    "abilities": [
      {
        "name": "EntryAbility",
        "srcEntry": "./ets/entryability/EntryAbility.ets",
        "description": "$string:EntryAbility_desc",
        "icon": "$media:icon",
        "label": "$string:EntryAbility_label",
        "startWindowIcon": "$media:startIcon",
        "startWindowBackground": "$color:start_window_background",
        "exported": true,
        "skills": [
          { "entities": ["entity.system.home"], "actions": ["ohos.want.action.home"] }
        ]
      }
    ]
  }
}
```

要点：`deviceTypes` 取值中手表为 **`"wearable"`**（智能手表，具备电话能力）。`distroFilter`（API 9 以前）/`distributionFilter`（API 10+）用于多 Entry 细分分发，单 Entry 不需要。

### 3.3 `hvigor/hvigor-config.json5`（与 DevEco 6.0.2 配套）

```json5
{
  "modelVersion": "6.0.0",
  "dependencies": {},
  "execution": {},
  "logging": {},
  "debugging": {},
  "nodeOptions": {}
}
```

要点：HarmonyOS 6.0.x（API 20~22）使用 `modelVersion: "6.0.0"`；6.1.0（API 23）为 `"6.1.0"`。新版（DevEco 5.0+）不再写 `hvigorVersion`，由 IDE/CLI 套件内嵌 hvigor 解析 `@ohos/hvigor-ohos-plugin`。

### 3.4 `hvigorfile.ts`

工程级：
```ts
import { appTasks } from '@ohos/hvigor-ohos-plugin';
export default { system: appTasks, plugins: [] };
```
模块级（entry）：
```ts
import { hapTasks } from '@ohos/hvigor-ohos-plugin';
export default { system: hapTasks, plugins: [] };
```

### 3.5 `entry/oh-package.json5`（接入 Kuikly render）

```json5
{
  "modelVersion": "6.0.0",
  "description": "Orbit entry",
  "dependencies": {
    "@kuikly-open/render": "2.24.0"
  },
  "devDependencies": {
    "@ohos/hypium": "1.0.25",
    "@ohos/hamock": "1.0.0"
  }
}
```

### 3.6 KMP 侧（shared/ohos，独立构建）

- 仓库：`maven("https://mirrors.tencent.com/nexus/repository/maven-tencent/")`；group `com.tencent.kuikly-open`，版本 `2.24.0`（与 render 一致）。
- Kotlin：默认 2.1.21；OHOS 构建用 `2.0.21-KBA-010`（支持 Windows/Linux）。
- target：`ohosArm64 { binaries.sharedLib { baseName = "shared" } }` → 产物 `libshared.so`、`libshared_api.h`，拷入 `ohosApp/entry/src/main/cpp/thirdparty/biz_entry/` 与 `entry/libs/arm64-v8a/`，由 CMake/NAPI 链接。

### 3.7 Linux 命令行构建（hvigorw，无需 DevEco）

```bash
# 前置：CLI 套件解压后设置 COMMANDLINE_TOOL_DIR；JAVA_HOME=JDK21
export PATH="$COMMANDLINE_TOOL_DIR/command-line-tools/bin:$PATH"
ohpm config set registry https://ohpm.openharmony.cn/ohpm/
ohpm install --all
hvigorw assembleHap --mode module -p product=default -p buildMode=debug --no-daemon
# 签名
java -jar "$COMMANDLINE_TOOL_DIR/command-line-tools/sdk/default/openharmony/toolchains/lib/hap-sign-tool.jar" sign-app ...
```

---

## 4. 版本获取/复现说明

- DevEco Studio / command-line-tools / 各 API SDK：华为开发者官网下载页（需要华为账号；部分 API 22 组件可能要求登录 DevEco 的 SDK Manager 下载）。API 22 SDK 单独下载路径 → **TODO** 实测。
- Kuikly：GitHub Releases（源码）+ ohpm（render）+ maven-tencent（KMP 制品）。CI 需在首次构建时固化 `libs.versions.toml`/`oh-package-lock.json5` 并缓存制品。
- Kotlin 2.0.21-KBA-010：由 Kuikly 官方发布（Windows 构建路径使用）；发布坐标 → **TODO** 在 Phase 1 首次拉取时固化。

---

## 5. 风险与开放问题

1. **版本口径冲突（决策项）**：计划写"HarmonyOS NEXT 6.1 / API 22"，但 API 22 = HarmonyOS 6.0.2，NEXT 6.1 = API 23。**需确认目标真机系统版本**再定锁：API 22 → DevEco 6.0.2 线；若实际是 6.1 设备，则锁 DevEco 6.1.0 线（Hvigor 6.23.x / ohpm 6.1.1.830 / SDK 6.1.0(23)）。
2. **本机 SDK 是 API 24，非 API 22**：`~/.local` 的 CLI 套件 6.1.1.300 只带 6.1.1(24) SDK。hvigor 解析 `compatibleSdkVersion: "6.0.2(22)"` 时若找不到已安装的 API 22 SDK 会报错（社区已有 "unable to find compatibleSdkVersion" 先例）。→ **TODO**：Phase 1 先验证 (a) 能否给 CLI 套件追加 API 22 SDK；(b) 不行则在 API 24 套件上临时把 `compileSdkVersion/targetSdkVersion/compatibleSdkVersion` 都设 `"6.1.1(24)"`，仅把"最低兼容"目标定为 6.0.2 真机。
3. **Kuikly OHOS .so 在 Linux 上构建未获官方背书**：README 只写了 Mac（`.sh`）和 Windows（`.bat`，2026-01 起支持）；但 OHOS 专用 Kotlin 工具链 `2.0.21-KBA-010` 官方注明支持 Linux。→ **TODO**：Phase 1 在 Linux 直接跑 `gradlew -c settings.2.0.ohos.gradle.kts :demo:linkSharedDebugSharedOhosArm64` 验证；失败则回退到 Mac/Windows 出 `.so`、Linux 只出 HAP。
4. **模拟器受限**：HarmonyOS 模拟器不支持 X86 Mac（需 Apple Silicon），Linux 无模拟器。Phase 1 全部在 API 22 真表验证。
5. **DevEco Studio 无 Linux 版**：本机只能 CLI 构建 HAP；IDE 内可视化/签名/调试需在 Win/Mac 机器上做，或全程命令行。
6. **API 22 NDK 精确版本未定**：当前仅测到 API 24 套件为 clang 15.0.4 / cmake 3.28.2 / ninja 1.12.0。API 22 SDK 的 NDK → **TODO** 安装后确认并回填。
7. **Kuikly 2.24.0 与 API 22 compileSdk 兼容性**：render 最低兼容 API 12，但 2.24.0 在 API 22 SDK 上编译/运行未经验证 → Phase 1 首步验证。
8. **Maven 制品依赖私有/镜像源**：2.5.0+ 只在 `mirrors.tencent.com`，CI 需配置该源并保留制品缓存；不满足"不依赖无法长期取得制品"的风险控制时需重新评估。
9. **Kotlin-OHOS 社区方案停更**（2024-09 后无提交，基于 NEXT Beta1/API 12），仅作 target 命名（`harmonyOSArm64`）与集成方式参考；**不作为生产主路径**。
10. **modelVersion / hvigor-ohos-plugin 精确配套**：`6.0.0` 与 hvigor 6.22.x 的映射基于官方升级文档推断，未在 API 22 工程实建验证 → Phase 1 生成工程后回填精确值。

---

## 6. 来源清单（获取日期均为 2026-08-07）

- DevEco Studio 6.0.2 组件版本（Hvigor 6.22.7 / ohpm 6.0.2.640 / Node 18.20.1 / SDK 6.0.2(22) / 模拟器 6.0.2.210）：https://developer.huawei.com/consumer/cn/doc/harmonyos-releases/deveco-studio-new-features-602
- DevEco Studio 6.1.0 组件版本（Hvigor 6.23.x / API 23）：https://developer.huawei.com/consumer/cn/doc/harmonyos-releases/deveco-studio-new-features-610
- HarmonyOS 6.0→6.1 改动点（modelVersion 6.0.0→6.1.0、oh-package.json5、targetSdkVersion 6.0.0(20)→6.1.0(23)）：https://segmentfault.com/a/1190000047731895
- 工程级 build-profile.json5（compileSdk/targetSdk/compatibleSdk 写法 `"6.0.2(22)"`）：https://device.harmonyos.com/cn/docs/apiref/harmonyos-guides/ide-hvigor-build-profile-app
- module.json5（deviceTypes 枚举，`wearable` = 智能手表）：https://device.harmonyos.com/cn/docs/apiref/harmonyos-guides/module-configuration-file
- hvigorfile.ts（appTasks/hapTasks）：https://segmentfault.com/a/1190000047755840
- Linux 命令行构建 HAP（CLI 工具、JDK 21、hvigorw/ohpm/hdc/hap-sign-tool）：https://device.harmonyos.com/cn/docs/apiref/harmonyos-guides/ide-command-line-building-app
- NDK 导读（NDK 随 SDK 附带）：https://device.harmonyos.com/cn/docs/apiref/harmonyos-guides/ndk-development-overview
- Kuikly Releases（2.24.0 = 2026-07-31）：https://github.com/Tencent-TDS/KuiklyUI/releases
- Kuikly Changelog（maven 源、2.5.0 起用 maven-tencent）：https://kuikly.tds.qq.com/ChangeLog/changelog.html
- Kuikly README（Kotlin 2.1.21、ohosArm64、2.0.21-KBA-010、DevEco 5.1.0+/API≥18、OHOS 构建 Mac/Windows）：https://github.com/Tencent-TDS/KuiklyUI
- Kuikly 鸿蒙接入（@kuikly-open/render、X86 Mac 不支持鸿蒙模拟器）：https://kuikly.tds.qq.com/QuickStart/harmony.html
- Kuikly 环境搭建（JDK17、Gradle 7.x）：https://kuikly.tds.qq.com/QuickStart/env-setup.html
- @kuikly-open/render（V2.24.0，min API 12）：https://ohpm.openharmony.cn/#/cn/detail/@kuikly-open%2Frender
- maven-tencent 源可达性：curl `https://mirrors.tencent.com/nexus/repository/maven-tencent/com/tencent/kuikly-open/` → HTTP 200（实测）
- Kotlin-OHOS（harmonyOSArm64、停更、仅 M 系 Mac）：https://github.com/zxystd/Kotlin-OHOS
- DevEco Studio 系统要求（仅 Windows/macOS）：https://devecostudio.huawei.com/
- 本机实测：`~/.local/version.txt`、`~/.local/sdk/default/sdk-pkg.json`、NDK clang/cmake/ninja 版本（Bash 实测）
