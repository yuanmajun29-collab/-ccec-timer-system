#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

"$ROOT/deploy/scripts/prerequisites.sh"
[[ -f .env ]] || cp .env.example .env

docker compose up -d --build
docker compose ps

"$ROOT/deploy/scripts/wait-for-backend.sh" 600 3
"$ROOT/deploy/scripts/print-endpoints.sh"
