# Orbit 功能矩阵（Android 现状 → HarmonyOS NEXT 6.1 / API 22 对等基线）

> Phase 0 / A02 产物。依据：`MainActivity.kt` 导航图、`presentation/**/*Screen.kt`、`presentation/settings/SettingsScreen.kt`（含全部 `Setting*` 子屏）、`app/src/main/AndroidManifest.xml`、`README.md`。
>
> "用户行为对等"以本矩阵的**用户结果（Android 现状）**列为准，而非 Android API/引擎数量。`Harmony 等效方案` 中"待定"项为需在 Phase 1 真机可行性门验证的点。

## 等效方案取值说明

- **原生等效：xxx** — HarmonyOS 有可直接对等的原生能力（含通过 `shared/core` 共享的纯算法/状态机）。
- **简化：xxx** — Android 提供多实现，Harmony 只需一个合格实现即达对等。
- **待定** — API 22 手表真机可用性未验证（表冠、Surface 叠层、后台任务、常亮、方向等）。
- **不适用** — Android 特有系统机制，Harmony 用沙盒/授权机制提供等效结果。

---

## A. 内容与账户

| 功能域 | 入口/route | 前置登录 | 用户结果（Android 现状） | 异常/空态 | 离线/后台 | 系统权限 | Harmony 等效方案 |
|---|---|---|---|---|---|---|---|
| 推荐信息流（分页卡片） | `home`（TabMode.RECOMMEND） | 否 | 推荐视频卡片列表，滚动到底自动加载更多，顶部浮动标题可开菜单 | 加载失败：2233 图+"加载失败，点击重试"；加载中转圈 | 无离线缓存 | INTERNET | 原生等效：共享网络层 + ArkUI/Kuikly 列表 |
| 热门/排行榜 | `home`（TabMode.POPULAR） | 否 | 热门视频列表，行为同推荐流 | 同上 | 无 | INTERNET | 原生等效 |
| 动态信息流 | `home`（TabMode.DYNAMIC） | 是 | 关注 UP 头像行（更新红点）、直播中行、动态卡片（图文/视频/转发），顶部"发布动态"按钮 | 同上；分页到底自动加载 | 无 | INTERNET | 原生等效 |
| 主页菜单/导航 | `home` 顶部菜单（AnimatedVisibility 覆盖层） | 部分（未登录显示"登录"按钮，登录后显示用户空间入口） | 菜单项：登录/用户空间、搜索、Tab 切换（推荐/热门/动态）、追番、收藏、历史、稍后再看、缓存、设置 | — | — | — | 原生等效：导航状态进 shared core |
| 推荐净化（一键拉黑） | `home` 推荐卡 `SwipeToReveal` 左滑 | 否 | 左滑"不感兴趣"= 隐藏+上报不感兴趣+拉黑 UP（可配置二次确认弹窗） | — | — | INTERNET | 原生等效：净化/屏蔽 reducer 进 shared core |
| 搜索输入 | `search` | 否 | 关键词搜索；本地搜索历史（最多 20 条，左滑删除）；`av\d+`/`bv…` 直接跳详情 | 无历史则隐藏"搜索历史"区 | 历史本地持久化（SharedPreferences） | — | 原生等效：历史存沙盒偏好；AV/BV 转换纯算法进 shared core |
| 搜索结果（多 Tab） | `search_result/{query}` | 否 | 视频/用户/专栏/直播 4 类结果，顶部 Tab 菜单切换，分页加载 | 空结果"没有找到相关结果"；错误显示错误文案 | 无 | INTERNET | 原生等效 |
| 视频详情 | `detail/{bvid}/{aid}` | 否（720p+ 播放、互动需登录） | 封面/标题/Staff 横滑/播放弹幕数/简介（含 HTML 链接）/标签/BV 复制；播放按钮（长按=播放器选择）；三连（长按点赞 3 秒触发）；投币游戏化弹窗；收藏夹选择；稍后再看；缓存；分 P 选择；评论 Tab；相关推荐 Tab | 加载失败整页可点重试；评论区失败可点重试；收藏夹为空提示"暂无收藏夹" | 历史进度展示"看到 xx:xx" | INTERNET | 原生等效：选流/进度/互动状态进 shared core |
| Bangumi 详情 | `bangumi_detail/{media_id}` | 否（追番/缓存需登录） | 封面/简介/追番按钮/继续观看（续播进度）/选集列表（已看/看到 xx:xx 标记）/缓存（分集选清晰度或仅音频+字幕）/评论 | 加载失败可点击重试；封面取色失败回退默认主题 | 返回时经 savedStateHandle 回写播放进度 | INTERNET | 原生等效 |
| 直播房间 | `live_room/{roomId}` | 否（发弹幕、部分画质需登录） | 房间封面/标题/主播信息/分区/简介（展开）；"观看直播"跳播放器；弹幕页（WebSocket 实时弹幕+发送）；推荐直播列表分页 | 房间加载失败+重试按钮；无弹幕显示"等待弹幕..." | 直播无离线 | INTERNET | 原生等效：WebSocket + 直播协议进 shared core |
| 文章详情 | `article_detail/{id}` | 否（互动需登录） | HTML 解析为 图文/链接/标题/引用 段落，点击外链开浏览器，图片查看器，评论 Tab | 加载失败+重试按钮；图片加载失败灰底占位 | 无 | INTERNET | 原生等效：HTML 解析纯算法进 shared core |
| Opus 详情 | `opus_detail/{id}` | 否（互动需登录） | 富文本段落（表情 inline/@用户/URL 跳转）、图片查看器、点赞/收藏（收藏为 TODO 占位）、评论 Tab | 同文章详情 | 无 | INTERNET | 原生等效 |
| 动态详情 | `dynamic_detail/{id}` | 否（互动需登录） | 动态正文（@用户可点）、图片、转发链（可跳原动态/视频）、视频/直播卡片、点赞/分享/评论 | 加载失败显示错误文本 | 无 | INTERNET | 原生等效 |
| 用户空间 | `user_space/{mid}` | 否（关注/互动需登录；自己空间需登录） | 头像/等级/VIP/粉丝关注数/签名展开；关注按钮（本人隐藏）；三 Tab：动态/发布视频/发布图文；从头像提取主题色生成动态配色 | 首屏转圈（等待取色）；无数据空白 | 无 | INTERNET | 原生等效：主题色提取算法进 shared core |
| 登录（扫码 + Cookie 导入） | `login` | 否（登录页本身） | HD 扫码登录（默认，可切网页端 QR）；Cookie JSON 导入；成功后刷新 navInfo、重置推荐流并自动返回 | QR 过期/失败可点重试；Cookie 无效则清除并提示 | 登录态本地持久化 | INTERNET | 原生等效：二维码/轮询逻辑进 shared core；Cookie 用 Harmony 安全存储（不复制明文方案） |
| 登录状态管理 | `settings_login_status` | 是 | 导出 Cookie 到剪贴板；手动刷新 Cookie；退出登录（清 Cookie + 重启 Activity） | 未登录提示"当前未登录"；刷新中按钮转圈 | — | — | 原生等效：安全存储 + 剪贴板 API |
| 消息/私信 | —（Android 现状：无消息页） | — | 无对应页面 | — | — | — | 不适用（对等基线不含） |

## B. 互动

| 功能域 | 入口/route | 前置登录 | 用户结果（Android 现状） | 异常/空态 | 离线/后台 | 系统权限 | Harmony 等效方案 |
|---|---|---|---|---|---|---|---|
| 评论列表/评论树 | `detail`/`bangumi_detail`/`article_detail`/`opus_detail`/`dynamic_detail` 评论 Tab；`reply_detail` | 否（点赞/回复需登录） | 评论分页；根评论+子评论树；评论详情页（根+子楼层）；删除本人评论本地移除 | 评论加载失败可点击重试 | 无 | INTERNET | 原生等效 |
| 发布评论/回复/动态 | `WriteReplyScreen`（各详情页复用） | 是 | 文本输入（多行）+ 表情包面板（分页）+ 图片（评论/动态）；直播特殊表情直接发送；手表键盘 IME | 发送失败 Toast 提示 | 无 | INTERNET | 原生等效：表情映射/富文本解析进 shared core |
| 点赞 | 详情页/动态/评论卡片 | 是 | 点赞/取消，计数实时更新 | — | 无 | INTERNET | 原生等效 |
| 投币 | `detail` 投币弹窗 | 是 | 游戏化弹窗（剩余硬币、2233 拖拽动画、横向选 1/2 枚）；版权限制（最多 1/2 枚）；达上限 Toast | 投币上限提示 | 无 | INTERNET | 原生等效：互动状态机进 shared core |
| 一键三连 | `detail` 长按点赞 | 是 | 长按 3 秒（持续触感反馈 + 进度动画）触发点赞+投币+收藏，成功后全屏"三连成功"覆盖层 | 失败 Toast | 无 | INTERNET | 原生等效 |
| 收藏/收藏夹 | `detail` 收藏弹窗；`favorite_folders`；`favorite_detail/{fid}/{mid}` | 是 | 收藏到所选收藏夹；收藏夹列表；夹内搜索；取消收藏（需确认弹窗） | "暂无收藏夹"/"暂无内容" 空态；错误文案 | 无 | INTERNET | 原生等效 |
| 关注/追番 | `user_space` 关注按钮；`follow_list`；`bangumi_detail` 追番按钮 | 是 | 关注/取消关注 UP；追番列表分页；取消追番（需确认弹窗） | 空态"暂无追番" | 无 | INTERNET | 原生等效 |
| 历史记录 | `history` | 是 | 历史列表分页；单条删除（左滑） | 空态"暂无历史记录" | 播放进度自动上报 | INTERNET | 原生等效 |
| 稍后再看 | `watch_later`；`detail` "添加到稍后再看" | 是 | 列表分页；删除（左滑）；跨类型跳转（视频/番剧/文章） | 空态"暂无稍后再看" | 无 | INTERNET | 原生等效 |
| 评论屏蔽（长按拉黑） | 评论卡片 `ReplyCard` 长按 | 是 | 长按评论=隐藏评论+拉黑发送者 | — | 无 | INTERNET | 原生等效 |

## C. 播放与弹幕

| 功能域 | 入口/route | 前置登录 | 用户结果（Android 现状） | 异常/空态 | 离线/后台 | 系统权限 | Harmony 等效方案 |
|---|---|---|---|---|---|---|---|
| 视频播放（MP4/DASH/本地/直播） | `player/{playerDataJson}` | 高画质（720p+）需登录 | 双播放引擎（IJK/Media3）、三播放器 UI（Apsis/纯音频/凉腕）；播放/暂停、进度、倍速（0.5–2.0x）、循环、断线重连、403 刷新 URL、A/V 分离 DASH | 加载中转圈+缓冲速度；无法取流提示"无法获取视频地址"/"网络请求失败" | 熄屏继续播放（后台模式）；`ON_STOP` 暂停 | INTERNET, FOREGROUND_SERVICE, FOREGROUND_SERVICE_MEDIA_PLAYBACK, WAKE_LOCK | 简化：Harmony AVPlayer + XComponent 单播放器即可；选流/心跳/重试逻辑进 shared core |
| 直播播放 | `player`（TYPE_LIVE） | 否 | 直播流播放、直播时长计时、低延迟（packet-buffering=0/infbuf） | 同上 | 后台不续播直播 | INTERNET | 原生等效：AVPlayer 直播能力 |
| 视频弹幕 | `player` 弹幕层 | 否 | 分段 protobuf 弹幕；双弹幕引擎（DFM/DFM-Next）；过滤：合并重复/防重叠/密度/字号/高级弹幕；播放同步 pause/seek/倍速 | 无弹幕正常播放；弹幕引擎就绪前保持加载态 | 本地缓存视频附带旁路 `.danmaku.xml` | — | 简化：共享弹幕协议/时间轴/过滤器进 shared core + Harmony Canvas/XComponent 单 renderer |
| 直播弹幕 | `player`（TYPE_LIVE）/ `live_room` 弹幕页 | 是（部分接口） | WebSocket 实时弹幕 + 表情 inline + 显示发送者开关；发送弹幕/直播特殊表情 | 断线自动重连 | 无 | INTERNET | 原生等效：WebSocket + 弹幕协议进 shared core |
| 字幕 | `player` 字幕对话框 | 否 | CC/多语言字幕选择；AI 字幕；本地字幕文件发现；"CC字幕默认开启"开关 | 加载字幕失败 Toast | 本地缓存视频旁路字幕文件 | — | 原生等效：字幕定位纯算法进 shared core |
| 手势交互 | `player` 手势层 | 否 | 双击暂停/继续；单击显隐控制层；横向拖动全屏 seek；纵向拖动系统音量；双指缩放+平移；单指缩放（可选）；长按倍速（2x） | — | — | — | 原生等效：ArkUI 手势系统；单指缩放判定逻辑进 shared core |
| 表冠控制 | `player` `onRotaryScrollEvent` | 否 | 表冠滚动调音量（默认）或 seek 进度（可配置） | — | — | — | **待定**：Harmony 表冠事件（RotaryEvent）API 22 可用性与坐标语义需真机验证 |
| 旋转/横屏 | `player` 旋转按钮 | 否 | 软件旋屏（0/90/180/270 可配置步进、保存角度）或系统方向；"默认横屏"开关 | — | — | — | **待定**：Harmony 屏幕方向/软件旋转控制 |
| 媒体会话/控制中心 | `player` MediaSession + 通知 + 前台服务 | 否 | 媒体通知/控制中心：播放/暂停/seek/上下集；元数据+封面；点击回播放页 | — | 后台播放由前台服务承载 | FOREGROUND_SERVICE_MEDIA_PLAYBACK | 原生等效：Harmony `avSession` 播控中心/系统媒体通知；**待定**：API 22 后台播放/长时任务限制 |
| 播放器界面自定义 | `settings_player_customization` | 否 | 四角按钮可映射 6 种操作（弹幕/倍速/音量/字幕/旋转/三段缩放），不可重复 | — | 布局持久化 | — | 原生等效 |
| 音量防护/静音开播 | `settings_apsis_player` | 否 | 表冠音量防干扰；开播静音（音量上限 20%）且记住用户手动调整 | — | — | — | 原生等效：音量状态逻辑进 shared core |

## D. 下载离线

| 功能域 | 入口/route | 前置登录 | 用户结果（Android 现状） | 异常/空态 | 离线/后台 | 系统权限 | Harmony 等效方案 |
|---|---|---|---|---|---|---|---|
| 缓存发起 | `detail`/`bangumi_detail` 缓存对话框 | 是 | 选择清晰度或"仅音频+字幕"入队；番剧需先选分集；下载前拉取 DASH 清晰度/音质列表 | 拉取清晰度期间转圈 | — | INTERNET | 原生等效：下载任务状态机/URL 刷新/Range 规则进 shared core |
| 下载管理列表 | `download_manager` | 否 | 进行中（等待/下载%/暂停/失败）/已完成分组；暂停/继续/重试；删除确认；左滑删除；长按已完成选播放器 | 空态"暂无缓存视频" | 任务内存+本地持久化；进程重启后运行中任务置为失败可手动重试 | INTERNET | 原生等效：Harmony 后台任务/下载代理；**待定**：沙盒续传与系统后台任务限制 |
| 下载进度页 | `video_download_progress/{downloadId}` | 否 | 环形进度 + 速度/剩余时间轮换 + 暂停/继续/重试 + 完成后直接播放；屏幕常亮 | 任务被删除则自动返回上一页 | 下载中保持屏幕常亮 | WAKE_LOCK | **待定**：Harmony 常亮/长时任务接口 |
| 缓存位置管理 | `settings_cache_location` | 否 | 内部存储（应用目录，卸载即删）vs 外部存储 Movies 公共目录（卸载保留，R+ 需 Shizuku 授权全部文件访问） | 未授权弹 Shizuku 授权/未安装/未激活引导对话框；拒绝后记录不再弹 | 外部目录可被用户/系统文件管理器访问 | MANAGE_EXTERNAL_STORAGE, READ/WRITE_EXTERNAL_STORAGE(≤29), moe.shizuku.manager.permission.API_V23 | **不适用→原生等效**：Harmony 应用沙盒 + 系统媒体库/用户授权目录（无全盘授权通道），卸载保留需用户导出 |
| 本地播放 | `download_manager` 点击已缓存；进度页"播放" | 否 | 播放本地 mp4/m4s；加载旁路 `.danmaku.xml`；发现本地字幕 | 本地文件缺失则失败 | 完全离线 | 存储读取 | 原生等效：沙盒文件 + AVPlayer 本地播放 |
| 纯音频播放 | `AudioPlayerScreen`（audioUrl=="audio"） | 否 | 原生风格音乐界面；封面取色；字幕/倍速/循环/音量；播放/暂停/±10s | — | 后台音频 | — | 原生等效：AVPlayer 纯音频 + avSession 后台播控 |

## E. 设置体验

| 功能域 | 入口/route | 前置登录 | 用户结果（Android 现状） | 异常/空态 | 离线/后台 | 系统权限 | Harmony 等效方案 |
|---|---|---|---|---|---|---|---|
| 设置主菜单 | `settings_main` | 部分（"登录状态管理"仅登录显示） | 播放器选择/播放器设置/界面/偏好/缓存位置/关于 | — | — | — | 原生等效 |
| 偏好设置 | `settings_preference` | 否 | HD 登录接口切换；拉黑二次确认；免登录观看 1080P（try-look）；缓存位置入口 | — | 本地持久化 | — | 原生等效：typed settings 进 shared core |
| 界面设置 | `settings_ui` | 否 | 沉浸式时间显示；屏幕形状模式（自动/强制圆屏/方屏）；全局 DPI 缩放（0.5–2.0） | — | 本地持久化 | — | **待定**：Harmony 圆屏形状/字体缩放适配；DPI 缩放映射为 Harmony 配置 |
| 默认播放器/清晰度 | `settings_player_choose` / `settings_video_quality` | 清晰度 720p+ 需登录 | 三播放器默认选择；清晰度 360/480/720/1080（720+ 锁定并提示登录） | 未登录选 720p+ Toast 提示"请先登录" | — | — | 简化：Harmony 单一播放器；清晰度档位/锁定逻辑进 shared core |
| 播放引擎设置 | `settings_player_engine` | 否 | IJK / Media3 切换（DASH 请求仅在 Media3 可用） | 构建无 IJK 时选项禁用并标注 | — | — | 简化：Harmony 仅 AVPlayer，无引擎切换项 |
| 视频渲染方式 | `settings_video_render` | 否 | SurfaceView（性能好）/TextureView（兼容好）切换；TextureView 兼容性探测 | 探测失败弹窗建议切换并可重试 | — | — | 简化：Harmony 用 XComponent 统一渲染，无切换项 |
| 弹幕引擎设置 | `settings_danmaku_engine` | 否 | DFM（烈焰弹幕使）/ DFM-Next 切换 | — | — | — | 简化：Harmony 单弹幕 renderer，无引擎切换项 |
| Apsis 播放器设置 | `settings_apsis_player` | 否 | 17+ 开关（长按倍速/循环/后台播放/后台音频/横屏/软旋屏/缩放/单指缩放/DASH/弹幕默认/字幕默认/高级弹幕/允许重叠/合并重复/显示发送者/显示弹幕按钮）+ 滚轮音量/音量防护/静音开播 + 旋转方向步进 + 弹幕密度 + 弹幕字号滑杆；单指缩放开启跳教程 | — | 本地持久化 | — | 原生等效：共享 typed settings；引擎/渲染等平台特有项在 Harmony 隐藏 |
| 单指缩放教程 | `one_finger_zoom_tutorial` | 否 | 2 页教程（理论+练习：放大/平移/缩小），成功确认弹窗返回 | — | — | — | 原生等效 |
| 关于/捐赠 | `about` | 否 | 版本/设备型号/简介链接/开发人员/捐赠弹窗 | 打开链接失败复制到剪贴板 Toast | — | — | 原生等效 |

## F. 手表交互

| 功能域 | 入口/route | 前置登录 | 用户结果（Android 现状） | 异常/空态 | 离线/后台 | 系统权限 | Harmony 等效方案 |
|---|---|---|---|---|---|---|---|
| 表冠滚动（列表） | 全部列表页 `TransformingLazyColumn` + `rotaryScrollable` | 否 | 表冠滚动列表、焦点管理、变换动画 | — | — | — | **待定**：Harmony 表冠事件 API 22 可用性（Kuikly/ArkUI 需真机验证） |
| 圆屏适配 | 全部页面 `LocalScreenRound` | 否 | 圆屏变换（SurfaceTransformation）、圆屏内边距、播放器圆屏安全区；屏幕形状模式可强制圆/方 | — | — | — | **待定**：ArkUI 圆屏安全区与字体缩放 |
| 屏幕常亮 | `player` / `VideoDownloadForegroundScreen`（keepScreenOn） | 否 | 播放与下载进度页保持屏幕常亮 | — | — | WAKE_LOCK | **待定**：Harmony 常亮接口/长时任务声明 |
| 触感反馈 | `detail` 三连、长按等 | 否 | HapticFeedback（TextHandleMove 持续振动/Confirm） | — | — | — | 原生等效：Harmony 振动接口 |
| 息屏/恢复 | `player` ON_STOP/ON_START | 否 | 后台音频模式切流+释放 Surface，恢复时重建 Surface；否则暂停；返回时恢复进度 | — | — | — | **待定**：Harmony 前后台生命周期与 Surface 恢复 |
| 字体缩放/DPI | `settings_ui` 全局 DPI 缩放 | 否 | 0.5–2.0 全局缩放滑杆，作用于全部页面 | — | — | — | 原生等效：Harmony 字体缩放/displayScale 配置 |
| 键盘输入 | `search` / `WriteReplyScreen` | 否 | 手表键盘/手写输入、IME Action、焦点管理、表冠收起键盘 | — | — | — | 原生等效：Harmony 输入法能力 |
| 返回/侧滑手势 | `SwipeDismissableNavHost` | 否 | 侧滑返回；小米手表 5 禁用侧滑改 BackHandler 双击返回确认；播放器双击返回退出 | — | — | — | 原生等效：Harmony 返回/侧滑手势；机型特判逻辑移入 shared core |

## G. 系统能力

| 功能域 | 入口/route | 前置登录 | 用户结果（Android 现状） | 异常/空态 | 离线/后台 | 系统权限 | Harmony 等效方案 |
|---|---|---|---|---|---|---|---|
| Shizuku 授权 | `settings_cache_location`、启动时 `LaunchedEffect` 检测 | 否 | Shizuku API V23 授权 + `appops` 授予 MANAGE_EXTERNAL_STORAGE；未安装/未激活分别弹引导 | 拒绝授权记录 `shizuku_declined` 不再打扰 | — | moe.shizuku.manager.permission.API_V23, MANAGE_EXTERNAL_STORAGE | **不适用**：Harmony 无 Shizuku 通道，改用沙盒 + 用户授权目录提供等效结果 |
| 前台服务/后台任务 | `PlayerForegroundService` | 否 | mediaPlayback 前台服务承载后台播放与媒体通知 | — | 后台续播、通知控制 | FOREGROUND_SERVICE, FOREGROUND_SERVICE_MEDIA_PLAYBACK | 原生等效：Harmony 后台任务/长时任务（播放类）；**待定**：API 22 后台与通知限制 |
| 全局 Cookie/登录态 | `CookieManager`（SharedPreferences 明文） | — | 全局 Cookie 管理、navInfo 缓存、登录态驱动各页 UI | — | 登录态持久化 | — | 原生等效：Cookie/凭据存入 Harmony 安全存储，不复制明文方案；解析逻辑进 shared core |
| 图片加载 | 全 App（Coil） | 否 | 内存/磁盘缓存、GIF/WebP、crossfade、尺寸裁剪参数 | 加载失败占位图 | 磁盘缓存 | — | 原生等效：Harmony ImageKit/PixelMap |
| 媒体通知 | `player` 通知 | 否 | 媒体样式通知、ongoing、点击回播放页 | — | — | （POST_NOTIFICATIONS 随系统） | 原生等效：avSession 系统媒体通知 |
| 系统跳转/分享 | 文章/Opus 外链、动态分享（TODO 占位） | 否 | ACTION_VIEW 打开浏览器；复制链接兜底 | 打开失败复制到剪贴板 | — | — | 原生等效：Harmony 打开链接/分享能力 |

---

## 平台特有机制差异（Android 有，Harmony 需原生等效替代）

以下为 Android 端独有的**机制/实现**，Harmony 不需要逐项复制，只需达成矩阵中的用户结果：

### 1. Shizuku 授权通道
- **Android**：`rikka.shizuku` API V23 + `ShizukuProvider`（exported）+ `appops set MANAGE_EXTERNAL_STORAGE allow`，用于无 root 环境授予"所有文件访问权限"，将缓存写入公共 Movies 目录。
- **Harmony**：无 Shizuku 机制。对等结果改为 **应用沙盒存储为主**，如需用户可访问的公共位置，使用系统文件授权（用户选目录/媒体库授权）。设置项"外部存储 (Movies)"改为"沙盒/系统授权目录"，卸载保留语义需要用户主动导出。
- **影响面**：`settings_cache_location`、启动时存储授权检测、`VideoDownloadManager` 路径解析、`ShizukuUtils.kt`。

### 2. Android 全盘存储（MANAGE_EXTERNAL_STORAGE）
- **Android**：Android 11+ 通过 Shizuku 拿到全盘访问；≤10 走 READ/WRITE_EXTERNAL_STORAGE 运行时权限。
- **Harmony**：沙盒模型，无全盘授权权限。等效结果 = 沙盒内离线缓存 + 系统媒体能力，**用户结果（可离线播放、重启恢复）保持，存储位置与可导出性作为明确差异写入验收矩阵**。

### 3. 双播放引擎 + 三播放器 UI
- **Android**：IJKPlayer（FFmpeg，full flavor）/ Media3 ExoPlayer（DASH 自适应）双引擎切换；Apsis Player / Apsis AudioPlayer / 凉腕播放器 三套播放器 UI 与默认选择。
- **Harmony**：按计划"只需一个合格播放器"，用 Harmony AVPlayer + XComponent 提供等效播放、seek、倍速、切集、直播与本地播放；DASH 选流逻辑进 `shared/core`，Media3/IJK 留在 Android adapter。设置项"播放引擎/默认播放器/渲染方式"在 Harmony **简化或隐藏**，但清晰度档位与锁定逻辑保持一致。

### 4. 双弹幕引擎
- **Android**：DanmakuFlameMaster / DFM-Next 双引擎（Android View 实现，DFMNext 为 Git 子模块）。
- **Harmony**：按计划只建**一个合格弹幕 renderer**（Canvas/XComponent/native），弹幕协议/时间轴/过滤/音画同步（pause/seek/speed/live event）进 `shared/core`；"弹幕引擎设置"项在 Harmony 隐藏。

### 5. SurfaceView / TextureView 双渲染（补充）
- **Android**：因兼容性问题提供双渲染 + TextureView 探测与切换 UI。
- **Harmony**：统一 XComponent/Surface，无需探测与切换。

### 6. 媒体会话与后台播放模型（补充）
- **Android**：MediaSession + MediaStyle 通知 + `PlayerForegroundService`（mediaPlayback 前台服务）+ 音频焦点，支撑锁屏/控制中心与熄屏续播。
- **Harmony**：`avSession` 播控中心/系统媒体通知 + 后台任务/长时任务声明；**API 22 手表的后台续播与系统授权限制待 Phase 1 真机验证**（计划阶段 1 第 7 项）。

### 7. Cookie 明文存储（补充）
- **Android**：Cookie 以明文存 SharedPreferences（`CookieManager.kt` 全局访问）。
- **Harmony**：按计划改用**安全存储**（ohos.security / 系统凭据），不复制明文方案；Cookie 解析/过滤/合并语义进 `shared/core` 保证跨端一致。

---

## 统计与后续

- 矩阵条目总数：**68** 条功能条目（A 17 + B 10 + C 11 + D 6 + E 10 + F 8 + G 6）。
- Harmony 等效方案标记为 **待定** 的难点（需 Phase 1 真机门验证）：
  1. 表冠事件（列表滚动、播放器音量/seek）在 API 22 的可用性与坐标语义；
  2. 圆屏安全区 / 屏幕形状模式 / 字体缩放（Kuikly 渲染在圆形手表的适配）；
  3. 屏幕常亮（播放页、下载进度页 keepScreenOn 等效）；
  4. 前后台切换与 Surface 释放/重建、后台续播（avSession + 长时任务在 API 22 的限制）；
  5. 软件旋屏 / 屏幕方向控制。
- 标记为 **简化/不适用** 的项（Shizuku、全盘存储、双播放器、双弹幕引擎、双渲染、播放引擎选择）由"平台特有机制差异"章节覆盖，验收时以用户结果为准，不复制 Android 实现数量。
