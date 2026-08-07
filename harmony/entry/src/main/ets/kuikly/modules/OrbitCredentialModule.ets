// entry/src/main/ets/kuikly/modules/OrbitCredentialModule.ets
// 凭证模块（ArkTS 实现）：Cookie 读写/清理、登录态标记，经 preferences 持久化。
// Kotlin 侧 com.qx.orbit.ohos.platform.OrbitCredentialModule 通过 asyncToNativeMethod 调用。
import { KuiklyRenderBaseModule, KRAny, KuiklyRenderCallback } from '@kuikly-open/render';
import { preferences } from '@kit.ArkData';

const COOKIE_KEY = 'bili_cookie';
const LOGIN_KEY = 'bili_logged_in';
const STORE_NAME = 'orbit_credentials';

export class OrbitCredentialModule extends KuiklyRenderBaseModule {
  syncMode(): boolean {
    return false;
  }

  call(method: string, param: KRAny, callback: KuiklyRenderCallback | null): KRAny {
    const ctx = this.controller?.getUIAbilityContext();
    if (!ctx) {
      if (callback) { callback(null); }
      return null;
    }
    const pref = preferences.getPreferencesSync(ctx, { name: STORE_NAME });
    switch (method) {
      case 'getCookie': {
        const cookie = pref.getSync(COOKIE_KEY, '') as string;
        if (callback) { callback(cookie); }
        break;
      }
      case 'setCookie': {
        const value = (param as Record<string, Object>)['0'] as string ?? '';
        pref.putSync(COOKIE_KEY, value);
        pref.flush();
        break;
      }
      case 'clear': {
        pref.deleteSync(COOKIE_KEY);
        pref.deleteSync(LOGIN_KEY);
        pref.flush();
        break;
      }
      case 'isLoggedIn': {
        const loggedIn = pref.getSync(LOGIN_KEY, false) as boolean;
        if (callback) { callback(loggedIn); }
        break;
      }
      case 'setLoggedIn': {
        const value = (param as Record<string, Object>)['0'] as boolean ?? false;
        pref.putSync(LOGIN_KEY, value);
        pref.flush();
        break;
      }
      default:
        break;
    }
    return null;
  }
}
