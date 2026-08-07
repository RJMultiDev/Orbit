// entry/src/main/ets/pages/Player.ets
// 播放页：取流（WBI 签名 playurl）→ AVPlayer 播放（视频走 XComponent Surface）。
// 当前为手表垂直切片 MVP：支持播放/暂停/seek。
import media from '@ohos.multimedia.media';
import router from '@ohos.router';
import { common } from '@kit.AbilityKit';
import { BiliApi } from '../core/bili/BiliApi';
import { PlayUrlResp } from '../core/bili/BiliTypes';

/** XComponent onLoad 回调提供的上下文（ArkUI 未导出具名类型）。 */
interface XComponentCtx {
  getXComponentSurfaceId(): string;
}

@Entry
@Component
struct Player {
  @State title: string = '';
  @State status: string = '加载中…';
  @State playing: boolean = false;
  @State durationMs: number = 0;
  @State positionMs: number = 0;
  private api?: BiliApi;
  private avPlayer?: media.AVPlayer;
  private surfaceId: string = '';
  private sliderTimer?: number;

  aboutToAppear(): void {
    const ctx = getContext(this) as common.UIAbilityContext;
    this.api = new BiliApi(ctx);
    const params = router.getParams() as Record<string, Object>;
    const bvid = (params?.bvid as string) ?? '';
    const cid = (params?.cid as number) ?? 0;
    this.startPlay(bvid, cid);
  }

  aboutToDisappear(): void {
    if (this.sliderTimer !== undefined) {
      clearInterval(this.sliderTimer);
      this.sliderTimer = undefined;
    }
    this.avPlayer?.release();
    this.avPlayer = undefined;
  }

  private async startPlay(bvid: string, cid: number): Promise<void> {
    if (!this.api || bvid.length === 0) {
      this.status = '参数错误';
      return;
    }
    try {
      const info = await this.api.videoInfo(bvid);
      if (info.code !== 0) {
        this.status = `详情失败: ${info.message}`;
        return;
      }
      this.title = info.data?.title ?? bvid;
      const realCid = cid > 0 ? cid : (info.data?.cid ?? 0);
      const pu = await this.api.playUrl(bvid, realCid);
      if (pu.code !== 0) {
        this.status = `取流失败: ${pu.message}`;
        return;
      }
      const videoUrl = this.pickVideoUrl(pu);
      if (videoUrl.length === 0) {
        this.status = '无可播放流';
        return;
      }
      await this.createPlayer(videoUrl);
    } catch (e) {
      this.status = `错误: ${(e as Error).message}`;
    }
  }

  private pickVideoUrl(pu: PlayUrlResp): string {
    const dash = pu.data?.dash?.video;
    if (dash && dash.length > 0) {
      return dash[0].baseUrl;
    }
    const durl = pu.data?.durl;
    if (durl && durl.length > 0) {
      return durl[0].url;
    }
    return '';
  }

  private async createPlayer(url: string): Promise<void> {
    const avPlayer = await media.createAVPlayer();
    this.avPlayer = avPlayer;

    avPlayer.on('stateChange', (state: media.AVPlayerState, reason: media.StateChangeReason) => {
      if (state === 'prepared') {
        this.durationMs = avPlayer.duration;
        this.status = '就绪';
        if (this.surfaceId.length > 0) {
          avPlayer.surfaceId = this.surfaceId;
        }
        avPlayer.play();
      } else if (state === 'playing') {
        this.playing = true;
        this.status = '';
      } else if (state === 'paused') {
        this.playing = false;
        this.status = '已暂停';
      } else if (state === 'error') {
        this.status = `播放错误: ${reason}`;
      }
    });

    avPlayer.on('timeUpdate', (time: number) => {
      this.positionMs = time;
    });

    avPlayer.url = url;
    await avPlayer.prepare().catch((e: Error) => {
      this.status = `prepare 失败: ${JSON.stringify(e)}`;
    });
  }

  private togglePlay(): void {
    const p = this.avPlayer;
    if (!p) {
      return;
    }
    if (this.playing) {
      p.pause();
    } else {
      p.play();
    }
  }

  private seekTo(v: number): void {
    this.avPlayer?.seek(v);
  }

  build() {
    Column() {
      Text(this.title)
        .fontSize(13)
        .maxLines(2)
        .textOverflow({ overflow: TextOverflow.Ellipsis })
        .padding({ left: 10, right: 10, top: 8 })
        .width('100%')

      XComponent({ id: 'playerXComponent', type: XComponentType.SURFACE, libraryname: '' })
        .width('100%')
        .height(150)
        .margin({ top: 6 })
        .onLoad((context) => {
          if (context) {
            this.surfaceId = (context as XComponentCtx).getXComponentSurfaceId();
            if (this.avPlayer && this.surfaceId.length > 0) {
              this.avPlayer.surfaceId = this.surfaceId;
            }
          }
        })

      Slider({
        value: this.positionMs,
        min: 0,
        max: Math.max(this.durationMs, 1),
        step: 500,
        style: SliderStyle.OutSet,
      })
        .width('90%')
        .onChange((v: number) => this.seekTo(v))

      Row() {
        Button(this.playing ? '暂停' : '播放')
          .fontSize(13)
          .onClick(() => this.togglePlay())
        Button('返回')
          .fontSize(13)
          .margin({ left: 16 })
          .onClick(() => router.back())
      }
      .margin({ top: 10 })

      Text(this.status)
        .fontSize(11)
        .fontColor('#e06565')
        .margin({ top: 6 })
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#0f0f0f')
    .expandSafeArea([SafeAreaType.SYSTEM])
  }
}
