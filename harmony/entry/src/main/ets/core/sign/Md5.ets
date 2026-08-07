// entry/src/main/ets/core/sign/Md5.ets
// 纯 ArkTS 的 MD5 实现（移植自经典 js-md5，字节语义与 shared/core jvm 端
// java.security.MessageDigest 输出小写 hex 一致）。ArkTS 严格类型约束下不使用 any。

export class Md5 {
  private static K: number[] = [];
  private static readonly S: number[] = [
    7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
    5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
    4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
    6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21,
  ];

  private static ensureK(): void {
    if (Md5.K.length === 0) {
      for (let i = 0; i < 64; i++) {
        Md5.K.push(Math.floor(Math.abs(Math.sin(i + 1)) * 4294967296));
      }
    }
  }

  private static rotl(x: number, c: number): number {
    return (x << c) | (x >>> (32 - c));
  }

  private static addUnsigned(x: number, y: number): number {
    const lsw = (x & 0xffff) + (y & 0xffff);
    const msw = (x >> 16) + (y >> 16) + (lsw >> 16);
    return (msw << 16) | (lsw & 0xffff);
  }

  /** UTF-8 编码字符串为字节数组（无 java 依赖）。 */
  private static utf8Bytes(s: string): number[] {
    const out: number[] = [];
    for (let i = 0; i < s.length; i++) {
      let c = s.codePointAt(i)!;
      if (c > 0xffff) {
        i++;
      }
      if (c < 0x80) {
        out.push(c);
      } else if (c < 0x800) {
        out.push(0xc0 | (c >> 6), 0x80 | (c & 0x3f));
      } else if (c < 0x10000) {
        out.push(0xe0 | (c >> 12), 0x80 | ((c >> 6) & 0x3f), 0x80 | (c & 0x3f));
      } else {
        out.push(
          0xf0 | (c >> 18),
          0x80 | ((c >> 12) & 0x3f),
          0x80 | ((c >> 6) & 0x3f),
          0x80 | (c & 0x3f),
        );
      }
    }
    return out;
  }

  /** 计算 MD5，返回 32 位小写十六进制字符串。 */
  static hex(input: string): string {
    Md5.ensureK();
    const bytes = Md5.utf8Bytes(input);
    // 填充：0x80 + 0x00... + 64 位 bit 长度（小端）
    const bitLen = bytes.length * 8;
    const padded = bytes.slice();
    padded.push(0x80);
    while (padded.length % 64 !== 56) {
      padded.push(0);
    }
    padded.push(bitLen & 0xff, (bitLen >>> 8) & 0xff, (bitLen >>> 16) & 0xff,
      (bitLen >>> 24) & 0xff, 0, 0, 0, 0);

    let a0 = 0x67452301;
    let b0 = 0xefcdab89;
    let c0 = 0x98badcfe;
    let d0 = 0x10325476;

    for (let off = 0; off < padded.length; off += 64) {
      const m: number[] = [];
      for (let j = 0; j < 16; j++) {
        const idx = off + j * 4;
        m.push(
          padded[idx] | (padded[idx + 1] << 8) | (padded[idx + 2] << 16) |
            (padded[idx + 3] << 24),
        );
      }
      let a = a0;
      let b = b0;
      let c = c0;
      let d = d0;

      for (let i = 0; i < 64; i++) {
        let f = 0;
        let g = 0;
        if (i < 16) {
          f = (b & c) | (~b & d);
          g = i;
        } else if (i < 32) {
          f = (d & b) | (~d & c);
          g = (5 * i + 1) % 16;
        } else if (i < 48) {
          f = b ^ c ^ d;
          g = (3 * i + 5) % 16;
        } else {
          f = c ^ (b | ~d);
          g = (7 * i) % 16;
        }
        const temp = d;
        d = c;
        c = b;
        b = Md5.addUnsigned(b, Md5.rotl(Md5.addUnsigned(a, Md5.addUnsigned(f,
          Md5.addUnsigned(Md5.K[i], m[g]))), Md5.S[i]));
        a = temp;
      }
      a0 = Md5.addUnsigned(a0, a);
      b0 = Md5.addUnsigned(b0, b);
      c0 = Md5.addUnsigned(c0, c);
      d0 = Md5.addUnsigned(d0, d);
    }

    const words = [a0, b0, c0, d0];
    let hex = '';
    for (const w of words) {
      for (let j = 0; j < 4; j++) {
        const b = (w >>> (j * 8)) & 0xff;
        hex += (b < 16 ? '0' : '') + b.toString(16);
      }
    }
    return hex;
  }
}
