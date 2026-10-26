#!/bin/bash
# ============================================================
# FILE    : backup_restore.sh
# PURPOSE : MySQL backup and restore commands for isp_db
# AUTHOR  : ISP Dev Team
# DATE    : 2026-10-26
# USAGE   : bash backup_restore.sh backup
#           bash backup_restore.sh restore backups/isp_db_20250101.sql
# ============================================================

DB_NAME="isp_db"
DB_USER="isp_app"
DB_HOST="localhost"
BACKUP_DIR="./backups"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

case "$1" in
  backup)
    mkdir -p "$BACKUP_DIR"
    echo "Backing up $DB_NAME..."
    mysqldump -h "$DB_HOST" -u "$DB_USER" -p \
      --single-transaction \
      --routines \
      --triggers \
      --events \
      --set-gtid-purged=OFF \
      "$DB_NAME" > "$BACKUP_DIR/${DB_NAME}_${TIMESTAMP}.sql"
    echo "Backup saved: $BACKUP_DIR/${DB_NAME}_${TIMESTAMP}.sql"
    ;;

  restore)
    if [ -z "$2" ]; then
      echo "Usage: $0 restore <backup_file.sql>"
      exit 1
    fi
    echo "Restoring $DB_NAME from $2 ..."
    mysql -h "$DB_HOST" -u "$DB_USER" -p "$DB_NAME" < "$2"
    echo "Restore complete."
    ;;

  *)
    echo "Usage: $0 {backup|restore <file>}"
    exit 1
    ;;
esac
