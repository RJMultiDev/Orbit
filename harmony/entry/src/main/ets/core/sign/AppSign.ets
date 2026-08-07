// entry/src/main/ets/core/sign/AppSign.ets
// AppSign 纯算法（对齐 shared/core 的 com.qx.orbit.core.sign.AppSign 与
// Android data/sign/AppSignUtil.kt）。

import { PercentEncoding } from './PercentEncoding';
import { Md5 } from './Md5';

export class AppSign {
  private readonly appKey: string;
  private readonly appSecret: string;
  private readonly nowMillis: () => number;

  constructor(appKey: string, appSecret: string, nowMillis: () => number = () => Date.now()) {
    this.appKey = appKey;
    this.appSecret = appSecret;
    this.nowMillis = nowMillis;
  }

  /** 对参数表签名，返回原参数 + appkey / ts / sign（不修改入参）。 */
  sign(params: Map<string, string>): Map<string, string> {
    const withKey = new Map<string, string>(params);
    withKey.set('appkey', this.appKey);
    withKey.set('ts', Math.floor(this.nowMillis() / 1000).toString());
    const keys = Array.from(withKey.keys()).sort();
    const raw = keys.map((k) => {
      return `${PercentEncoding.encodeFormComponent(k)}=${PercentEncoding.encodeFormComponent(withKey.get(k)!)}`;
    }).join('&');
    const sign = Md5.hex(raw + this.appSecret);
    withKey.set('sign', sign);
    return withKey;
  }
}
