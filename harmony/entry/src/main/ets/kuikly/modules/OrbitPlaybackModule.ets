// entry/src/main/ets/kuikly/modules/OrbitPlaybackModule.ets
// 播放模块（ArkTS 实现）：基于 media.AVPlayer 播放 B 站流媒体。
// Kotlin 侧 com.qx.orbit.ohos.platform.OrbitPlaybackModule 通过 asyncToNativeMethod 调用。
//
// 说明：当前简化实现仅播放音频（B 站 playurl dash 的 audio m4s 流，AVPlayer 可直接播放）。
// 视频画面需要 XComponent 提供 surfaceId 并经 avPlayer.setVideoSurface() 绑定，页面侧尚未接入，
// 待后续在播放页 XComponent 回调中把 surfaceId 传入（可新增 setSurfaceId 桥接方法）。
import { KuiklyRenderBaseModule, KRAny, KuiklyRenderCallback } from '@kuikly-open/render';
import { media } from '@kit.MediaKit';

// AVPlayer 状态放在模块顶层：Kuikly 按模块名经 creator 工厂实例化模块，实例可能被重建，
// 顶层单例保证播放器在整个进程生命周期内只有一个，状态不会因实例重建而丢失。
let gPlayer: media.AVPlayer | null = null;
let gState: media.AVPlayerState = media.AVPlayerState.IDLE;

async function createPlayer(): Promise<media.AVPlayer> {
  const player: media.AVPlayer = await media.createAVPlayer();
  player.on('stateChange', (state: media.AVPlayerState) => {
    gState = state;
    console.info(`[OrbitPlayback] stateChange -> ${state}`);
  });
  gPlayer = player;
  return player;
}

async function playInternal(url: string, title: string): Promise<void> {
  try {
    console.info(`[OrbitPlayback] play title=${title} url=${url}`);
    let player: media.AVPlayer | null = gPlayer;
    if (player === null || gState === media.AVPlayerState.RELEASED) {
      player = await createPlayer();
    }
    // 切换源：先回到 idle 再设置新 URL
    if (gState !== media.AVPlayerState.IDLE) {
      await player.reset();
    }
    player.setAVUrl(url);
    await player.prepare();
    await player.play();
  } catch (e) {
    console.error(`[OrbitPlayback] play failed: ${JSON.stringify(e)}`);
  }
}

/** 对现有播放器执行一次性异步操作，失败仅记日志。 */
function run(action: (p: media.AVPlayer) => Promise<void>): void {
  const p = gPlayer;
  if (p === null) {
    return;
  }
  action(p).catch((e: Error) => {
    console.error(`[OrbitPlayback] action failed: ${e.message}`);
  });
}

export class OrbitPlaybackModule extends KuiklyRenderBaseModule {
  syncMode(): boolean {
    return false;
  }

  call(method: string, param: KRAny, callback: KuiklyRenderCallback | null): KRAny {
    const args = (param ?? {}) as Record<string, Object>;
    switch (method) {
      case 'play': {
        const url = (args['0'] as string) ?? '';
        const title = (args['1'] as string) ?? '';
        playInternal(url, title).then(() => {
          if (callback) { callback(null); }
        });
        break;
      }
      case 'pause':
        run((p: media.AVPlayer) => p.pause());
        break;
      case 'resume':
        run((p: media.AVPlayer) => p.play());
        break;
      case 'seek': {
        const ms = (args['0'] as number) ?? 0;
        run((p: media.AVPlayer) => p.seek(ms, media.SeekMode.SEEK_CLOSEST_SYNC));
        break;
      }
      case 'release': {
        const p = gPlayer;
        if (p !== null) {
          p.release().then(() => {
            gPlayer = null;
            gState = media.AVPlayerState.IDLE;
          }).catch((e: Error) => {
            console.error(`[OrbitPlayback] release failed: ${e.message}`);
            gPlayer = null;
            gState = media.AVPlayerState.IDLE;
          });
        }
        break;
      }
      case 'getPosition': {
        let pos = 0;
        if (gPlayer !== null &&
          gState !== media.AVPlayerState.IDLE &&
          gState !== media.AVPlayerState.INITIALIZED) {
          pos = gPlayer.currentTime ?? 0;
        }
        if (callback) { callback(pos); }
        break;
      }
      case 'isPlaying': {
        if (callback) { callback(gState === media.AVPlayerState.PLAYING); }
        break;
      }
      default:
        break;
    }
    return null;
  }
}
