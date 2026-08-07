// entry/src/main/ets/pages/Home.ets
// 首页：推荐流。未登录时可跳转扫码登录；点击卡片进入播放页。
import router from '@ohos.router';
import { common } from '@kit.AbilityKit';
import { BiliApi } from '../core/bili/BiliApi';
import { RecommendCard } from '../core/bili/BiliTypes';

@Entry
@Component
struct Home {
  @State cards: RecommendCard[] = [];
  @State loading: boolean = false;
  @State error: string = '';
  @State loggedIn: boolean = false;
  @State uname: string = '';
  private api?: BiliApi;

  aboutToAppear(): void {
    const ctx = getContext(this) as common.UIAbilityContext;
    this.api = new BiliApi(ctx);
    this.checkLogin();
    this.loadRecommend();
  }

  private checkLogin(): void {
    if (!this.api) {
      return;
    }
    this.api.nav().then((nav) => {
      this.loggedIn = nav.data?.isLogin === true;
      this.uname = nav.data?.uname ?? '';
      if (this.loggedIn) {
        this.api!.setLoggedIn(true);
      }
    }).catch(() => {
      this.loggedIn = this.api!.isLoggedIn();
    });
  }

  private loadRecommend(): void {
    if (!this.api || this.loading) {
      return;
    }
    this.loading = true;
    this.error = '';
    this.api.recommend().then((resp) => {
      if (resp.code !== 0) {
        this.error = `推荐失败: ${resp.message}`;
        return;
      }
      this.cards = resp.data?.item ?? [];
    }).catch((e: Error) => {
      this.error = `网络错误: ${(e as Error).message}`;
    }).finally(() => {
      this.loading = false;
    });
  }

  private gotoLogin(): void {
    router.pushUrl({ url: 'pages/Login' });
  }

  private gotoPlayer(card: RecommendCard): void {
    router.pushUrl({
      url: 'pages/Player',
      params: { bvid: card.bvid, cid: card.cid ?? 0 },
    });
  }

  private doLogout(): void {
    this.api?.logout().then(() => {
      this.loggedIn = false;
      this.uname = '';
    }).catch(() => {
      this.loggedIn = false;
      this.uname = '';
    });
  }

  build() {
    Column() {
      Row() {
        Text(this.loggedIn ? (this.uname.length > 0 ? this.uname : '已登录') : '未登录')
          .fontSize(12)
          .fontColor('#888888')
          .layoutWeight(1)
          .margin({ left: 8 })
        Button('Kuikly')
          .fontSize(11)
          .height(26)
          .onClick(() => router.pushUrl({ url: 'kuikly/pages/Index' }))
        if (this.loggedIn) {
          Button('退出')
            .fontSize(11)
            .height(26)
            .onClick(() => this.doLogout())
        } else {
          Button('登录')
            .fontSize(11)
            .height(26)
            .onClick(() => this.gotoLogin())
        }
      }
      .width('100%')
      .height(40)
      .alignItems(VerticalAlign.Center)

      if (this.error.length > 0) {
        Text(this.error)
          .fontSize(12)
          .fontColor('#e06565')
          .margin(8)
      }

      if (this.loading && this.cards.length === 0) {
        Text('加载中…')
          .fontSize(13)
          .fontColor('#666666')
          .layoutWeight(1)
          .textAlign(TextAlign.Center)
          .width('100%')
      } else {
        Scroll() {
          Column() {
            ForEach(this.cards, (card: RecommendCard) => {
              Column() {
                Text(card.title)
                  .fontSize(13)
                  .maxLines(2)
                  .textOverflow({ overflow: TextOverflow.Ellipsis })
                  .width('100%')
                Row() {
                  Text(card.owner?.name ?? '')
                    .fontSize(11)
                    .fontColor('#888888')
                    .layoutWeight(1)
                  Text(`${this.formatCount(card.stat?.view ?? 0)}播放`)
                    .fontSize(11)
                    .fontColor('#888888')
                }
                .width('100%')
                .margin({ top: 4 })
              }
              .width('100%')
              .padding(10)
              .backgroundColor('#1a1a1a')
              .borderRadius(10)
              .margin({ bottom: 8 })
              .onClick(() => this.gotoPlayer(card))
            }, (card: RecommendCard) => card.bvid)
          }
          .width('100%')
          .padding({ left: 10, right: 10, top: 4 })
        }
        .layoutWeight(1)
        .scrollBar(BarState.Off)
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#0f0f0f')
    .expandSafeArea([SafeAreaType.SYSTEM])
  }

  private formatCount(n: number): string {
    if (n >= 10000) {
      return `${(n / 10000).toFixed(1)}万`;
    }
    return n.toString();
  }
}
