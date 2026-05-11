#!/usr/bin/env bash
# 生产编排：docker-compose.yml + docker-compose.prod.yml（监控、资源限制等）。
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

[[ -f .env ]] || cp .env.example .env
set -a
. ./.env
set +a

"$ROOT/deploy/scripts/prerequisites.sh"

COMPOSE_FILES=( -f docker-compose.yml -f docker-compose.prod.yml )

docker compose "${COMPOSE_FILES[@]}" --env-file .env pull || true
docker compose "${COMPOSE_FILES[@]}" --env-file .env up -d --build
docker compose "${COMPOSE_FILES[@]}" ps

echo "等待核心业务后端就绪（生产栈服务较多，可适当延长等待）"
"$ROOT/deploy/scripts/wait-for-backend.sh" 900 5 || true

"$ROOT/deploy/scripts/print-endpoints.sh"
echo "Prometheus: http://${PUBLIC_HOST:-localhost}:${PROMETHEUS_PORT:-9090}"
echo "Grafana:    http://${PUBLIC_HOST:-localhost}:${GRAFANA_PORT:-3000}"
