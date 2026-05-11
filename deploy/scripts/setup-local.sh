#!/usr/bin/env bash
# 从零搭建本地/服务器上的完整运行环境（Docker Compose 全栈）。
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

echo ">>> 1/4 前置检查"
"$ROOT/deploy/scripts/prerequisites.sh"

echo ">>> 2/4 环境文件"
if [[ ! -f .env ]]; then
  cp .env.example .env
  echo "已创建 .env（由 .env.example 复制），可按需编辑 PLC/密码/端口。"
else
  echo ".env 已存在，跳过复制。"
fi

echo ">>> 3/4 构建并启动容器（首次 Oracle 镜像与初始化可能较慢）"
docker compose up -d --build
docker compose ps

echo ">>> 4/4 等待后端就绪"
"$ROOT/deploy/scripts/wait-for-backend.sh" 600 3

"$ROOT/deploy/scripts/print-endpoints.sh"
