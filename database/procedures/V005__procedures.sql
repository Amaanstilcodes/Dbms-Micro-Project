-- ============================================================
-- FILE    : V005__procedures.sql
-- PURPOSE : Stored procedures and functions for core business logic
-- AUTHOR  : ISP Dev Team
-- DATE    : 2026-10-26
-- HOW TO CALL FROM JAVA: See CustomerDAO / InvoiceDAO for
--   CallableStatement examples. Pattern:
--   CallableStatement cs = conn.prepareCall("{CALL sp_name(?,?)}");
-- ============================================================

DELIMITER //

-- ============================================================
-- FUNCTION: fn_customer_outstanding_balance
-- Returns total unpaid amount across all invoices for a customer.
-- DEMONSTRATES: scalar user-defined function, subquery
-- JAVA CALL: {? = CALL fn_customer_outstanding_balance(?)}
-- ============================================================
CREATE FUNCTION fn_customer_outstanding_balance(p_customer_id INT)
RETURNS DECIMAL(12,2)
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE v_balance DECIMAL(12,2) DEFAULT 0.00;
    SELECT COALESCE(SUM(i.total_amount - i.amount_paid), 0.00)
    INTO v_balance
    FROM invoice i
    INNER JOIN subscription s ON i.subscription_id = s.subscription_id
    WHERE s.customer_id = p_customer_id
      AND i.status NOT IN ('PAID','CANCELLED');
    RETURN v_balance;
END //

-- ============================================================
-- FUNCTION: fn_calculate_late_fee
-- Calculates a 2% per month late fee on the outstanding amount.
-- DEMONSTRATES: date arithmetic, DATEDIFF in a function
-- ============================================================
CREATE FUNCTION fn_calculate_late_fee(p_invoice_id INT)
RETURNS DECIMAL(12,2)
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE v_outstanding  DECIMAL(12,2) DEFAULT 0.00;
    DECLARE v_due_date     DATE;
    DECLARE v_months_late  INT DEFAULT 0;
    DECLARE v_late_fee     DECIMAL(12,2) DEFAULT 0.00;

    SELECT (total_amount - amount_paid), due_date
    INTO v_outstanding, v_due_date
    FROM invoice WHERE invoice_id = p_invoice_id;

    IF CURDATE() > v_due_date AND v_outstanding > 0 THEN
        SET v_months_late = GREATEST(1, CEIL(DATEDIFF(CURDATE(), v_due_date) / 30.0));
        SET v_late_fee = ROUND(v_outstanding * 0.02 * v_months_late, 2);
    END IF;

    RETURN v_late_fee;
END //

-- ============================================================
-- PROCEDURE: sp_record_payment
-- Records a payment against an invoice and auto-updates invoice
-- status to PAID or PARTIALLY_PAID.
-- DEMONSTRATES: OUT parameter, transaction, UPDATE cascade
-- JAVA CALL: {CALL sp_record_payment(?,?,?,?,?,?)}
-- ============================================================
CREATE PROCEDURE sp_record_payment(
    IN  p_invoice_id      INT,
    IN  p_amount          DECIMAL(12,2),
    IN  p_method          VARCHAR(20),
    IN  p_transaction_ref VARCHAR(100),
    IN  p_recorded_by     INT,
    OUT p_result_msg      VARCHAR(255)
)
BEGIN
    DECLARE v_total       DECIMAL(12,2);
    DECLARE v_paid        DECIMAL(12,2);
    DECLARE v_new_paid    DECIMAL(12,2);
    DECLARE v_new_status  VARCHAR(30);
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_result_msg = 'ERROR: Payment recording failed.';
    END;

    START TRANSACTION;

    SELECT total_amount, amount_paid INTO v_total, v_paid
    FROM invoice WHERE invoice_id = p_invoice_id FOR UPDATE;

    IF v_total IS NULL THEN
        SET p_result_msg = 'ERROR: Invoice not found.';
        ROLLBACK;
    ELSEIF p_amount <= 0 THEN
        SET p_result_msg = 'ERROR: Payment amount must be > 0.';
        ROLLBACK;
    ELSE
        SET v_new_paid = v_paid + p_amount;
        IF v_new_paid >= v_total THEN
            SET v_new_status = 'PAID';
        ELSE
            SET v_new_status = 'PARTIALLY_PAID';
        END IF;

        INSERT INTO payment (invoice_id, amount, payment_method, transaction_ref, payment_date, recorded_by)
        VALUES (p_invoice_id, p_amount, p_method, p_transaction_ref, CURDATE(), p_recorded_by);

        UPDATE invoice
        SET amount_paid = v_new_paid, status = v_new_status, updated_at = NOW()
        WHERE invoice_id = p_invoice_id;

        COMMIT;
        SET p_result_msg = CONCAT('SUCCESS: Payment of ', p_amount, ' recorded. Status: ', v_new_status);
    END IF;
END //

-- ============================================================
-- PROCEDURE: sp_generate_monthly_invoices
-- Generates DRAFT invoices for all active subscriptions
-- that do not yet have an invoice for the given billing month.
-- DEMONSTRATES: CURSOR, LOOP, batch INSERT, date functions
-- JAVA CALL: {CALL sp_generate_monthly_invoices(?,?,?)}
-- ============================================================
CREATE PROCEDURE sp_generate_monthly_invoices(
    IN  p_billing_year    INT,
    IN  p_billing_month   INT,
    OUT p_invoices_created INT
)
BEGIN
    DECLARE done            INT DEFAULT FALSE;
    DECLARE v_sub_id        INT;
    DECLARE v_price         DECIMAL(12,2);
    DECLARE v_tax_rate      DECIMAL(5,4) DEFAULT 0.18;
    DECLARE v_period_start  DATE;
    DECLARE v_period_end    DATE;
    DECLARE v_tax           DECIMAL(12,2);
    DECLARE v_total         DECIMAL(12,2);
    DECLARE v_due_date      DATE;
    DECLARE v_count         INT DEFAULT 0;

    DECLARE cur CURSOR FOR
        SELECT s.subscription_id, p.price
        FROM subscription s
        INNER JOIN plan p ON s.plan_id = p.plan_id
        WHERE s.status = 'ACTIVE'
          AND NOT EXISTS (
              SELECT 1 FROM invoice i
              WHERE i.subscription_id = s.subscription_id
                AND YEAR(i.billing_period_start)  = p_billing_year
                AND MONTH(i.billing_period_start) = p_billing_month
          );

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    SET v_period_start = STR_TO_DATE(CONCAT(p_billing_year,'-',LPAD(p_billing_month,2,'0'),'-01'), '%Y-%m-%d');
    SET v_period_end   = LAST_DAY(v_period_start);
    SET v_due_date     = DATE_ADD(v_period_end, INTERVAL 15 DAY);
    SET p_invoices_created = 0;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO v_sub_id, v_price;
        IF done THEN LEAVE read_loop; END IF;

        SET v_tax   = ROUND(v_price * v_tax_rate, 2);
        SET v_total = v_price + v_tax;

        INSERT INTO invoice (subscription_id, billing_period_start, billing_period_end,
            subtotal, tax_amount, total_amount, amount_paid, due_date, status, issued_at)
        VALUES (v_sub_id, v_period_start, v_period_end,
            v_price, v_tax, v_total, 0.00, v_due_date, 'ISSUED', NOW());

        SET v_count = v_count + 1;
    END LOOP;
    CLOSE cur;

    SET p_invoices_created = v_count;
END //

-- ============================================================
-- PROCEDURE: sp_suspend_overdue_accounts
-- Suspends subscriptions with invoices overdue > 30 days.
-- DEMONSTRATES: multi-table UPDATE, business rule enforcement
-- JAVA CALL: {CALL sp_suspend_overdue_accounts(?)}
-- ============================================================
CREATE PROCEDURE sp_suspend_overdue_accounts(
    OUT p_suspended_count INT
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_suspended_count = -1;
    END;

    START TRANSACTION;

    UPDATE invoice
    SET status = 'OVERDUE', updated_at = NOW()
    WHERE status IN ('ISSUED','PARTIALLY_PAID')
      AND due_date < DATE_SUB(CURDATE(), INTERVAL 30 DAY);

    UPDATE subscription s
    SET s.status = 'SUSPENDED', s.updated_at = NOW()
    WHERE s.status = 'ACTIVE'
      AND EXISTS (
          SELECT 1 FROM invoice i
          WHERE i.subscription_id = s.subscription_id
            AND i.status = 'OVERDUE'
            AND i.due_date < DATE_SUB(CURDATE(), INTERVAL 30 DAY)
      );

    SELECT ROW_COUNT() INTO p_suspended_count;
    COMMIT;
END //

DELIMITER ;
