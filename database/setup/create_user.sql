-- ============================================================
-- FILE    : create_user.sql
-- PURPOSE : Create ISP application schema/user in Oracle XE 21c
-- AUTHOR  : ISP Dev Team
-- DATE    : 2026-10-26
-- RUN AS  : SYSTEM or SYS AS SYSDBA in the PDB (FREEPDB1)
--
-- SQL*Plus connect command:
--   sqlplus system/oracle@//localhost:1521/FREEPDB1
--
-- CONNECTION URL FORMATS:
--   Service name (recommended for PDB):
--     jdbc:oracle:thin:@//localhost:1521/FREEPDB1
--   SID (legacy, for non-CDB):
--     jdbc:oracle:thin:@localhost:1521:XE
--
-- To find your service name:
--   lsnrctl status          (OS command, shows registered services)
--   SELECT value FROM v$parameter WHERE name = 'service_names';
--   SHOW CON_NAME;          (inside SQL*Plus to see current container)
-- ============================================================

-- Run inside the PDB, not CDB$ROOT
ALTER SESSION SET CONTAINER = FREEPDB1;

-- ============================================================
-- Optional: Create a dedicated tablespace for ISP data
-- ============================================================
CREATE TABLESPACE isp_tbs
    DATAFILE 'isp_tbs01.dbf' SIZE 100M
    AUTOEXTEND ON NEXT 50M MAXSIZE 2G
    EXTENT MANAGEMENT LOCAL SEGMENT SPACE MANAGEMENT AUTO;

-- ============================================================
-- Drop user if it already exists (for clean re-run)
-- ============================================================
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM dba_users WHERE username = 'ISP_APP';
    IF v_count > 0 THEN
        EXECUTE IMMEDIATE 'DROP USER isp_app CASCADE';
        DBMS_OUTPUT.PUT_LINE('Dropped existing user ISP_APP.');
    END IF;
END;
/

-- ============================================================
-- Create application user
-- ============================================================
CREATE USER isp_app
    IDENTIFIED BY "ISP_Secure#2025"
    DEFAULT TABLESPACE isp_tbs
    TEMPORARY TABLESPACE temp
    QUOTA UNLIMITED ON isp_tbs;

-- ============================================================
-- Grant privileges (least-privilege set)
-- ============================================================
GRANT CONNECT, RESOURCE TO isp_app;
GRANT CREATE VIEW      TO isp_app;
GRANT CREATE SEQUENCE  TO isp_app;
GRANT CREATE TRIGGER   TO isp_app;
GRANT CREATE PROCEDURE TO isp_app;
GRANT CREATE TYPE      TO isp_app;

-- Allow reading AWR/explain plans for performance demo
GRANT SELECT ON sys.v_$session        TO isp_app;
GRANT SELECT ANY DICTIONARY           TO isp_app;  -- for EXPLAIN PLAN

COMMIT;

PROMPT ISP_APP user created successfully.
PROMPT Connect with: sqlplus isp_app/"ISP_Secure#2025"@//localhost:1521/FREEPDB1
