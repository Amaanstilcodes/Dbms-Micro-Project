-- ============================================================
-- FILE    : run_all.sql
-- PURPOSE : Master SQL*Plus script that runs all database scripts
--           in the correct dependency order.
-- AUTHOR  : ISP Dev Team
-- DATE    : 2026-10-26
--
-- HOW TO RUN:
--   sqlplus isp_app/"ISP_Secure#2025"@//localhost:1521/XEPDB1 @run_all.sql
--
-- For a CLEAN INSTALL (drop everything first):
--   sqlplus isp_app/"ISP_Secure#2025"@//localhost:1521/XEPDB1 @setup/drop_all.sql
--   sqlplus isp_app/"ISP_Secure#2025"@//localhost:1521/XEPDB1 @run_all.sql
-- ============================================================

SET SERVEROUTPUT ON SIZE UNLIMITED
SET FEEDBACK ON
SET ECHO OFF
SET VERIFY OFF

-- Stop on first error and roll back
WHENEVER SQLERROR EXIT ROLLBACK

PROMPT ========================================================
PROMPT ISP Management System - Oracle Database Setup
PROMPT ========================================================

PROMPT --- Step 1: Core Tables ---
@@schema/V001__create_core_tables.sql

PROMPT --- Step 2: Indexes ---
@@migrations/V002__indexes.sql

PROMPT --- Step 3: Seed Data ---
@@seeds/V003__seed_data.sql

PROMPT --- Step 4: Views ---
@@views/V004__views.sql

PROMPT --- Step 5: PL/SQL Package (Billing) ---
@@procedures/V005__pkg_billing.sql

PROMPT --- Step 6: Triggers ---
@@triggers/V006__triggers.sql

PROMPT ========================================================
PROMPT Setup complete. Verifying tables...
PROMPT ========================================================

-- Quick verification: list all tables created
SELECT table_name, num_rows
FROM   user_tables
ORDER  BY table_name;

-- List all views
SELECT view_name FROM user_views ORDER BY view_name;

-- List all invalid objects (should be zero)
SELECT object_name, object_type, status
FROM   user_objects
WHERE  status != 'VALID'
ORDER  BY object_type, object_name;

PROMPT ========================================================
PROMPT All done! Connect app with:
PROMPT   jdbc:oracle:thin:@//localhost:1521/XEPDB1
PROMPT   user: isp_app
PROMPT ========================================================
