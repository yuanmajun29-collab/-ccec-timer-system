#!/usr/bin/env bash
# 等待本机映射的后端健康检查通过（容器 Flyway + Oracle 冷启动可能较慢）。
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
# shellcheck source=deploy/scripts/lib/common.sh
source "$ROOT/deploy/scripts/lib/common.sh"
ccec_load_env_ports "$ROOT"

URL="http://127.0.0.1:${BACKEND_PORT}/actuator/health"
MAX_WAIT_SEC="${1:-600}"
INTERVAL="${2:-3}"

if ! command -v curl >/dev/null 2>&1; then
  echo "需要 curl 以探测健康检查。macOS/Linux 通常已自带。" >&2
  exit 1
fi

echo "等待后端就绪: $URL （最长 ${MAX_WAIT_SEC}s）"
started=$(date +%s)
while true; now=$(date +%s); do
  if curl -sf "$URL" | grep -q 'UP'; then
    echo "后端健康检查已通过。"
    exit 0
  fi
  elapsed=$((now - started))
  if [[ "$elapsed" -ge "$MAX_WAIT_SEC" ]]; then
    echo "超时: 后端仍未就绪。请查看日志: ./deploy/scripts/logs.sh timer-backend" >&2
    exit 1
  fi
  sleep "$INTERVAL"
done
