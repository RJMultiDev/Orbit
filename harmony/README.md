# Orbit HarmonyOS 手表客户端

Orbit 的 HarmonyOS 客户端（目标形态：**手表 wearable**）。独立 DevEco Stage 工程，与根目录
Android/Wear 工程（Gradle）完全隔离，使用 Hvigor/ohpm 构建，产出 arm64-v8a HAP。

## 当前状态（2026-08-07）

- ✅ **HAP 可构建**：`./scripts/build-hap.sh`（OpenHarmony 6.1.1 / API 24 / hvigor 6.24.4）
- ✅ **Kuikly 渲染器已集成**：`@kuikly-open/render@2.24.0`（ohpm 中心仓），NAPI 胶水
  `libentry.so` 随包打入；`libshared.so` 缺失时 `initKuikly` 降级返回 0
- ✅ **原生 ArkUI 垂直切片**（当前默认入口，`pages/Home`）：
  - 推荐流（WBI 签名）→ 扫码登录（QRCode 组件 + 轮询状态机）→ AVPlayer 播放
  - ArkTS 移植的 shared/core 纯算法：`core/sign/`（MD5/WBI/AppSign/编码）、
    `core/net/`（Cookie/HTTP）、`core/bili/`（BiliApi/CredentialStore/类型）
- ✅ **shared/ohos KBA 工具链打通**：`libshared.so` + `libshared_api.h` 已产出并打入 HAP，
  Kuikly 容器页（pageName=test）可渲染 TestPage（Home 页「Kuikly」按钮进入）
- ⏳ 签名（真机安装必需）、真机验收

## 目录结构

```
harmony/
├── scripts/build-hap.sh          # 可复现构建脚本（env + hvigorw assembleHap）
├── local.properties              # sdk.dir（OpenHarmony SDK 新版布局接入）
├── build-profile.json5           # compileSdk=24、runtimeOS=OpenHarmony、useNormalizedOHMUrl
├── hvigor/hvigor-config.json5    # @ohos/hvigor-ohos-plugin 6.24.4
├── AppScope/                     # bundleName=com.qx.orbit
└── entry/
    ├── oh-package.json5          # @kuikly-open/render@2.24.0 + @ohos/hypium
    └── src/main/
        ├── module.json5          # deviceTypes=["wearable"]、INTERNET 权限
        ├── cpp/                  # N-API glue（libentry.so）+ CMakeLists（libshared 缺失降级）
        └── ets/
            ├── entryability/EntryAbility.ets   # 加载 pages/Home
            ├── pages/            # Home（推荐流）/ Login（扫码）/ Player（AVPlayer）
            ├── kuikly/           # Kuikly 渲染器宿主（Index 容器、适配器、NAPI manager）
            └── core/             # ArkTS 移植的共享算法与 B 站 API
                ├── sign/         # Md5 / PercentEncoding / WbiSigner / AppSign
                ├── net/          # Cookie / BiliHttp
                └── bili/         # BiliApi / BiliTypes / CredentialStore / WbiKeys
```

## 构建

```bash
# 前置：OpenHarmony SDK ~/.local/sdk/default/openharmony（6.1.1/API24）、
#       hvigor 6.24.4（~/.local/hvigor）、ohpm、npm registry 华为仓。
./scripts/build-hap.sh            # 产出 entry/build/default/outputs/default/entry-default-unsigned.hap
```

### 环境注意（本次打通踩坑记录）

1. hvigor 6.24.4 使用**新版 SDK 布局** `{sdkRoot}/{apiVersion}/{component}`；
   本机 SDK 为旧布局，通过 `~/.local/sdk/orbit/openharmony/24/*` 符号链接接入，
   `local.properties` 的 `sdk.dir` 指向该目录。
2. `@ohos/hvigor-ohos-plugin` 发布在华为 npm 仓 `https://repo.harmonyos.com/npm/`
   （`npm_config_registry` 需指向它，ohpm 依赖仍走 `ohpm.openharmony.cn`）。
3. API 10~25 时 `compileSdkVersion`/`compatibleSdkVersion` 写**数字**（24）。
4. `@kuikly-open/render` 是 Bytecode HAR，须在 `strictMode.useNormalizedOHMUrl=true`。
5. 本机 SDK/NDK 若干可执行文件是损坏的「裸名称 shim」（clang、libxml2.so.16 等），
   已转为符号链接修复（备份在 /tmp/ndk-shim-bak）；新环境需先检查 `native/llvm/bin` 与 `lib`。

## 签名（真机安装必需）

`build-profile.json5` 的 `app.signingConfigs` 当前为空（未签名，产物为 unsigned.hap）。
用 DevEco Studio `File > Project Structure > Signing Configs` 自动签名，或 CLI 配置证书后
`hvigorw assembleHap -p buildMode=release`。

## 后续路线

1. 签名 + 真机（HarmonyOS 6.x / API 24 圆形手表）验收：登录→推荐→播放。
2. shared/ohos KBA 工具链（Kotlin 2.0.21-KBA + Gradle 8.0）产出 `libshared.so`，
   Kuikly 共享页面接入（切回 `kuikly/pages/Index`）。
3. 弹幕、下载、后台续播、表冠/圆屏适配（Phase 4 全量对等）。
4. ArkTS 核心与 shared/core 的交叉校验测试（同一脱敏 fixture 双端比对）。
