// entry/src/main/ets/core/bili/WbiKeys.ets
// WBI mixinKey 计算（标准 B 站算法）与 key 记忆。

const MIXIN_KEY_ENC_TAB: number[] = [
  46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
  27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13,
  37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4,
  22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52,
];

export function calcMixinKey(imgUrl: string, subUrl: string): string {
  const imgKey = imgUrl.substring(imgUrl.lastIndexOf('/') + 1).split('.')[0];
  const subKey = subUrl.substring(subUrl.lastIndexOf('/') + 1).split('.')[0];
  const raw = imgKey + subKey;
  let out = '';
  for (const idx of MIXIN_KEY_ENC_TAB) {
    out += raw[idx];
  }
  return out;
}

export class WbiKeyStore {
  private mixinKey: string = '';
  private fetched: boolean = false;
  private readonly fetchKey: () => Promise<string>;

  constructor(fetchKey: () => Promise<string>) {
    this.fetchKey = fetchKey;
  }

  /** 获取 mixinKey；首次调用会拉取 nav 接口，成功后缓存。 */
  async getMixinKey(): Promise<string> {
    if (this.mixinKey.length > 0) {
      return this.mixinKey;
    }
    this.mixinKey = await this.fetchKey();
    this.fetched = true;
    return this.mixinKey;
  }

  reset(): void {
    this.mixinKey = '';
    this.fetched = false;
  }

  isFetched(): boolean {
    return this.fetched;
  }
}
