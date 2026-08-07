# shared/core 包规划与协作约定

> 状态：Phase 2（A09 地基）已建骨架，各包由子代理在各自包名下填充实现。
> 负责人为占位名，由 lead 统一核对后更新为实际代理 ID。

## 包规划

| 包 | 职责 | 负责人（占位） | 落地阶段 |
| --- | --- | --- | --- |
| `net` | URL/query/form 规范化、Cookie 解析、重定向、请求头语义 | net-agent | Phase 2 |
| `sign` | WBI/AppSign 纯算法（注入 clock/encoder/digest/key provider） | sign-agent | Phase 2 |
| `model` | 领域模型（无 Parcelable / `@SerializedName` / `JSONObject`） | model-agent | Phase 2 |
| `player` | 播放器 port：`PlaybackController`、`MediaSource`/`Track` | player-agent | Phase 3 |
| `danmaku` | 弹幕协议/时间轴/过滤/lane、`DanmakuRenderer` port | danmaku-agent | Phase 3 |
| `download` | 下载任务状态机、URL 刷新、Range 规则（无 I/O） | download-agent | Phase 4 |
| `settings` | typed settings schema 与迁移（无 SharedPreferences） | settings-agent | Phase 4 |
| `repository` | repository/port 契约与 DTO/domain 映射边界 | repository-agent | Phase 2 |
| `usecase` | use case / 交互编排、分页与业务 reducer | usecase-agent | Phase 2–3 |
| `playback` | 播放进度、心跳策略、播放状态机 | playback-agent | Phase 3 |

## 依赖原则

- **无 Android**：commonMain 禁止 import `android.*`；Parcelable/Context/View/Surface 一律留在平台层。
- **无 Java UI / 序列化注解**：领域模型不得带 Gson `@SerializedName`、kotlinx `@Serializable`。
  若确需序列化协议（如桥接 DTO），需先在 lead 处协商并在此文档登记。
- **依赖单向**：`usecase` → `repository` / `playback` → `model`；`net`/`sign` 只提供纯函数。
- **状态机与错误模型**：错误模型放 `model`（或各领域包内），跨域复用错误集中在顶层 `error` 包（如后续引入）。
- **跨端一致性**：Android 与 Harmony 对同一 fixture 必须产出一致签名、选流结果、错误码和状态迁移。

## 测试要求

- 每个领域包至少在 `commonTest` 提供单测：URL/WBI/AppSign/Cookie、DTO mapping、DASH 选流、
  字幕、弹幕包与 lane、分页/reducer、下载状态机、typed settings migration。
- **golden fixtures 必须脱敏**：不得包含真实账号、Cookie、签名密钥、私有 URL 参数或真实 UID。
  共享 fixture 需说明来源与脱敏规则；禁止把线上抓包原文提交进仓库。
- 运行：`./gradlew :shared:core:jvmTest`。
- 子代理职责边界：只写自己包名下的文件；新增公共 API 需保持 `jvm()` 编译通过
  （`./gradlew :shared:core:compileKotlinJvm`）。

## 构建约束

- 根构建接线（`settings.gradle.kts`、根 `build.gradle.kts`、`gradle/libs.versions.toml`）
  仅由 A09（shared/core 地基代理）修改；子代理不得改动根构建文件。
- 依赖版本一律经 `gradle/libs.versions.toml` 统一管理，不直接在 build 文件写版本号。
- 禁止运行 `assemble` 或改动 `:app` 等既有模块；验证仅限 `:shared:core:jvmTest` 相关 task。
