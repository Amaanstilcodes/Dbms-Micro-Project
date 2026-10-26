-- ============================================================
-- FILE    : V002__indexes.sql
-- PURPOSE : Performance indexes for all frequently queried columns
-- AUTHOR  : ISP Dev Team
-- DATE    : 2026-10-26
-- ============================================================

-- customer searches by email, phone, status
CREATE INDEX idx_customer_email   ON customer(email);
CREATE INDEX idx_customer_phone   ON customer(phone);
CREATE INDEX idx_customer_status  ON customer(status);
CREATE INDEX idx_customer_area    ON customer(area_id);

-- subscription lookups by customer, plan, status
CREATE INDEX idx_sub_customer     ON subscription(customer_id);
CREATE INDEX idx_sub_plan         ON subscription(plan_id);
CREATE INDEX idx_sub_status       ON subscription(status);
CREATE INDEX idx_sub_start_date   ON subscription(start_date);

-- invoice lookups by subscription, status, due date
CREATE INDEX idx_inv_sub          ON invoice(subscription_id);
CREATE INDEX idx_inv_status       ON invoice(status);
CREATE INDEX idx_inv_due_date     ON invoice(due_date);
-- Composite: overdue detection query  (status, due_date)
CREATE INDEX idx_inv_overdue      ON invoice(status, due_date);

-- payment lookups by invoice and date
CREATE INDEX idx_pay_invoice      ON payment(invoice_id);
CREATE INDEX idx_pay_date         ON payment(payment_date);

-- usage_record range scans per subscription and date
CREATE INDEX idx_usage_sub_date   ON usage_record(subscription_id, usage_date);

-- support ticket lookups
CREATE INDEX idx_ticket_customer  ON support_ticket(customer_id);
CREATE INDEX idx_ticket_status    ON support_ticket(status);
CREATE INDEX idx_ticket_priority  ON support_ticket(priority);
CREATE INDEX idx_ticket_assigned  ON support_ticket(assigned_to);

-- equipment by status for inventory queries
CREATE INDEX idx_equip_status     ON equipment(status);
CREATE INDEX idx_equip_sub        ON equipment(subscription_id);

-- audit_log queries by table and date
CREATE INDEX idx_audit_table_date ON audit_log(table_name, changed_at);

-- network_outage by area and time
CREATE INDEX idx_outage_area      ON network_outage(area_id, start_time);

/*
  EXPLAIN EXAMPLES:
  -- This query uses idx_inv_overdue (status, due_date composite):
  EXPLAIN SELECT * FROM invoice WHERE status = 'ISSUED' AND due_date < CURDATE();

  -- This uses idx_usage_sub_date for monthly aggregation:
  EXPLAIN SELECT subscription_id, SUM(data_used_mb)
          FROM usage_record
          WHERE subscription_id = 1 AND usage_date BETWEEN '2025-01-01' AND '2025-01-31'
          GROUP BY subscription_id;
*/
