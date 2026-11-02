-- ============================================================
-- FILE    : seed_billing.sql
-- PURPOSE : Seed invoices in every status + some overdue.
-- LOCATION: database/seeds/
-- NOTES   :
--   Re-runnable: each block is guarded with NOT EXISTS on notes
--   marker SEED_BILL_n. Requires at least 1 seeded subscription.
-- ============================================================

SET SERVEROUTPUT ON SIZE UNLIMITED

DECLARE
    v_sub1      NUMBER;
    v_sub2      NUMBER;
    v_sub3      NUMBER;
    v_sub4      NUMBER;
    v_inserted  NUMBER := 0;
    v_skipped   NUMBER := 0;

    PROCEDURE try_inv(
        p_sub     NUMBER,
        p_start   DATE,
        p_end     DATE,
        p_due     DATE,
        p_price   NUMBER,
        p_paid    NUMBER,
        p_status  VARCHAR2,
        p_marker  VARCHAR2
    ) IS
        v_exists  NUMBER;
        v_tax     NUMBER := ROUND(p_price * 0.18, 2);
        v_total   NUMBER := p_price + v_tax;
        v_new_inv NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_exists
        FROM   invoice
        WHERE  notes LIKE '%' || p_marker || '%';

        IF v_exists = 0 THEN
            INSERT INTO invoice (
                subscription_id, billing_period_start, billing_period_end,
                subtotal, tax_amount, total_amount, amount_paid,
                due_date, status, issued_at, notes
            ) VALUES (
                p_sub, p_start, p_end,
                p_price, v_tax, v_total, p_paid,
                p_due, p_status, SYSDATE,
                'Seeded for testing. [' || p_marker || ']'
            );
            v_inserted := v_inserted + 1;
            DBMS_OUTPUT.PUT_LINE('  INSERTED  ' || p_marker || ' (' || p_status || ')');
        ELSE
            v_skipped := v_skipped + 1;
            DBMS_OUTPUT.PUT_LINE('  SKIPPED   ' || p_marker || ' (exists)');
        END IF;
    END try_inv;

BEGIN
    DBMS_OUTPUT.PUT_LINE('=== seed_billing.sql starting ===');

    -- Pick up to 4 subscription IDs dynamically
    BEGIN
        SELECT subscription_id INTO v_sub1
        FROM   subscription WHERE status IN ('ACTIVE','PENDING','SUSPENDED')
        ORDER BY subscription_id FETCH FIRST 1 ROWS ONLY;
    EXCEPTION WHEN NO_DATA_FOUND THEN
        DBMS_OUTPUT.PUT_LINE('WARN: No subscriptions found — aborting.');
        RETURN;
    END;

    BEGIN
        SELECT subscription_id INTO v_sub2 FROM subscription
        WHERE status IN ('ACTIVE','PENDING','SUSPENDED') ORDER BY subscription_id
        OFFSET 1 ROWS FETCH NEXT 1 ROWS ONLY;
    EXCEPTION WHEN NO_DATA_FOUND THEN v_sub2 := v_sub1; END;

    BEGIN
        SELECT subscription_id INTO v_sub3 FROM subscription
        WHERE status IN ('ACTIVE','PENDING','SUSPENDED') ORDER BY subscription_id
        OFFSET 2 ROWS FETCH NEXT 1 ROWS ONLY;
    EXCEPTION WHEN NO_DATA_FOUND THEN v_sub3 := v_sub2; END;

    BEGIN
        SELECT subscription_id INTO v_sub4 FROM subscription
        WHERE status IN ('ACTIVE','PENDING','SUSPENDED') ORDER BY subscription_id
        OFFSET 3 ROWS FETCH NEXT 1 ROWS ONLY;
    EXCEPTION WHEN NO_DATA_FOUND THEN v_sub4 := v_sub3; END;

    DBMS_OUTPUT.PUT_LINE('Subscriptions: ' || v_sub1 || ', ' || v_sub2 ||
                         ', ' || v_sub3 || ', ' || v_sub4);

    -- 1. DRAFT invoice (current month, not yet issued)
    try_inv(v_sub1,
        TRUNC(SYSDATE,'MM'), LAST_DAY(TRUNC(SYSDATE,'MM')),
        LAST_DAY(TRUNC(SYSDATE,'MM')) + 15,
        899, 0, 'DRAFT', 'SEED_BILL_1');

    -- 2. ISSUED invoice (last month, not yet paid)
    try_inv(v_sub2,
        ADD_MONTHS(TRUNC(SYSDATE,'MM'), -1),
        LAST_DAY(ADD_MONTHS(TRUNC(SYSDATE,'MM'), -1)),
        LAST_DAY(ADD_MONTHS(TRUNC(SYSDATE,'MM'), -1)) + 15,
        1499, 0, 'ISSUED', 'SEED_BILL_2');

    -- 3. PARTIALLY_PAID invoice (2 months ago)
    try_inv(v_sub3,
        ADD_MONTHS(TRUNC(SYSDATE,'MM'), -2),
        LAST_DAY(ADD_MONTHS(TRUNC(SYSDATE,'MM'), -2)),
        LAST_DAY(ADD_MONTHS(TRUNC(SYSDATE,'MM'), -2)) + 15,
        2099, 1000, 'PARTIALLY_PAID', 'SEED_BILL_3');

    -- 4. PAID invoice (3 months ago)
    try_inv(v_sub4,
        ADD_MONTHS(TRUNC(SYSDATE,'MM'), -3),
        LAST_DAY(ADD_MONTHS(TRUNC(SYSDATE,'MM'), -3)),
        LAST_DAY(ADD_MONTHS(TRUNC(SYSDATE,'MM'), -3)) + 15,
        899, ROUND(899 * 1.18, 2), 'PAID', 'SEED_BILL_4');

    -- 5. OVERDUE invoice (60 days ago, well past 30-day grace)
    try_inv(v_sub1,
        TRUNC(SYSDATE) - 90, TRUNC(SYSDATE) - 61,
        TRUNC(SYSDATE) - 46,   -- due 46 days ago → overdue > 30 days
        1499, 0, 'OVERDUE', 'SEED_BILL_5');

    -- 6. OVERDUE invoice on another sub (45 days past due)
    try_inv(v_sub2,
        TRUNC(SYSDATE) - 75, TRUNC(SYSDATE) - 46,
        TRUNC(SYSDATE) - 31,   -- due 31 days ago → qualifies for suspend
        2099, 0, 'OVERDUE', 'SEED_BILL_6');

    -- 7. CANCELLED invoice
    try_inv(v_sub3,
        ADD_MONTHS(TRUNC(SYSDATE,'MM'), -4),
        LAST_DAY(ADD_MONTHS(TRUNC(SYSDATE,'MM'), -4)),
        LAST_DAY(ADD_MONTHS(TRUNC(SYSDATE,'MM'), -4)) + 15,
        899, 0, 'CANCELLED', 'SEED_BILL_7');

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('=== Done: inserted=' || v_inserted ||
                         ', skipped=' || v_skipped || ' ===');
END;
/
