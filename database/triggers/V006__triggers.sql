-- ============================================================
-- FILE    : V006__triggers.sql
-- PURPOSE : Oracle PL/SQL triggers for audit, business rules,
--           and cascade logic. Includes compound trigger pattern
--           to avoid ORA-04091 mutating table error.
-- AUTHOR  : ISP Dev Team
-- DATE    : 2026-10-26
-- RUN AS  : isp_app@//localhost:1521/FREEPDB1
-- ============================================================
SET SERVEROUTPUT ON SIZE UNLIMITED

-- ============================================================
-- TRIGGER: trg_customer_updated_at
-- BEFORE UPDATE: auto-stamp updated_at on every row change.
-- DEMONSTRATES: BEFORE row-level trigger, :NEW pseudo-record.
-- ============================================================
CREATE OR REPLACE TRIGGER trg_customer_updated_at
BEFORE UPDATE ON customer
FOR EACH ROW
BEGIN
    :NEW.updated_at := SYSDATE;
END;
/
SHOW ERRORS TRIGGER trg_customer_updated_at;

-- ============================================================
-- TRIGGER: trg_customer_audit
-- AFTER INSERT/UPDATE/DELETE: writes to AUDIT_LOG.
-- DEMONSTRATES: AFTER trigger, :OLD and :NEW, USER pseudocolumn,
--   compound DML trigger (INSERT/UPDATE/DELETE in one block).
-- ============================================================
CREATE OR REPLACE TRIGGER trg_customer_audit
AFTER INSERT OR UPDATE OR DELETE ON customer
FOR EACH ROW
DECLARE
    v_op        VARCHAR2(10);
    v_old_vals  CLOB := NULL;
    v_new_vals  CLOB := NULL;
    v_rec_id    VARCHAR2(100);
BEGIN
    IF INSERTING THEN
        v_op     := 'INSERT';
        v_rec_id := TO_CHAR(:NEW.customer_id);
        v_new_vals := '{"email":"' || :NEW.email ||
                      '","status":"'|| :NEW.status || '"}';
    ELSIF UPDATING THEN
        v_op     := 'UPDATE';
        v_rec_id := TO_CHAR(:OLD.customer_id);
        v_old_vals := '{"email":"' || :OLD.email ||
                      '","status":"'|| :OLD.status || '"}';
        v_new_vals := '{"email":"' || :NEW.email ||
                      '","status":"'|| :NEW.status || '"}';
    ELSIF DELETING THEN
        v_op     := 'DELETE';
        v_rec_id := TO_CHAR(:OLD.customer_id);
        v_old_vals := '{"email":"' || :OLD.email ||
                      '","status":"'|| :OLD.status || '"}';
    END IF;

    INSERT INTO audit_log (table_name, operation, record_id,
                           old_values, new_values, changed_by)
    VALUES ('CUSTOMER', v_op, v_rec_id, v_old_vals, v_new_vals, USER);

EXCEPTION
    WHEN OTHERS THEN
        -- Audit failure must never block the main transaction
        NULL;
END;
/
SHOW ERRORS TRIGGER trg_customer_audit;

-- ============================================================
-- TRIGGER: trg_payment_audit
-- AFTER INSERT on PAYMENT: logs payment to AUDIT_LOG.
-- ============================================================
CREATE OR REPLACE TRIGGER trg_payment_audit
AFTER INSERT ON payment
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (table_name, operation, record_id, new_values, changed_by)
    VALUES ('PAYMENT', 'INSERT', TO_CHAR(:NEW.payment_id),
            '{"invoice_id":' || :NEW.invoice_id  ||
            ',"amount":'     || :NEW.amount       ||
            ',"method":"'    || :NEW.payment_method || '"}',
            USER);
EXCEPTION
    WHEN OTHERS THEN NULL;
END;
/
SHOW ERRORS TRIGGER trg_payment_audit;

-- ============================================================
-- TRIGGER: trg_plan_price_audit
-- AFTER UPDATE on PLAN when price changes: audits price changes.
-- ============================================================
CREATE OR REPLACE TRIGGER trg_plan_price_audit
AFTER UPDATE OF price ON plan
FOR EACH ROW
WHEN (OLD.price != NEW.price)    -- WHEN clause avoids PL/SQL overhead
BEGIN
    INSERT INTO audit_log (table_name, operation, record_id,
                           old_values, new_values, changed_by)
    VALUES ('PLAN', 'UPDATE', TO_CHAR(:OLD.plan_id),
            '{"price":' || :OLD.price || '}',
            '{"price":' || :NEW.price || '}',
            USER);
EXCEPTION
    WHEN OTHERS THEN NULL;
END;
/
SHOW ERRORS TRIGGER trg_plan_price_audit;

-- ============================================================
-- TRIGGER: trg_prevent_delete_customer_with_balance
-- BEFORE DELETE on CUSTOMER: blocks deletion if unpaid invoices exist.
-- DEMONSTRATES: BEFORE DELETE, RAISE_APPLICATION_ERROR.
-- ============================================================
CREATE OR REPLACE TRIGGER trg_prevent_delete_customer_with_balance
BEFORE DELETE ON customer
FOR EACH ROW
DECLARE
    v_balance NUMBER;
BEGIN
    SELECT NVL(SUM(i.total_amount - i.amount_paid), 0)
    INTO   v_balance
    FROM   invoice i
    JOIN   subscription s ON i.subscription_id = s.subscription_id
    WHERE  s.customer_id = :OLD.customer_id
      AND  i.status NOT IN ('PAID','CANCELLED');

    IF v_balance > 0 THEN
        RAISE_APPLICATION_ERROR(-20050,
            'Cannot delete customer ' || :OLD.customer_id ||
            '. Outstanding balance: ' || v_balance ||
            '. Clear all invoices first.');
    END IF;
END;
/
SHOW ERRORS TRIGGER trg_prevent_delete_customer_with_balance;

-- ============================================================
-- TRIGGER: trg_equipment_prevent_double_assign
-- BEFORE UPDATE on EQUIPMENT: prevents assigning already-assigned equipment.
-- DEMONSTRATES: business-rule enforcement trigger.
-- ============================================================
CREATE OR REPLACE TRIGGER trg_equipment_prevent_double_assign
BEFORE UPDATE OF subscription_id ON equipment
FOR EACH ROW
BEGIN
    IF :NEW.subscription_id IS NOT NULL
       AND :OLD.subscription_id IS NOT NULL
       AND :NEW.subscription_id != :OLD.subscription_id
    THEN
        RAISE_APPLICATION_ERROR(-20051,
            'Equipment ' || :OLD.serial_number ||
            ' is already assigned to subscription ' ||
            :OLD.subscription_id || '. Return it first.');
    END IF;
END;
/
SHOW ERRORS TRIGGER trg_equipment_prevent_double_assign;

-- ============================================================
-- COMPOUND TRIGGER: trg_invoice_status_compound
-- Updates invoice.amount_paid and status AFTER payment INSERT.
--
-- WHY COMPOUND TRIGGER?
--   A simple AFTER INSERT on PAYMENT that queries/updates INVOICE
--   would cause ORA-04091 (mutating table error) if INVOICE also
--   had an active trigger. The compound trigger defers the DML
--   to the AFTER STATEMENT section, by which time the triggering
--   statement has completed and no mutation occurs.
--
-- Oracle compound trigger structure:
--   COMPOUND TRIGGER
--     BEFORE STATEMENT   -- runs once before the whole DML
--     BEFORE EACH ROW    -- runs per row before change
--     AFTER EACH ROW     -- runs per row after change (collect data here)
--     AFTER STATEMENT    -- runs once after all rows (do DML here)
--   END;
-- ============================================================
CREATE OR REPLACE TRIGGER trg_invoice_status_compound
FOR INSERT ON payment
COMPOUND TRIGGER

    -- Package-level collection to hold invoice IDs from per-row section
    TYPE t_invoice_tab IS TABLE OF NUMBER INDEX BY PLS_INTEGER;
    g_invoice_ids t_invoice_tab;
    g_idx         PLS_INTEGER := 0;

    -- Collect invoice IDs affected by each inserted payment row
    AFTER EACH ROW IS
    BEGIN
        g_idx := g_idx + 1;
        g_invoice_ids(g_idx) := :NEW.invoice_id;
    END AFTER EACH ROW;

    -- After all rows inserted, update invoice statuses in bulk
    AFTER STATEMENT IS
        v_total  NUMBER;
        v_paid   NUMBER;
        v_status VARCHAR2(20);
    BEGIN
        FOR i IN 1 .. g_idx LOOP
            SELECT total_amount, amount_paid
            INTO   v_total, v_paid
            FROM   invoice
            WHERE  invoice_id = g_invoice_ids(i);

            IF v_paid >= v_total THEN
                v_status := 'PAID';
            ELSIF v_paid > 0 THEN
                v_status := 'PARTIALLY_PAID';
            ELSE
                v_status := 'ISSUED';
            END IF;

            UPDATE invoice
               SET status     = v_status,
                   updated_at = SYSDATE
             WHERE invoice_id = g_invoice_ids(i);
        END LOOP;
    END AFTER STATEMENT;

END trg_invoice_status_compound;
/
SHOW ERRORS TRIGGER trg_invoice_status_compound;

-- ============================================================
-- TRIGGER: trg_subscription_updated_at
-- BEFORE UPDATE: auto-stamp updated_at on subscription changes.
-- ============================================================
CREATE OR REPLACE TRIGGER trg_subscription_updated_at
BEFORE UPDATE ON subscription
FOR EACH ROW
BEGIN
    :NEW.updated_at := SYSDATE;
END;
/
SHOW ERRORS TRIGGER trg_subscription_updated_at;

PROMPT All triggers created successfully.
