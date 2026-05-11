#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
source .env 2>/dev/null || true
BACKUP_DIR="${BACKUP_DIR:-/opt/ccec-timer/backups}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"
APP_USER="${ORACLE_APP_USER:-CCEC_TIMER}"
APP_PASSWORD="${ORACLE_APP_PASSWORD:-CCEC_TIMER_123}"
mkdir -p "$BACKUP_DIR"
STAMP="$(date +%Y%m%d_%H%M%S)"
docker exec ccec-oracle bash -lc "mkdir -p /tmp/ccec_backup && expdp ${APP_USER}/${APP_PASSWORD}@FREEPDB1 schemas=${APP_USER} directory=DATA_PUMP_DIR dumpfile=ccec_${STAMP}.dmp logfile=ccec_${STAMP}.log" || {
  echo "Oracle Data Pump 备份失败，请确认容器已运行且用户具备导出权限。"
  exit 1
}
docker cp "ccec-oracle:/opt/oracle/admin/FREE/dpdump/ccec_${STAMP}.dmp" "$BACKUP_DIR/"
docker cp "ccec-oracle:/opt/oracle/admin/FREE/dpdump/ccec_${STAMP}.log" "$BACKUP_DIR/" || true
find "$BACKUP_DIR" -type f -name 'ccec_*' -mtime +"$RETENTION_DAYS" -delete
echo "Backup saved to $BACKUP_DIR/ccec_${STAMP}.dmp"
