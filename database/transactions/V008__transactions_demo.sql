-- ============================================================
-- FILE    : V008__transactions_demo.sql
-- PURPOSE : Demonstrate transactions, savepoints, isolation
--           levels, and concurrency scenarios
-- AUTHOR  : ISP Dev Team
-- DATE    : 2026-10-26
-- ============================================================

-- ============================================================
-- 1. Basic Transaction: Safe payment recording
--    (mirrors what sp_record_payment does at DB level)
-- ============================================================
START TRANSACTION;

  UPDATE invoice SET amount_paid = amount_paid + 500.00 WHERE invoice_id = 7;
  INSERT INTO payment (invoice_id, amount, payment_method, payment_date)
  VALUES (7, 500.00, 'UPI', CURDATE());
  UPDATE invoice SET status = 'PARTIALLY_PAID' WHERE invoice_id = 7
    AND amount_paid < total_amount;

COMMIT;
-- If any step fails, the application calls ROLLBACK to undo all.

-- ============================================================
-- 2. SAVEPOINT: Create subscription with rollback to savepoint
--    If equipment assignment fails, roll back only that step,
--    not the whole subscription creation.
-- ============================================================
START TRANSACTION;

  -- Step 1: Create subscription
  INSERT INTO subscription (customer_id, plan_id, area_id, start_date, status)
  VALUES (5, 2, 4, CURDATE(), 'PENDING');
  SET @sub_id = LAST_INSERT_ID();
  SAVEPOINT after_subscription;

  -- Step 2: Create installation record
  INSERT INTO installation (subscription_id, status) VALUES (@sub_id, 'PENDING');
  SAVEPOINT after_installation;

  -- Step 3: Assign equipment (may fail if none available)
  UPDATE equipment SET subscription_id = @sub_id, status = 'ASSIGNED'
  WHERE equipment_id = 10 AND status = 'IN_STOCK';
  -- If 0 rows affected, roll back to savepoint and continue without equipment:
  -- ROLLBACK TO SAVEPOINT after_installation;

COMMIT;

-- ============================================================
-- 3. ISOLATION LEVEL DEMO
--    Run in Session A, then Session B to see different behaviors
-- ============================================================
-- SESSION A: Begin a transaction that reads invoice #1
SET TRANSACTION ISOLATION LEVEL READ COMMITTED; -- default
START TRANSACTION;
  SELECT amount_paid, status FROM invoice WHERE invoice_id = 1;
  -- Session B updates invoice #1 here (amount_paid changes)
  SELECT amount_paid, status FROM invoice WHERE invoice_id = 1;
  -- READ COMMITTED: sees Session B's committed change
  -- REPEATABLE READ: would see original value both times
COMMIT;

-- For REPEATABLE READ (MySQL default InnoDB):
SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;
START TRANSACTION;
  SELECT amount_paid FROM invoice WHERE invoice_id = 1;
  -- Session B commits a payment here
  SELECT amount_paid FROM invoice WHERE invoice_id = 1; -- same as first read
COMMIT;

-- ============================================================
-- 4. Lost Update Prevention using SELECT FOR UPDATE
--    Both sessions try to update the same invoice amount.
--    FOR UPDATE locks the row; Session B waits for Session A.
-- ============================================================
-- SESSION A:
START TRANSACTION;
  SELECT amount_paid FROM invoice WHERE invoice_id = 2 FOR UPDATE;
  -- Process payment
  UPDATE invoice SET amount_paid = amount_paid + 300 WHERE invoice_id = 2;
COMMIT;
-- Session B would block on SELECT FOR UPDATE until A commits.

-- ============================================================
-- 5. DEADLOCK DEMO (run concurrently in two sessions)
-- ============================================================
-- Session A: locks invoice 1 then tries invoice 2
-- START TRANSACTION;
-- UPDATE invoice SET status='OVERDUE' WHERE invoice_id = 1;
-- <wait>
-- UPDATE invoice SET status='OVERDUE' WHERE invoice_id = 2;
-- COMMIT;

-- Session B: locks invoice 2 then tries invoice 1
-- START TRANSACTION;
-- UPDATE invoice SET status='PAID' WHERE invoice_id = 2;
-- <wait>
-- UPDATE invoice SET status='PAID' WHERE invoice_id = 1;
-- COMMIT;
-- MySQL detects the deadlock, rolls back one session automatically.
-- In Java: catch SQLException, check SQLState = '40001' for deadlock.
