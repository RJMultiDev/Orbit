# Orbit - Optimized Reincarnation of BiliTerminal

哔哩终端的 Wear OS 重写项目。

## 简介

Orbit 是一个运行在 Wear OS 智能手表上的第三方哔哩哔哩客户端，借鉴了哔哩终端的部分 API 接口调用和 UI 设计。

## 技术栈

- **UI**: Jetpack Compose + Wear Material 3
- **网络**: Retrofit + OkHttp
- **视频播放**: ijkplayer
- **弹幕**: DanmakuFlameMaster
- **图片加载**: Coil

## 目前已经实现的功能

- 推荐视频浏览
- 视频播放与弹幕
- 评论查看
- 搜索
- 用户空间
- 登录
- 动态和文章查看

## 构建

```bash
./gradlew assembleDebug
```

## 系统要求

- 目前暂定 Wear OS 3.0+ (API 30+)，后续会调整兼容性至 API 24+。
- 最低内存(RAM)：1G
- 最低储存(ROM)：4G
- 推荐内存(RAM)：2G+
- 推荐储存：8G+

## 和其他开源项目的关系

本项目仅参考了[哔哩终端](https://github.com/huanli233/BiliClient)的API调用逻辑，未直接使用其源代码。
本项目使用了[IJKPlayer](https://github.com/bilibili/ijkplayer)作为视频播放器。
本项目使用了[烈焰弹幕使](https://github.com/bilibili/danmakuflamemaster)作为弹幕引擎。
本项目与 [BiliZepam](https://github.com/Re-BiliTerminal/BiliZepam-Compose) 没有任何关系。
本项目与 [Re-WearBili](https://github.com/SpaceXC/Re-WearBili) 没有任何关系。

## 许可证
本项目使用 [GNU GPL-3.0 License](LICENSE) 开源。
