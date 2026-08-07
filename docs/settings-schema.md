# Orbit Typed Settings Schema

> 现状盘点（Phase 4 / A17）：Android 端此前将设置以裸字符串 key 散落在
> `SharedPreferencesUtil`（prefs 文件 `orbit_settings`）与 `AppConfig`（`orbit_config`）
> 的直接调用处。本文档定义了 typed settings schema —— 每个 key 的规范名、类型、
> 默认值、平台归属与当前调用点，作为 Android adapter 迁移与 Harmony 原生替代的依据。

## 总览

- **设置项总数：40**
  - **COMMON（共享业务设置）：20** —— 清晰度、弹幕显示、循环、后台播放、字幕、
    登录扫码、确认弹窗等，Android 与 Harmony 必须保持相同用户语义。
  - **ANDROID（平台特有）：20** —— 播放/弹幕引擎选择、TextureView、屏幕旋转与
    手势、自定义控制按钮、缓存位置（全盘存储）、Shizuku、圆屏模拟与 DPI。
  - **HARMONY（仅 Harmony）：0** —— 当前无 Harmony 独有设置；枚举值保留给
    Harmony adapter 扩展。
- Schema 迁移版本：**1**（`SettingsMigration.VERSION`）。
- 存储位置：规范 key 名 = **迁移后**名；旧名见「迁移规则」。

## Key 清单

图例：归属 C=COMMON, A=ANDROID, H=HARMONY。

| key | 类型 | 默认值 | 用途 | 归属 | 现状调用处 | Harmony 处理 |
|---|---|---|---|---|---|---|
| `play_qn` | Int | 16 | 默认视频清晰度 | C | `SettingsScreen.kt:1677,1705,1749`；`VideoDetailScreen.kt:356,516,596`；`BangumiDetailScreen.kt:442` | 等效 |
| `play_audio_qn` | Int | 30280 | 默认音频清晰度 | C | `VideoDetailScreen.kt:357` | 等效 |
| `play_try_look` | Bool | true | 播放链接附加 try_look 试看参数（旧名 `try_look_enable`） | C | `PlayerConfig.kt:4,9,13`；`PlayerApi.kt:152,166,243,257,277,281`；`SettingsScreen.kt:301,306` | 等效 |
| `player_request_dash` | Bool | false | 优先请求 DASH 流 | C | `PlayerScreen.kt:646`；`PlayerViewModel.kt:214` | 等效 |
| `player_background` | Bool | false | 退到后台继续播放 | C | `PlayerScreen.kt:520,967` | 等效（系统媒体/后台任务） |
| `player_background_audio_only` | Bool | false | 后台时仅保留音频 | C | `PlayerScreen.kt:968` | 等效 |
| `player_loop` | Bool | false | 循环播放 | C | `PlayerScreen.kt:813,1884,1888`；`AudioPlayerScreen.kt:457,461` | 等效 |
| `player_subtitle_default_show` | Bool | false | 有字幕时默认显示字幕 | C | `PlayerScreen.kt:748,756` | 等效 |
| `player_longclick` | Bool | true | 长按画面倍速播放 | C | `PlayerScreen.kt:1105` | 等效 |
| `player_danmaku_default_show` | Bool | true | 播放时默认显示弹幕 | C | `PlayerScreen.kt:224` | 等效 |
| `player_danmaku_advanced_enable` | Bool | true | 启用弹幕高级功能 | C | `PlayerScreen.kt:670,697,730`；`PlayerDanmuClientListener.kt:265` | 等效 |
| `player_danmaku_textsize` | Float | 1.0 | 弹幕字号缩放 | C | `PlayerScreen.kt:658`；`SettingsScreen.kt:704,727` | 等效 |
| `player_danmaku_mergeduplicate` | Bool | false | 合并重复弹幕 | C | `PlayerScreen.kt:667,693` | 等效 |
| `player_danmaku_allowoverlap` | Bool | true | 允许弹幕重叠 | C | `PlayerScreen.kt:668,694` | 等效 |
| `player_danmaku_maxline` | Int | 0 | 弹幕最大轨道数（0=自动） | C | `PlayerScreen.kt:669,695`；`SettingsScreen.kt:677,687` | 等效 |
| `player_danmaku_showsender` | Bool | true | 显示弹幕发送者昵称 | C | `PlayerScreen.kt:890` | 等效 |
| `login_use_hd_qr` | Bool | true | 登录使用高清二维码（旧名 `use_hd_qr_code`） | C | `LoginScreen.kt:78`；`SettingsScreen.kt:252,257` | 等效 |
| `confirm_dislike` | Bool | true | 不喜欢前二次确认 | C | `MainActivity.kt:946,958`；`SettingsScreen.kt:276,281` | 等效 |
| `ui_immersive_time` | Bool | true | 状态栏沉浸式时间显示 | C | `SettingsScreen.kt:780,785`；`WysTimeText.kt:36,40` | 等效 |
| `search_history` | String | "" | 搜索历史（`||` 分隔，业务数据） | C | `SearchScreen.kt:82,90,95` | 等效 |
| `player_engine` | String | "ijk" | 播放内核：`ijk`/`media3` | A | `PlayerFactory.kt:9`；`PlayerScreen.kt:645`；`PlayerViewModel.kt:213`；`SettingsScreen.kt:653,1228,1268` | 单一合格播放器，不复制双引擎 |
| `player` | String | "apsisPlayer" | 默认播放器：`apsisPlayer`/`pureAudioPlayer`/`aliangPlayer` | A | `PlayerApi.kt:477`；`SettingsScreen.kt:1543,1570,1605,1640`；`VideoDetailScreen.kt:358` | 单一合格播放器，无三选一 |
| `player_texture_view` | Bool | true | Media3 用 TextureView（否则 SurfaceView） | A | `PlayerScreen.kt:561,1762`；`SettingsScreen.kt:1036,1063,1098` | Surface/XComponent 原生渲染，无此选项 |
| `player_texture_view_probe` | Int | 0 | TextureView 可用性探测结果 -1/0/1（旧名 `isTextureViewOk`，运行时状态） | A | `PlayerScreen.kt:551,1234,1756` | 无对应概念 |
| `player_autolandscape` | Bool | false | 自动横屏 | A | `PlayerScreen.kt:267` | 平台层原生实现或省略 |
| `player_softrotate` | Bool | false | 手动旋转 | A | `PlayerScreen.kt:268` | 平台层原生实现或省略 |
| `player_softrotate_steps` | String | "0,90,180,270" | 旋转角度列表（逗号分隔） | A | `PlayerScreen.kt:270`；`SettingsScreen.kt:552,583` | 同上 |
| `player_softrotate_deg` | Float | 0 | 当前旋转角度（运行时状态） | A | `PlayerScreen.kt:283,1566` | 同上 |
| `player_scale` | Bool | true | 双指缩放视频 | A | `PlayerScreen.kt:1323` | 手势平台层，可等效或省略 |
| `player_doublemove` | Bool | true | 双指移动视频 | A | `PlayerScreen.kt:1326` | 同上 |
| `player_one_finger_zoom` | Bool | false | 单指缩放视频 | A | `PlayerScreen.kt:1169` | 同上 |
| `player_custom_btn_left` | Int | 6 | 播放器左上按钮动作（PLAYER_ACTION_*） | A | `PlayerControlLayout.kt:22`；`PlayerCustomizationScreen.kt:243` | 原生控制布局，可不复制 |
| `player_custom_btn_left_bottom` | Int | 5 | 播放器左下按钮动作 | A | `PlayerControlLayout.kt:23`；`PlayerCustomizationScreen.kt:247` | 同上 |
| `player_custom_btn_right` | Int | 2 | 播放器右上按钮动作 | A | `PlayerControlLayout.kt:24`；`PlayerCustomizationScreen.kt:251` | 同上 |
| `player_custom_btn_right_bottom` | Int | 1 | 播放器右下按钮动作 | A | `PlayerControlLayout.kt:25`；`PlayerCustomizationScreen.kt:255` | 同上 |
| `danmaku_engine` | String | "dfm" | 弹幕引擎：`dfm`/`dfmnext` | A | `DanmakuParser.kt:19,23,27`；`DanmakuConfig.kt:17`；`DanmakuPlayer.kt:29`；`SettingsScreen.kt:1150,1184` | 单一合格弹幕 renderer，不复制双引擎 |
| `cache_location` | String | "internal" | 缓存位置：`internal`/`external`（全盘存储） | A | `VideoDownloadManager.kt:57`；`MainActivity.kt:231`；`SettingsScreen.kt:1315,1406,1441` | 应用沙盒，无全盘选项 |
| `shizuku_declined` | Bool | false | 用户已拒绝 Shizuku 授权 | A | `MainActivity.kt:230,254` | 无 Shizuku，改为系统授权/沙盒 |
| `screen_mode` | Int | 0 | 屏幕模式：0=自动 1=圆屏 2=方屏 | A | `AppConfig.kt:23,31` | 手表恒圆屏，可省略 |
| `dpi_scale` | Float | 1.0 | 界面 DPI 缩放 | A | `AppConfig.kt:25,39` | 原生缩放或省略 |

## 迁移规则（v0 → v1）

来源 `SettingsMigration.RULES`，旧名 → 规范名（值原样保留）：

| 旧 key | 新 key |
|---|---|
| `isTextureViewOk` | `player_texture_view_probe` |
| `try_look_enable` | `play_try_look` |
| `use_hd_qr_code` | `login_use_hd_qr` |

其余 key 名即为规范名。迁移流程：`applyMigrations(store)`
1. 触发 `store.migrationHook(from, to)`；
2. 重命名旧 key；
3. 为缺失的 schema key 回填默认值（不覆盖已有值）；
4. 清空重写 store，并写入版本 key `settings_schema_version = 1`。

## 不在设置范围内的遗留 key

| key | 存储 | 说明 | 处理 |
|---|---|---|---|
| `cookie` | `orbit_cookies` | B 站登录 Cookie（含凭据） | 凭据，非设置；由 `CredentialStore`/`CookieStore` 承接（计划 Phase 2），Harmony 用安全存储 |
| `mid` | `orbit_cookies` | 登录用户 mid | 同上 |

## Android adapter 迁移要点

- `SharedPreferencesUtil` 的 `orbit_settings`、`AppConfig` 的 `orbit_config` 两个 prefs
  文件将合并到统一 typed store；`CookieManager` 的 `orbit_cookies` 不并入设置。
- 迁移时机：首次启动时调用 `SettingsMigration.applyMigrations(store)`；平台 adapter
  仅需实现 `SettingsStore` 的原始读写，序列化由 `toPrefString`/`fromPrefString` 承担。
- 运行时状态类 key（`player_texture_view_probe`、`player_softrotate_deg`）虽非用户
  设置，也纳入 schema 以保证迁移与存储类型一致。
