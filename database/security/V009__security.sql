-- ============================================================
-- FILE    : V009__security.sql
-- PURPOSE : MySQL roles, least-privilege app user, GRANT/REVOKE
-- AUTHOR  : ISP Dev Team
-- DATE    : 2026-10-26
-- ============================================================

-- ============================================================
-- 1. Create a least-privilege application user
--    (the Java app connects as isp_app, NOT as root)
-- ============================================================
CREATE USER IF NOT EXISTS 'isp_app'@'localhost'
    IDENTIFIED BY 'changeme_strong_password_here';

-- Grant only what the app needs (no DROP, ALTER, GRANT)
GRANT SELECT, INSERT, UPDATE, DELETE ON isp_db.* TO 'isp_app'@'localhost';
GRANT EXECUTE ON isp_db.* TO 'isp_app'@'localhost';  -- call stored procedures
FLUSH PRIVILEGES;

-- ============================================================
-- 2. Read-only reporting user (for analytics / dashboards)
-- ============================================================
CREATE USER IF NOT EXISTS 'isp_readonly'@'localhost'
    IDENTIFIED BY 'readonly_password_here';
GRANT SELECT ON isp_db.* TO 'isp_readonly'@'localhost';
FLUSH PRIVILEGES;

-- ============================================================
-- 3. Revoke example: remove INSERT from readonly user
-- ============================================================
-- REVOKE INSERT ON isp_db.* FROM 'isp_readonly'@'localhost';

-- ============================================================
-- 4. Show grants for the app user
-- ============================================================
-- SHOW GRANTS FOR 'isp_app'@'localhost';

-- ============================================================
-- NOTES for db.properties:
--   db.url=jdbc:mysql://localhost:3306/isp_db
--   db.username=isp_app
--   db.password=changeme_strong_password_here
-- ============================================================
