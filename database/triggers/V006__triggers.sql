-- ============================================================
-- FILE    : V006__triggers.sql
-- PURPOSE : Audit logging, business rule enforcement triggers
-- AUTHOR  : ISP Dev Team
-- DATE    : 2026-10-26
-- ============================================================

DELIMITER //

-- ============================================================
-- TRIGGER: trg_customer_audit_update
-- Logs customer updates to audit_log as JSON snapshots.
-- DEMONSTRATES: AFTER UPDATE trigger, JSON_OBJECT, audit pattern
-- ============================================================
CREATE TRIGGER trg_customer_audit_update
AFTER UPDATE ON customer
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (table_name, operation, record_id, old_values, new_values, changed_by)
    VALUES (
        'customer',
        'UPDATE',
        OLD.customer_id,
        JSON_OBJECT(
            'email',  OLD.email,
            'phone',  OLD.phone,
            'status', OLD.status
        ),
        JSON_OBJECT(
            'email',  NEW.email,
            'phone',  NEW.phone,
            'status', NEW.status
        ),
        USER()
    );
END //

-- ============================================================
-- TRIGGER: trg_customer_audit_delete
-- Prevents deletion of customers with outstanding invoices.
-- DEMONSTRATES: BEFORE DELETE trigger with SIGNAL (app-level
--   guard; also enforced via FK RESTRICT in the DAO).
-- ============================================================
CREATE TRIGGER trg_customer_audit_delete
BEFORE DELETE ON customer
FOR EACH ROW
BEGIN
    DECLARE v_outstanding DECIMAL(12,2);
    SET v_outstanding = fn_customer_outstanding_balance(OLD.customer_id);
    IF v_outstanding > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Cannot delete customer with outstanding balance. Clear invoices first.';
    END IF;
    INSERT INTO audit_log (table_name, operation, record_id, old_values, changed_by)
    VALUES ('customer', 'DELETE', OLD.customer_id,
        JSON_OBJECT('email', OLD.email, 'status', OLD.status), USER());
END //

-- ============================================================
-- TRIGGER: trg_payment_audit
-- Logs every payment insert to audit_log.
-- DEMONSTRATES: AFTER INSERT trigger
-- ============================================================
CREATE TRIGGER trg_payment_audit
AFTER INSERT ON payment
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (table_name, operation, record_id, new_values, changed_by)
    VALUES (
        'payment',
        'INSERT',
        NEW.payment_id,
        JSON_OBJECT(
            'invoice_id', NEW.invoice_id,
            'amount',     NEW.amount,
            'method',     NEW.payment_method,
            'ref',        NEW.transaction_ref
        ),
        USER()
    );
END //

-- ============================================================
-- TRIGGER: trg_plan_audit_update
-- Audits plan price changes.
-- ============================================================
CREATE TRIGGER trg_plan_audit_update
AFTER UPDATE ON plan
FOR EACH ROW
BEGIN
    IF OLD.price != NEW.price THEN
        INSERT INTO audit_log (table_name, operation, record_id, old_values, new_values, changed_by)
        VALUES ('plan', 'UPDATE', OLD.plan_id,
            JSON_OBJECT('price', OLD.price),
            JSON_OBJECT('price', NEW.price),
            USER());
    END IF;
END //

-- ============================================================
-- TRIGGER: trg_equipment_prevent_double_assign
-- Prevents assigning equipment that is already assigned to
-- a DIFFERENT active subscription.
-- DEMONSTRATES: BEFORE UPDATE business rule with SIGNAL
-- ============================================================
CREATE TRIGGER trg_equipment_prevent_double_assign
BEFORE UPDATE ON equipment
FOR EACH ROW
BEGIN
    IF NEW.subscription_id IS NOT NULL
       AND OLD.subscription_id IS NOT NULL
       AND NEW.subscription_id != OLD.subscription_id THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Equipment is already assigned to another subscription. Return it first.';
    END IF;
END //

-- ============================================================
-- TRIGGER: trg_invoice_status_update
-- After payment is inserted, re-evaluate invoice status.
-- (Mirrors sp_record_payment logic for direct inserts.)
-- DEMONSTRATES: AFTER INSERT trigger updating a related table
-- ============================================================
CREATE TRIGGER trg_invoice_status_update
AFTER INSERT ON payment
FOR EACH ROW
BEGIN
    DECLARE v_total   DECIMAL(12,2);
    DECLARE v_paid    DECIMAL(12,2);

    SELECT total_amount, amount_paid INTO v_total, v_paid
    FROM invoice WHERE invoice_id = NEW.invoice_id;

    IF v_paid >= v_total THEN
        UPDATE invoice SET status = 'PAID', updated_at = NOW()
        WHERE invoice_id = NEW.invoice_id;
    ELSEIF v_paid > 0 THEN
        UPDATE invoice SET status = 'PARTIALLY_PAID', updated_at = NOW()
        WHERE invoice_id = NEW.invoice_id;
    END IF;
END //

DELIMITER ;
