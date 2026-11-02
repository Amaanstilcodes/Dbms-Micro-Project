-- ============================================================================
-- SEED SCRIPT: seed_current_month_payments.sql
-- PURPOSE: Inserts sample payments and ensures invoices exist for the CURRENT
--          month (and last month) so that the Monthly Revenue KPI tile and
--          trend LineChart display live non-zero figures and deltas.
--          Fully re-runnable with idempotency checks and trigger integration.
-- RUN VIA SQL*PLUS:
--   sqlplus isp_app/"ISP_Secure#2025"@//localhost:1521/FREEPDB1 @database/seeds/seed_current_month_payments.sql
-- ============================================================================

SET FEEDBACK ON;
SET SERVEROUTPUT ON;

PROMPT [INFO] Seeding payments for current and previous calendar months...

DECLARE
    v_inv_curr_1  NUMBER;
    v_inv_curr_2  NUMBER;
    v_inv_prev    NUMBER;
    v_sub_1       NUMBER;
    v_sub_2       NUMBER;
    v_exists      NUMBER;
    v_ref_curr_1  VARCHAR2(100);
    v_ref_curr_2  VARCHAR2(100);
    v_ref_prev    VARCHAR2(100);
BEGIN
    -- 1. Grab two active subscriptions
    SELECT MIN(subscription_id) INTO v_sub_1 FROM subscription WHERE status = 'ACTIVE';
    SELECT MAX(subscription_id) INTO v_sub_2 FROM subscription WHERE status = 'ACTIVE';

    IF v_sub_1 IS NULL THEN
        DBMS_OUTPUT.PUT_LINE('No active subscriptions found. Run seeds first.');
        RETURN;
    END IF;

    -- Stable transaction reference keys for re-runnability
    v_ref_curr_1 := 'DEMO_PAY_CURR_' || TO_CHAR(TRUNC(SYSDATE, 'MM'), 'YYYYMM') || '_01';
    v_ref_curr_2 := 'DEMO_PAY_CURR_' || TO_CHAR(TRUNC(SYSDATE, 'MM'), 'YYYYMM') || '_02';
    v_ref_prev   := 'DEMO_PAY_PREV_' || TO_CHAR(ADD_MONTHS(TRUNC(SYSDATE, 'MM'), -1), 'YYYYMM') || '_01';

    -- 2. Seed Current Month Payment #1 (Subscription 1)
    SELECT COUNT(*) INTO v_exists FROM payment WHERE transaction_ref = v_ref_curr_1;
    IF v_exists = 0 THEN
        -- Insert invoice in ISSUED status with amount_paid = 0
        INSERT INTO invoice (
            subscription_id, billing_period_start, billing_period_end,
            subtotal, tax_amount, total_amount, amount_paid, due_date, status, issued_at
        ) VALUES (
            v_sub_1, TRUNC(SYSDATE, 'MM'), LAST_DAY(SYSDATE),
            2499.00, 449.82, 2948.82, 0.00, TRUNC(SYSDATE, 'MM') + 15, 'ISSUED', SYSDATE
        ) RETURNING invoice_id INTO v_inv_curr_1;

        -- Record payment (amount_paid updated on invoice to total_amount; trg_invoice_status_compound transitions status to PAID)
        INSERT INTO payment (
            invoice_id, amount, payment_method, transaction_ref, payment_date
        ) VALUES (
            v_inv_curr_1, 2948.82, 'UPI', v_ref_curr_1, SYSDATE
        );

        UPDATE invoice
           SET amount_paid = amount_paid + 2948.82
         WHERE invoice_id = v_inv_curr_1;
    END IF;

    -- 3. Seed Current Month Payment #2 (Subscription 2)
    IF v_sub_2 IS NOT NULL AND v_sub_2 <> v_sub_1 THEN
        SELECT COUNT(*) INTO v_exists FROM payment WHERE transaction_ref = v_ref_curr_2;
        IF v_exists = 0 THEN
            INSERT INTO invoice (
                subscription_id, billing_period_start, billing_period_end,
                subtotal, tax_amount, total_amount, amount_paid, due_date, status, issued_at
            ) VALUES (
                v_sub_2, TRUNC(SYSDATE, 'MM'), LAST_DAY(SYSDATE),
                1499.00, 269.82, 1768.82, 0.00, TRUNC(SYSDATE, 'MM') + 15, 'ISSUED', SYSDATE
            ) RETURNING invoice_id INTO v_inv_curr_2;

            INSERT INTO payment (
                invoice_id, amount, payment_method, transaction_ref, payment_date
            ) VALUES (
                v_inv_curr_2, 1768.82, 'CARD', v_ref_curr_2, SYSDATE - 1/24
            );

            UPDATE invoice
               SET amount_paid = amount_paid + 1768.82
             WHERE invoice_id = v_inv_curr_2;
        END IF;
    END IF;

    -- 4. Seed Previous Month Payment (to calculate +/- % delta)
    SELECT COUNT(*) INTO v_exists FROM payment WHERE transaction_ref = v_ref_prev;
    IF v_exists = 0 THEN
        INSERT INTO invoice (
            subscription_id, billing_period_start, billing_period_end,
            subtotal, tax_amount, total_amount, amount_paid, due_date, status, issued_at
        ) VALUES (
            v_sub_1, ADD_MONTHS(TRUNC(SYSDATE, 'MM'), -1), TRUNC(SYSDATE, 'MM') - 1,
            2499.00, 449.82, 2948.82, 0.00, TRUNC(SYSDATE, 'MM') - 5, 'ISSUED', ADD_MONTHS(SYSDATE, -1)
        ) RETURNING invoice_id INTO v_inv_prev;

        INSERT INTO payment (
            invoice_id, amount, payment_method, transaction_ref, payment_date
        ) VALUES (
            v_inv_prev, 2948.82, 'BANK_TRANSFER', v_ref_prev, ADD_MONTHS(SYSDATE, -1)
        );

        UPDATE invoice
           SET amount_paid = amount_paid + 2948.82
         WHERE invoice_id = v_inv_prev;
    END IF;

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Seeding complete. Current & previous month payments are ready.');
END;
/
