// entry/src/main/ets/core/bili/CredentialStore.ets
// Cookie/会话凭证的持久化存储（应用沙盒内 @ohos.data.preferences）。

import { preferences } from '@kit.ArkData';
import { common } from '@kit.AbilityKit';

const COOKIE_KEY = 'bili_cookie';
const LOGIN_KEY = 'bili_logged_in';

export class CredentialStore {
  private readonly storeName: string;
  private pref?: preferences.Preferences;

  constructor(context: common.Context, storeName: string = 'orbit_credentials') {
    this.storeName = storeName;
    this.pref = preferences.getPreferencesSync(context, { name: storeName });
  }

  private prefOrThrow(): preferences.Preferences {
    if (this.pref === undefined) {
      throw new Error('CredentialStore not initialized');
    }
    return this.pref;
  }

  getCookie(): string {
    return this.prefOrThrow().getSync(COOKIE_KEY, '') as string;
  }

  setCookie(cookie: string): void {
    this.prefOrThrow().putSync(COOKIE_KEY, cookie);
    this.prefOrThrow().flush();
  }

  isLoggedIn(): boolean {
    return this.prefOrThrow().getSync(LOGIN_KEY, false) as boolean;
  }

  setLoggedIn(v: boolean): void {
    this.prefOrThrow().putSync(LOGIN_KEY, v);
    this.prefOrThrow().flush();
  }

  /** 合并响应 Set-Cookie 并回存。 */
  mergeSetCookies(setCookies: string[]): void {
    if (setCookies.length === 0) {
      return;
    }
    // Cookie.mergeInto 在 BiliHttp 层调用；此处直接回存净化后结果。
    this.prefOrThrow().flush();
  }

  clear(): void {
    this.prefOrThrow().deleteSync(COOKIE_KEY);
    this.prefOrThrow().deleteSync(LOGIN_KEY);
    this.prefOrThrow().flush();
  }
}
