-- ============================================================
-- FILE    : V005__pkg_billing.sql
-- PURPOSE : PL/SQL Package PKG_BILLING with procedures, functions,
--           cursors, exception handling, SYS_REFCURSOR output.
-- AUTHOR  : ISP Dev Team
-- DATE    : 2026-10-26
-- RUN AS  : isp_app@//localhost:1521/XEPDB1
-- ============================================================
SET SERVEROUTPUT ON SIZE UNLIMITED

-- ============================================================
-- PACKAGE SPEC
-- ============================================================
CREATE OR REPLACE PACKAGE pkg_billing AS

    -- Function: returns outstanding balance for a customer
    -- JAVA CALL:  {? = call pkg_billing.fn_customer_outstanding_balance(?)}
    FUNCTION fn_customer_outstanding_balance(p_customer_id IN NUMBER)
        RETURN NUMBER;

    -- Function: calculates 2% per month late fee on overdue invoice
    -- JAVA CALL:  {? = call pkg_billing.fn_calculate_late_fee(?)}
    FUNCTION fn_calculate_late_fee(p_invoice_id IN NUMBER)
        RETURN NUMBER;

    -- Procedure: record a payment, auto-update invoice status
    -- JAVA CALL:  {call pkg_billing.sp_record_payment(?,?,?,?,?,?)}
    PROCEDURE sp_record_payment(
        p_invoice_id      IN  NUMBER,
        p_amount          IN  NUMBER,
        p_method          IN  VARCHAR2,
        p_transaction_ref IN  VARCHAR2,
        p_recorded_by     IN  NUMBER,
        p_result_msg      OUT VARCHAR2
    );

    -- Procedure: generate monthly invoices for all active subscriptions
    -- JAVA CALL:  {call pkg_billing.sp_generate_monthly_invoices(?,?,?)}
    PROCEDURE sp_generate_monthly_invoices(
        p_billing_year    IN  NUMBER,
        p_billing_month   IN  NUMBER,
        p_invoices_created OUT NUMBER
    );

    -- Procedure: suspend accounts with invoices overdue > 30 days
    -- JAVA CALL:  {call pkg_billing.sp_suspend_overdue_accounts(?)}
    PROCEDURE sp_suspend_overdue_accounts(
        p_suspended_count OUT NUMBER
    );

    -- Procedure: return active subscriptions as SYS_REFCURSOR
    -- JAVA CALL:  {call pkg_billing.sp_get_active_subscriptions(?)}
    -- Java type:  OracleTypes.CURSOR registered with registerOutParameter
    PROCEDURE sp_get_active_subscriptions(
        p_cursor OUT SYS_REFCURSOR
    );

END pkg_billing;
/
SHOW ERRORS PACKAGE pkg_billing;

-- ============================================================
-- PACKAGE BODY
-- ============================================================
CREATE OR REPLACE PACKAGE BODY pkg_billing AS

    -- ===========================================================
    -- FUNCTION: fn_customer_outstanding_balance
    -- Uses implicit cursor (SELECT INTO) + NVL for null safety.
    -- ===========================================================
    FUNCTION fn_customer_outstanding_balance(p_customer_id IN NUMBER)
        RETURN NUMBER
    IS
        v_balance NUMBER := 0;
    BEGIN
        SELECT NVL(SUM(i.total_amount - i.amount_paid), 0)
        INTO   v_balance
        FROM   invoice i
        JOIN   subscription s ON i.subscription_id = s.subscription_id
        WHERE  s.customer_id = p_customer_id
          AND  i.status NOT IN ('PAID','CANCELLED');

        RETURN v_balance;

    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RETURN 0;
        WHEN OTHERS THEN
            RAISE_APPLICATION_ERROR(-20001,
                'fn_customer_outstanding_balance failed: ' || SQLERRM);
    END fn_customer_outstanding_balance;

    -- ===========================================================
    -- FUNCTION: fn_calculate_late_fee
    -- 2% per month, calculated from due date to today.
    -- DEMONSTRATES: DATE arithmetic with CEIL and MONTHS_BETWEEN.
    -- ===========================================================
    FUNCTION fn_calculate_late_fee(p_invoice_id IN NUMBER)
        RETURN NUMBER
    IS
        v_outstanding  NUMBER;
        v_due_date     DATE;
        v_months_late  NUMBER;
        v_late_fee     NUMBER := 0;
    BEGIN
        SELECT total_amount - amount_paid, due_date
        INTO   v_outstanding, v_due_date
        FROM   invoice
        WHERE  invoice_id = p_invoice_id;

        IF TRUNC(SYSDATE) > v_due_date AND v_outstanding > 0 THEN
            v_months_late := GREATEST(1, CEIL(MONTHS_BETWEEN(TRUNC(SYSDATE), v_due_date)));
            v_late_fee    := ROUND(v_outstanding * 0.02 * v_months_late, 2);
        END IF;

        RETURN v_late_fee;

    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20002, 'Invoice not found: ' || p_invoice_id);
        WHEN OTHERS THEN
            RAISE_APPLICATION_ERROR(-20003, 'fn_calculate_late_fee failed: ' || SQLERRM);
    END fn_calculate_late_fee;

    -- ===========================================================
    -- PROCEDURE: sp_record_payment
    -- DEMONSTRATES: SELECT FOR UPDATE, autonomous DML, OUT param,
    --   EXCEPTION block, RAISE_APPLICATION_ERROR for -20xxx codes.
    -- ===========================================================
    PROCEDURE sp_record_payment(
        p_invoice_id      IN  NUMBER,
        p_amount          IN  NUMBER,
        p_method          IN  VARCHAR2,
        p_transaction_ref IN  VARCHAR2,
        p_recorded_by     IN  NUMBER,
        p_result_msg      OUT VARCHAR2
    )
    IS
        v_total       NUMBER;
        v_paid        NUMBER;
        v_new_paid    NUMBER;
        v_new_status  VARCHAR2(20);
        e_not_found   EXCEPTION;
        e_bad_amount  EXCEPTION;
    BEGIN
        -- Lock the invoice row to prevent concurrent updates
        SELECT total_amount, amount_paid
        INTO   v_total, v_paid
        FROM   invoice
        WHERE  invoice_id = p_invoice_id
        FOR UPDATE NOWAIT;

        IF p_amount <= 0 THEN
            RAISE e_bad_amount;
        END IF;

        v_new_paid := v_paid + p_amount;

        IF v_new_paid >= v_total THEN
            v_new_status := 'PAID';
        ELSE
            v_new_status := 'PARTIALLY_PAID';
        END IF;

        INSERT INTO payment (invoice_id, amount, payment_method,
                             transaction_ref, payment_date, recorded_by)
        VALUES (p_invoice_id, p_amount, p_method,
                p_transaction_ref, TRUNC(SYSDATE), p_recorded_by);

        UPDATE invoice
           SET amount_paid = v_new_paid,
               status      = v_new_status,
               updated_at  = SYSDATE
         WHERE invoice_id  = p_invoice_id;

        p_result_msg := 'SUCCESS: Payment of ' || p_amount ||
                        ' recorded. Invoice status: ' || v_new_status;
        -- Caller owns COMMIT/ROLLBACK (service layer responsibility)

    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            p_result_msg := 'ERROR: Invoice ' || p_invoice_id || ' not found.';
        WHEN e_bad_amount THEN
            p_result_msg := 'ERROR: Payment amount must be > 0.';
        WHEN OTHERS THEN
            -- Wrap Oracle error and surface to Java as -20010
            RAISE_APPLICATION_ERROR(-20010,
                'sp_record_payment failed for invoice ' ||
                p_invoice_id || ': ' || SQLERRM);
    END sp_record_payment;

    -- ===========================================================
    -- PROCEDURE: sp_generate_monthly_invoices
    -- DEMONSTRATES: explicit cursor, cursor FOR loop, batch INSERT,
    --   ADD_MONTHS date function, OUT parameter.
    -- ===========================================================
    PROCEDURE sp_generate_monthly_invoices(
        p_billing_year    IN  NUMBER,
        p_billing_month   IN  NUMBER,
        p_invoices_created OUT NUMBER
    )
    IS
        v_period_start DATE;
        v_period_end   DATE;
        v_due_date     DATE;
        v_tax          NUMBER;
        v_total        NUMBER;
        v_tax_rate     CONSTANT NUMBER := 0.18;
        v_count        NUMBER := 0;

        -- Explicit cursor: active subscriptions without an invoice this month
        CURSOR c_subs IS
            SELECT s.subscription_id, p.price
            FROM   subscription s
            JOIN   plan p ON s.plan_id = p.plan_id
            WHERE  s.status = 'ACTIVE'
              AND  NOT EXISTS (
                SELECT 1 FROM invoice i
                WHERE  i.subscription_id = s.subscription_id
                  AND  EXTRACT(YEAR  FROM i.billing_period_start) = p_billing_year
                  AND  EXTRACT(MONTH FROM i.billing_period_start) = p_billing_month
              );
    BEGIN
        v_period_start := TO_DATE(p_billing_year || '-' ||
                          LPAD(p_billing_month,2,'0') || '-01', 'YYYY-MM-DD');
        v_period_end   := LAST_DAY(v_period_start);
        v_due_date     := ADD_MONTHS(v_period_end, 0) + 15;

        -- Cursor FOR loop: Oracle automatically opens, fetches, and closes cursor
        FOR rec IN c_subs LOOP
            v_tax   := ROUND(rec.price * v_tax_rate, 2);
            v_total := rec.price + v_tax;

            INSERT INTO invoice (
                subscription_id, billing_period_start, billing_period_end,
                subtotal, tax_amount, total_amount, amount_paid,
                due_date, status, issued_at)
            VALUES (
                rec.subscription_id, v_period_start, v_period_end,
                rec.price, v_tax, v_total, 0,
                v_due_date, 'ISSUED', SYSDATE);

            v_count := v_count + 1;
        END LOOP;

        p_invoices_created := v_count;
        DBMS_OUTPUT.PUT_LINE('Invoices generated: ' || v_count);

    EXCEPTION
        WHEN OTHERS THEN
            RAISE_APPLICATION_ERROR(-20020,
                'sp_generate_monthly_invoices failed: ' || SQLERRM);
    END sp_generate_monthly_invoices;

    -- ===========================================================
    -- PROCEDURE: sp_suspend_overdue_accounts
    -- DEMONSTRATES: bulk UPDATE, implicit cursor attributes %ROWCOUNT
    -- ===========================================================
    PROCEDURE sp_suspend_overdue_accounts(
        p_suspended_count OUT NUMBER
    )
    IS
    BEGIN
        -- Mark invoices as OVERDUE if they are past the 30-day grace period
        UPDATE invoice
           SET status     = 'OVERDUE',
               updated_at = SYSDATE
         WHERE status IN ('ISSUED','PARTIALLY_PAID')
           AND due_date < TRUNC(SYSDATE) - 30;

        DBMS_OUTPUT.PUT_LINE('Invoices marked OVERDUE: ' || SQL%ROWCOUNT);

        -- Suspend subscriptions linked to overdue invoices
        UPDATE subscription s
           SET s.status     = 'SUSPENDED',
               s.updated_at = SYSDATE
         WHERE s.status = 'ACTIVE'
           AND EXISTS (
               SELECT 1 FROM invoice i
               WHERE  i.subscription_id = s.subscription_id
                 AND  i.status = 'OVERDUE'
                 AND  i.due_date < TRUNC(SYSDATE) - 30
           );

        p_suspended_count := SQL%ROWCOUNT;
        DBMS_OUTPUT.PUT_LINE('Subscriptions suspended: ' || p_suspended_count);

    EXCEPTION
        WHEN OTHERS THEN
            RAISE_APPLICATION_ERROR(-20030,
                'sp_suspend_overdue_accounts failed: ' || SQLERRM);
    END sp_suspend_overdue_accounts;

    -- ===========================================================
    -- PROCEDURE: sp_get_active_subscriptions
    -- DEMONSTRATES: SYS_REFCURSOR OUT parameter.
    -- Java: cs.registerOutParameter(1, OracleTypes.CURSOR);
    --       ResultSet rs = (ResultSet) cs.getObject(1);
    -- ===========================================================
    PROCEDURE sp_get_active_subscriptions(
        p_cursor OUT SYS_REFCURSOR
    )
    IS
    BEGIN
        OPEN p_cursor FOR
            SELECT s.subscription_id,
                   c.first_name || ' ' || c.last_name AS customer_name,
                   p.plan_name, p.price, s.start_date, s.status
            FROM   subscription s
            JOIN   customer c  ON s.customer_id = c.customer_id
            JOIN   plan p      ON s.plan_id     = p.plan_id
            WHERE  s.status = 'ACTIVE'
            ORDER  BY s.subscription_id;

    EXCEPTION
        WHEN OTHERS THEN
            RAISE_APPLICATION_ERROR(-20040,
                'sp_get_active_subscriptions failed: ' || SQLERRM);
    END sp_get_active_subscriptions;

END pkg_billing;
/
SHOW ERRORS PACKAGE BODY pkg_billing;

PROMPT PKG_BILLING created successfully.

/*
=== HOW TO CALL FROM JAVA (CallableStatement examples) ===

1. Function with return value:
   String sql = "{? = call pkg_billing.fn_customer_outstanding_balance(?)}";
   try (CallableStatement cs = conn.prepareCall(sql)) {
       cs.registerOutParameter(1, Types.NUMERIC);
       cs.setInt(2, customerId);
       cs.execute();
       BigDecimal balance = cs.getBigDecimal(1);
   }

2. Procedure with OUT param:
   String sql = "{call pkg_billing.sp_record_payment(?,?,?,?,?,?)}";
   try (CallableStatement cs = conn.prepareCall(sql)) {
       cs.setInt(1, invoiceId);
       cs.setBigDecimal(2, amount);
       cs.setString(3, method);
       cs.setString(4, ref);
       cs.setObject(5, Types.NULL);
       cs.registerOutParameter(6, Types.VARCHAR);
       cs.execute();
       String msg = cs.getString(6);
   }

3. Procedure with SYS_REFCURSOR:
   String sql = "{call pkg_billing.sp_get_active_subscriptions(?)}";
   try (CallableStatement cs = conn.prepareCall(sql)) {
       cs.registerOutParameter(1, OracleTypes.CURSOR);
       cs.execute();
       try (ResultSet rs = (ResultSet) cs.getObject(1)) {
           while (rs.next()) { ... }
       }
   }
*/
