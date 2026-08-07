// entry/src/main/ets/kuikly/modules/OrbitToastModule.ets
// Toast 提示模块（ArkTS 实现）：桥到 promptAction.showToast。
// Kotlin 侧 com.qx.orbit.ohos.platform.OrbitToastModule 通过 asyncToNativeMethod 调用。
import { KuiklyRenderBaseModule, KRAny, KuiklyRenderCallback } from '@kuikly-open/render';
import { promptAction } from '@kit.ArkUI';

export class OrbitToastModule extends KuiklyRenderBaseModule {
  syncMode(): boolean {
    return false;
  }

  call(method: string, param: KRAny, callback: KuiklyRenderCallback | null): KRAny {
    if (method === 'show') {
      const args = (param ?? {}) as Record<string, Object>;
      const text = (args['0'] as string) ?? '';
      promptAction.showToast({ message: text });
    }
    return null;
  }
}
