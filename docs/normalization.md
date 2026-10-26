# Normalization Walkthrough — ISP Management System

## Starting Point: Unnormalized Form (UNF)

Imagine one fat "order" table that captures everything:

| CustomerName | Email | Phone | PlanName | Speed | Price | InvoiceDate | TotalAmount | PaymentAmount | TechName | EquipSerial |
|---|---|---|---|---|---|---|---|---|---|---|
| Arjun Mehta | arjun@.. | 981.. | Gold 200 | 200 | 999 | 2025-01-01 | 1178 | 1178 | Rajesh K | TPLINK001 |
| Arjun Mehta | arjun@.. | 981.. | Gold 200 | 200 | 999 | 2025-02-01 | 1178 | 0   | Rajesh K | TPLINK001 |
| Kavya Reddy | kavya@.. | 981.. | Silver 100 | 100 | 699 | 2025-01-01 | 824 | 824 | Priya S | NETG001 |

**Problems:** Repeating groups (multiple invoices per customer), redundancy, update anomalies.

---

## First Normal Form (1NF)
**Rule:** Atomic values; no repeating groups; each row is unique.

**Action:**
- Split repeating invoice rows into separate rows (already done above, one row per invoice).
- Ensure each column holds a single atomic value (no comma-separated lists).
- Add a primary key (customer_id + invoice_date → still has redundancy).

**Result:** Table with one row per invoice but customer/plan data repeats.

---

## Second Normal Form (2NF)
**Rule:** Must be in 1NF + no partial dependencies (all non-key attributes depend on the WHOLE primary key).

**Composite key assumed:** (customer_id, invoice_date)

**Partial dependencies found:**
- `customer_name`, `email`, `phone` depend only on `customer_id` → not on invoice_date
- `plan_name`, `speed`, `price` depend only on `plan_id` → not on invoice_date

**Action:** Decompose into:
- `customer(customer_id, name, email, phone, area_id)` — PK: customer_id
- `plan(plan_id, plan_name, speed_mbps, price, billing_cycle)` — PK: plan_id
- `invoice(invoice_id, customer_id FK, plan_id FK, invoice_date, total_amount)` — PK: invoice_id

**Result:** No partial dependencies. Customer and plan data stored once.

---

## Third Normal Form (3NF)
**Rule:** Must be in 2NF + no transitive dependencies (non-key attributes must depend ONLY on the key).

**Transitive dependencies found in 2NF:**
- In `customer`: `city`, `state` depend on `pin_code`, not directly on `customer_id`
  → `pin_code → city, state` is a transitive dependency

**Action:**
- Extract `service_area(area_id, area_name, city, state, pin_code)` — PK: area_id, UNIQUE: pin_code
- In `customer`, replace city/state/pin_code with `area_id FK`

**Additional 3NF decompositions:**
- `subscription(subscription_id, customer_id FK, plan_id FK, start_date, status)` — separates the contract from billing
- `invoice(invoice_id, subscription_id FK, billing_period_start, total_amount, ...)` — invoice depends on subscription, not directly on customer/plan
- `equipment` separated from `subscription` (equipment has its own attributes)
- `technician` separated from `installation` (technician works on many installations)

---

## BCNF (Boyce-Codd Normal Form)
**Rule:** For every non-trivial functional dependency X → Y, X must be a superkey.

**Check on `installation`:**
- FD: `subscription_id → installation_id` (1:1 relationship enforced by UNIQUE constraint)
- FD: `installation_id → subscription_id, technician_id, status`
- Both determinants are superkeys → **Installation is in BCNF** ✓

**Check on `usage_record`:**
- FD: `(subscription_id, usage_date) → data_used_mb`
- Composite PK is the only determinant → **BCNF** ✓

**Final schema is in 3NF/BCNF.** The only deliberate denormalization is `amount_paid` stored on `invoice` (derived from SUM of payments) for query performance — documented as intentional.

---

## Functional Dependencies Summary

| Table | Functional Dependency |
|---|---|
| service_area | area_id → area_name, city, state, pin_code |
| customer | customer_id → first_name, last_name, email, phone, area_id, status |
| plan | plan_id → plan_name, plan_type, speed_mbps, price, billing_cycle |
| subscription | subscription_id → customer_id, plan_id, area_id, start_date, status |
| invoice | invoice_id → subscription_id, billing_period_start, total_amount, due_date, status |
| payment | payment_id → invoice_id, amount, method, payment_date |
| usage_record | (subscription_id, usage_date) → data_used_mb |
| equipment | equipment_id → type, serial_number, mac_address, status, subscription_id |
| installation | installation_id → subscription_id, technician_id, scheduled_date, status |
