// entry/src/main/ets/core/net/Cookie.ets
// Cookie 纯算法（对齐 shared/core 的 com.qx.orbit.core.net.Cookie）。
// ArkTS 严格模式：不使用元组/解构，用 string[][]（[key, value]）承载键值对。

export class Cookie {
  name: string;
  value: string;
  domain: string;
  hostOnly: boolean;
  path: string;
  secure: boolean;
  httpOnly: boolean;

  constructor(name: string, value: string, domain: string = '', hostOnly: boolean = true,
    path: string = '/', secure: boolean = false, httpOnly: boolean = false) {
    this.name = name;
    this.value = value;
    this.domain = domain;
    this.hostOnly = hostOnly;
    this.path = path;
    this.secure = secure;
    this.httpOnly = httpOnly;
  }

  toRequestHeader(): string {
    return `${this.name}=${this.value}`;
  }

  /** 解析单个 Set-Cookie 头（RFC 6265 子集），解析失败返回 null。 */
  static parseSetCookie(header: string): Cookie | null {
    const parts = header.split(';');
    const nameValue = parts[0].trim();
    const eq = nameValue.indexOf('=');
    if (eq <= 0) {
      return null;
    }
    const name = nameValue.substring(0, eq);
    const value = nameValue.substring(eq + 1);

    let domain = '';
    let hostOnly = true;
    let path = '/';
    let secure = false;
    let httpOnly = false;

    for (let i = 1; i < parts.length; i++) {
      const attr = parts[i].trim();
      if (attr.length === 0) {
        continue;
      }
      const eqIdx = attr.indexOf('=');
      const key = eqIdx < 0 ? attr : attr.substring(0, eqIdx);
      const attrValue = eqIdx < 0 ? null : attr.substring(eqIdx + 1);
      switch (key.toLowerCase()) {
        case 'domain': {
          domain = (attrValue ?? '').trim().replace(/^\./, '').toLowerCase();
          hostOnly = false;
          break;
        }
        case 'path': {
          const p = (attrValue ?? '').trim();
          if (p.startsWith('/')) {
            path = p;
          }
          break;
        }
        case 'secure':
          secure = true;
          break;
        case 'httponly':
          httpOnly = true;
          break;
      }
    }
    return new Cookie(name, value, domain, hostOnly, path, secure, httpOnly);
  }

  /** 解析 Cookie 请求头：`k=v; k=v`。返回 [[key, value], ...]；跳过无 `=` 或空 name 的片段。 */
  static parseRequestHeader(header: string): string[][] {
    const result: string[][] = [];
    if (header.length === 0) {
      return result;
    }
    for (const part of header.split(';')) {
      const trimmed = part.trim();
      if (trimmed.length === 0) {
        continue;
      }
      const eq = trimmed.indexOf('=');
      if (eq <= 0) {
        continue;
      }
      result.push([trimmed.substring(0, eq), trimmed.substring(eq + 1)]);
    }
    return result;
  }

  /**
   * 将响应中的 Set-Cookie 合并进现有 Cookie 串：覆盖同名 key（保留原顺序，新 key 追加在末尾）；
   * 显式声明 Domain 且不在 B 站域名内的跳过。返回新的 Cookie 请求头字符串。
   */
  static mergeInto(raw: string, setCookies: string[]): string {
    const map: string[][] = [];
    const existing = Cookie.parseRequestHeader(raw);
    for (const kv of existing) {
      map.push([kv[0], kv[1]]);
    }
    for (const setCookie of setCookies) {
      const c = Cookie.parseSetCookie(setCookie);
      if (c === null) {
        continue;
      }
      if (!c.hostOnly && c.domain.length > 0 && !c.domain.endsWith('bilibili.com')) {
        continue;
      }
      let replaced = false;
      for (let i = 0; i < map.length; i++) {
        if (map[i][0] === c.name) {
          map[i][1] = c.value;
          replaced = true;
          break;
        }
      }
      if (!replaced) {
        map.push([c.name, c.value]);
      }
    }
    const parts: string[] = [];
    for (const kv of map) {
      parts.push(`${kv[0]}=${kv[1]}`);
    }
    return parts.join('; ');
  }

  /** 剔除 B 站 WAF 不兼容的敏感自定义字段。 */
  static filterSensitive(raw: string): string {
    const sensitive = ['refresh_token', 'access_token', 'expires_in', 'mid'];
    const kept: string[][] = [];
    for (const kv of Cookie.parseRequestHeader(raw)) {
      if (!sensitive.includes(kv[0])) {
        kept.push([kv[0], kv[1]]);
      }
    }
    const parts: string[] = [];
    for (const kv of kept) {
      parts.push(`${kv[0]}=${kv[1]}`);
    }
    return parts.join('; ');
  }
}
