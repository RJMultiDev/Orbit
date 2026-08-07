# entry/src/main/cpp/CMakeLists.txt
# 参考 Kuikly QuickStart/harmony.html 5.3 节，但对缺失产物做优雅降级：
#  - render::kuikly：由 @kuikly-open/render ohpm 包提供（已安装时存在该 target）
#  - kuikly_shared（libshared.so）：由 shared/ohos 的 KBA 工具链产出，
#    本机尚无该工具链时 libshared.so 不存在，跳过链接（napi_init.cpp 内已用
#    __has_include 降级，InitKuikly 返回 0）。
cmake_minimum_required(VERSION 3.5.0)
project(orbit_entry)

set(NATIVERENDER_ROOT_PATH ${CMAKE_CURRENT_SOURCE_DIR})

# 入口模块 so：libentry.so（ArkTS 侧通过 `import Napi from 'libentry.so'` 使用，
# 对应 napi_init.cpp 中 nm_modname = "entry"）
add_library(entry SHARED napi_init.cpp)

set(ENTRY_LINK_LIBS libace_napi.z.so)

# Kuikly SDK 原生库：@kuikly-open/render 安装后提供 render::kuikly target
if(TARGET render::kuikly)
  list(APPEND ENTRY_LINK_LIBS render::kuikly)
endif()

# 业务产物 libshared.so：存在才链接（TODO: shared/ohos KBA 工具链就绪后启用）
set(SHARED_SO ${NATIVERENDER_ROOT_PATH}/../../../libs/${OHOS_ARCH}/libshared.so)
if(EXISTS ${SHARED_SO})
  add_library(kuikly_shared SHARED IMPORTED)
  set_target_properties(kuikly_shared PROPERTIES IMPORTED_LOCATION ${SHARED_SO})
  list(APPEND ENTRY_LINK_LIBS kuikly_shared)
endif()

target_link_libraries(entry PUBLIC ${ENTRY_LINK_LIBS})
