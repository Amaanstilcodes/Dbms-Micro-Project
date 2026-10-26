-- ============================================================
-- FILE    : V001__create_core_tables.sql
-- PURPOSE : Oracle DDL for all ISP Management System tables.
--           Run as: isp_app@//localhost:1521/XEPDB1
-- AUTHOR  : ISP Dev Team
-- DATE    : 2026-10-26
-- NOTES   :
--   - Oracle has no ON UPDATE CASCADE; FK references are handled
--     via ON DELETE CASCADE or ON DELETE SET NULL only.
--   - No ENUM types in Oracle -> VARCHAR2 + CHECK constraints.
--   - GENERATED ALWAYS AS IDENTITY (Oracle 12c+) used for PKs.
--   - One table (TECHNICIAN) uses old-style SEQUENCE + trigger
--     to demonstrate the traditional approach.
--   - Oracle DATE stores both date and time components.
--   - Empty string '' = NULL in Oracle; validated in Java layer.
--   - All VARCHAR2 lengths in bytes unless CHAR specified.
-- ============================================================

SET SERVEROUTPUT ON SIZE UNLIMITED
SET FEEDBACK ON
WHENEVER SQLERROR EXIT ROLLBACK

-- ============================================================
-- TABLE: SERVICE_AREA
-- Geographic coverage zones identified by pin code.
-- PRIMARY KEY: identity column (Oracle 12c+ style)
-- ============================================================
CREATE TABLE service_area (
    area_id     NUMBER         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    area_name   VARCHAR2(100)  NOT NULL,
    city        VARCHAR2(100)  NOT NULL,
    state_name  VARCHAR2(100)  NOT NULL,
    pin_code    VARCHAR2(10)   NOT NULL,
    is_active   CHAR(1)        DEFAULT 'Y' NOT NULL,
    created_at  DATE           DEFAULT SYSDATE NOT NULL,
    CONSTRAINT uq_area_pin UNIQUE (pin_code),
    CONSTRAINT chk_area_active CHECK (is_active IN ('Y','N'))
);

COMMENT ON TABLE  service_area IS 'Geographic PIN-code zones covered by the ISP';
COMMENT ON COLUMN service_area.is_active IS 'Y=active coverage zone, N=discontinued';

-- ============================================================
-- TABLE: CUSTOMER
-- Individual or business subscriber.
-- ============================================================
CREATE TABLE customer (
    customer_id     NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_type   VARCHAR2(20)    NOT NULL,
    first_name      VARCHAR2(100)   NOT NULL,
    last_name       VARCHAR2(100)   NOT NULL,
    company_name    VARCHAR2(200),              -- populated for BUSINESS type
    email           VARCHAR2(255)   NOT NULL,
    phone           VARCHAR2(20)    NOT NULL,
    alternate_phone VARCHAR2(20),
    area_id         NUMBER          NOT NULL,
    status          VARCHAR2(20)    DEFAULT 'ACTIVE' NOT NULL,
    id_proof_type   VARCHAR2(50),
    id_proof_number VARCHAR2(100),
    gst_number      VARCHAR2(20),               -- for BUSINESS type
    created_at      DATE            DEFAULT SYSDATE NOT NULL,
    updated_at      DATE            DEFAULT SYSDATE NOT NULL,
    CONSTRAINT fk_customer_area FOREIGN KEY (area_id)
        REFERENCES service_area(area_id) ON DELETE RESTRICT,
    CONSTRAINT uq_customer_email  UNIQUE (email),
    CONSTRAINT chk_cust_type   CHECK (customer_type IN ('INDIVIDUAL','BUSINESS')),
    CONSTRAINT chk_cust_status CHECK (status         IN ('ACTIVE','INACTIVE','BLACKLISTED')),
    CONSTRAINT chk_cust_company CHECK (
        customer_type = 'INDIVIDUAL' OR company_name IS NOT NULL
    )
);

COMMENT ON TABLE  customer IS 'ISP subscribers - individual and business accounts';
COMMENT ON COLUMN customer.updated_at IS 'Updated by trg_customer_updated_at trigger';

-- ============================================================
-- TABLE: ADDRESS
-- Multiple addresses per customer (billing / installation).
-- ============================================================
CREATE TABLE address (
    address_id    NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id   NUMBER          NOT NULL,
    address_type  VARCHAR2(20)    DEFAULT 'BOTH' NOT NULL,
    street_line1  VARCHAR2(255)   NOT NULL,
    street_line2  VARCHAR2(255),
    city          VARCHAR2(100)   NOT NULL,
    state_name    VARCHAR2(100)   NOT NULL,
    pin_code      VARCHAR2(10)    NOT NULL,
    is_primary    CHAR(1)         DEFAULT 'N' NOT NULL,
    CONSTRAINT fk_address_customer FOREIGN KEY (customer_id)
        REFERENCES customer(customer_id) ON DELETE CASCADE,
    CONSTRAINT chk_addr_type    CHECK (address_type IN ('BILLING','INSTALLATION','BOTH')),
    CONSTRAINT chk_addr_primary CHECK (is_primary   IN ('Y','N'))
);

-- ============================================================
-- TABLE: PLAN
-- Internet service plans. No Oracle ENUM: CHECK used instead.
-- ============================================================
CREATE TABLE plan (
    plan_id        NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    plan_name      VARCHAR2(100)   NOT NULL,
    plan_type      VARCHAR2(20)    NOT NULL,
    speed_mbps     NUMBER(6)       NOT NULL,
    upload_mbps    NUMBER(6)       DEFAULT 0 NOT NULL,
    data_cap_gb    NUMBER(6),                   -- NULL = unlimited
    price          NUMBER(12,2)    NOT NULL,
    billing_cycle  VARCHAR2(20)    DEFAULT 'MONTHLY' NOT NULL,
    is_active      CHAR(1)         DEFAULT 'Y' NOT NULL,
    description    CLOB,
    created_at     DATE            DEFAULT SYSDATE NOT NULL,
    CONSTRAINT uq_plan_name      UNIQUE (plan_name),
    CONSTRAINT chk_plan_type     CHECK (plan_type      IN ('BROADBAND','FIBER','WIRELESS','LEASED_LINE')),
    CONSTRAINT chk_plan_cycle    CHECK (billing_cycle  IN ('MONTHLY','QUARTERLY','ANNUALLY')),
    CONSTRAINT chk_plan_active   CHECK (is_active      IN ('Y','N')),
    CONSTRAINT chk_plan_speed    CHECK (speed_mbps > 0),
    CONSTRAINT chk_plan_price    CHECK (price >= 0),
    CONSTRAINT chk_plan_cap      CHECK (data_cap_gb IS NULL OR data_cap_gb > 0)
);

-- ============================================================
-- TABLE: SUBSCRIPTION
-- Contract between a customer and a plan.
-- ============================================================
CREATE TABLE subscription (
    subscription_id NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id     NUMBER          NOT NULL,
    plan_id         NUMBER          NOT NULL,
    area_id         NUMBER          NOT NULL,
    start_date      DATE            NOT NULL,
    end_date        DATE,                        -- NULL = open-ended
    status          VARCHAR2(20)    DEFAULT 'PENDING' NOT NULL,
    notes           CLOB,
    created_at      DATE            DEFAULT SYSDATE NOT NULL,
    updated_at      DATE            DEFAULT SYSDATE NOT NULL,
    CONSTRAINT fk_sub_customer FOREIGN KEY (customer_id)
        REFERENCES customer(customer_id) ON DELETE RESTRICT,
    CONSTRAINT fk_sub_plan     FOREIGN KEY (plan_id)
        REFERENCES plan(plan_id) ON DELETE RESTRICT,
    CONSTRAINT fk_sub_area     FOREIGN KEY (area_id)
        REFERENCES service_area(area_id) ON DELETE RESTRICT,
    CONSTRAINT chk_sub_status  CHECK (status IN ('PENDING','ACTIVE','SUSPENDED','TERMINATED')),
    CONSTRAINT chk_sub_dates   CHECK (end_date IS NULL OR end_date >= start_date)
);

-- ============================================================
-- TABLE: EQUIPMENT
-- Physical network equipment. Old-style SEQUENCE + trigger
-- to demonstrate the traditional Oracle PK pattern.
-- TRADE-OFF vs IDENTITY:
--   IDENTITY: simpler DDL, value managed by DB, cannot pre-allocate.
--   SEQUENCE: more control (CACHE, NOCYCLE, order), sequence value
--   accessible before INSERT via seq.NEXTVAL, reusable across tables.
-- ============================================================
CREATE SEQUENCE seq_equipment_id
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE equipment (
    equipment_id   NUMBER          DEFAULT seq_equipment_id.NEXTVAL PRIMARY KEY,
    equipment_type VARCHAR2(20)    NOT NULL,
    brand          VARCHAR2(100)   NOT NULL,
    model          VARCHAR2(100)   NOT NULL,
    serial_number  VARCHAR2(100)   NOT NULL,
    mac_address    VARCHAR2(17),               -- format AA:BB:CC:DD:EE:FF
    status         VARCHAR2(20)    DEFAULT 'IN_STOCK' NOT NULL,
    subscription_id NUMBER,                    -- NULL = not assigned
    purchase_date  DATE,
    warranty_expiry DATE,
    created_at     DATE            DEFAULT SYSDATE NOT NULL,
    updated_at     DATE            DEFAULT SYSDATE NOT NULL,
    CONSTRAINT uq_equip_serial    UNIQUE (serial_number),
    CONSTRAINT uq_equip_mac       UNIQUE (mac_address),
    CONSTRAINT fk_equip_sub       FOREIGN KEY (subscription_id)
        REFERENCES subscription(subscription_id) ON DELETE SET NULL,
    CONSTRAINT chk_equip_type     CHECK (equipment_type IN ('ROUTER','ONT','MODEM','SWITCH','OTHER')),
    CONSTRAINT chk_equip_status   CHECK (status IN ('IN_STOCK','ASSIGNED','FAULTY','RETIRED'))
);

-- ============================================================
-- TABLE: TECHNICIAN
-- ISP employees who handle installations and support.
-- ============================================================
CREATE TABLE technician (
    technician_id   NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    first_name      VARCHAR2(100)   NOT NULL,
    last_name       VARCHAR2(100)   NOT NULL,
    email           VARCHAR2(255)   NOT NULL,
    phone           VARCHAR2(20)    NOT NULL,
    specialization  VARCHAR2(100),
    area_id         NUMBER          NOT NULL,
    is_active       CHAR(1)         DEFAULT 'Y' NOT NULL,
    hired_date      DATE,
    CONSTRAINT uq_tech_email      UNIQUE (email),
    CONSTRAINT fk_tech_area       FOREIGN KEY (area_id)
        REFERENCES service_area(area_id) ON DELETE RESTRICT,
    CONSTRAINT chk_tech_active    CHECK (is_active IN ('Y','N'))
);

-- ============================================================
-- TABLE: INSTALLATION
-- One installation record per subscription (1:1 enforced by UNIQUE).
-- ============================================================
CREATE TABLE installation (
    installation_id NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subscription_id NUMBER          NOT NULL,
    technician_id   NUMBER,                     -- NULL = not yet assigned
    scheduled_date  DATE,
    completed_date  DATE,
    status          VARCHAR2(20)    DEFAULT 'PENDING' NOT NULL,
    notes           CLOB,
    created_at      DATE            DEFAULT SYSDATE NOT NULL,
    CONSTRAINT uq_install_sub      UNIQUE (subscription_id),   -- enforces 1:1
    CONSTRAINT fk_install_sub      FOREIGN KEY (subscription_id)
        REFERENCES subscription(subscription_id) ON DELETE CASCADE,
    CONSTRAINT fk_install_tech     FOREIGN KEY (technician_id)
        REFERENCES technician(technician_id) ON DELETE SET NULL,
    CONSTRAINT chk_install_status  CHECK (status IN ('PENDING','SCHEDULED','IN_PROGRESS','COMPLETED','CANCELLED'))
);

-- ============================================================
-- TABLE: INVOICE
-- Periodic bill for a subscription.
-- ============================================================
CREATE TABLE invoice (
    invoice_id            NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subscription_id       NUMBER          NOT NULL,
    billing_period_start  DATE            NOT NULL,
    billing_period_end    DATE            NOT NULL,
    subtotal              NUMBER(12,2)    DEFAULT 0 NOT NULL,
    tax_amount            NUMBER(12,2)    DEFAULT 0 NOT NULL,
    discount_amount       NUMBER(12,2)    DEFAULT 0 NOT NULL,
    late_fee              NUMBER(12,2)    DEFAULT 0 NOT NULL,
    total_amount          NUMBER(12,2)    DEFAULT 0 NOT NULL,
    amount_paid           NUMBER(12,2)    DEFAULT 0 NOT NULL,
    due_date              DATE            NOT NULL,
    status                VARCHAR2(20)    DEFAULT 'DRAFT' NOT NULL,
    issued_at             DATE,
    created_at            DATE            DEFAULT SYSDATE NOT NULL,
    updated_at            DATE            DEFAULT SYSDATE NOT NULL,
    CONSTRAINT fk_invoice_sub     FOREIGN KEY (subscription_id)
        REFERENCES subscription(subscription_id) ON DELETE RESTRICT,
    CONSTRAINT chk_inv_status     CHECK (status IN ('DRAFT','ISSUED','PARTIALLY_PAID','PAID','OVERDUE','CANCELLED')),
    CONSTRAINT chk_inv_total      CHECK (total_amount >= 0),
    CONSTRAINT chk_inv_paid       CHECK (amount_paid  >= 0),
    CONSTRAINT chk_inv_dates      CHECK (billing_period_end >= billing_period_start)
);

-- ============================================================
-- TABLE: INVOICE_ITEM
-- Line items within an invoice.
-- ============================================================
CREATE TABLE invoice_item (
    item_id      NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    invoice_id   NUMBER          NOT NULL,
    description  VARCHAR2(255)   NOT NULL,
    quantity     NUMBER(5)       DEFAULT 1 NOT NULL,
    unit_price   NUMBER(12,2)    NOT NULL,
    total_price  NUMBER(12,2)    NOT NULL,
    CONSTRAINT fk_item_invoice FOREIGN KEY (invoice_id)
        REFERENCES invoice(invoice_id) ON DELETE CASCADE
);

-- ============================================================
-- TABLE: PAYMENT
-- Payment transactions; partial payments supported.
-- ============================================================
CREATE TABLE payment (
    payment_id      NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    invoice_id      NUMBER          NOT NULL,
    amount          NUMBER(12,2)    NOT NULL,
    payment_method  VARCHAR2(20)    NOT NULL,
    transaction_ref VARCHAR2(100),
    payment_date    DATE            NOT NULL,
    recorded_by     NUMBER,                     -- app_user_id
    notes           CLOB,
    created_at      DATE            DEFAULT SYSDATE NOT NULL,
    CONSTRAINT fk_payment_invoice  FOREIGN KEY (invoice_id)
        REFERENCES invoice(invoice_id) ON DELETE RESTRICT,
    CONSTRAINT chk_pay_method  CHECK (payment_method IN ('CASH','CARD','UPI','BANK_TRANSFER','CHEQUE')),
    CONSTRAINT chk_pay_amount  CHECK (amount > 0)
);

-- ============================================================
-- TABLE: USAGE_RECORD
-- Daily data consumption per subscription.
-- ============================================================
CREATE TABLE usage_record (
    usage_id        NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subscription_id NUMBER          NOT NULL,
    usage_date      DATE            NOT NULL,
    data_used_mb    NUMBER(12,2)    DEFAULT 0 NOT NULL,
    created_at      DATE            DEFAULT SYSDATE NOT NULL,
    CONSTRAINT uq_usage_daily       UNIQUE (subscription_id, usage_date),
    CONSTRAINT fk_usage_sub         FOREIGN KEY (subscription_id)
        REFERENCES subscription(subscription_id) ON DELETE CASCADE,
    CONSTRAINT chk_usage_data       CHECK (data_used_mb >= 0)
);

-- ============================================================
-- TABLE: SUPPORT_TICKET
-- ============================================================
CREATE TABLE support_ticket (
    ticket_id       NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id     NUMBER          NOT NULL,
    subscription_id NUMBER,                     -- NULL = account-level
    category        VARCHAR2(20)    NOT NULL,
    priority        VARCHAR2(10)    DEFAULT 'MEDIUM' NOT NULL,
    subject         VARCHAR2(255)   NOT NULL,
    description     CLOB,
    status          VARCHAR2(20)    DEFAULT 'OPEN' NOT NULL,
    assigned_to     NUMBER,                     -- technician_id
    resolution      CLOB,
    created_at      DATE            DEFAULT SYSDATE NOT NULL,
    updated_at      DATE            DEFAULT SYSDATE NOT NULL,
    resolved_at     DATE,
    CONSTRAINT fk_ticket_customer  FOREIGN KEY (customer_id)
        REFERENCES customer(customer_id) ON DELETE RESTRICT,
    CONSTRAINT fk_ticket_sub       FOREIGN KEY (subscription_id)
        REFERENCES subscription(subscription_id) ON DELETE SET NULL,
    CONSTRAINT fk_ticket_tech      FOREIGN KEY (assigned_to)
        REFERENCES technician(technician_id) ON DELETE SET NULL,
    CONSTRAINT chk_ticket_cat      CHECK (category IN ('CONNECTIVITY','BILLING','EQUIPMENT','SPEED','OTHER')),
    CONSTRAINT chk_ticket_priority CHECK (priority  IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT chk_ticket_status   CHECK (status    IN ('OPEN','IN_PROGRESS','PENDING_CUSTOMER','RESOLVED','CLOSED'))
);

-- ============================================================
-- TABLE: TICKET_UPDATE
-- ============================================================
CREATE TABLE ticket_update (
    update_id   NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticket_id   NUMBER          NOT NULL,
    author_id   NUMBER,                         -- technician_id
    comment_txt CLOB            NOT NULL,       -- "comment" is not reserved but "COMMENT" used for DDL
    new_status  VARCHAR2(20),
    created_at  DATE            DEFAULT SYSDATE NOT NULL,
    CONSTRAINT fk_tupdate_ticket  FOREIGN KEY (ticket_id)
        REFERENCES support_ticket(ticket_id) ON DELETE CASCADE,
    CONSTRAINT fk_tupdate_author  FOREIGN KEY (author_id)
        REFERENCES technician(technician_id) ON DELETE SET NULL,
    CONSTRAINT chk_tupdate_status CHECK (new_status IS NULL OR
        new_status IN ('OPEN','IN_PROGRESS','PENDING_CUSTOMER','RESOLVED','CLOSED'))
);

-- ============================================================
-- TABLE: NETWORK_OUTAGE
-- ============================================================
CREATE TABLE network_outage (
    outage_id   NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title       VARCHAR2(255)   NOT NULL,
    cause       CLOB,
    area_id     NUMBER          NOT NULL,
    severity    VARCHAR2(10)    DEFAULT 'MEDIUM' NOT NULL,
    start_time  DATE            NOT NULL,
    end_time    DATE,                           -- NULL = ongoing
    resolved_by NUMBER,                         -- technician_id
    created_at  DATE            DEFAULT SYSDATE NOT NULL,
    CONSTRAINT fk_outage_area  FOREIGN KEY (area_id)
        REFERENCES service_area(area_id) ON DELETE RESTRICT,
    CONSTRAINT fk_outage_tech  FOREIGN KEY (resolved_by)
        REFERENCES technician(technician_id) ON DELETE SET NULL,
    CONSTRAINT chk_outage_sev  CHECK (severity IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT chk_outage_time CHECK (end_time IS NULL OR end_time >= start_time)
);

-- ============================================================
-- TABLE: APP_USER
-- Application login accounts. Named APP_USER (not USER, which
-- is a reserved Oracle word / built-in pseudocolumn).
-- ============================================================
CREATE TABLE app_user (
    user_id         NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username        VARCHAR2(100)   NOT NULL,
    password_hash   VARCHAR2(255)   NOT NULL,   -- BCrypt hash; NEVER plain text
    role            VARCHAR2(20)    NOT NULL,
    customer_id     NUMBER,                     -- linked for CUSTOMER role
    technician_id   NUMBER,                     -- linked for TECHNICIAN role
    is_active       CHAR(1)         DEFAULT 'Y' NOT NULL,
    last_login      DATE,
    created_at      DATE            DEFAULT SYSDATE NOT NULL,
    CONSTRAINT uq_user_username    UNIQUE (username),
    CONSTRAINT fk_user_customer    FOREIGN KEY (customer_id)
        REFERENCES customer(customer_id) ON DELETE SET NULL,
    CONSTRAINT fk_user_tech        FOREIGN KEY (technician_id)
        REFERENCES technician(technician_id) ON DELETE SET NULL,
    CONSTRAINT chk_user_role       CHECK (role IN ('ADMIN','BILLING_CLERK','TECHNICIAN','SUPPORT_AGENT','CUSTOMER')),
    CONSTRAINT chk_user_active     CHECK (is_active IN ('Y','N'))
);

-- ============================================================
-- TABLE: AUDIT_LOG
-- Written by database triggers using :OLD / :NEW pseudo-records.
-- ============================================================
CREATE TABLE audit_log (
    log_id      NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    table_name  VARCHAR2(100)   NOT NULL,
    operation   VARCHAR2(10)    NOT NULL,
    record_id   VARCHAR2(100)   NOT NULL,       -- PK of changed row as string
    old_values  CLOB,                           -- JSON-like snapshot of old row
    new_values  CLOB,                           -- JSON-like snapshot of new row
    changed_by  VARCHAR2(100),                  -- Oracle USER function value
    changed_at  DATE            DEFAULT SYSDATE NOT NULL,
    CONSTRAINT chk_audit_op CHECK (operation IN ('INSERT','UPDATE','DELETE'))
);

COMMIT;
PROMPT All core tables created successfully.
