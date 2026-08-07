// entry/src/main/ets/kuikly/adapters/AppKRLogAdapter.ets
// 参考 Kuikly QuickStart/harmony.html 4.1 节：用 hilog 实现 IKRLogAdapter。
import { IKRLogAdapter } from '@kuikly-open/render';
import { hilog } from '@kit.PerformanceAnalysisKit';

const LOG_DOMAIN: number = 0x30;

export class AppKRLogAdapter implements IKRLogAdapter {
  i(tag: string, msg: string): void {
    hilog.info(LOG_DOMAIN, tag, '%{public}s', msg);
  }

  d(tag: string, msg: string): void {
    hilog.debug(LOG_DOMAIN, tag, '%{public}s', msg);
  }

  e(tag: string, msg: string): void {
    hilog.error(LOG_DOMAIN, tag, '%{public}s', msg);
  }
}
