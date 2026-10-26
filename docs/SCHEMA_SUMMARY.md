# Oracle Database Schema Summary — ISP Management System

This document provides a comprehensive technical overview of the Oracle SQL layer under `database/` for application developers and DBAs.

---

## 1. Tables & Primary Key Generation

| Table Name | Primary Key | Generation Strategy | Description |
|---|---|---|---|
| **SERVICE_AREA** | `area_id NUMBER` | `GENERATED ALWAYS AS IDENTITY` | Geographic service zones with PIN codes |
| **CUSTOMER** | `customer_id NUMBER` | `GENERATED ALWAYS AS IDENTITY` | Subscribers (individual and business) |
| **ADDRESS** | `address_id NUMBER` | `GENERATED ALWAYS AS IDENTITY` | Customer addresses (billing, installation) |
| **PLAN** | `plan_id NUMBER` | `GENERATED ALWAYS AS IDENTITY` | Internet speed and data tiers |
| **SUBSCRIPTION** | `subscription_id NUMBER` | `GENERATED ALWAYS AS IDENTITY` | Active contracts between customers & plans |
| **EQUIPMENT** | `equipment_id NUMBER` | `DEFAULT seq_equipment_id.NEXTVAL` | Hardware inventory using SEQUENCE |
| **TECHNICIAN** | `technician_id NUMBER` | `GENERATED ALWAYS AS IDENTITY` | Service and installation technicians |
| **INSTALLATION** | `installation_id NUMBER` | `GENERATED ALWAYS AS IDENTITY` | 1:1 hardware installation per subscription |
| **INVOICE** | `invoice_id NUMBER` | `GENERATED ALWAYS AS IDENTITY` | Periodic billing statements |
| **INVOICE_ITEM** | `item_id NUMBER` | `GENERATED ALWAYS AS IDENTITY` | Line items itemizing an invoice |
| **PAYMENT** | `payment_id NUMBER` | `GENERATED ALWAYS AS IDENTITY` | Payment transactions against invoices |
| **USAGE_RECORD** | `usage_id NUMBER` | `GENERATED ALWAYS AS IDENTITY` | Daily data usage meter per subscription |
| **SUPPORT_TICKET** | `ticket_id NUMBER` | `GENERATED ALWAYS AS IDENTITY` | Customer service tickets |
| **TICKET_UPDATE** | `update_id NUMBER` | `GENERATED ALWAYS AS IDENTITY` | Comments and timeline on tickets |
| **NETWORK_OUTAGE**| `outage_id NUMBER` | `GENERATED ALWAYS AS IDENTITY` | Outages and incidents by service area |
| **APP_USER** | `user_id NUMBER` | `GENERATED ALWAYS AS IDENTITY` | User logins with BCrypt password hashes |
| **AUDIT_LOG** | `log_id NUMBER` | `GENERATED ALWAYS AS IDENTITY` | Trigger-based change logs |

---

## 2. Table Column Specifications & Constraints

### 1. `SERVICE_AREA`
- `area_id NUMBER` (PK, Identity)
- `area_name VARCHAR2(100) NOT NULL`
- `city VARCHAR2(100) NOT NULL`
- `state_name VARCHAR2(100) NOT NULL`
- `pin_code VARCHAR2(10) NOT NULL` (UNIQUE `uq_area_pin`)
- `is_active CHAR(1) DEFAULT 'Y' NOT NULL` (CHECK `is_active IN ('Y','N')`)
- `created_at DATE DEFAULT SYSDATE NOT NULL`

### 2. `CUSTOMER`
- `customer_id NUMBER` (PK, Identity)
- `customer_type VARCHAR2(20) NOT NULL` (CHECK `customer_type IN ('INDIVIDUAL','BUSINESS')`)
- `first_name VARCHAR2(100) NOT NULL`
- `last_name VARCHAR2(100) NOT NULL`
- `company_name VARCHAR2(200)`
- `email VARCHAR2(255) NOT NULL` (UNIQUE `uq_customer_email`)
- `phone VARCHAR2(20) NOT NULL`
- `alternate_phone VARCHAR2(20)`
- `area_id NUMBER NOT NULL` (FK -> `service_area(area_id)`)
- `status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL` (CHECK `status IN ('ACTIVE','INACTIVE','BLACKLISTED')`)
- `id_proof_type VARCHAR2(50)`
- `id_proof_number VARCHAR2(100)`
- `gst_number VARCHAR2(20)`
- `created_at DATE DEFAULT SYSDATE NOT NULL`
- `updated_at DATE DEFAULT SYSDATE NOT NULL`
- CHECK: `customer_type = 'INDIVIDUAL' OR company_name IS NOT NULL`

### 3. `ADDRESS`
- `address_id NUMBER` (PK, Identity)
- `customer_id NUMBER NOT NULL` (FK -> `customer(customer_id) ON DELETE CASCADE`)
- `address_type VARCHAR2(20) DEFAULT 'BOTH' NOT NULL` (CHECK `address_type IN ('BILLING','INSTALLATION','BOTH')`)
- `street_line1 VARCHAR2(255) NOT NULL`
- `street_line2 VARCHAR2(255)`
- `city VARCHAR2(100) NOT NULL`
- `state_name VARCHAR2(100) NOT NULL`
- `pin_code VARCHAR2(10) NOT NULL`
- `is_primary CHAR(1) DEFAULT 'N' NOT NULL` (CHECK `is_primary IN ('Y','N')`)

### 4. `PLAN`
- `plan_id NUMBER` (PK, Identity)
- `plan_name VARCHAR2(100) NOT NULL` (UNIQUE `uq_plan_name`)
- `plan_type VARCHAR2(20) NOT NULL` (CHECK `plan_type IN ('BROADBAND','FIBER','WIRELESS','LEASED_LINE')`)
- `speed_mbps NUMBER(6) NOT NULL` (CHECK `speed_mbps > 0`)
- `upload_mbps NUMBER(6) DEFAULT 0 NOT NULL`
- `data_cap_gb NUMBER(6)` (CHECK `data_cap_gb IS NULL OR data_cap_gb > 0`)
- `price NUMBER(12,2) NOT NULL` (CHECK `price >= 0`)
- `billing_cycle VARCHAR2(20) DEFAULT 'MONTHLY' NOT NULL` (CHECK `billing_cycle IN ('MONTHLY','QUARTERLY','ANNUALLY')`)
- `is_active CHAR(1) DEFAULT 'Y' NOT NULL` (CHECK `is_active IN ('Y','N')`)
- `description CLOB`
- `created_at DATE DEFAULT SYSDATE NOT NULL`

### 5. `SUBSCRIPTION`
- `subscription_id NUMBER` (PK, Identity)
- `customer_id NUMBER NOT NULL` (FK -> `customer(customer_id)`)
- `plan_id NUMBER NOT NULL` (FK -> `plan(plan_id)`)
- `area_id NUMBER NOT NULL` (FK -> `service_area(area_id)`)
- `start_date DATE NOT NULL`
- `end_date DATE`
- `status VARCHAR2(20) DEFAULT 'PENDING' NOT NULL` (CHECK `status IN ('PENDING','ACTIVE','SUSPENDED','TERMINATED')`)
- `notes CLOB`
- `created_at DATE DEFAULT SYSDATE NOT NULL`
- `updated_at DATE DEFAULT SYSDATE NOT NULL`
- CHECK: `end_date IS NULL OR end_date >= start_date`

### 6. `EQUIPMENT`
- `equipment_id NUMBER DEFAULT seq_equipment_id.NEXTVAL` (PK)
- `equipment_type VARCHAR2(20) NOT NULL` (CHECK `equipment_type IN ('ROUTER','ONT','MODEM','SWITCH','OTHER')`)
- `brand VARCHAR2(100) NOT NULL`
- `model VARCHAR2(100) NOT NULL`
- `serial_number VARCHAR2(100) NOT NULL` (UNIQUE `uq_equip_serial`)
- `mac_address VARCHAR2(17)` (UNIQUE `uq_equip_mac`)
- `status VARCHAR2(20) DEFAULT 'IN_STOCK' NOT NULL` (CHECK `status IN ('IN_STOCK','ASSIGNED','FAULTY','RETIRED')`)
- `subscription_id NUMBER` (FK -> `subscription(subscription_id) ON DELETE SET NULL`)
- `purchase_date DATE`
- `warranty_expiry DATE`
- `created_at DATE DEFAULT SYSDATE NOT NULL`
- `updated_at DATE DEFAULT SYSDATE NOT NULL`

### 7. `TECHNICIAN`
- `technician_id NUMBER` (PK, Identity)
- `first_name VARCHAR2(100) NOT NULL`
- `last_name VARCHAR2(100) NOT NULL`
- `email VARCHAR2(255) NOT NULL` (UNIQUE `uq_tech_email`)
- `phone VARCHAR2(20) NOT NULL`
- `specialization VARCHAR2(100)`
- `area_id NUMBER NOT NULL` (FK -> `service_area(area_id)`)
- `is_active CHAR(1) DEFAULT 'Y' NOT NULL` (CHECK `is_active IN ('Y','N')`)
- `hired_date DATE`

### 8. `INSTALLATION`
- `installation_id NUMBER` (PK, Identity)
- `subscription_id NUMBER NOT NULL` (UNIQUE `uq_install_sub`, FK -> `subscription(subscription_id) ON DELETE CASCADE`)
- `technician_id NUMBER` (FK -> `technician(technician_id) ON DELETE SET NULL`)
- `scheduled_date DATE`
- `completed_date DATE`
- `status VARCHAR2(20) DEFAULT 'PENDING' NOT NULL` (CHECK `status IN ('PENDING','SCHEDULED','IN_PROGRESS','COMPLETED','CANCELLED')`)
- `notes CLOB`
- `created_at DATE DEFAULT SYSDATE NOT NULL`

### 9. `INVOICE`
- `invoice_id NUMBER` (PK, Identity)
- `subscription_id NUMBER NOT NULL` (FK -> `subscription(subscription_id)`)
- `billing_period_start DATE NOT NULL`
- `billing_period_end DATE NOT NULL`
- `subtotal NUMBER(12,2) DEFAULT 0 NOT NULL`
- `tax_amount NUMBER(12,2) DEFAULT 0 NOT NULL`
- `discount_amount NUMBER(12,2) DEFAULT 0 NOT NULL`
- `late_fee NUMBER(12,2) DEFAULT 0 NOT NULL`
- `total_amount NUMBER(12,2) DEFAULT 0 NOT NULL` (CHECK `total_amount >= 0`)
- `amount_paid NUMBER(12,2) DEFAULT 0 NOT NULL` (CHECK `amount_paid >= 0`)
- `due_date DATE NOT NULL`
- `status VARCHAR2(20) DEFAULT 'DRAFT' NOT NULL` (CHECK `status IN ('DRAFT','ISSUED','PARTIALLY_PAID','PAID','OVERDUE','CANCELLED')`)
- `issued_at DATE`
- `created_at DATE DEFAULT SYSDATE NOT NULL`
- `updated_at DATE DEFAULT SYSDATE NOT NULL`
- CHECK: `billing_period_end >= billing_period_start`

### 10. `INVOICE_ITEM`
- `item_id NUMBER` (PK, Identity)
- `invoice_id NUMBER NOT NULL` (FK -> `invoice(invoice_id) ON DELETE CASCADE`)
- `description VARCHAR2(255) NOT NULL`
- `quantity NUMBER(5) DEFAULT 1 NOT NULL`
- `unit_price NUMBER(12,2) NOT NULL`
- `total_price NUMBER(12,2) NOT NULL`

### 11. `PAYMENT`
- `payment_id NUMBER` (PK, Identity)
- `invoice_id NUMBER NOT NULL` (FK -> `invoice(invoice_id)`)
- `amount NUMBER(12,2) NOT NULL` (CHECK `amount > 0`)
- `payment_method VARCHAR2(20) NOT NULL` (CHECK `payment_method IN ('CASH','CARD','UPI','BANK_TRANSFER','CHEQUE')`)
- `transaction_ref VARCHAR2(100)`
- `payment_date DATE NOT NULL`
- `recorded_by NUMBER` (app_user_id)
- `notes CLOB`
- `created_at DATE DEFAULT SYSDATE NOT NULL`

### 12. `USAGE_RECORD`
- `usage_id NUMBER` (PK, Identity)
- `subscription_id NUMBER NOT NULL` (FK -> `subscription(subscription_id) ON DELETE CASCADE`)
- `usage_date DATE NOT NULL`
- `data_used_mb NUMBER(12,2) DEFAULT 0 NOT NULL` (CHECK `data_used_mb >= 0`)
- `created_at DATE DEFAULT SYSDATE NOT NULL`
- UNIQUE constraint: `uq_usage_daily (subscription_id, usage_date)`

### 13. `SUPPORT_TICKET`
- `ticket_id NUMBER` (PK, Identity)
- `customer_id NUMBER NOT NULL` (FK -> `customer(customer_id)`)
- `subscription_id NUMBER` (FK -> `subscription(subscription_id) ON DELETE SET NULL`)
- `category VARCHAR2(20) NOT NULL` (CHECK `category IN ('CONNECTIVITY','BILLING','EQUIPMENT','SPEED','OTHER')`)
- `priority VARCHAR2(10) DEFAULT 'MEDIUM' NOT NULL` (CHECK `priority IN ('LOW','MEDIUM','HIGH','CRITICAL')`)
- `subject VARCHAR2(255) NOT NULL`
- `description CLOB`
- `status VARCHAR2(20) DEFAULT 'OPEN' NOT NULL` (CHECK `status IN ('OPEN','IN_PROGRESS','PENDING_CUSTOMER','RESOLVED','CLOSED')`)
- `assigned_to NUMBER` (FK -> `technician(technician_id) ON DELETE SET NULL`)
- `resolution CLOB`
- `created_at DATE DEFAULT SYSDATE NOT NULL`
- `updated_at DATE DEFAULT SYSDATE NOT NULL`
- `resolved_at DATE`

### 14. `TICKET_UPDATE`
- `update_id NUMBER` (PK, Identity)
- `ticket_id NUMBER NOT NULL` (FK -> `support_ticket(ticket_id) ON DELETE CASCADE`)
- `author_id NUMBER` (FK -> `technician(technician_id) ON DELETE SET NULL`)
- `comment_txt CLOB NOT NULL`
- `new_status VARCHAR2(20)` (CHECK `new_status IS NULL OR new_status IN ('OPEN','IN_PROGRESS','PENDING_CUSTOMER','RESOLVED','CLOSED')`)
- `created_at DATE DEFAULT SYSDATE NOT NULL`

### 15. `NETWORK_OUTAGE`
- `outage_id NUMBER` (PK, Identity)
- `title VARCHAR2(255) NOT NULL`
- `cause CLOB`
- `area_id NUMBER NOT NULL` (FK -> `service_area(area_id)`)
- `severity VARCHAR2(10) DEFAULT 'MEDIUM' NOT NULL` (CHECK `severity IN ('LOW','MEDIUM','HIGH','CRITICAL')`)
- `start_time DATE NOT NULL`
- `end_time DATE`
- `resolved_by NUMBER` (FK -> `technician(technician_id) ON DELETE SET NULL`)
- `created_at DATE DEFAULT SYSDATE NOT NULL`
- CHECK: `end_time IS NULL OR end_time >= start_time`

### 16. `APP_USER`
- `user_id NUMBER` (PK, Identity)
- `username VARCHAR2(100) NOT NULL` (UNIQUE `uq_user_username`)
- `password_hash VARCHAR2(255) NOT NULL`
- `role VARCHAR2(20) NOT NULL` (CHECK `role IN ('ADMIN','BILLING_CLERK','TECHNICIAN','SUPPORT_AGENT','CUSTOMER')`)
- `customer_id NUMBER` (FK -> `customer(customer_id) ON DELETE SET NULL`)
- `technician_id NUMBER` (FK -> `technician(technician_id) ON DELETE SET NULL`)
- `is_active CHAR(1) DEFAULT 'Y' NOT NULL` (CHECK `is_active IN ('Y','N')`)
- `last_login DATE`
- `created_at DATE DEFAULT SYSDATE NOT NULL`

### 17. `AUDIT_LOG`
- `log_id NUMBER` (PK, Identity)
- `table_name VARCHAR2(100) NOT NULL`
- `operation VARCHAR2(10) NOT NULL` (CHECK `operation IN ('INSERT','UPDATE','DELETE')`)
- `record_id VARCHAR2(100) NOT NULL`
- `old_values CLOB` (JSON-like snapshot)
- `new_values CLOB` (JSON-like snapshot)
- `changed_by VARCHAR2(100)` (DB `USER`)
- `changed_at DATE DEFAULT SYSDATE NOT NULL`

---

## 3. Views and Output Columns

1. **`V_ACTIVE_SUBSCRIPTIONS`**:
   - `subscription_id`, `start_date`, `subscription_status`, `customer_id`, `customer_name`, `email`, `phone`, `plan_name`, `plan_type`, `speed_mbps`, `monthly_price`, `area_name`, `city`, `pin_code`
2. **`V_OVERDUE_INVOICES`**:
   - `invoice_id`, `subscription_id`, `customer_name`, `email`, `phone`, `plan_name`, `total_amount`, `amount_paid`, `outstanding`, `due_date`, `days_overdue`, `status`
3. **`V_CUSTOMER_BALANCE`**:
   - `customer_id`, `customer_name`, `email`, `total_invoices`, `total_billed`, `total_paid`, `outstanding_balance`
4. **`V_MONTHLY_REVENUE`**:
   - `revenue_month`, `plan_type`, `plan_name`, `payment_count`, `revenue_collected`
5. **`V_TECHNICIAN_WORKLOAD`**:
   - `technician_id`, `technician_name`, `specialization`, `area_name`, `pending_installations`, `open_tickets`, `total_workload`, `workload_status`
6. **`V_DATA_USAGE_SUMMARY`**:
   - `subscription_id`, `customer_name`, `plan_name`, `data_cap_gb`, `usage_month`, `data_used_gb`, `cap_status`

---

## 4. Package `PKG_BILLING` Procedures & Functions

All subprograms reside in `pkg_billing`:

| Subprogram Name | Type | Signature | Description |
|---|---|---|---|
| `fn_customer_outstanding_balance` | Function | `(p_customer_id IN NUMBER) RETURN NUMBER` | Sums `total_amount - amount_paid` for unpaid/non-cancelled invoices for a customer. |
| `fn_calculate_late_fee` | Function | `(p_invoice_id IN NUMBER) RETURN NUMBER` | Computes 2% monthly late fee on overdue balance based on days past due date. |
| `sp_record_payment` | Procedure | `(p_invoice_id IN NUMBER, p_amount IN NUMBER, p_method IN VARCHAR2, p_transaction_ref IN VARCHAR2, p_recorded_by IN NUMBER, p_result_msg OUT VARCHAR2)` | Locks the invoice, inserts a row into `payment`, updates `invoice.amount_paid` and `invoice.status`, returns status message. |
| `sp_generate_monthly_invoices` | Procedure | `(p_billing_year IN NUMBER, p_billing_month IN NUMBER, p_invoices_created OUT NUMBER)` | Identifies active subscriptions lacking an invoice for the given month, generates invoices with 18% GST tax, returns count. |
| `sp_suspend_overdue_accounts` | Procedure | `(p_suspended_count OUT NUMBER)` | Marks invoices > 30 days overdue and suspends subscriptions with unpaid overdue invoices. |
| `sp_get_active_subscriptions` | Procedure | `(p_cursor OUT SYS_REFCURSOR)` | Returns a ref cursor of all subscriptions in 'ACTIVE' status. |

---

## 5. APP_USER & Security Analysis

- **Columns**: `user_id`, `username`, `password_hash`, `role`, `customer_id`, `technician_id`, `is_active`, `last_login`, `created_at`
- **Allowed Roles**: `'ADMIN'`, `'BILLING_CLERK'`, `'TECHNICIAN'`, `'SUPPORT_AGENT'`, `'CUSTOMER'`
- **Seeded Users in `V003__seed_data.sql`**:
  - `admin` (Role: `ADMIN`)
  - `billing1` (Role: `BILLING_CLERK`)
  - `tech.rajesh` (Role: `TECHNICIAN`, technician_id: 1)
  - `support.priya` (Role: `SUPPORT_AGENT`, technician_id: 2)
- **Password Format**:
  - In `V003__seed_data.sql`, the seeded values are dummy placeholders:
    `$2a$12$REPLACE_HASH_admin`, `$2a$12$REPLACE_HASH_billing`, `$2a$12$REPLACE_HASH_tech`, `$2a$12$REPLACE_HASH_supp`.
  - These are **not valid BCrypt hashes of real passwords**. A one-time Java utility `PasswordUtil` must be run to generate genuine BCrypt hashes in `APP_USER`.

---

## 6. Status and Enum Values from CHECK Constraints

- **`customer.customer_type`**: `'INDIVIDUAL'`, `'BUSINESS'`
- **`customer.status`**: `'ACTIVE'`, `'INACTIVE'`, `'BLACKLISTED'`
- **`address.address_type`**: `'BILLING'`, `'INSTALLATION'`, `'BOTH'`
- **`plan.plan_type`**: `'BROADBAND'`, `'FIBER'`, `'WIRELESS'`, `'LEASED_LINE'`
- **`plan.billing_cycle`**: `'MONTHLY'`, `'QUARTERLY'`, `'ANNUALLY'`
- **`subscription.status`**: `'PENDING'`, `'ACTIVE'`, `'SUSPENDED'`, `'TERMINATED'`
- **`equipment.equipment_type`**: `'ROUTER'`, `'ONT'`, `'MODEM'`, `'SWITCH'`, `'OTHER'`
- **`equipment.status`**: `'IN_STOCK'`, `'ASSIGNED'`, `'FAULTY'`, `'RETIRED'`
- **`installation.status`**: `'PENDING'`, `'SCHEDULED'`, `'IN_PROGRESS'`, `'COMPLETED'`, `'CANCELLED'`
- **`invoice.status`**: `'DRAFT'`, `'ISSUED'`, `'PARTIALLY_PAID'`, `'PAID'`, `'OVERDUE'`, `'CANCELLED'`
- **`payment.payment_method`**: `'CASH'`, `'CARD'`, `'UPI'`, `'BANK_TRANSFER'`, `'CHEQUE'`
- **`support_ticket.category`**: `'CONNECTIVITY'`, `'BILLING'`, `'EQUIPMENT'`, `'SPEED'`, `'OTHER'`
- **`support_ticket.priority`**: `'LOW'`, `'MEDIUM'`, `'HIGH'`, `'CRITICAL'`
- **`support_ticket.status`**: `'OPEN'`, `'IN_PROGRESS'`, `'PENDING_CUSTOMER'`, `'RESOLVED'`, `'CLOSED'`
- **`network_outage.severity`**: `'LOW'`, `'MEDIUM'`, `'HIGH'`, `'CRITICAL'`
- **`app_user.role`**: `'ADMIN'`, `'BILLING_CLERK'`, `'TECHNICIAN'`, `'SUPPORT_AGENT'`, `'CUSTOMER'`
- **Boolean flags (`CHAR(1)`)**: `'Y'`, `'N'` (`is_active`, `is_primary`)

---

## 7. How Billing Works: Payments & Triggers

### Who updates `invoice.amount_paid`?
- **`trg_invoice_status_compound`**: As inspected in `V006__triggers.sql`, this compound trigger **does NOT update `amount_paid`**. It only reads `amount_paid` and `total_amount` to adjust `invoice.status` to `'PAID'` or `'PARTIALLY_PAID'`.
- **`pkg_billing.sp_record_payment`**: This stored procedure performs:
  ```sql
  SELECT total_amount, amount_paid INTO v_total, v_paid FROM invoice WHERE invoice_id = p_invoice_id FOR UPDATE NOWAIT;
  ...
  INSERT INTO payment (...) VALUES (...);
  UPDATE invoice SET amount_paid = v_new_paid, status = v_new_status WHERE invoice_id = p_invoice_id;
  ```
- **Architectural Directive for Java**:
  The Java application **MUST call `pkg_billing.sp_record_payment`** when recording a payment. If Java inserts directly into `payment`, `amount_paid` will never increment (it remains 0), causing the compound trigger to incorrectly keep the invoice status as `'ISSUED'`. `sp_record_payment` handles pessimistic locking (`FOR UPDATE NOWAIT`), validation, payment insertion, and invoice balance calculation atomically.

---

## 8. Empty Tables: Origins & Populating Logic

Four tables contain 0 rows after initial setup:

1. **`ADDRESS`**:
   - *Why empty*: `V003__seed_data.sql` seeds customer contact details and PIN codes via `service_area`, but did not populate the optional normalized `address` table.
   - *How filled*: Populated by user input when managing customer delivery/installation physical addresses.
2. **`INVOICE_ITEM`**:
   - *Why empty*: In `V003__seed_data.sql` and `pkg_billing.sp_generate_monthly_invoices`, invoices are generated with aggregate headers (`subtotal`, `tax_amount`, `total_amount`) directly into `invoice`. Neither the seed script nor `sp_generate_monthly_invoices` writes into `invoice_item`.
   - *How filled*: Can be populated when detailed line-item breakdowns (e.g. plan fee, equipment deposit, router rental) are entered by billing clerks or expanded in billing procedures.
3. **`TICKET_UPDATE`**:
   - *Why empty*: `V003__seed_data.sql` inserts base `support_ticket` records with their initial description, but no follow-up timeline comments.
   - *How filled*: Populated when agents or technicians post comments, status transitions, or work logs on an open ticket.
4. **`AUDIT_LOG`**:
   - *Why empty*: Triggers in `V006__triggers.sql` (`trg_customer_audit`, `trg_payment_audit`, `trg_plan_price_audit`) were created **after** `V003__seed_data.sql` was executed in `run_all.sql`.
   - *How filled*: Automatically populated by database triggers on subsequent DML operations (customer INSERT/UPDATE/DELETE, payment INSERT, and plan price UPDATE).
