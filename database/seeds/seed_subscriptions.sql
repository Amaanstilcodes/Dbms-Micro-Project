-- ============================================================
-- FILE    : seed_subscriptions.sql
-- PURPOSE : Seed 6 test subscriptions covering all 4 statuses.
-- LOCATION: database/seeds/
-- NOTES   :
--   - Re-runnable: each INSERT is guarded by a NOT EXISTS check
--     on the SEED_SUB_TEST marker in the notes column.
--   - Customer, plan, and area IDs are selected dynamically
--     using FETCH FIRST n ROWS ONLY (Oracle 12c+, safe for 23ai).
--   - Skips gracefully if there are fewer than 4 active customers,
--     3 active plans, or 1 active area in the DB.
-- ============================================================

SET SERVEROUTPUT ON SIZE UNLIMITED

DECLARE
    -- ── Dynamic IDs: picked in ascending PK order ──────────────────
    v_cust1  NUMBER;
    v_cust2  NUMBER;
    v_cust3  NUMBER;
    v_cust4  NUMBER;
    v_plan1  NUMBER;
    v_plan2  NUMBER;
    v_plan3  NUMBER;
    v_area1  NUMBER;

    -- ── Counters ────────────────────────────────────────────────────
    v_inserted  NUMBER := 0;
    v_skipped   NUMBER := 0;

    -- ── Helper: insert one row if not already seeded ────────────────
    PROCEDURE try_insert(
        p_customer NUMBER,
        p_plan     NUMBER,
        p_area     NUMBER,
        p_date     DATE,
        p_status   VARCHAR2,
        p_marker   VARCHAR2   -- unique tag, e.g. 'SEED_SUB_TEST_1'
    ) IS
        v_exists NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_exists
        FROM   subscription
        WHERE  notes LIKE '%' || p_marker || '%';

        IF v_exists = 0 THEN
            INSERT INTO subscription
                (customer_id, plan_id, area_id, start_date, status, notes)
            VALUES
                (p_customer, p_plan, p_area, p_date, p_status,
                 'Seeded for testing. [' || p_marker || ']');
            v_inserted := v_inserted + 1;
            DBMS_OUTPUT.PUT_LINE('  INSERTED  ' || p_marker ||
                                 ' (' || p_status || ')');
        ELSE
            v_skipped := v_skipped + 1;
            DBMS_OUTPUT.PUT_LINE('  SKIPPED   ' || p_marker || ' (already exists)');
        END IF;
    END try_insert;

BEGIN
    DBMS_OUTPUT.PUT_LINE('=== seed_subscriptions.sql starting ===');

    -- ── Fetch real customer IDs (ACTIVE only, ordered by PK) ────────
    BEGIN
        SELECT customer_id INTO v_cust1
        FROM   customer WHERE status = 'ACTIVE'
        ORDER BY customer_id
        FETCH FIRST 1 ROWS ONLY;
    EXCEPTION WHEN NO_DATA_FOUND THEN
        DBMS_OUTPUT.PUT_LINE('WARN: No ACTIVE customers found — seeding aborted.');
        RETURN;
    END;

    BEGIN
        SELECT customer_id INTO v_cust2
        FROM   customer WHERE status = 'ACTIVE'
        ORDER BY customer_id
        OFFSET 1 ROWS FETCH NEXT 1 ROWS ONLY;
    EXCEPTION WHEN NO_DATA_FOUND THEN v_cust2 := v_cust1; END;

    BEGIN
        SELECT customer_id INTO v_cust3
        FROM   customer WHERE status = 'ACTIVE'
        ORDER BY customer_id
        OFFSET 2 ROWS FETCH NEXT 1 ROWS ONLY;
    EXCEPTION WHEN NO_DATA_FOUND THEN v_cust3 := v_cust2; END;

    BEGIN
        SELECT customer_id INTO v_cust4
        FROM   customer WHERE status = 'ACTIVE'
        ORDER BY customer_id
        OFFSET 3 ROWS FETCH NEXT 1 ROWS ONLY;
    EXCEPTION WHEN NO_DATA_FOUND THEN v_cust4 := v_cust3; END;

    DBMS_OUTPUT.PUT_LINE('Customers: ' || v_cust1 || ', ' || v_cust2 ||
                         ', ' || v_cust3 || ', ' || v_cust4);

    -- ── Fetch real plan IDs (active plans, cheapest first) ──────────
    BEGIN
        SELECT plan_id INTO v_plan1
        FROM   plan WHERE is_active = 'Y'
        ORDER BY price, plan_id
        FETCH FIRST 1 ROWS ONLY;
    EXCEPTION WHEN NO_DATA_FOUND THEN
        DBMS_OUTPUT.PUT_LINE('WARN: No active plans found — seeding aborted.');
        RETURN;
    END;

    BEGIN
        SELECT plan_id INTO v_plan2
        FROM   plan WHERE is_active = 'Y'
        ORDER BY price, plan_id
        OFFSET 1 ROWS FETCH NEXT 1 ROWS ONLY;
    EXCEPTION WHEN NO_DATA_FOUND THEN v_plan2 := v_plan1; END;

    BEGIN
        SELECT plan_id INTO v_plan3
        FROM   plan WHERE is_active = 'Y'
        ORDER BY price, plan_id
        OFFSET 2 ROWS FETCH NEXT 1 ROWS ONLY;
    EXCEPTION WHEN NO_DATA_FOUND THEN v_plan3 := v_plan2; END;

    DBMS_OUTPUT.PUT_LINE('Plans: ' || v_plan1 || ', ' || v_plan2 || ', ' || v_plan3);

    -- ── Fetch one active service area ────────────────────────────────
    BEGIN
        SELECT area_id INTO v_area1
        FROM   service_area WHERE is_active = 'Y'
        ORDER BY area_id
        FETCH FIRST 1 ROWS ONLY;
    EXCEPTION WHEN NO_DATA_FOUND THEN
        DBMS_OUTPUT.PUT_LINE('WARN: No active service areas found — seeding aborted.');
        RETURN;
    END;

    DBMS_OUTPUT.PUT_LINE('Area: ' || v_area1);
    DBMS_OUTPUT.PUT_LINE('Inserting seed rows...');

    -- ── 6 seed rows: 3 ACTIVE, 1 PENDING, 1 SUSPENDED, 1 TERMINATED ─
    try_insert(v_cust1, v_plan1, v_area1, SYSDATE - 90, 'ACTIVE',     'SEED_SUB_TEST_1');
    try_insert(v_cust2, v_plan2, v_area1, SYSDATE - 60, 'ACTIVE',     'SEED_SUB_TEST_2');
    try_insert(v_cust3, v_plan3, v_area1, SYSDATE - 30, 'ACTIVE',     'SEED_SUB_TEST_3');
    try_insert(v_cust4, v_plan1, v_area1, SYSDATE,      'PENDING',    'SEED_SUB_TEST_4');
    try_insert(v_cust2, v_plan3, v_area1, SYSDATE - 45, 'SUSPENDED',  'SEED_SUB_TEST_5');
    try_insert(v_cust3, v_plan2, v_area1, SYSDATE - 120,'TERMINATED', 'SEED_SUB_TEST_6');

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('=== Done: inserted=' || v_inserted ||
                         ', skipped=' || v_skipped || ' ===');
END;
/
