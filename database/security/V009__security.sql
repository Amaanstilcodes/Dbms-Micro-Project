-- ============================================================
-- FILE    : V009__security.sql
-- PURPOSE : Oracle roles, least-privilege grants, password profile
-- AUTHOR  : ISP Dev Team
-- DATE    : 2026-10-26
-- RUN AS  : SYSTEM or SYS AS SYSDBA in FREEPDB1
-- ============================================================

-- ============================================================
-- 1. Create a read-only reporting role
-- ============================================================
CREATE ROLE isp_readonly_role;
GRANT SELECT ON isp_app.customer         TO isp_readonly_role;
GRANT SELECT ON isp_app.subscription     TO isp_readonly_role;
GRANT SELECT ON isp_app.invoice          TO isp_readonly_role;
GRANT SELECT ON isp_app.payment          TO isp_readonly_role;
GRANT SELECT ON isp_app.plan             TO isp_readonly_role;
GRANT SELECT ON isp_app.v_monthly_revenue TO isp_readonly_role;

-- ============================================================
-- 2. Read-only reporting user
-- ============================================================
CREATE USER isp_readonly IDENTIFIED BY "ReadOnly#2025"
    DEFAULT TABLESPACE users TEMPORARY TABLESPACE temp QUOTA 0 ON users;
GRANT CREATE SESSION TO isp_readonly;
GRANT isp_readonly_role TO isp_readonly;

-- ============================================================
-- 3. Password profile (optional but good practice)
-- ============================================================
CREATE PROFILE isp_app_profile LIMIT
    FAILED_LOGIN_ATTEMPTS   5
    PASSWORD_LOCK_TIME      1/24        -- lock for 1 hour after 5 failures
    PASSWORD_LIFE_TIME      90          -- expire every 90 days
    PASSWORD_REUSE_TIME     180         -- cannot reuse within 180 days
    PASSWORD_REUSE_MAX      5           -- cannot reuse last 5 passwords
    PASSWORD_GRACE_TIME     7           -- 7-day grace period before expiry;

ALTER USER isp_app PROFILE isp_app_profile;

-- ============================================================
-- 4. REVOKE example
-- ============================================================
-- REVOKE SELECT ON isp_app.audit_log FROM isp_readonly_role;

-- ============================================================
-- 5. Show grants
-- ============================================================
-- SELECT * FROM dba_sys_privs WHERE grantee = 'ISP_APP';
-- SELECT * FROM dba_tab_privs WHERE grantee = 'ISP_READONLY_ROLE';

COMMIT;
PROMPT Security configuration applied.
