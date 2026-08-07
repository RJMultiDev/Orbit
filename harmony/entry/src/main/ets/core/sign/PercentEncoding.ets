// entry/src/main/ets/core/sign/PercentEncoding.ets
// 与 shared/core（com.qx.orbit.core.net.UrlQueryBuilder）编码语义逐字节一致。
//  - encodeUriComponent：WBI 用。= URLEncoder.encode + 替换（+→%20, %21→!, %27→', %28→(, %29→), %7E→~）
//    最终未转义集：A-Z a-z 0-9 - _ . * ~ ! ' ( )；空格 → %20
//  - encodeFormComponent：AppSign 用。= URLEncoder.encode 原样。未转义集：A-Z a-z 0-9 - _ . *；空格 → +

export class PercentEncoding {
  /** UTF-8 字节（number[]）用于百分号编码。 */
  private static utf8Bytes(cp: number): number[] {
    if (cp < 0x80) {
      return [cp];
    } else if (cp < 0x800) {
      return [0xc0 | (cp >> 6), 0x80 | (cp & 0x3f)];
    } else if (cp < 0x10000) {
      return [0xe0 | (cp >> 12), 0x80 | ((cp >> 6) & 0x3f), 0x80 | (cp & 0x3f)];
    } else {
      return [0xf0 | (cp >> 18), 0x80 | ((cp >> 12) & 0x3f), 0x80 | ((cp >> 6) & 0x3f),
        0x80 | (cp & 0x3f)];
    }
  }

  private static isWbiUnreserved(c: string): boolean {
    if (c.length !== 1) {
      return false;
    }
    const cc = c.charCodeAt(0);
    if ((cc >= 0x41 && cc <= 0x5a) || (cc >= 0x61 && cc <= 0x7a) || (cc >= 0x30 && cc <= 0x39)) {
      return true;
    }
    return c === '-' || c === '_' || c === '.' || c === '*' || c === '~' || c === '!' ||
      c === '\'' || c === '(' || c === ')';
  }

  private static isFormUnreserved(c: string): boolean {
    if (c.length !== 1) {
      return false;
    }
    const cc = c.charCodeAt(0);
    if ((cc >= 0x41 && cc <= 0x5a) || (cc >= 0x61 && cc <= 0x7a) || (cc >= 0x30 && cc <= 0x39)) {
      return true;
    }
    return c === '-' || c === '_' || c === '.' || c === '*';
  }

  private static encode(s: string, isUnreserved: (c: string) => boolean, spaceReplacement: string): string {
    let out = '';
    for (let i = 0; i < s.length; i++) {
      const cp = s.codePointAt(i)!;
      if (cp > 0xffff) {
        i++;
      }
      const ch = String.fromCodePoint(cp);
      if (ch === ' ') {
        out += spaceReplacement;
        continue;
      }
      if (isUnreserved(ch)) {
        out += ch;
        continue;
      }
      const bytes = PercentEncoding.utf8Bytes(cp);
      for (const b of bytes) {
        out += '%' + (b < 16 ? '0' : '') + b.toString(16).toUpperCase();
      }
    }
    return out;
  }

  /** WBI 签名用：空格→%20，~ ! ' ( ) 保持未转义。 */
  static encodeUriComponent(s: string): string {
    return PercentEncoding.encode(s, PercentEncoding.isWbiUnreserved, '%20');
  }

  /** AppSign 用：空格→+，~ ! ' ( ) 均百分号编码。 */
  static encodeFormComponent(s: string): string {
    return PercentEncoding.encode(s, PercentEncoding.isFormUnreserved, '+');
  }

  /** 百分号解码（对齐 java.net.URLDecoder）：+→空格，%XX 按 UTF-8 解码。 */
  static percentDecode(s: string): string {
    let out = '';
    let bytes: number[] = [];
    let i = 0;
    const hexVal = (c: string): number => {
      const cc = c.charCodeAt(0);
      if (cc >= 0x30 && cc <= 0x39) return cc - 0x30;
      if (cc >= 0x61 && cc <= 0x66) return cc - 0x61 + 10;
      if (cc >= 0x41 && cc <= 0x46) return cc - 0x41 + 10;
      return -1;
    };
    const flush = (): void => {
      if (bytes.length > 0) {
        out += PercentEncoding.bytesToUtf8(bytes);
        bytes = [];
      }
    };
    while (i < s.length) {
      const c = s[i];
      if (c === '+') {
        flush();
        out += ' ';
        i++;
      } else if (c === '%' && i + 2 < s.length) {
        const hi = hexVal(s[i + 1]);
        const lo = hexVal(s[i + 2]);
        if (hi >= 0 && lo >= 0) {
          bytes.push((hi << 4) | lo);
          i += 3;
        } else {
          flush();
          out += c;
          i++;
        }
      } else {
        flush();
        out += c;
        i++;
      }
    }
    flush();
    return out;
  }

  private static bytesToUtf8(bytes: number[]): string {
    let out = '';
    let i = 0;
    while (i < bytes.length) {
      const b = bytes[i];
      if (b < 0x80) {
        out += String.fromCharCode(b);
        i++;
      } else if ((b & 0xe0) === 0xc0 && i + 1 < bytes.length) {
        out += String.fromCharCode(((b & 0x1f) << 6) | (bytes[i + 1] & 0x3f));
        i += 2;
      } else if ((b & 0xf0) === 0xe0 && i + 2 < bytes.length) {
        out += String.fromCharCode(((b & 0x0f) << 12) | ((bytes[i + 1] & 0x3f) << 6) |
          (bytes[i + 2] & 0x3f));
        i += 3;
      } else if ((b & 0xf8) === 0xf0 && i + 3 < bytes.length) {
        const cp = ((b & 0x07) << 18) | ((bytes[i + 1] & 0x3f) << 12) |
          ((bytes[i + 2] & 0x3f) << 6) | (bytes[i + 3] & 0x3f);
        out += String.fromCodePoint(cp);
        i += 4;
      } else {
        out += String.fromCharCode(b);
        i++;
      }
    }
    return out;
  }
}
