-- ============================================================
-- FILE    : V004__views.sql
-- PURPOSE : Oracle views for common ISP business queries
-- AUTHOR  : ISP Dev Team
-- DATE    : 2026-10-26
-- RUN AS  : isp_app@//localhost:1521/XEPDB1
-- ============================================================
SET SERVEROUTPUT ON SIZE UNLIMITED

-- ============================================================
-- VIEW: v_active_subscriptions
-- DEMONSTRATES: 4-table INNER JOIN
-- ============================================================
CREATE OR REPLACE VIEW v_active_subscriptions AS
SELECT
    s.subscription_id,
    s.start_date,
    s.status                              AS subscription_status,
    c.customer_id,
    c.first_name || ' ' || c.last_name   AS customer_name,
    c.email,
    c.phone,
    p.plan_name,
    p.plan_type,
    p.speed_mbps,
    p.price                               AS monthly_price,
    sa.area_name,
    sa.city,
    sa.pin_code
FROM subscription s
JOIN customer    c  ON s.customer_id = c.customer_id
JOIN plan        p  ON s.plan_id     = p.plan_id
JOIN service_area sa ON s.area_id   = sa.area_id
WHERE s.status = 'ACTIVE';

-- ============================================================
-- VIEW: v_overdue_invoices
-- DEMONSTRATES: JOIN + date arithmetic (SYSDATE, TRUNC)
-- ============================================================
CREATE OR REPLACE VIEW v_overdue_invoices AS
SELECT
    i.invoice_id,
    i.subscription_id,
    c.first_name || ' ' || c.last_name  AS customer_name,
    c.email,
    c.phone,
    p.plan_name,
    i.total_amount,
    i.amount_paid,
    i.total_amount - i.amount_paid       AS outstanding,
    i.due_date,
    TRUNC(SYSDATE) - TRUNC(i.due_date)  AS days_overdue,
    i.status
FROM invoice i
JOIN subscription s  ON i.subscription_id = s.subscription_id
JOIN customer c      ON s.customer_id     = c.customer_id
JOIN plan p          ON s.plan_id         = p.plan_id
WHERE i.status IN ('ISSUED','PARTIALLY_PAID','OVERDUE')
  AND i.due_date < TRUNC(SYSDATE)
ORDER BY days_overdue DESC;

-- ============================================================
-- VIEW: v_customer_balance
-- DEMONSTRATES: LEFT JOIN, NVL (Oracle null-handling), GROUP BY
-- ============================================================
CREATE OR REPLACE VIEW v_customer_balance AS
SELECT
    c.customer_id,
    c.first_name || ' ' || c.last_name  AS customer_name,
    c.email,
    COUNT(i.invoice_id)                 AS total_invoices,
    NVL(SUM(i.total_amount), 0)         AS total_billed,
    NVL(SUM(i.amount_paid),  0)         AS total_paid,
    NVL(SUM(i.total_amount - i.amount_paid), 0) AS outstanding_balance
FROM customer c
LEFT JOIN subscription s ON c.customer_id    = s.customer_id
LEFT JOIN invoice i      ON s.subscription_id = i.subscription_id
GROUP BY c.customer_id, c.first_name, c.last_name, c.email;

-- ============================================================
-- VIEW: v_monthly_revenue
-- DEMONSTRATES: TO_CHAR date formatting, GROUP BY, analytic ROLLUP
-- ============================================================
CREATE OR REPLACE VIEW v_monthly_revenue AS
SELECT
    TO_CHAR(pay.payment_date, 'YYYY-MM')  AS revenue_month,
    p.plan_type,
    p.plan_name,
    COUNT(DISTINCT pay.payment_id)         AS payment_count,
    SUM(pay.amount)                        AS revenue_collected
FROM payment pay
JOIN invoice     i  ON pay.invoice_id    = i.invoice_id
JOIN subscription s ON i.subscription_id = s.subscription_id
JOIN plan         p ON s.plan_id         = p.plan_id
GROUP BY TO_CHAR(pay.payment_date,'YYYY-MM'), p.plan_type, p.plan_name
ORDER BY revenue_month DESC, revenue_collected DESC;

-- ============================================================
-- VIEW: v_technician_workload
-- DEMONSTRATES: LEFT JOIN, COUNT, CASE expression, multi-table
-- ============================================================
CREATE OR REPLACE VIEW v_technician_workload AS
SELECT
    t.technician_id,
    t.first_name || ' ' || t.last_name   AS technician_name,
    t.specialization,
    sa.area_name,
    COUNT(DISTINCT inst.installation_id)  AS pending_installations,
    COUNT(DISTINCT tk.ticket_id)          AS open_tickets,
    COUNT(DISTINCT inst.installation_id) + COUNT(DISTINCT tk.ticket_id) AS total_workload,
    CASE
        WHEN COUNT(DISTINCT inst.installation_id) + COUNT(DISTINCT tk.ticket_id) > 10 THEN 'OVERLOADED'
        WHEN COUNT(DISTINCT inst.installation_id) + COUNT(DISTINCT tk.ticket_id) > 5  THEN 'BUSY'
        ELSE 'AVAILABLE'
    END AS workload_status
FROM technician t
JOIN service_area sa ON t.area_id = sa.area_id
LEFT JOIN installation inst ON t.technician_id = inst.technician_id
    AND inst.status IN ('PENDING','SCHEDULED','IN_PROGRESS')
LEFT JOIN support_ticket tk ON t.technician_id = tk.assigned_to
    AND tk.status IN ('OPEN','IN_PROGRESS')
WHERE t.is_active = 'Y'
GROUP BY t.technician_id, t.first_name, t.last_name, t.specialization, sa.area_name;

-- ============================================================
-- VIEW: v_data_usage_summary
-- DEMONSTRATES: CASE, NVL2, TO_CHAR, computed columns
-- ============================================================
CREATE OR REPLACE VIEW v_data_usage_summary AS
SELECT
    s.subscription_id,
    c.first_name || ' ' || c.last_name  AS customer_name,
    p.plan_name,
    p.data_cap_gb,
    TO_CHAR(ur.usage_date,'YYYY-MM')    AS usage_month,
    ROUND(SUM(ur.data_used_mb) / 1024, 2) AS data_used_gb,
    CASE
        WHEN p.data_cap_gb IS NULL THEN 'UNLIMITED'
        WHEN SUM(ur.data_used_mb)/1024 >= p.data_cap_gb       THEN 'CAP_EXCEEDED'
        WHEN SUM(ur.data_used_mb)/1024 >= p.data_cap_gb * 0.8 THEN 'NEAR_CAP'
        ELSE 'NORMAL'
    END AS usage_status,
    NVL2(p.data_cap_gb,
        ROUND((SUM(ur.data_used_mb)/1024 / p.data_cap_gb) * 100, 1),
        NULL) AS pct_used
FROM subscription s
JOIN customer    c  ON s.customer_id    = c.customer_id
JOIN plan        p  ON s.plan_id        = p.plan_id
JOIN usage_record ur ON s.subscription_id = ur.subscription_id
GROUP BY s.subscription_id, c.first_name, c.last_name,
         p.plan_name, p.data_cap_gb, TO_CHAR(ur.usage_date,'YYYY-MM');

COMMIT;
PROMPT Views created successfully.
