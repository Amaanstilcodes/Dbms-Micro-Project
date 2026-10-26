-- ============================================================
-- FILE    : find_datapump_dir.sql
-- PURPOSE : Find Oracle Data Pump directory path for expdp/impdp
-- RUN AS  : SYSTEM or SYS AS SYSDBA
-- ============================================================
SELECT directory_name, directory_path
FROM   dba_directories
WHERE  directory_name = 'DATA_PUMP_DIR';

-- Grant access to isp_app user if needed:
-- GRANT READ, WRITE ON DIRECTORY DATA_PUMP_DIR TO isp_app;
