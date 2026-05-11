#!/usr/bin/env bash
set -euo pipefail
if [ $# -ne 1 ]; then
  echo "Usage: $0 /path/to/ccec_YYYYmmdd_HHMMSS.dmp"
  exit 1
fi
cd "$(dirname "$0")/../.."
source .env 2>/dev/null || true
APP_USER="${ORACLE_APP_USER:-CCEC_TIMER}"
APP_PASSWORD="${ORACLE_APP_PASSWORD:-CCEC_TIMER_123}"
DUMP="$1"
BASENAME="$(basename "$DUMP")"
docker cp "$DUMP" "ccec-oracle:/opt/oracle/admin/FREE/dpdump/${BASENAME}"
docker exec ccec-oracle bash -lc "impdp ${APP_USER}/${APP_PASSWORD}@FREEPDB1 schemas=${APP_USER} directory=DATA_PUMP_DIR dumpfile=${BASENAME} table_exists_action=replace"
