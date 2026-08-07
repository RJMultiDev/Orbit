// entry/src/main/cpp/napi_init.cpp
// Kuikly 渲染器的 N-API 初始化入口（对应 libentry.so）。
//
// 注意：
// - libshared_api.h 与 libshared.so 来自 shared/ohos（A06）的 KMP/OHOS 构建产物，
//   需从该产物目录拷贝到本模块（libshared_api.h 放本目录，libshared.so 放
//   entry/libs/${OHOS_ARCH}/ 下，见 CMakeLists.txt）。
// - symbols 入口名（libshared_symbols / api->kotlin.root.initKuikly）依赖 kuikly
//   工程里的 @Serializable 命名配置，真实集成时以 shared/ohos 产物自带头文件
//   里的实际声明为准（Kotlin root 包名/函数名变化会导致此处编译错误）。
// - 本机尚无 shared/ohos KBA 工具链时 libshared_api.h 不存在，用 __has_include
//   降级：InitKuikly 返回 0（ArkTS 侧据此跳过 Kuikly 启动，走原生 ArkUI 页面）。
#if __has_include("libshared_api.h")
#include "libshared_api.h"
#define ORBIT_HAVE_LIBSHARED 1
#else
#define ORBIT_HAVE_LIBSHARED 0
#endif
#include "napi/native_api.h"

static napi_value InitKuikly(napi_env env, napi_callback_info info) {
    int handler = 0;
#if ORBIT_HAVE_LIBSHARED
    // symbols 入口名和 kuikly 工程的配置有关，具体查看产物的头文件
    auto api = libshared_symbols();
    handler = api->kotlin.root.initKuikly();
#else
    // libshared.so 未构建：Kuikly 业务层不可用，返回 0 由 ArkTS 层降级到原生页面
#endif

    napi_value result;
    napi_create_int32(env, handler, &result);
    return result;
}

EXTERN_C_START
static napi_value Init(napi_env env, napi_value exports) {
    napi_property_descriptor desc[] = {
        // 导出 initKuikly，使其可以被 ArkTS 层访问和调用
        {"initKuikly", nullptr, InitKuikly, nullptr, nullptr, nullptr, napi_default, nullptr},
    };
    napi_define_properties(env, exports, sizeof(desc) / sizeof(desc[0]), desc);
    return exports;
}
EXTERN_C_END

static napi_module demoModule = {
    .nm_version = 1,
    .nm_flags = 0,
    .nm_filename = nullptr,
    .nm_register_func = Init,
    .nm_modname = "entry",
    .nm_priv = ((void*)0),
    .reserved = { 0 },
};

extern "C" __attribute__((constructor)) void RegisterEntryModule(void) {
    napi_module_register(&demoModule);
}
