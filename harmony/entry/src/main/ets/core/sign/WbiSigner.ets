// entry/src/main/ets/core/sign/WbiSigner.ets
// WBI 签名纯算法（对齐 shared/core 的 com.qx.orbit.core.sign.WbiSigner 与
// Android data/api/WbiSigner.kt）。mixinKey 由 WbiKeyProvider 异步提供
//（首次会拉取 nav 接口），clock 可注入便于测试。

import { PercentEncoding } from './PercentEncoding';
import { Md5 } from './Md5';

export class WbiSigner {
  private readonly keyProvider: () => Promise<string>;
  private readonly nowMillis: () => number;

  constructor(keyProvider: () => Promise<string>, nowMillis: () => number = () => Date.now()) {
    this.keyProvider = keyProvider;
    this.nowMillis = nowMillis;
  }

  /** 对参数表签名，返回原参数 + w_rid / wts（不修改入参）。 */
  async signParams(params: Map<string, string>): Promise<Map<string, string>> {
    const mixinKey = await this.keyProvider();
    const ts = Math.floor(this.nowMillis() / 1000).toString();
    const encoded: string[] = [];
    params.forEach((v, k) => {
      encoded.push(`${PercentEncoding.encodeUriComponent(k)}=${PercentEncoding.encodeUriComponent(v)}`);
    });
    encoded.push(`wts=${ts}`);
    encoded.sort();
    const wrid = Md5.hex(encoded.join('&') + mixinKey);
    const out = new Map<string, string>(params);
    out.set('w_rid', wrid);
    out.set('wts', ts);
    return out;
  }

  /** 对带 query 的 URL 签名；输出保留原 URL（与 Android signUrl 一致，仅追加签名）。 */
  async signUrl(url: string): Promise<string> {
    const mixinKey = await this.keyProvider();
    const ts = Math.floor(this.nowMillis() / 1000).toString();
    const separator = url.includes('?') ? '&' : '?';
    const queryPart = url.substring(url.indexOf('?') + 1);
    const entries: string[] = [];
    if (queryPart.length > 0 && url.includes('?')) {
      for (const piece of queryPart.split('&')) {
        const eq = piece.indexOf('=');
        if (eq > 0) {
          const k = PercentEncoding.encodeUriComponent(PercentEncoding.percentDecode(piece.substring(0, eq)));
          const v = PercentEncoding.encodeUriComponent(PercentEncoding.percentDecode(piece.substring(eq + 1)));
          entries.push(`${k}=${v}`);
        } else {
          entries.push(piece);
        }
      }
    }
    entries.push(`wts=${ts}`);
    entries.sort();
    const wrid = Md5.hex(entries.join('&') + mixinKey);
    return `${url}${separator}w_rid=${wrid}&wts=${ts}`;
  }
}
