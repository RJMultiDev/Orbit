// entry/src/main/ets/entryability/EntryAbility.ets
// 参考 Kuikly QuickStart/harmony.html 4.3 节：
// onWindowStageCreate 里注册 Kuikly 渲染器的日志/路由适配器后再加载首页。
import { UIAbility, AbilityConstant, Want } from '@kit.AbilityKit';
import { window } from '@kit.ArkUI';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { KuiklyRenderAdapterManager } from '@kuikly-open/render';
import { AppKRLogAdapter } from '../kuikly/adapters/AppKRLogAdapter';
import { AppKRRouterAdapter } from '../kuikly/adapters/AppKRRouterAdapter';

const DOMAIN = 0x0000;
const TAG = 'Orbit';

export default class EntryAbility extends UIAbility {
  onCreate(want: Want, launchParam: AbilityConstant.LaunchParam): void {
    hilog.info(DOMAIN, TAG, 'EntryAbility onCreate');
  }

  onDestroy(): void {
    hilog.info(DOMAIN, TAG, 'EntryAbility onDestroy');
  }

  onWindowStageCreate(windowStage: window.WindowStage): void {
    // 日志适配器
    KuiklyRenderAdapterManager.krLogAdapter = new AppKRLogAdapter();
    // 路由适配器
    KuiklyRenderAdapterManager.krRouterAdapter = new AppKRRouterAdapter();

    // 首页加载原生 ArkUI Home 页（ets/pages/Home.ets）。
    // 说明：Kuikly 容器页（kuikly/pages/Index）在 shared/ohos 产出 libshared.so 前
    // 无 Kotlin 业务 UI（initKuikly 返回 0 降级），故默认入口走原生 ArkUI；
    // libshared.so 就绪后可切回 'kuikly/pages/Index' 验证 Kuikly 共享页面。
    windowStage.loadContent('pages/Home', (err) => {
      if (err.code) {
        hilog.error(DOMAIN, TAG, 'Failed to load content. Cause: %{public}s', JSON.stringify(err) ?? '');
        return;
      }
      hilog.info(DOMAIN, TAG, 'Succeeded in loading the content.');
    });
  }

  onWindowStageDestroy(): void {
    hilog.info(DOMAIN, TAG, 'EntryAbility onWindowStageDestroy');
  }

  onForeground(): void {
    hilog.info(DOMAIN, TAG, 'EntryAbility onForeground');
  }

  onBackground(): void {
    hilog.info(DOMAIN, TAG, 'EntryAbility onBackground');
  }
}
