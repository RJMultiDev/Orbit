// entry/src/main/ets/kuikly/adapters/AppKRRouterAdapter.ets
// 参考 Kuikly QuickStart/harmony.html 4.2 节。
//
// 注意：Kuikly 官方 demo 的路由目标是 'pages/Index'（容器页位于 ets/pages/Index.ets）。
// 本工程容器页位于 ets/kuikly/pages/Index.ets，与 main_pages.json 注册的
// 'kuikly/pages/Index' 保持一致，因此这里 pushUrl 的目标也是 'kuikly/pages/Index'。
import { KRRecord, IKRRouterAdapter } from '@kuikly-open/render';
import router from '@ohos.router';
import { common } from '@kit.AbilityKit';

export class AppKRRouterAdapter implements IKRRouterAdapter {
  openPage(context: common.UIAbilityContext, pageName: string, pageData: KRRecord): void {
    router.pushUrl({
      url: 'kuikly/pages/Index',
      params: {
        pageName,
        pageData
      }
    });
  }

  closePage(context: common.UIAbilityContext): void {
    router.back();
  }
}
