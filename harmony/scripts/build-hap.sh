#!/usr/bin/env bash
# 构建 Orbit HarmonyOS 手表 HAP（OpenHarmony 6.1.1 / API 24 / hvigor 6.24.4）。
#
# 用法:
#   ./scripts/build-hap.sh [assembleHap|clean] [--release]
#
# 环境说明:
#   - OpenHarmony SDK 位于 ~/.local/sdk/default/openharmony（旧布局）
#   - hvigor 6.24.4 使用新版 SDK 布局 {sdkRoot}/{apiVersion}/{component}，
#     故通过 ~/.local/sdk/orbit/openharmony/24 符号链接接入
#   - @ohos/hvigor-ohos-plugin 从华为 npm 仓解析（npm_config_registry）
set -euo pipefail
cd "$(dirname "$0")/.."

export DEVECO_SDK_HOME=/home/rj/.local/sdk/default
export OHOS_BASE_SDK_HOME=/home/rj/.local/sdk/orbit/openharmony
export npm_config_registry=https://repo.harmonyos.com/npm/

TASK="${1:-assembleHap}"
MODE="--no-daemon"
if [[ "${2:-}" == "--release" ]]; then
  TASK="assembleHap"
  # 目前仅 debug 免签名；release 需要 signingConfigs
  MODE="--no-daemon -p buildMode=release"
fi

exec /home/rj/.local/bin/hvigorw "$TASK" $MODE
