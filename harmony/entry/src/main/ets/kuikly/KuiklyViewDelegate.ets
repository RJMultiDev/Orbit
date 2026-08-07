// entry/src/main/ets/kuikly/KuiklyViewDelegate.ets
// 参考 Kuikly QuickStart/harmony.html（IKuiklyViewDelegate 接口定义）。
import { IKuiklyViewDelegate, KRRenderModuleExportCreator, KRRenderViewExportCreator } from '@kuikly-open/render';
import { OrbitCredentialModule } from './modules/OrbitCredentialModule';
import { OrbitPlaybackModule } from './modules/OrbitPlaybackModule';
import { OrbitToastModule } from './modules/OrbitToastModule';

export class KuiklyViewDelegate extends IKuiklyViewDelegate {
  getCustomRenderViewCreatorRegisterMap(): Map<string, KRRenderViewExportCreator> {
    // 暂无自定义原生 View，返回空 Map
    const map: Map<string, KRRenderViewExportCreator> = new Map();
    return map;
  }

  getCustomRenderModuleCreatorRegisterMap(): Map<string, KRRenderModuleExportCreator> {
    const map: Map<string, KRRenderModuleExportCreator> = new Map();
    // 凭证模块：Kotlin 侧 Cookie/登录态持久化
    map.set('OrbitCredentialModule', () => new OrbitCredentialModule());
    // 播放模块：AVPlayer 播放桥接（当前音频；视频 Surface 待接）
    map.set('OrbitPlaybackModule', () => new OrbitPlaybackModule());
    // Toast 提示模块
    map.set('OrbitToastModule', () => new OrbitToastModule());
    return map;
  }

  // 页面生命周期透传给 Kuikly 引擎，由容器页 Index.ets 的
  // onPageShow()/onPageHide() 调用（与 @Entry 页面生命周期配合）。
  pageDidAppear(): void {
    super.pageDidAppear();
  }

  pageDidDisappear(): void {
    super.pageDidDisappear();
  }
}
