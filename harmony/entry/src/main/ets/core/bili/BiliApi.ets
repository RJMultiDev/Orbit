// entry/src/main/ets/core/bili/BiliApi.ets
// B 站接口门面：装配 CredentialStore + BiliHttp + WBI/AppSign，提供
// 登录 / 推荐 / 搜索 / 详情 / 播放取流等能力（ArkTS 端，对齐 shared/core 契约）。

import { common } from '@kit.AbilityKit';
import { BiliHttp } from '../net/BiliHttp';
import { Cookie } from '../net/Cookie';
import { CredentialStore } from './CredentialStore';
import { WbiKeyStore, calcMixinKey } from './WbiKeys';
import { WbiSigner } from '../sign/WbiSigner';
import { AppSign } from '../sign/AppSign';
import {
  ApiResp, NavResp, NavData, QrGenerateResp, QrPollResp, RecommendResp,
  VideoViewResp, PlayUrlResp, SearchResp,
} from './BiliTypes';

export const BILI_UA =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.95 Safari/537.36';
export const BILI_REFERER = 'https://www.bilibili.com/';
export const BILI_APP_KEY = 'dfca71928277209b';
export const BILI_APP_SECRET = 'b5475a8825547a4fc26c7d518eaaa02e';

export class BiliApi {
  private readonly store: CredentialStore;
  private readonly http: BiliHttp;
  private readonly wbi: WbiKeyStore;
  private readonly wbiSigner: WbiSigner;
  private readonly appSign: AppSign;

  constructor(context: common.Context) {
    this.store = new CredentialStore(context);
    this.http = new BiliHttp({
      userAgent: BILI_UA,
      referer: BILI_REFERER,
      cookieHeader: () => this.store.getCookie(),
      onSetCookies: (cookies) => {
        const merged = Cookie.mergeInto(this.store.getCookie(), cookies);
        this.store.setCookie(merged);
      },
    });
    this.wbi = new WbiKeyStore(() => this.fetchMixinKey());
    this.wbiSigner = new WbiSigner(() => this.wbi.getMixinKey());
    this.appSign = new AppSign(BILI_APP_KEY, BILI_APP_SECRET);
  }

  private async fetchMixinKey(): Promise<string> {
    const nav = await this.nav();
    const img = nav?.data?.wbi_img;
    if (!img || !img.img_url || !img.sub_url) {
      throw new Error('nav 无 wbi_img，无法生成 WBI key');
    }
    return calcMixinKey(img.img_url, img.sub_url);
  }

  // ---- 认证 ----

  /** 当前登录态（未登录时 data.isLogin=false）。 */
  async nav(): Promise<NavResp> {
    return (await this.http.getJson('https://api.bilibili.com/x/web-interface/nav')) as NavResp;
  }

  /** 生成 Web 扫码登录二维码。 */
  async createQr(): Promise<QrGenerateResp> {
    return (await this.http.getJson(
      'https://passport.bilibili.com/x/passport-login/web/qrcode/generate')) as QrGenerateResp;
  }

  /** 轮询扫码状态：code 0=成功并带 refresh_token；86038=过期；86090=已扫码；86101=未扫码。 */
  async pollQr(qrcodeKey: string): Promise<QrPollResp> {
    return (await this.http.getJson(
      `https://passport.bilibili.com/x/passport-login/web/qrcode/poll?qrcode_key=${qrcodeKey}`)) as QrPollResp;
  }

  /** 退出登录。 */
  async logout(): Promise<ApiResp> {
    const params = new Map<string, string>();
    const pairs = Cookie.parseRequestHeader(this.store.getCookie());
    let csrf = '';
    for (const kv of pairs) {
      if (kv[0] === 'bili_jct') {
        csrf = kv[1];
        break;
      }
    }
    params.set('biliCSRF', csrf);
    const resp = await this.http.postForm(
      'https://passport.bilibili.com/x/passport-login/web/logout', params);
    this.store.clear();
    return resp as ApiResp;
  }

  // ---- 内容 ----

  /** 推荐流（WBI 签名）。 */
  async recommend(): Promise<RecommendResp> {
    const params = new Map<string, string>();
    params.set('ps', '20');
    params.set('fresh_idx', '0');
    const signed = await this.wbiSigner.signParams(params);
    const qparts: string[] = [];
    signed.forEach((v, k) => {
      qparts.push(`${encodeURIComponent(k)}=${encodeURIComponent(v)}`);
    });
    const query = qparts.join('&');
    return (await this.http.getJson(
      `https://api.bilibili.com/x/web-interface/wbi/index/top/feed/rcmd?${query}`)) as RecommendResp;
  }

  /** 搜索（WBI 签名）。 */
  async search(keyword: string): Promise<SearchResp> {
    const params = new Map<string, string>();
    params.set('keyword', keyword);
    params.set('page', '1');
    params.set('page_size', '10');
    params.set('search_type', 'video');
    const signed = await this.wbiSigner.signParams(params);
    const qparts: string[] = [];
    signed.forEach((v, k) => {
      qparts.push(`${encodeURIComponent(k)}=${encodeURIComponent(v)}`);
    });
    const query = qparts.join('&');
    return (await this.http.getJson(
      `https://api.bilibili.com/x/web-interface/wbi/search/all/v2?${query}`)) as SearchResp;
  }

  /** 视频详情。 */
  async videoInfo(bvid: string): Promise<VideoViewResp> {
    return (await this.http.getJson(
      `https://api.bilibili.com/x/web-interface/view?bvid=${bvid}`)) as VideoViewResp;
  }

  /** 播放取流（WBI 签名，DASH/FLV 两种）。 */
  async playUrl(bvid: string, cid: number, fnval: number = 16): Promise<PlayUrlResp> {
    const params = new Map<string, string>();
    params.set('bvid', bvid);
    params.set('cid', cid.toString());
    params.set('qn', '64');
    params.set('fnval', fnval.toString());
    params.set('fourk', '1');
    const signed = await this.wbiSigner.signParams(params);
    const qparts: string[] = [];
    signed.forEach((v, k) => {
      qparts.push(`${encodeURIComponent(k)}=${encodeURIComponent(v)}`);
    });
    const query = qparts.join('&');
    return (await this.http.getJson(
      `https://api.bilibili.com/x/player/wbi/playurl?${query}`)) as PlayUrlResp;
  }

  /** 登录态。 */
  isLoggedIn(): boolean {
    return this.store.isLoggedIn();
  }

  setLoggedIn(v: boolean): void {
    this.store.setLoggedIn(v);
  }

  clearCredentials(): void {
    this.store.clear();
  }
}
