-- ============================================================
-- FILE    : seed_reports.sql
-- PURPOSE : Seed payments across the last 6 calendar months for
--           the Revenue trend LineChart and Plan BarChart in Reports.
-- LOCATION: database/seeds/
-- NOTES   : Fully re-runnable with idempotency checks on transaction_ref
--           and invoice notes. Requires at least 1 active subscription.
-- ============================================================

SET SERVEROUTPUT ON SIZE UNLIMITED

DECLARE
    v_sub_1       NUMBER;
    v_sub_2       NUMBER;
    v_sub_3       NUMBER;
    v_exists      NUMBER;
    v_inv_id      NUMBER;
    v_ref         VARCHAR2(100);
    v_inserted    NUMBER := 0;
    v_skipped     NUMBER := 0;

    PROCEDURE seed_month_pay(
        p_sub      NUMBER,
        p_months   NUMBER,
        p_price    NUMBER,
        p_method   VARCHAR2,
        p_ref_key  VARCHAR2
    ) IS
        v_chk       NUMBER;
        v_tax       NUMBER := ROUND(p_price * 0.18, 2);
        v_total     NUMBER := p_price + v_tax;
        v_cur_inv   NUMBER;
        v_base_date DATE := ADD_MONTHS(TRUNC(SYSDATE, 'MM'), -p_months);
    BEGIN
        SELECT COUNT(*) INTO v_chk
        FROM   payment
        WHERE  transaction_ref = p_ref_key;

        IF v_chk = 0 THEN
            INSERT INTO invoice (
                subscription_id, billing_period_start, billing_period_end,
                subtotal, tax_amount, total_amount, amount_paid,
                due_date, status, issued_at, notes
            ) VALUES (
                p_sub, v_base_date, LAST_DAY(v_base_date),
                p_price, v_tax, v_total, v_total,
                v_base_date + 15, 'PAID', v_base_date,
                'Reports test seed [' || p_ref_key || ']'
            ) RETURNING invoice_id INTO v_cur_inv;

            INSERT INTO payment (
                invoice_id, amount, payment_method, transaction_ref, payment_date
            ) VALUES (
                v_cur_inv, v_total, p_method, p_ref_key, v_base_date + 5
            );

            v_inserted := v_inserted + 1;
            DBMS_OUTPUT.PUT_LINE('  INSERTED  ' || p_ref_key || ' month -' || p_months || ' (Rs ' || v_total || ')');
        ELSE
            v_skipped := v_skipped + 1;
            DBMS_OUTPUT.PUT_LINE('  SKIPPED   ' || p_ref_key || ' (exists)');
        END IF;
    END seed_month_pay;

BEGIN
    DBMS_OUTPUT.PUT_LINE('=== seed_reports.sql starting ===');

    -- Pick 3 distinct subscriptions if available
    SELECT MIN(subscription_id) INTO v_sub_1 FROM subscription;
    SELECT NVL(MAX(subscription_id), v_sub_1) INTO v_sub_2 FROM subscription WHERE subscription_id > v_sub_1;
    IF v_sub_2 IS NULL THEN v_sub_2 := v_sub_1; END IF;
    SELECT NVL(MAX(subscription_id), v_sub_2) INTO v_sub_3 FROM subscription WHERE subscription_id > v_sub_2;
    IF v_sub_3 IS NULL THEN v_sub_3 := v_sub_2; END IF;

    IF v_sub_1 IS NULL THEN
        DBMS_OUTPUT.PUT_LINE('ERROR: No subscriptions exist. Run subscription seeds first.');
        RETURN;
    END IF;

    -- Seed payments across last 5 months + current month (total 6 months)
    seed_month_pay(v_sub_1, 5,  999.00, 'UPI',           'SEED_REP_M5_SUB1');
    seed_month_pay(v_sub_2, 5, 1499.00, 'CARD',          'SEED_REP_M5_SUB2');

    seed_month_pay(v_sub_1, 4,  999.00, 'UPI',           'SEED_REP_M4_SUB1');
    seed_month_pay(v_sub_3, 4, 2499.00, 'BANK_TRANSFER', 'SEED_REP_M4_SUB3');

    seed_month_pay(v_sub_2, 3, 1499.00, 'UPI',           'SEED_REP_M3_SUB2');
    seed_month_pay(v_sub_3, 3, 2499.00, 'CARD',          'SEED_REP_M3_SUB3');

    seed_month_pay(v_sub_1, 2,  999.00, 'UPI',           'SEED_REP_M2_SUB1');
    seed_month_pay(v_sub_2, 2, 1499.00, 'BANK_TRANSFER', 'SEED_REP_M2_SUB2');

    seed_month_pay(v_sub_1, 1,  999.00, 'UPI',           'SEED_REP_M1_SUB1');
    seed_month_pay(v_sub_3, 1, 2499.00, 'UPI',           'SEED_REP_M1_SUB3');

    seed_month_pay(v_sub_2, 0, 1499.00, 'UPI',           'SEED_REP_M0_SUB2');
    seed_month_pay(v_sub_3, 0, 2499.00, 'BANK_TRANSFER', 'SEED_REP_M0_SUB3');

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('=== seed_reports.sql finished: ' || v_inserted || ' inserted, ' || v_skipped || ' skipped ===');
END;
/
