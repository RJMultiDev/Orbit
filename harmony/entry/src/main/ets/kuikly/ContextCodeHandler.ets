// entry/src/main/ets/kuikly/ContextCodeHandler.ets
// 处理 Kuikly 的 contextCode（页面代码分发的凭据）获取逻辑。
// 参考 Kuikly demo 思路的最小可用版本。
import { KRRenderExecuteModeBase, KRRenderNativeMode } from '@kuikly-open/render';

export class ContextCodeHandler {
  // TODO: 使用远程/代码分发（如云端下发 Kuikly code）时返回 true 并实现
  // handleGetContextCode 拉取 contextCode；当前最小可用版本直接走本地 Native 模式。
  isNeedGetContextCode(params: Record<string, Object>): boolean {
    return false;
  }

  // TODO: 实现 contextCode 获取（网络拉取/缓存读取），成功回调 callback(contextCode)。
  // 当前最小可用版本：与 isNeedGetContextCode()=false 保持一致，回传空串。
  handleGetContextCode(context: Context,
    params: Record<string, Object>,
    callback: (contextCodeParam: string) => void,
    exceptionCallback: (stack: string) => void): void {
    callback('');
  }

  getExecuteMode(contextCode: string): KRRenderExecuteModeBase {
    // TODO: 根据 contextCode 是否为空选择 KRRenderCodeMode.Code / KRRenderNativeMode.Native；
    // 当前固定使用本地 Native 渲染模式。
    return KRRenderNativeMode.Native;
  }
}
