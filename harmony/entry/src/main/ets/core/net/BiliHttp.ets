// entry/src/main/ets/core/net/BiliHttp.ets
// 基于 @ohos.net.http 的 HTTP 客户端：默认 UA/Referer、Cookie 注入与回存、
// 跟随重定向、表单/GET 请求。返回解析后的 JSON（object | null）。

import http from '@ohos.net.http';
import { Cookie } from './Cookie';

export interface BiliHttpOptions {
  userAgent: string;
  referer: string;
  cookieHeader: () => string;
  onSetCookies: (setCookies: string[]) => void;
  readTimeoutMs?: number;
}

export class BiliHttp {
  private readonly userAgent: string;
  private readonly referer: string;
  private readonly cookieHeader: () => string;
  private readonly onSetCookies: (setCookies: string[]) => void;
  private readonly readTimeoutMs: number;

  constructor(opts: BiliHttpOptions) {
    this.userAgent = opts.userAgent;
    this.referer = opts.referer;
    this.cookieHeader = opts.cookieHeader;
    this.onSetCookies = opts.onSetCookies;
    this.readTimeoutMs = opts.readTimeoutMs ?? 15000;
  }

  private baseHeaders(custom: Record<string, string>): Record<string, string> {
    const h: Record<string, string> = {
      'User-Agent': this.userAgent,
      'Referer': this.referer,
    };
    const cookie = Cookie.filterSensitive(this.cookieHeader());
    if (cookie.length > 0) {
      h['Cookie'] = cookie;
    }
    for (const k of Object.keys(custom)) {
      h[k] = custom[k];
    }
    return h;
  }

  private request(url: string, method: http.RequestMethod, extraData: string | undefined,
    customHeaders: Record<string, string>): Promise<object | null> {
    return new Promise<object | null>((resolve, reject) => {
      const client = http.createHttp();
      const headers = this.baseHeaders(customHeaders);
      if (extraData !== undefined) {
        headers['Content-Type'] = 'application/x-www-form-urlencoded';
      }
      const options: http.HttpRequestOptions = {
        method: method,
        header: headers,
        readTimeout: this.readTimeoutMs,
        connectTimeout: this.readTimeoutMs,
        expectDataType: http.HttpDataType.STRING,
      };
      if (extraData !== undefined) {
        options.extraData = extraData;
      }
      client.request(url, options, (err, data) => {
        client.destroy();
        if (err) {
          reject(new Error(`HTTP ${method} ${url} failed: ${JSON.stringify(err)}`));
          return;
        }
        const code = data.responseCode;
        if (code !== 200) {
          reject(new Error(`HTTP ${code} for ${url}`));
          return;
        }
        // 回存 Set-Cookie
        if (data.cookies && Array.isArray(data.cookies) && data.cookies.length > 0) {
          this.onSetCookies(data.cookies as string[]);
        }
        const text = data.result as string;
        try {
          resolve(JSON.parse(text));
        } catch (e) {
          reject(new Error(`Invalid JSON from ${url}`));
        }
      });
    });
  }

  getJson(url: string, customHeaders: Record<string, string> = {}): Promise<object | null> {
    return this.request(url, http.RequestMethod.GET, undefined, customHeaders);
  }

  postForm(url: string, params: Map<string, string>,
    customHeaders: Record<string, string> = {}): Promise<object | null> {
    const pairs: string[] = [];
    params.forEach((v, k) => {
      pairs.push(`${encodeURIComponent(k)}=${encodeURIComponent(v)}`);
    });
    const body = pairs.join('&');
    return this.request(url, http.RequestMethod.POST, body, customHeaders);
  }
}
