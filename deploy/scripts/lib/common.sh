#!/usr/bin/env bash
# shellcheck source=deploy/scripts/lib/common.sh
# 被其它脚本 source；勿直接执行。

ccec_repo_root() {
  local here
  here="$(cd "$(dirname "${BASH_SOURCE[1]}")/../.." && pwd)"
  echo "$here"
}

ccec_load_env_ports() {
  local root="${1:-}"
  if [[ -z "$root" ]]; then
    root="$(cd "$(dirname "${BASH_SOURCE[1]}")/../.." && pwd)"
  fi
  export CCEC_ROOT="$root"
  if [[ -f "$root/.env" ]]; then
    set -a
    # shellcheck disable=SC1090
    source "$root/.env"
    set +a
  fi
  : "${HTTP_PORT:=80}"
  : "${BACKEND_PORT:=8080}"
  : "${ORACLE_PORT:=1521}"
  : "${REDIS_PORT:=6379}"
}
