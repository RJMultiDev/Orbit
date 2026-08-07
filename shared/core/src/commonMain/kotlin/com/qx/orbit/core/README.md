# com.qx.orbit.core 包归属

共享核心领域层按领域分包。本目录为骨架占位：**每个包由对应子代理负责实现**，
各自在包名下新增文件即可，无需改动根构建或本 README（除非新增包，需同步更新）。

> 负责人列为占位代理名，由 lead 统一核对后更新为实际代理 ID。

| 包 | 职责 | 负责人（占位） |
| --- | --- | --- |
| `net` | URL/query/form 规范化、Cookie 解析、重定向、请求头语义（无 Android HTTP 栈） | net-agent |
| `sign` | WBI/AppSign 等纯算法签名，注入 clock/encoder/key provider | sign-agent |
| `model` | 领域模型（无 Parcelable、无 `@SerializedName`、无 `JSONObject`） | model-agent |
| `player` | 播放器 port 契约：`PlaybackController`、`MediaSource`/`Track`（无 Surface/View） | player-agent |
| `danmaku` | 弹幕协议解析、时间轴、过滤、lane scheduler、`DanmakuRenderer` port | danmaku-agent |
| `download` | 下载任务状态机、URL 刷新、Range 规则（无实际 I/O） | download-agent |
| `settings` | typed settings schema 与迁移（无 SharedPreferences） | settings-agent |
| `repository` | repository/port 契约与数据映射边界 | repository-agent |
| `usecase` | use case / 交互编排、分页与业务 reducer | usecase-agent |
| `playback` | 播放进度、心跳策略、播放状态机 | playback-agent |

## 铁律

- **无 Android**：不得 import `android.*`；Parcelable/Context/View 一律留在平台层。
- **无 Java UI / 序列化注解**：领域模型不得带 Gson `@SerializedName`、kotlinx `@Serializable`（若确需序列化，协商后再加并文档化）。
- **依赖单向**：`usecase` → `repository`/`playback` → `model`；`net`/`sign` 只提供纯函数，不反向依赖 use case。
- **fixture 脱敏**：任何 golden/test fixture 不得含真实账号、Cookie、URL 中的私有参数。
