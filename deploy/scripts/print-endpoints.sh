#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
# shellcheck source=deploy/scripts/lib/common.sh
source "$ROOT/deploy/scripts/lib/common.sh"
ccec_load_env_ports "$ROOT"

HOST="${PUBLIC_HOST:-localhost}"
BASE="http://${HOST}:${HTTP_PORT}"

cat <<EOF

======== CCEC 访问入口 ========
工位屏:     ${BASE}/?station=A601
管理控制台: ${BASE}/admin/
  登录页:   ${BASE}/admin/login.html
网关健康:   ${BASE}/healthz
入口清单:   ${BASE}/edgebox-gate/manifest.json
后端直连:   http://127.0.0.1:${BACKEND_PORT}/actuator/health
默认管理员: admin / Admin123! （仅空库首次创建，生产请改密）

常用命令:
  docker compose ps
  ./deploy/scripts/logs.sh
  ./deploy/scripts/stop.sh
================================

EOF
