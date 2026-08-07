// entry/src/main/ets/kuikly/pages/Index.ets
// Kuikly 容器页。参考 Kuikly QuickStart/harmony.html 3.3 节。
// 本文件路径（ets/kuikly/pages/Index.ets）与 main_pages.json 注册的
// 'kuikly/pages/Index' 保持一致。
import { KRRecord, KRNativeRenderController, Kuikly } from '@kuikly-open/render';
import globalNativeManager from '../MyNativeManager';
import { KuiklyViewDelegate } from '../KuiklyViewDelegate';
import router from '@ohos.router';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { ContextCodeHandler } from '../ContextCodeHandler';

const TAG = 'OrbitKuikly';
const LOG_DOMAIN = 0x0000;

@Entry
@Component
struct Index {
  private kuiklyViewDelegate = new KuiklyViewDelegate();
  private kuiklyController: KRNativeRenderController | null = null;

  private pageName: string | null = null;
  private pageData?: KRRecord;
  private contextCode: string = '';
  private contextCodeHandler: ContextCodeHandler = new ContextCodeHandler();
  private useDefaultBackPress = true;
  @State showKuikly: boolean = false;

  onBackPress(): boolean | void {
    if (this.useDefaultBackPress) {
      // 默认返回行为：交给系统/上一路由处理
      return;
    }

    if (this.kuiklyViewDelegate) {
      // 需要 Kuikly 页面接管返回时，把返回事件透传给 Kuikly 引擎
      return this.kuiklyController?.onBackPress() ?? false;
    }
  }

  aboutToAppear(): void {
    const params = router.getParams() as Record<string, Object>;
    this.pageName = (params?.pageName as string) ?? null;
    this.pageData = (params?.pageData as KRRecord | null) ?? {};

    if (this.contextCodeHandler.isNeedGetContextCode(params)) {
      this.contextCodeHandler.handleGetContextCode(getContext(this), params, (contextCode) => {
        this.contextCode = contextCode;
        this.showKuikly = true;
      }, (stack) => {
        this.showExceptionDialog(stack);
      });
    } else {
      this.showKuikly = true;
    }
  }

  build() {
    Stack() {
      if (this.showKuikly) {
        Kuikly({
          pageName: this.pageName ?? 'home',
          pageData: this.pageData ?? {},
          delegate: this.kuiklyViewDelegate,
          contextCode: this.contextCode,
          executeMode: this.contextCodeHandler.getExecuteMode(this.contextCode),
          // 可选：设置 Kuikly 容器的初始尺寸
          // initialSize: { width: this.calculateWidth(), height: this.calculateHeight() },
          onControllerReadyCallback: (controller) => {
            this.kuiklyController = controller;
            controller.registerExceptionCallback((executeMode, stack) => {
              this.showExceptionDialog(stack);
              const stackInfo: KRRecord = JSON.parse(stack);
              stackInfo['stack'].toString().split('\n').forEach((it) => {
                hilog.error(LOG_DOMAIN, TAG, '%{public}s', it);
              });
            });
          },
          nativeManager: globalNativeManager,
        });
      }
    }.expandSafeArea([SafeAreaType.KEYBOARD])
  }

  private showExceptionDialog(stack: string): void {
    // TODO: 生产环境可替换为 UI 弹窗展示异常堆栈
    hilog.error(LOG_DOMAIN, TAG, 'Kuikly exception: %{public}s', stack);
  }

  onPageShow(): void {
    // 与容器页生命周期配合，通知 Kuikly 引擎页面已展示
    this.kuiklyViewDelegate.pageDidAppear();
  }

  onPageHide(): void {
    this.kuiklyViewDelegate.pageDidDisappear();
  }
}
