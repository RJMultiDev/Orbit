# shared/core —— Orbit 共享核心

平台无关的 Kotlin 共享业务层，承载 Orbit 在 **Android（Wear OS）与 HarmonyOS 手表**上
复用的领域模型、错误模型、repository/port 契约、use case、状态机与测试 fixture。
不包含任何 Android / Java UI / 平台序列化依赖。

## 模块目的

- 让 Android（`:app`）与未来的 HarmonyOS 宿主对**同一份业务逻辑**产出**用户行为对等**的结果。
- 优先抽取：URL/query/form 规范化、Cookie 解析、WBI/AppSign 纯算法、DASH 选流、
  字幕定位、弹幕协议/时间轴/过滤、播放进度与心跳策略、下载任务状态机、typed settings。
- Android adapter 继续复用现有 Retrofit/OkHttp/Gson、SharedPreferences、Media3/IJK、DFM；
  Harmony adapter 由其原生网络、安全存储、媒体、后台任务与文件系统实现。

## Target

- 当前仅配置 `jvm()`（`jvmTarget = 17`，与 `:app` 的 Java 17 兼容目标一致）。
- 保持纯 Kotlin、无 Android/AGP 依赖，便于 Android 模块与任意 JVM 宿主直接消费。
- 后续如需 iOS，可追加 `iosArm64()` / `iosSimulatorArm64()`，并用 expect/actual 补齐平台差异。
- 注意：官方 Kotlin/Native **不含 HarmonyOS/OHOS target**；HarmonyOS 侧走独立
  `shared/ohos/` 构建（见 `docs/shared-core-layout.md`），本模块只共享源码/契约，不直接产出 HAP。

## 依赖

- `kotlinx-coroutines-core`（版本经 `gradle/libs.versions.toml` 的 `coroutines` 统一管理）
- `kotlinx-datetime`（领域层时间/时长/时间轴常用）
- `commonTest`：`kotlin("test")`

## 如何跑测试

```bash
./gradlew :shared:core:jvmTest
```

## 与 Android / Harmony 的关系

```
Android(:app) ──implementation──▶ shared/core (领域/契约/状态机)
                                    ▲
                                    │ 平台无关
Harmony(shared/ohos 独立构建) ─────┘
```

- `:app` 通过依赖本模块复用业务逻辑，Android 平台机制（Media3/IJK、DFM、SharedPreferences、
  Shizuku 等）留在 `:app`。
- HarmonyOS 宿主在独立工具链中消费同一份 Kotlin 源码，用原生能力实现 `repository`/port。
- 平台特有机制只要求用户结果等效，不要求 API 对齐。
