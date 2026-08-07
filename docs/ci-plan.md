# CI 计划（Phase 0 测试脚手架）

状态：本文件描述当前 CI 覆盖与后续分离设计（阶段门）。Phase 0（A03）先落地 Android 侧脚手架与 CI；源码抽取（shared/core、Kuikly/OHOS）按计划在脚手架就绪后推进。

## 当前 CI 覆盖（.github/workflows/android-ci.yml）

- **触发**：push 到 `main` 与所有 pull_request；`concurrency` 取消同一 ref 的过期运行。
- **单 Job（Android）**：`ubuntu-latest`，JDK 21（Temurin）+ Android SDK（`android-actions/setup-android`，sdk.dir 指向 `ANDROID_HOME`）+ Gradle 依赖缓存（`gradle/actions/setup-gradle@v4`）。
- **任务**：`:app:assembleLiteDebug` + `:app:lintLiteDebug` + `:app:testLiteDebugUnitTest`（纯 JVM 单测，不依赖设备）。
- **子模块**：`DFMNext` 以 git submodule 引入，`.gitmodules` 为 SSH URL；CI 无部署密钥，故在 checkout 后将 URL 改写为 HTTPS 再 `git submodule update`。
- **失败产物**：测试/lint 报告以 artifact 上传，便于排查。

### 现有 JVM 单测（app/src/test）

| 文件 | 覆盖 | 手段 |
|---|---|---|
| `data/api/WbiSignerTest.kt` | `encodeUriComponent` 编码规则；`signParams`/`signUrl` 的 `w_rid`/`wts` 结构与确定性 | 反射访问私有方法；`ConfInfoApi.wbiKey` 反射打桩，零网络 |
| `data/remote/HttpClientTest.kt` | 重定向策略（http→https 同域跟随、b23.tv 非 https 不跟随、非重定向直通）、默认 UA/Referer、Cookie 敏感字段过滤 | 反射实例化私有拦截器 + 动态 Proxy 伪造 `Interceptor.Chain`/`SharedPreferences` |
| `util/FormatUtilTest.kt` | `formatCount` 阈值/万单位、`fixCoverUrl` 协议归一与 avif→webp | 纯函数直测 |

## 计划中的 CI 分离设计

按计划（Phase 0→5）将 CI 拆为**两个独立 Job 族**，工具链与缓存完全隔离，互不干扰：

### Job 族 A：Android / 标准 KMP
- 模块：`:app`（full/lite 双 flavor）+ `shared/core`（标准 KMP）。
- 任务：`assembleLiteDebug` / `assembleFullDebug`、lint、JVM 单测、契约测试（Retrofit/OkHttp adapter、SharedPreferences 迁移、Media3/IJK、DFM 两 flavor）、关键 Wear UI 冒烟。
- 工具链：JDK 21 + AGP/Kotlin 官方工具链 + `gradle/actions` 缓存。
- 运行时机：每个 push/PR；模拟器/UI 冒烟放到夜间。

### Job 族 B：Kuikly / OHOS / Hvigor
- 模块：`shared/ohos/` + `harmony/`（DevEco Stage 工程，API 22 HAP）。
- 任务：ArkTS adapter、N-API ABI/version/error/cancel、Kuikly 页面生命周期、原生媒体/存储/后台 port 单测；Hvigor 构建 arm64 `.so` 与 HAP。
- 工具链：锁定 DevEco/Harmony SDK/Hvigor/NDK/Kuikly/Kotlin fork/JDK/宿主 OS；独立缓存目录，不与 A 族共享 wrapper/plugin resolution。
- 运行时机：门通过后每个 push/PR；**API 22 真表**冒烟（播放、弹幕、下载、后台）由自托管 runner 夜间执行。

### 阶段门（Stage Gates）
- **Gate 0（本任务）**：Android 基线可构建 + 单测/lint/CI 就绪；功能矩阵与脱敏 fixture 建立。未通过不得移动源码。
- **Gate 1：Kuikly/API 22 可行性门（Go/No-Go）**：真机验证 HAP 安装、圆屏/表冠、跨线程桥接、WBI/登录、分离 DASH/直播、Surface 叠层弹幕、后台续播、下载续传。Go → 投资 Kuikly 共享页面；No-Go → 共享业务层保留、UI 转 ArkUI。
- **Gate 2：shared/core 抽取**：WBI/AppSign、Cookie、重定向、请求头、DASH 选流、字幕/弹幕、下载状态机逐域下沉，并以 Android 当前输出建立 golden fixtures。
- **Gate 3：端到端垂直切片**：Android 与 OHOS native 对同一 fixture 比较签名/query/header/Cookie/模型/选流/状态机输出（跨端一致性 Job）。
- **Gate 4：完整对等矩阵关闭**：矩阵 100% 标记实现/平台等效/明确不适用；API 22 真机全量回归 + 性能/功耗预算 profile。

### 跨端一致性
同一组脱敏 golden fixtures 同时喂给 Job 族 A 与 B，输出（签名、query、header、Cookie、模型、选流、状态机）做字节级/逐字段比较，确保共享业务层不因平台 adapter 漂移。

## 待办（脚手架之后）
- 每个 PR 运行构建、lint、单测、契约测试与模拟器 smoke；夜间由自托管 runner 驱动 API 22 真表。
- 逐步把 Phase 0 列出的测试域（WBI/AppSign、Cookie、重定向、请求 headers、DASH 选择、字幕/弹幕解析、下载恢复）从样例扩展到全量。
