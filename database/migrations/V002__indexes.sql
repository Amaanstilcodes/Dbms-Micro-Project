-- ============================================================
-- FILE    : V002__indexes.sql
-- PURPOSE : Performance indexes for Oracle ISP schema.
--           Each index annotated with the query it accelerates.
-- AUTHOR  : ISP Dev Team
-- DATE    : 2026-10-26
-- RUN AS  : isp_app@//localhost:1521/FREEPDB1
-- ============================================================
SET SERVEROUTPUT ON SIZE UNLIMITED

-- CUSTOMER searches (email lookup on login already indexed by uq_customer_email UNIQUE constraint, status filter for active list)
-- CREATE INDEX idx_customer_email   ON customer(email); -- ORA-01408: already indexed by uq_customer_email
CREATE INDEX idx_customer_status  ON customer(status);
CREATE INDEX idx_customer_area    ON customer(area_id);

-- SUBSCRIPTION range scans (most queries filter by status or customer)
CREATE INDEX idx_sub_customer     ON subscription(customer_id);
CREATE INDEX idx_sub_plan         ON subscription(plan_id);
CREATE INDEX idx_sub_status       ON subscription(status);
CREATE INDEX idx_sub_start_date   ON subscription(start_date);

-- INVOICE overdue detection: composite (status, due_date) avoids full scan
-- Query: WHERE status IN ('ISSUED','PARTIALLY_PAID') AND due_date < SYSDATE
CREATE INDEX idx_inv_sub          ON invoice(subscription_id);
CREATE INDEX idx_inv_status       ON invoice(status);
CREATE INDEX idx_inv_due_date     ON invoice(due_date);
CREATE INDEX idx_inv_overdue      ON invoice(status, due_date);

-- PAYMENT lookups by invoice and date for reconciliation reports
CREATE INDEX idx_pay_invoice      ON payment(invoice_id);
CREATE INDEX idx_pay_date         ON payment(payment_date);

-- USAGE_RECORD monthly aggregation: (subscription_id, usage_date) is already indexed by uq_usage_daily UNIQUE constraint
-- CREATE INDEX idx_usage_sub_date   ON usage_record(subscription_id, usage_date); -- ORA-01408: indexed by uq_usage_daily

-- SUPPORT_TICKET assignment and status queries
CREATE INDEX idx_ticket_customer  ON support_ticket(customer_id);
CREATE INDEX idx_ticket_status    ON support_ticket(status);
CREATE INDEX idx_ticket_priority  ON support_ticket(priority);
CREATE INDEX idx_ticket_assigned  ON support_ticket(assigned_to);

-- EQUIPMENT inventory filter by status
CREATE INDEX idx_equip_status     ON equipment(status);
CREATE INDEX idx_equip_sub        ON equipment(subscription_id);

-- AUDIT_LOG queries by table name and date for compliance reports
CREATE INDEX idx_audit_table_date ON audit_log(table_name, changed_at);

-- NETWORK_OUTAGE lookup by area and time window
CREATE INDEX idx_outage_area      ON network_outage(area_id, start_time);

COMMIT;
PROMPT Indexes created.

-- ============================================================
-- EXPLAIN PLAN EXAMPLES (run as isp_app after loading data)
-- ============================================================
/*
-- Example 1: Invoice overdue query -- should use idx_inv_overdue
EXPLAIN PLAN FOR
  SELECT i.invoice_id, i.total_amount - i.amount_paid AS outstanding
  FROM   invoice i
  WHERE  i.status IN ('ISSUED','PARTIALLY_PAID')
  AND    i.due_date < SYSDATE;

SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY);

-- Example 2: Usage aggregation -- should use idx_usage_sub_date
EXPLAIN PLAN FOR
  SELECT subscription_id, SUM(data_used_mb) AS total_mb
  FROM   usage_record
  WHERE  subscription_id = 1
  AND    usage_date BETWEEN TO_DATE('2025-01-01','YYYY-MM-DD')
                        AND TO_DATE('2025-01-31','YYYY-MM-DD')
  GROUP BY subscription_id;

SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY);
*/
