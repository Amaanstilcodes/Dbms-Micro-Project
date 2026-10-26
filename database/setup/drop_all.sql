-- ============================================================
-- FILE    : drop_all.sql
-- PURPOSE : Drop all ISP objects for clean re-run (development only)
-- RUN AS  : isp_app in SQL*Plus
-- WARNING : Drops ALL tables with CASCADE CONSTRAINTS. Irreversible.
-- ============================================================
SET SERVEROUTPUT ON SIZE UNLIMITED
SET FEEDBACK OFF

BEGIN
    -- Drop tables in reverse FK dependency order
    FOR t IN (
        SELECT table_name FROM user_tables
        ORDER BY table_name
    ) LOOP
        BEGIN
            EXECUTE IMMEDIATE 'DROP TABLE ' || t.table_name || ' CASCADE CONSTRAINTS PURGE';
            DBMS_OUTPUT.PUT_LINE('Dropped table: ' || t.table_name);
        EXCEPTION WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('Skipped: ' || t.table_name || ' - ' || SQLERRM);
        END;
    END LOOP;

    -- Drop sequences
    FOR s IN (SELECT sequence_name FROM user_sequences) LOOP
        BEGIN
            EXECUTE IMMEDIATE 'DROP SEQUENCE ' || s.sequence_name;
            DBMS_OUTPUT.PUT_LINE('Dropped sequence: ' || s.sequence_name);
        EXCEPTION WHEN OTHERS THEN NULL;
        END;
    END LOOP;

    -- Drop views
    FOR v IN (SELECT view_name FROM user_views) LOOP
        BEGIN
            EXECUTE IMMEDIATE 'DROP VIEW ' || v.view_name;
        EXCEPTION WHEN OTHERS THEN NULL;
        END;
    END LOOP;

    -- Drop procedures, functions, packages, triggers
    FOR p IN (SELECT object_name, object_type FROM user_objects
              WHERE object_type IN ('PROCEDURE','FUNCTION','PACKAGE','TRIGGER')) LOOP
        BEGIN
            EXECUTE IMMEDIATE 'DROP ' || p.object_type || ' ' || p.object_name;
        EXCEPTION WHEN OTHERS THEN NULL;
        END;
    END LOOP;

    DBMS_OUTPUT.PUT_LINE('Schema reset complete.');
END;
/

COMMIT;
