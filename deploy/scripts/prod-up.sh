#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
test -f .env || cp .env.example .env
docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env pull || true
docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env up -d --build
docker compose -f docker-compose.yml -f docker-compose.prod.yml ps
