-- ============================================================
-- FILE    : V007__demo_queries.sql
-- PURPOSE : Demonstration queries covering all required SQL
--           concepts: joins, subqueries, window functions,
--           CTEs, set ops, CASE, aggregation, date functions
-- AUTHOR  : ISP Dev Team
-- DATE    : 2026-10-26
-- ============================================================

-- ============================================================
-- 1. INNER JOIN: Subscriptions with customer and plan details
-- ============================================================
SELECT s.subscription_id,
       CONCAT(c.first_name,' ',c.last_name) AS customer,
       p.plan_name, p.speed_mbps, p.price,
       s.start_date, s.status
FROM subscription s
INNER JOIN customer c ON s.customer_id = c.customer_id
INNER JOIN plan p     ON s.plan_id     = p.plan_id;

-- ============================================================
-- 2. LEFT JOIN: All customers with their subscription count
--    (includes customers with NO subscription)
-- ============================================================
SELECT c.customer_id,
       CONCAT(c.first_name,' ',c.last_name) AS customer,
       COUNT(s.subscription_id) AS sub_count
FROM customer c
LEFT JOIN subscription s ON c.customer_id = s.customer_id
GROUP BY c.customer_id, c.first_name, c.last_name
ORDER BY sub_count DESC;

-- ============================================================
-- 3. SELF JOIN: Customers in the same service area
--    (pairs of different customers sharing an area)
-- ============================================================
SELECT c1.customer_id AS cust1_id,
       CONCAT(c1.first_name,' ',c1.last_name) AS cust1_name,
       c2.customer_id AS cust2_id,
       CONCAT(c2.first_name,' ',c2.last_name) AS cust2_name,
       sa.area_name
FROM customer c1
INNER JOIN customer c2   ON c1.area_id = c2.area_id AND c1.customer_id < c2.customer_id
INNER JOIN service_area sa ON c1.area_id = sa.area_id
ORDER BY sa.area_name;

-- ============================================================
-- 4. NON-CORRELATED SUBQUERY: Subscriptions on plans cheaper
--    than the average plan price
-- ============================================================
SELECT s.subscription_id,
       CONCAT(c.first_name,' ',c.last_name) AS customer,
       p.plan_name, p.price
FROM subscription s
INNER JOIN customer c ON s.customer_id = c.customer_id
INNER JOIN plan p     ON s.plan_id     = p.plan_id
WHERE p.price < (SELECT AVG(price) FROM plan WHERE is_active = 1);

-- ============================================================
-- 5. CORRELATED SUBQUERY: Customers whose last invoice
--    is overdue (exists overdue invoice for that customer)
-- ============================================================
SELECT c.customer_id,
       CONCAT(c.first_name,' ',c.last_name) AS customer,
       c.email
FROM customer c
WHERE EXISTS (
    SELECT 1 FROM invoice i
    INNER JOIN subscription s ON i.subscription_id = s.subscription_id
    WHERE s.customer_id = c.customer_id
      AND i.status IN ('OVERDUE','ISSUED')
      AND i.due_date < CURDATE()
);

-- ============================================================
-- 6. AGGREGATION with GROUP BY / HAVING:
--    Areas with more than 2 active subscriptions
-- ============================================================
SELECT sa.area_name, sa.city,
       COUNT(s.subscription_id) AS active_subs,
       SUM(p.price)             AS monthly_revenue
FROM service_area sa
INNER JOIN subscription s ON sa.area_id = s.area_id AND s.status = 'ACTIVE'
INNER JOIN plan p         ON s.plan_id  = p.plan_id
GROUP BY sa.area_id, sa.area_name, sa.city
HAVING COUNT(s.subscription_id) > 2
ORDER BY monthly_revenue DESC;

-- ============================================================
-- 7. WINDOW FUNCTIONS: Rank customers by outstanding balance
--    using RANK(); running total of payments using SUM OVER
-- ============================================================
-- 7a. RANK by outstanding balance
SELECT customer_id, customer_name, outstanding_balance,
       RANK() OVER (ORDER BY outstanding_balance DESC) AS balance_rank,
       ROW_NUMBER() OVER (ORDER BY outstanding_balance DESC) AS row_num
FROM v_customer_balance
WHERE outstanding_balance > 0;

-- 7b. Running total of daily payments per month
SELECT pay.payment_id,
       pay.payment_date,
       pay.amount,
       SUM(pay.amount) OVER (
           PARTITION BY DATE_FORMAT(pay.payment_date,'%Y-%m')
           ORDER BY pay.payment_date, pay.payment_id
           ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
       ) AS running_monthly_total,
       LAG(pay.amount)  OVER (ORDER BY pay.payment_date) AS prev_payment,
       LEAD(pay.amount) OVER (ORDER BY pay.payment_date) AS next_payment
FROM payment pay
ORDER BY pay.payment_date;

-- ============================================================
-- 8. CTE: Monthly revenue with month-over-month growth
-- ============================================================
WITH monthly AS (
    SELECT DATE_FORMAT(payment_date,'%Y-%m') AS yr_mo,
           SUM(amount) AS revenue
    FROM payment
    GROUP BY DATE_FORMAT(payment_date,'%Y-%m')
),
growth AS (
    SELECT yr_mo, revenue,
           LAG(revenue) OVER (ORDER BY yr_mo) AS prev_revenue
    FROM monthly
)
SELECT yr_mo,
       revenue,
       prev_revenue,
       CASE
           WHEN prev_revenue IS NULL THEN NULL
           ELSE ROUND((revenue - prev_revenue) / prev_revenue * 100, 2)
       END AS growth_pct
FROM growth
ORDER BY yr_mo;

-- ============================================================
-- 9. RECURSIVE CTE: (if you model manager-subordinate or
--    area hierarchy; here: generate a date series for usage)
-- ============================================================
WITH RECURSIVE date_series AS (
    SELECT DATE_SUB(CURDATE(), INTERVAL 30 DAY) AS dt
    UNION ALL
    SELECT DATE_ADD(dt, INTERVAL 1 DAY)
    FROM date_series WHERE dt < CURDATE()
)
SELECT d.dt,
       COALESCE(SUM(ur.data_used_mb), 0) AS total_mb_used
FROM date_series d
LEFT JOIN usage_record ur ON ur.usage_date = d.dt
GROUP BY d.dt
ORDER BY d.dt;

-- ============================================================
-- 10. SET OPERATIONS: UNION
--     All contacts (customers + technicians) in an area
-- ============================================================
SELECT 'CUSTOMER'   AS contact_type,
       customer_id  AS id,
       CONCAT(first_name,' ',last_name) AS full_name,
       email, phone, area_id
FROM customer WHERE area_id = 1
UNION ALL
SELECT 'TECHNICIAN' AS contact_type,
       technician_id, CONCAT(first_name,' ',last_name),
       email, phone, area_id
FROM technician WHERE area_id = 1
ORDER BY contact_type, full_name;

-- ============================================================
-- 11. CASE expression: Payment method distribution report
-- ============================================================
SELECT
    COUNT(*) AS total_payments,
    SUM(amount) AS total_amount,
    SUM(CASE WHEN payment_method = 'UPI'           THEN amount ELSE 0 END) AS upi_amount,
    SUM(CASE WHEN payment_method = 'BANK_TRANSFER' THEN amount ELSE 0 END) AS bank_amount,
    SUM(CASE WHEN payment_method = 'CARD'          THEN amount ELSE 0 END) AS card_amount,
    SUM(CASE WHEN payment_method = 'CASH'          THEN amount ELSE 0 END) AS cash_amount,
    CONCAT(ROUND(SUM(CASE WHEN payment_method='UPI' THEN 1 ELSE 0 END)*100.0/COUNT(*),1),'%') AS upi_pct
FROM payment;

-- ============================================================
-- 12. DATE FUNCTIONS: Subscriptions expiring within 30 days
-- ============================================================
SELECT s.subscription_id,
       CONCAT(c.first_name,' ',c.last_name) AS customer,
       s.end_date,
       DATEDIFF(s.end_date, CURDATE()) AS days_remaining,
       p.plan_name
FROM subscription s
INNER JOIN customer c ON s.customer_id = c.customer_id
INNER JOIN plan p     ON s.plan_id     = p.plan_id
WHERE s.end_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY)
  AND s.status = 'ACTIVE'
ORDER BY days_remaining;

-- ============================================================
-- 13. Ticket resolution time report (SLA analysis)
-- ============================================================
SELECT
    category,
    priority,
    COUNT(*) AS total_tickets,
    COUNT(CASE WHEN status = 'RESOLVED' OR status = 'CLOSED' THEN 1 END) AS resolved,
    AVG(CASE WHEN resolved_at IS NOT NULL
             THEN TIMESTAMPDIFF(HOUR, created_at, resolved_at) END) AS avg_resolution_hours,
    MAX(CASE WHEN resolved_at IS NOT NULL
             THEN TIMESTAMPDIFF(HOUR, created_at, resolved_at) END) AS max_resolution_hours
FROM support_ticket
GROUP BY category, priority
ORDER BY priority, category;
