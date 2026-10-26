-- ============================================================
-- FILE    : V004__views.sql
-- PURPOSE : Reusable views for common business queries
-- AUTHOR  : ISP Dev Team
-- DATE    : 2026-10-26
-- ============================================================

-- ============================================================
-- VIEW: v_active_subscriptions
-- All currently active subscriptions with customer and plan details
-- DEMONSTRATES: INNER JOIN across 3 tables
-- ============================================================
CREATE OR REPLACE VIEW v_active_subscriptions AS
SELECT
    s.subscription_id,
    s.start_date,
    s.status                        AS subscription_status,
    c.customer_id,
    CONCAT(c.first_name,' ',c.last_name) AS customer_name,
    c.email,
    c.phone,
    p.plan_name,
    p.plan_type,
    p.speed_mbps,
    p.price                         AS monthly_price,
    sa.area_name,
    sa.city,
    sa.pin_code
FROM subscription s
INNER JOIN customer c   ON s.customer_id = c.customer_id
INNER JOIN plan p       ON s.plan_id     = p.plan_id
INNER JOIN service_area sa ON s.area_id = sa.area_id
WHERE s.status = 'ACTIVE';

-- ============================================================
-- VIEW: v_overdue_invoices
-- Invoices past due date and not fully paid
-- DEMONSTRATES: LEFT JOIN, date comparison
-- ============================================================
CREATE OR REPLACE VIEW v_overdue_invoices AS
SELECT
    i.invoice_id,
    i.subscription_id,
    CONCAT(c.first_name,' ',c.last_name) AS customer_name,
    c.email,
    c.phone,
    p.plan_name,
    i.total_amount,
    i.amount_paid,
    (i.total_amount - i.amount_paid)    AS outstanding,
    i.due_date,
    DATEDIFF(CURDATE(), i.due_date)     AS days_overdue,
    i.status
FROM invoice i
INNER JOIN subscription s   ON i.subscription_id = s.subscription_id
INNER JOIN customer c       ON s.customer_id     = c.customer_id
INNER JOIN plan p           ON s.plan_id         = p.plan_id
WHERE i.status IN ('ISSUED','PARTIALLY_PAID','OVERDUE')
  AND i.due_date < CURDATE()
ORDER BY days_overdue DESC;

-- ============================================================
-- VIEW: v_customer_balance
-- Outstanding balance per customer across all invoices
-- DEMONSTRATES: aggregation (SUM), GROUP BY
-- ============================================================
CREATE OR REPLACE VIEW v_customer_balance AS
SELECT
    c.customer_id,
    CONCAT(c.first_name,' ',c.last_name) AS customer_name,
    c.email,
    COUNT(i.invoice_id)                  AS total_invoices,
    SUM(i.total_amount)                  AS total_billed,
    SUM(i.amount_paid)                   AS total_paid,
    SUM(i.total_amount - i.amount_paid)  AS outstanding_balance
FROM customer c
LEFT JOIN subscription s ON c.customer_id = s.customer_id
LEFT JOIN invoice i      ON s.subscription_id = i.subscription_id
GROUP BY c.customer_id, c.first_name, c.last_name, c.email;

-- ============================================================
-- VIEW: v_monthly_revenue
-- Revenue aggregated by month and plan type
-- DEMONSTRATES: DATE functions, GROUP BY, aggregation
-- ============================================================
CREATE OR REPLACE VIEW v_monthly_revenue AS
SELECT
    DATE_FORMAT(pay.payment_date, '%Y-%m')  AS revenue_month,
    p.plan_type,
    p.plan_name,
    COUNT(DISTINCT pay.payment_id)          AS payment_count,
    SUM(pay.amount)                         AS revenue_collected
FROM payment pay
INNER JOIN invoice i    ON pay.invoice_id    = i.invoice_id
INNER JOIN subscription s ON i.subscription_id = s.subscription_id
INNER JOIN plan p        ON s.plan_id        = p.plan_id
GROUP BY DATE_FORMAT(pay.payment_date, '%Y-%m'), p.plan_type, p.plan_name
ORDER BY revenue_month DESC, revenue_collected DESC;

-- ============================================================
-- VIEW: v_technician_workload
-- Open installations and tickets per technician
-- DEMONSTRATES: LEFT JOIN, COUNT, CASE expression
-- ============================================================
CREATE OR REPLACE VIEW v_technician_workload AS
SELECT
    t.technician_id,
    CONCAT(t.first_name,' ',t.last_name) AS technician_name,
    t.specialization,
    sa.area_name,
    COUNT(DISTINCT inst.installation_id)  AS pending_installations,
    COUNT(DISTINCT tk.ticket_id)          AS open_tickets,
    (COUNT(DISTINCT inst.installation_id) + COUNT(DISTINCT tk.ticket_id)) AS total_workload,
    CASE
        WHEN (COUNT(DISTINCT inst.installation_id) + COUNT(DISTINCT tk.ticket_id)) > 10 THEN 'OVERLOADED'
        WHEN (COUNT(DISTINCT inst.installation_id) + COUNT(DISTINCT tk.ticket_id)) > 5  THEN 'BUSY'
        ELSE 'AVAILABLE'
    END AS workload_status
FROM technician t
INNER JOIN service_area sa ON t.area_id = sa.area_id
LEFT JOIN installation inst ON t.technician_id = inst.technician_id
    AND inst.status IN ('PENDING','SCHEDULED','IN_PROGRESS')
LEFT JOIN support_ticket tk ON t.technician_id = tk.assigned_to
    AND tk.status IN ('OPEN','IN_PROGRESS')
WHERE t.is_active = 1
GROUP BY t.technician_id, t.first_name, t.last_name, t.specialization, sa.area_name;

-- ============================================================
-- VIEW: v_data_usage_summary
-- Monthly data usage per subscription vs plan cap
-- DEMONSTRATES: aggregation, CASE, comparison with cap
-- ============================================================
CREATE OR REPLACE VIEW v_data_usage_summary AS
SELECT
    s.subscription_id,
    CONCAT(c.first_name,' ',c.last_name) AS customer_name,
    p.plan_name,
    p.data_cap_gb,
    DATE_FORMAT(ur.usage_date, '%Y-%m')  AS usage_month,
    ROUND(SUM(ur.data_used_mb) / 1024, 2) AS data_used_gb,
    CASE
        WHEN p.data_cap_gb IS NULL THEN 'UNLIMITED'
        WHEN SUM(ur.data_used_mb) / 1024 >= p.data_cap_gb THEN 'CAP_EXCEEDED'
        WHEN SUM(ur.data_used_mb) / 1024 >= p.data_cap_gb * 0.8 THEN 'NEAR_CAP'
        ELSE 'NORMAL'
    END AS usage_status,
    CASE
        WHEN p.data_cap_gb IS NULL THEN NULL
        ELSE ROUND((SUM(ur.data_used_mb) / 1024 / p.data_cap_gb) * 100, 1)
    END AS pct_used
FROM subscription s
INNER JOIN customer c     ON s.customer_id = c.customer_id
INNER JOIN plan p         ON s.plan_id     = p.plan_id
INNER JOIN usage_record ur ON s.subscription_id = ur.subscription_id
GROUP BY s.subscription_id, c.first_name, c.last_name,
         p.plan_name, p.data_cap_gb, DATE_FORMAT(ur.usage_date, '%Y-%m');
