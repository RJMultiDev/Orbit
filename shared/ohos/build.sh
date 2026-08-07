#!/usr/bin/env bash
# 构建 shared/ohos KBA Kotlin → libshared.so（Kuikly 业务层）。
#
# 前置：
#   - JDK 17（org.gradle.java.home 已在 gradle.properties 指向 /usr/lib/jvm/java-17-openjdk）
#   - OHOS_SDK_HOME 指向 OpenHarmony SDK（KBA 工具链读取该 env 定位 sysroot/NDK）
#   - 独立 Gradle 8.0 wrapper（shared/ohos/gradle/wrapper）
#
# 用法：
#   ./build.sh                     # 产出 debug libshared.so + libshared_api.h
#   ./build.sh install             # 构建并拷贝到 harmony/entry（libs/arm64-v8a + cpp）
set -euo pipefail
cd "$(dirname "$0")"

export OHOS_SDK_HOME="${OHOS_SDK_HOME:-/home/rj/.local/sdk/default/openharmony}"

./gradlew :shared:linkSharedDebugSharedOhosArm64 --console=plain --no-daemon

SO=shared/build/bin/ohosArm64/sharedDebugShared/libshared.so
HDR=shared/build/bin/ohosArm64/sharedDebugShared/libshared_api.h

if [[ "${1:-}" == "install" ]]; then
  cp "$SO" ../harmony/entry/libs/arm64-v8a/libshared.so
  cp "$HDR" ../harmony/entry/src/main/cpp/libshared_api.h
  echo "已拷贝 libshared.so / libshared_api.h 到 harmony/entry"
fi
