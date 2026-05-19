#!/usr/bin/env bash
# 检查本机是否具备运行 Docker Compose 栈的前置条件（不修改系统）。
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
# shellcheck source=deploy/scripts/lib/common.sh
source "$ROOT/deploy/scripts/lib/common.sh"
ccec_load_env_ports "$ROOT"

fail=0

need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "缺少命令: $1" >&2
    fail=1
  fi
}

echo "== CCEC 运行环境检查 =="

need_cmd docker
if command -v docker >/dev/null 2>&1; then
  docker version --format '{{.Server.Version}}' >/dev/null 2>&1 || {
    echo "Docker 守护进程未运行或无权访问，请启动 Docker Desktop / dockerd。" >&2
    fail=1
  }
fi

if docker compose version >/dev/null 2>&1; then
  echo "Docker Compose: $(docker compose version --short 2>/dev/null || docker compose version)"
elif command -v docker-compose >/dev/null 2>&1; then
  echo "docker-compose: $(docker-compose version --short 2>/dev/null || true)"
else
  echo "缺少 Docker Compose 插件（docker compose）或 docker-compose 可执行文件。" >&2
  fail=1
fi

port_busy() {
  local p="$1"
  if command -v nc >/dev/null 2>&1; then
    nc -z 127.0.0.1 "$p" >/dev/null 2>&1
    return $?
  fi
  if bash -c "echo >/dev/tcp/127.0.0.1/$p" >/dev/null 2>&1; then
    return 0
  fi
  return 1
}

check_port_free() {
  local name="$1" port="$2"
  if port_busy "$port"; then
    echo "警告: 本机端口 ${port} (${name}) 已被占用，可能导致 compose 启动失败。请关闭占用进程或修改 .env 中的端口。" >&2
  fi
}

check_port_free "HTTP (edgebox-gate)" "$HTTP_PORT"
check_port_free "后端直连" "$BACKEND_PORT"
check_port_free "Redis" "$REDIS_PORT"
check_port_free "Oracle" "$ORACLE_PORT"

echo "建议: 至少 8GB 内存；Oracle Free 首次拉取与初始化可能需数分钟。"
echo "磁盘: Docker 卷将占用数 GB（Oracle 数据卷）。"

if [[ "$fail" -ne 0 ]]; then
  echo "前置检查未通过，请先安装/启动 Docker。" >&2
  exit 1
fi
echo "前置检查通过。"
exit 0
