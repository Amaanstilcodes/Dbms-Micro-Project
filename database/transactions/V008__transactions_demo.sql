-- ============================================================
-- FILE    : V008__transactions_demo.sql
-- PURPOSE : Oracle transaction demos: COMMIT/ROLLBACK, SAVEPOINT,
--           isolation levels, SELECT FOR UPDATE, deadlock demo.
-- AUTHOR  : ISP Dev Team
-- DATE    : 2026-10-26
-- ============================================================

-- ============================================================
-- 1. Basic COMMIT / ROLLBACK
-- Oracle does NOT auto-commit DML. Every INSERT/UPDATE/DELETE
-- starts an implicit transaction; you must COMMIT or ROLLBACK.
-- ============================================================
-- Record a payment safely:
INSERT INTO payment (invoice_id, amount, payment_method, payment_date)
VALUES (1, 100, 'CASH', TRUNC(SYSDATE));

UPDATE invoice SET amount_paid = amount_paid + 100 WHERE invoice_id = 1;

COMMIT;   -- makes changes permanent and releases row locks
-- ROLLBACK;  -- would undo both statements

-- ============================================================
-- 2. SAVEPOINT: Partial rollback within a transaction
-- Use case: Create subscription (SAVEPOINT), then assign equipment.
-- If equipment step fails, rollback to SAVEPOINT (keep subscription).
-- ============================================================
-- Step 1: Insert subscription
INSERT INTO subscription (customer_id, plan_id, area_id, start_date, status)
VALUES (5, 2, 4, TRUNC(SYSDATE), 'PENDING');

SAVEPOINT after_subscription;   -- mark rollback point

-- Step 2: Insert installation
INSERT INTO installation (subscription_id, status)
VALUES (21, 'PENDING');         -- adjust ID as needed

SAVEPOINT after_installation;

-- Step 3: Assign equipment (might fail if none available)
-- UPDATE equipment SET subscription_id = 21, status = 'ASSIGNED'
-- WHERE equipment_id = 5 AND status = 'IN_STOCK';
-- If 0 rows updated: ROLLBACK TO SAVEPOINT after_installation;

COMMIT;  -- commits all steps that succeeded

-- ============================================================
-- 3. ISOLATION LEVELS
-- Oracle supports only READ COMMITTED (default) and SERIALIZABLE.
-- READ ONLY is a transaction-level mode, not an isolation level.
--
-- READ COMMITTED (default):
--   Each statement sees data committed BEFORE the statement began.
--   Non-repeatable reads and phantom reads are possible.
--
-- SERIALIZABLE:
--   The whole transaction sees data as of its start (snapshot).
--   ORA-08177 raised if a serializable tx tries to modify data
--   changed by another committed tx after this tx started.
--
-- READ ONLY:
--   Like SERIALIZABLE but disallows any DML in this transaction.
-- ============================================================

-- Set isolation level for current transaction:
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;   -- default
-- SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
-- SET TRANSACTION READ ONLY;   -- read-only snapshot

SELECT amount_paid FROM invoice WHERE invoice_id = 1;

-- Session B commits a change here in a real concurrency test.
-- With READ COMMITTED: this query would see B's committed change.
-- With SERIALIZABLE : this query sees original snapshot value.

ROLLBACK;  -- ends the transaction

-- ============================================================
-- 4. SELECT FOR UPDATE: Row-level lock to prevent lost updates
-- Both sessions try to update invoice #2.
-- Session A locks first; Session B WAITS (or NOWAIT raises error).
-- ============================================================
-- Session A:
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
SELECT amount_paid FROM invoice WHERE invoice_id = 2 FOR UPDATE;
-- Row is now locked; Session B cannot update it
UPDATE invoice SET amount_paid = amount_paid + 300 WHERE invoice_id = 2;
COMMIT;  -- lock released

-- Session B (after Session A commits):
SELECT amount_paid FROM invoice WHERE invoice_id = 2 FOR UPDATE NOWAIT;
-- NOWAIT raises ORA-00054 if row is locked, instead of blocking.
-- In Java: catch SQLException where sqlState.startsWith("61") or errorCode == 54

-- FOR UPDATE SKIP LOCKED: process only unlocked rows (useful in batch jobs)
SELECT invoice_id, total_amount - amount_paid AS outstanding
FROM   invoice
WHERE  status IN ('ISSUED','OVERDUE')
FOR UPDATE SKIP LOCKED;

-- ============================================================
-- 5. DEADLOCK DEMONSTRATION (run in two separate SQL*Plus sessions)
-- Oracle automatically detects deadlocks and rolls back one victim.
-- In Java: catch SQLException where getErrorCode() == 60 (ORA-00060)
-- ============================================================
-- SESSION A                    | SESSION B
-- UPDATE invoice               | UPDATE invoice
--   SET status='OVERDUE'       |   SET status='PAID'
--   WHERE invoice_id=1;        |   WHERE invoice_id=2;
--                              |
-- UPDATE invoice               | UPDATE invoice
--   SET status='OVERDUE'       |   SET status='PAID'
--   WHERE invoice_id=2; <WAIT> |   WHERE invoice_id=1; <WAIT>
--                              |
-- Oracle detects cycle, rolls back one session with ORA-00060.
-- Java code must retry the rolled-back transaction.
