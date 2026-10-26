-- ============================================================
-- FILE    : V007__demo_queries.sql
-- PURPOSE : Oracle SQL demo queries covering all required concepts:
--           joins, subqueries, analytic functions, CTEs (WITH),
--           ROLLUP/CUBE, set ops (UNION/INTERSECT/MINUS),
--           CASE/DECODE/NVL/COALESCE, date functions, FETCH FIRST
-- AUTHOR  : ISP Dev Team
-- DATE    : 2026-10-26
-- ============================================================

-- ============================================================
-- 1. INNER JOIN: Subscriptions with customer, plan, area details
-- ============================================================
SELECT s.subscription_id,
       c.first_name || ' ' || c.last_name AS customer_name,
       p.plan_name, p.speed_mbps, p.price,
       sa.area_name, sa.city,
       s.start_date, s.status
FROM   subscription s
JOIN   customer     c  ON s.customer_id = c.customer_id
JOIN   plan         p  ON s.plan_id     = p.plan_id
JOIN   service_area sa ON s.area_id     = sa.area_id
ORDER  BY s.subscription_id;

-- ============================================================
-- 2. LEFT JOIN: All customers with subscription count
--    (includes customers with NO subscription)
-- ============================================================
SELECT c.customer_id,
       c.first_name || ' ' || c.last_name AS customer_name,
       COUNT(s.subscription_id)            AS sub_count
FROM   customer c
LEFT   JOIN subscription s ON c.customer_id = s.customer_id
GROUP  BY c.customer_id, c.first_name, c.last_name
ORDER  BY sub_count DESC;

-- ============================================================
-- 3. RIGHT JOIN: All plans and how many subscriptions use them
-- ============================================================
SELECT p.plan_name, p.price,
       COUNT(s.subscription_id) AS subscriber_count
FROM   subscription s
RIGHT  JOIN plan p ON s.plan_id = p.plan_id
GROUP  BY p.plan_id, p.plan_name, p.price
ORDER  BY subscriber_count DESC;

-- ============================================================
-- 4. FULL OUTER JOIN: All customers and all subscriptions
--    (shows unmatched rows on both sides)
-- ============================================================
SELECT c.customer_id,
       c.first_name || ' ' || c.last_name AS customer_name,
       s.subscription_id, s.status
FROM   customer c
FULL OUTER JOIN subscription s ON c.customer_id = s.customer_id
WHERE  c.customer_id IS NULL OR s.subscription_id IS NULL;

-- ============================================================
-- 5. SELF JOIN: Customers in the same service area
-- ============================================================
SELECT c1.customer_id  AS cust1_id,
       c1.first_name || ' ' || c1.last_name AS cust1_name,
       c2.customer_id  AS cust2_id,
       c2.first_name || ' ' || c2.last_name AS cust2_name,
       sa.area_name
FROM   customer     c1
JOIN   customer     c2  ON c1.area_id = c2.area_id
                       AND c1.customer_id < c2.customer_id
JOIN   service_area sa  ON c1.area_id = sa.area_id
ORDER  BY sa.area_name;

-- ============================================================
-- 6. NON-CORRELATED SUBQUERY: Plans cheaper than average price
-- ============================================================
SELECT s.subscription_id,
       c.first_name || ' ' || c.last_name AS customer_name,
       p.plan_name, p.price
FROM   subscription s
JOIN   customer c ON s.customer_id = c.customer_id
JOIN   plan     p ON s.plan_id     = p.plan_id
WHERE  p.price < (SELECT AVG(price) FROM plan WHERE is_active = 'Y');

-- ============================================================
-- 7. CORRELATED SUBQUERY: Customers with at least one overdue invoice
-- ============================================================
SELECT c.customer_id,
       c.first_name || ' ' || c.last_name AS customer_name,
       c.email
FROM   customer c
WHERE  EXISTS (
    SELECT 1 FROM invoice i
    JOIN   subscription s ON i.subscription_id = s.subscription_id
    WHERE  s.customer_id = c.customer_id
      AND  i.status IN ('OVERDUE','ISSUED')
      AND  i.due_date < TRUNC(SYSDATE)
);

-- ============================================================
-- 8. GROUP BY / HAVING: Areas with > 2 active subscriptions
-- ============================================================
SELECT sa.area_name, sa.city,
       COUNT(s.subscription_id)  AS active_subs,
       SUM(p.price)              AS monthly_revenue
FROM   service_area sa
JOIN   subscription s ON sa.area_id = s.area_id AND s.status = 'ACTIVE'
JOIN   plan         p ON s.plan_id  = p.plan_id
GROUP  BY sa.area_id, sa.area_name, sa.city
HAVING COUNT(s.subscription_id) > 2
ORDER  BY monthly_revenue DESC;

-- ============================================================
-- 9. ROLLUP: Revenue by area, city and grand total
-- ============================================================
SELECT NVL(sa.city,'** GRAND TOTAL **')      AS city,
       NVL(p.plan_type,'All Plans')           AS plan_type,
       SUM(pay.amount)                        AS revenue
FROM   payment pay
JOIN   invoice     i  ON pay.invoice_id    = i.invoice_id
JOIN   subscription s ON i.subscription_id = s.subscription_id
JOIN   plan         p ON s.plan_id         = p.plan_id
JOIN   service_area sa ON s.area_id        = sa.area_id
GROUP  BY ROLLUP(sa.city, p.plan_type)
ORDER  BY sa.city, p.plan_type;

-- ============================================================
-- 10. CUBE: Revenue matrix by city and plan type
-- ============================================================
SELECT NVL(sa.city,'All Cities')    AS city,
       NVL(p.plan_type,'All Plans') AS plan_type,
       SUM(pay.amount)              AS revenue
FROM   payment pay
JOIN   invoice     i  ON pay.invoice_id    = i.invoice_id
JOIN   subscription s ON i.subscription_id = s.subscription_id
JOIN   plan         p ON s.plan_id         = p.plan_id
JOIN   service_area sa ON s.area_id        = sa.area_id
GROUP  BY CUBE(sa.city, p.plan_type)
ORDER  BY sa.city, p.plan_type;

-- ============================================================
-- 11. ANALYTIC FUNCTIONS: RANK, DENSE_RANK, ROW_NUMBER, running total
-- ============================================================
-- 11a: Rank customers by outstanding balance
SELECT customer_id, customer_name, outstanding_balance,
       RANK()       OVER (ORDER BY outstanding_balance DESC) AS bal_rank,
       DENSE_RANK() OVER (ORDER BY outstanding_balance DESC) AS dense_rank,
       ROW_NUMBER() OVER (ORDER BY outstanding_balance DESC) AS row_num
FROM   v_customer_balance
WHERE  outstanding_balance > 0;

-- 11b: Running total of payments and LAG/LEAD
SELECT payment_id,
       TO_CHAR(payment_date,'YYYY-MM-DD') AS pay_date,
       amount,
       SUM(amount) OVER (
           PARTITION BY TO_CHAR(payment_date,'YYYY-MM')
           ORDER BY payment_date, payment_id
           ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
       )                                  AS running_monthly_total,
       LAG(amount)  OVER (ORDER BY payment_date) AS prev_payment,
       LEAD(amount) OVER (ORDER BY payment_date) AS next_payment
FROM   payment
ORDER  BY payment_date;

-- ============================================================
-- 12. CTE (WITH clause): Month-over-month revenue growth
-- ============================================================
WITH monthly AS (
    SELECT TO_CHAR(payment_date,'YYYY-MM') AS yr_mo,
           SUM(amount)                      AS revenue
    FROM   payment
    GROUP  BY TO_CHAR(payment_date,'YYYY-MM')
),
growth AS (
    SELECT yr_mo, revenue,
           LAG(revenue) OVER (ORDER BY yr_mo) AS prev_revenue
    FROM   monthly
)
SELECT yr_mo,
       ROUND(revenue, 2)                                           AS revenue,
       ROUND(prev_revenue, 2)                                      AS prev_revenue,
       CASE WHEN prev_revenue IS NULL THEN NULL
            ELSE ROUND((revenue - prev_revenue) / prev_revenue * 100, 2)
       END                                                         AS growth_pct
FROM   growth
ORDER  BY yr_mo;

-- ============================================================
-- 13. RECURSIVE CTE: Generate date series for usage report
-- ============================================================
WITH date_series (dt, lvl) AS (
    SELECT TRUNC(SYSDATE) - 30  AS dt, 1 AS lvl FROM DUAL
    UNION ALL
    SELECT dt + 1, lvl + 1
    FROM   date_series
    WHERE  dt + 1 <= TRUNC(SYSDATE)
)
SELECT d.dt,
       NVL(SUM(ur.data_used_mb), 0) AS total_mb_used
FROM   date_series d
LEFT   JOIN usage_record ur ON ur.usage_date = d.dt
GROUP  BY d.dt
ORDER  BY d.dt;

-- ============================================================
-- 14. SET OPERATIONS: UNION / INTERSECT / MINUS
-- ============================================================
-- UNION: All contacts in area 1 (customers + technicians)
SELECT 'CUSTOMER'   AS contact_type, customer_id AS id,
       first_name || ' ' || last_name AS full_name, email, phone
FROM   customer WHERE area_id = 1
UNION ALL
SELECT 'TECHNICIAN' AS contact_type, technician_id,
       first_name || ' ' || last_name, email, phone
FROM   technician WHERE area_id = 1
ORDER  BY contact_type, full_name;

-- INTERSECT: customers who have BOTH active subscription AND open ticket
SELECT customer_id FROM subscription WHERE status = 'ACTIVE'
INTERSECT
SELECT customer_id FROM support_ticket WHERE status = 'OPEN';

-- MINUS: customers with subscription but NO support ticket
SELECT customer_id FROM subscription
MINUS
SELECT customer_id FROM support_ticket;

-- ============================================================
-- 15. CASE / DECODE / NVL / COALESCE
-- ============================================================
SELECT payment_id,
       amount,
       payment_method,
       CASE payment_method
           WHEN 'UPI'           THEN 'Digital Wallet'
           WHEN 'BANK_TRANSFER' THEN 'Net Banking'
           WHEN 'CARD'          THEN 'Debit/Credit Card'
           WHEN 'CASH'          THEN 'Cash Counter'
           ELSE                      'Other'
       END                                   AS method_display,
       DECODE(payment_method,'CASH','Y','N') AS is_cash,
       NVL(transaction_ref,'N/A')            AS ref_display,
       COALESCE(transaction_ref,'CASH-'||payment_id) AS ref_or_default
FROM   payment
ORDER  BY payment_date;

-- ============================================================
-- 16. DATE FUNCTIONS: TRUNC, ADD_MONTHS, MONTHS_BETWEEN, TO_CHAR
-- ============================================================
SELECT c.first_name || ' ' || c.last_name AS customer_name,
       s.start_date,
       TRUNC(s.start_date,'MM')             AS billing_month_start,
       ADD_MONTHS(s.start_date, 12)         AS one_year_anniversary,
       ROUND(MONTHS_BETWEEN(SYSDATE, s.start_date), 1) AS months_active,
       TO_CHAR(s.start_date,'Day, DD-MON-YYYY') AS formatted_start,
       TO_CHAR(SYSDATE,'YYYY-MM-DD HH24:MI:SS') AS current_ts
FROM   subscription s
JOIN   customer c ON s.customer_id = c.customer_id
WHERE  s.status = 'ACTIVE'
ORDER  BY months_active DESC;

-- ============================================================
-- 17. TOP-N with FETCH FIRST (Oracle 12c+, replaces ROWNUM trick)
-- ============================================================
-- Top 5 customers by outstanding balance
SELECT customer_name, outstanding_balance
FROM   v_customer_balance
WHERE  outstanding_balance > 0
ORDER  BY outstanding_balance DESC
FETCH  FIRST 5 ROWS ONLY;

-- 18. OFFSET / FETCH for pagination (page 2, 10 rows per page)
SELECT customer_id, first_name, last_name, email
FROM   customer
ORDER  BY customer_id
OFFSET 10 ROWS FETCH NEXT 10 ROWS ONLY;

-- 19. Ticket SLA analysis: resolution time by priority
SELECT category, priority,
       COUNT(*)                                           AS total_tickets,
       SUM(CASE WHEN status IN ('RESOLVED','CLOSED') THEN 1 ELSE 0 END) AS resolved,
       ROUND(AVG(
           CASE WHEN resolved_at IS NOT NULL
                THEN (resolved_at - created_at) * 24  -- hours
           END), 1)                                       AS avg_hours,
       MAX(CASE WHEN resolved_at IS NOT NULL
                THEN (resolved_at - created_at) * 24
           END)                                           AS max_hours
FROM   support_ticket
GROUP  BY category, priority
ORDER  BY priority, category;
