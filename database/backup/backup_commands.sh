#!/bin/bash
# ============================================================
# FILE    : backup_commands.sh
# PURPOSE : Oracle Data Pump export/import and RMAN backup notes
# AUTHOR  : ISP Dev Team
# DATE    : 2026-10-26
# ============================================================

# ============================================================
# DATA PUMP EXPORT (expdp) — full schema backup
# Run as: oracle OS user or user with EXP_FULL_DATABASE privilege
# ============================================================

# Export entire ISP_APP schema to a dump file
expdp system/oracle@//localhost:1521/XEPDB1 \
    SCHEMAS=ISP_APP \
    DIRECTORY=DATA_PUMP_DIR \
    DUMPFILE=isp_app_$(date +%Y%m%d).dmp \
    LOGFILE=isp_app_exp_$(date +%Y%m%d).log \
    COMPRESSION=ALL

# Export only specific tables
expdp isp_app/"ISP_Secure#2025"@//localhost:1521/XEPDB1 \
    TABLES=CUSTOMER,SUBSCRIPTION,INVOICE,PAYMENT \
    DIRECTORY=DATA_PUMP_DIR \
    DUMPFILE=isp_billing_$(date +%Y%m%d).dmp

# ============================================================
# DATA PUMP IMPORT (impdp) — restore schema
# ============================================================

# Import to the same schema (restore)
impdp system/oracle@//localhost:1521/XEPDB1 \
    SCHEMAS=ISP_APP \
    DIRECTORY=DATA_PUMP_DIR \
    DUMPFILE=isp_app_20250101.dmp \
    LOGFILE=isp_app_imp.log

# Import to a different schema (migration / clone)
impdp system/oracle@//localhost:1521/XEPDB1 \
    REMAP_SCHEMA=ISP_APP:ISP_APP_TEST \
    DIRECTORY=DATA_PUMP_DIR \
    DUMPFILE=isp_app_20250101.dmp

# Find the DATA_PUMP_DIR path in Oracle:
# SELECT directory_path FROM dba_directories WHERE directory_name = 'DATA_PUMP_DIR';
# Default on XE: /opt/oracle/admin/XEPDB1/dpdump/ (Linux)
#                C:\app\oracle\admin\XE\dpdump\ (Windows)

# ============================================================
# RMAN BACKUP NOTES
# ============================================================
# RMAN (Recovery Manager) is Oracle's integrated backup tool.
# For XE it is simpler to use Data Pump for logical backups.
# For physical backups with point-in-time recovery, use RMAN:
#
# rman target /
#   BACKUP DATABASE PLUS ARCHIVELOG;
#   BACKUP CURRENT CONTROLFILE;
#   LIST BACKUP;
#   RESTORE DATABASE;
#   RECOVER DATABASE;
# ============================================================
