// entry/src/main/ets/kuikly/MyNativeManager.ets
// 参考 Kuikly QuickStart/harmony.html 2.3 节。
import { KuiklyNativeManager } from '@kuikly-open/render';
import Napi from 'libentry.so';

class MyNativeManager extends KuiklyNativeManager {
  protected loadNative(): number {
    // 调用 NAPI 接口，初始化 Kuikly Native（对应 cpp/napi_init.cpp 的 initKuikly）
    return Napi.initKuikly();
  }
}

// 导出一个全局的 KuiklyNativeManager 实例给 Kuikly 页面共用
const globalNativeManager = new MyNativeManager();

export default globalNativeManager;
