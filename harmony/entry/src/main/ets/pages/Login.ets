// entry/src/main/ets/pages/Login.ets
// 扫码登录页：生成 B 站二维码并轮询扫码状态（对齐 shared/core QrLoginUseCase 状态机）。
import router from '@ohos.router';
import { common } from '@kit.AbilityKit';
import { BiliApi } from '../core/bili/BiliApi';

@Entry
@Component
struct Login {
  @State qrUrl: string = '';
  @State statusText: string = '正在生成二维码…';
  @State qrcodeKey: string = '';
  @State finished: boolean = false;
  private api?: BiliApi;
  private pollTimer?: number;
  private pollCount: number = 0;

  aboutToAppear(): void {
    const ctx = getContext(this) as common.UIAbilityContext;
    this.api = new BiliApi(ctx);
    this.createQr();
  }

  aboutToDisappear(): void {
    if (this.pollTimer !== undefined) {
      clearInterval(this.pollTimer);
      this.pollTimer = undefined;
    }
  }

  private createQr(): void {
    this.api?.createQr().then((resp) => {
      if (resp.code !== 0) {
        this.statusText = `获取二维码失败: ${resp.message}`;
        return;
      }
      this.qrUrl = resp.data?.url ?? '';
      this.qrcodeKey = resp.data?.qrcode_key ?? '';
      this.statusText = '请用手机 B 站 App 扫码';
      this.startPoll();
    }).catch((e: Error) => {
      this.statusText = `网络错误: ${(e as Error).message}`;
    });
  }

  private startPoll(): void {
    this.pollTimer = setInterval(() => {
      this.pollOnce();
    }, 1000);
  }

  private pollOnce(): void {
    this.pollCount++;
    if (this.pollCount > 180) {
      this.statusText = '二维码已过期，请返回重试';
      this.stopPoll();
      return;
    }
    this.api?.pollQr(this.qrcodeKey).then((resp) => {
      const pollCode = resp.data?.code ?? -1;
      switch (pollCode) {
        case 0:
          // 登录成功
          this.stopPoll();
          this.api?.setLoggedIn(true);
          this.statusText = '登录成功';
          this.finished = true;
          setTimeout(() => {
            router.back();
          }, 800);
          break;
        case 86038:
          this.statusText = '二维码已失效，请返回重试';
          this.stopPoll();
          break;
        case 86090:
          this.statusText = '已扫码，请在手机上确认';
          break;
        case 86101:
        default:
          this.statusText = '等待扫码…';
          break;
      }
    }).catch((e: Error) => {
      this.statusText = `轮询错误: ${(e as Error).message}`;
      this.stopPoll();
    });
  }

  private stopPoll(): void {
    if (this.pollTimer !== undefined) {
      clearInterval(this.pollTimer);
      this.pollTimer = undefined;
    }
  }

  build() {
    Column() {
      if (this.qrUrl.length > 0) {
        QRCode(this.qrUrl)
          .width(160)
          .height(160)
          .margin({ top: 24 })
      } else {
        Text('加载中…')
          .fontSize(14)
          .margin({ top: 60 })
      }
      Text(this.statusText)
        .fontSize(12)
        .fontColor(this.finished ? '#3eaf5a' : '#888888')
        .margin({ top: 16 })
      Button('返回')
        .fontSize(12)
        .margin({ top: 20 })
        .onClick(() => router.back())
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#0f0f0f')
    .expandSafeArea([SafeAreaType.SYSTEM])
  }
}
