-- ============================================================
-- FILE    : V001__create_core_tables.sql
-- PURPOSE : DDL for all core tables of the ISP Management System
-- AUTHOR  : ISP Dev Team
-- DATE    : 2026-10-26
-- NOTES   : Run against MySQL 8.0+. Create the database first:
--           CREATE DATABASE isp_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
--           USE isp_db;
-- ============================================================

-- Ensures a clean re-run during development. Remove in production.
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS
  audit_log, ticket_update, support_ticket, outage_affected_area,
  network_outage, usage_record, payment, invoice_item, invoice,
  installation, equipment, subscription, plan, address,
  service_area, customer, `user`;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- TABLE: service_area
-- Represents geographic coverage zones. One area per PIN code.
-- ============================================================
CREATE TABLE service_area (
    area_id       INT           NOT NULL AUTO_INCREMENT,
    area_name     VARCHAR(100)  NOT NULL,
    city          VARCHAR(100)  NOT NULL,
    state         VARCHAR(100)  NOT NULL,
    pin_code      VARCHAR(10)   NOT NULL,
    is_active     TINYINT(1)    NOT NULL DEFAULT 1,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_service_area     PRIMARY KEY (area_id),
    CONSTRAINT uq_service_area_pin UNIQUE      (pin_code)
) ENGINE=InnoDB COMMENT='Geographic zones/PIN codes served by the ISP';

-- ============================================================
-- TABLE: customer
-- Individual or business customer. Linked to a service_area.
-- ============================================================
CREATE TABLE customer (
    customer_id     INT              NOT NULL AUTO_INCREMENT,
    customer_type   ENUM('INDIVIDUAL','BUSINESS') NOT NULL DEFAULT 'INDIVIDUAL',
    first_name      VARCHAR(100)     NOT NULL,
    last_name       VARCHAR(100)     NOT NULL,
    company_name    VARCHAR(200)     NULL,       -- populated for BUSINESS type
    email           VARCHAR(255)     NOT NULL,
    phone           VARCHAR(20)      NOT NULL,
    alternate_phone VARCHAR(20)      NULL,
    area_id         INT              NOT NULL,
    status          ENUM('ACTIVE','INACTIVE','BLACKLISTED') NOT NULL DEFAULT 'ACTIVE',
    id_proof_type   VARCHAR(50)      NULL,       -- Aadhaar / PAN / Passport
    id_proof_number VARCHAR(100)     NULL,
    gst_number      VARCHAR(20)      NULL,       -- for BUSINESS type
    created_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_customer          PRIMARY KEY (customer_id),
    CONSTRAINT uq_customer_email    UNIQUE      (email),
    CONSTRAINT fk_customer_area     FOREIGN KEY (area_id)
        REFERENCES service_area(area_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_customer_company CHECK (
        customer_type = 'INDIVIDUAL' OR company_name IS NOT NULL
    )
) ENGINE=InnoDB COMMENT='ISP subscribers - individual and business';

-- ============================================================
-- TABLE: address
-- Multiple addresses per customer (billing / installation / both)
-- ============================================================
CREATE TABLE address (
    address_id    INT          NOT NULL AUTO_INCREMENT,
    customer_id   INT          NOT NULL,
    address_type  ENUM('BILLING','INSTALLATION','BOTH') NOT NULL DEFAULT 'BOTH',
    street_line1  VARCHAR(255) NOT NULL,
    street_line2  VARCHAR(255) NULL,
    city          VARCHAR(100) NOT NULL,
    state         VARCHAR(100) NOT NULL,
    pin_code      VARCHAR(10)  NOT NULL,
    is_primary    TINYINT(1)   NOT NULL DEFAULT 0,
    CONSTRAINT pk_address           PRIMARY KEY (address_id),
    CONSTRAINT fk_address_customer  FOREIGN KEY (customer_id)
        REFERENCES customer(customer_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Customer addresses; CASCADE delete when customer is removed';

-- ============================================================
-- TABLE: plan
-- Internet service plans. plan_type maps to Java enum PlanType.
-- ============================================================
CREATE TABLE plan (
    plan_id         INT             NOT NULL AUTO_INCREMENT,
    plan_name       VARCHAR(100)    NOT NULL,
    plan_type       ENUM('BROADBAND','FIBER','WIRELESS','LEASED_LINE') NOT NULL,
    speed_mbps      INT             NOT NULL,            -- download speed
    upload_mbps     INT             NOT NULL DEFAULT 0,  -- 0 = same as download (symmetric)
    data_cap_gb     INT             NULL,                -- NULL = unlimited
    price           DECIMAL(12,2)   NOT NULL,
    billing_cycle   ENUM('MONTHLY','QUARTERLY','ANNUALLY') NOT NULL DEFAULT 'MONTHLY',
    is_active       TINYINT(1)      NOT NULL DEFAULT 1,
    description     TEXT            NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_plan              PRIMARY KEY (plan_id),
    CONSTRAINT uq_plan_name         UNIQUE      (plan_name),
    CONSTRAINT chk_plan_speed       CHECK       (speed_mbps > 0),
    CONSTRAINT chk_plan_price       CHECK       (price >= 0),
    CONSTRAINT chk_plan_data_cap    CHECK       (data_cap_gb IS NULL OR data_cap_gb > 0)
) ENGINE=InnoDB COMMENT='Available internet service plans';

-- ============================================================
-- TABLE: subscription
-- The contract between a customer and a plan.
-- status: PENDING -> ACTIVE -> SUSPENDED | TERMINATED
-- ============================================================
CREATE TABLE subscription (
    subscription_id INT          NOT NULL AUTO_INCREMENT,
    customer_id     INT          NOT NULL,
    plan_id         INT          NOT NULL,
    area_id         INT          NOT NULL,       -- confirms coverage at subscription time
    start_date      DATE         NOT NULL,
    end_date        DATE         NULL,           -- NULL = open-ended
    status          ENUM('PENDING','ACTIVE','SUSPENDED','TERMINATED') NOT NULL DEFAULT 'PENDING',
    notes           TEXT         NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_subscription          PRIMARY KEY (subscription_id),
    CONSTRAINT fk_sub_customer          FOREIGN KEY (customer_id)
        REFERENCES customer(customer_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_sub_plan              FOREIGN KEY (plan_id)
        REFERENCES plan(plan_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_sub_area              FOREIGN KEY (area_id)
        REFERENCES service_area(area_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_sub_dates            CHECK (end_date IS NULL OR end_date >= start_date)
) ENGINE=InnoDB COMMENT='Customer-plan subscription contracts';

-- ============================================================
-- TABLE: equipment
-- Physical network equipment in inventory or assigned.
-- ============================================================
CREATE TABLE equipment (
    equipment_id    INT          NOT NULL AUTO_INCREMENT,
    equipment_type  ENUM('ROUTER','ONT','MODEM','SWITCH','OTHER') NOT NULL,
    brand           VARCHAR(100) NOT NULL,
    model           VARCHAR(100) NOT NULL,
    serial_number   VARCHAR(100) NOT NULL,
    mac_address     VARCHAR(17)  NULL,          -- format: AA:BB:CC:DD:EE:FF
    status          ENUM('IN_STOCK','ASSIGNED','FAULTY','RETIRED') NOT NULL DEFAULT 'IN_STOCK',
    subscription_id INT          NULL,          -- NULL = not currently assigned
    purchase_date   DATE         NULL,
    warranty_expiry DATE         NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_equipment             PRIMARY KEY (equipment_id),
    CONSTRAINT uq_equipment_serial      UNIQUE      (serial_number),
    CONSTRAINT uq_equipment_mac         UNIQUE      (mac_address),
    CONSTRAINT fk_equipment_sub         FOREIGN KEY (subscription_id)
        REFERENCES subscription(subscription_id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='ISP network equipment inventory and assignment';

-- ============================================================
-- TABLE: technician
-- ISP employee who handles installations and support.
-- ============================================================
CREATE TABLE technician (
    technician_id   INT          NOT NULL AUTO_INCREMENT,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    phone           VARCHAR(20)  NOT NULL,
    specialization  VARCHAR(100) NULL,          -- e.g., 'Fiber Optic', 'Wireless'
    area_id         INT          NOT NULL,      -- primary service area
    is_active       TINYINT(1)   NOT NULL DEFAULT 1,
    hired_date      DATE         NULL,
    CONSTRAINT pk_technician            PRIMARY KEY (technician_id),
    CONSTRAINT uq_technician_email      UNIQUE      (email),
    CONSTRAINT fk_technician_area       FOREIGN KEY (area_id)
        REFERENCES service_area(area_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB COMMENT='Technicians who perform installations and repairs';

-- ============================================================
-- TABLE: installation
-- One installation record per subscription (1:1).
-- ============================================================
CREATE TABLE installation (
    installation_id INT          NOT NULL AUTO_INCREMENT,
    subscription_id INT          NOT NULL,
    technician_id   INT          NULL,          -- NULL = not yet assigned
    scheduled_date  DATETIME     NULL,
    completed_date  DATETIME     NULL,
    status          ENUM('PENDING','SCHEDULED','IN_PROGRESS','COMPLETED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    notes           TEXT         NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_installation          PRIMARY KEY (installation_id),
    CONSTRAINT uq_installation_sub      UNIQUE      (subscription_id),   -- enforces 1:1
    CONSTRAINT fk_install_sub           FOREIGN KEY (subscription_id)
        REFERENCES subscription(subscription_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_install_tech          FOREIGN KEY (technician_id)
        REFERENCES technician(technician_id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='Installation jobs linked 1:1 to subscriptions';

-- ============================================================
-- TABLE: invoice
-- Monthly/periodic bill for a subscription.
-- ============================================================
CREATE TABLE invoice (
    invoice_id      INT           NOT NULL AUTO_INCREMENT,
    subscription_id INT           NOT NULL,
    billing_period_start DATE     NOT NULL,
    billing_period_end   DATE     NOT NULL,
    subtotal        DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    tax_amount      DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    late_fee        DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_amount    DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    amount_paid     DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    due_date        DATE          NOT NULL,
    status          ENUM('DRAFT','ISSUED','PARTIALLY_PAID','PAID','OVERDUE','CANCELLED') NOT NULL DEFAULT 'DRAFT',
    issued_at       DATETIME      NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_invoice               PRIMARY KEY (invoice_id),
    CONSTRAINT fk_invoice_sub           FOREIGN KEY (subscription_id)
        REFERENCES subscription(subscription_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_invoice_total        CHECK (total_amount >= 0),
    CONSTRAINT chk_invoice_paid         CHECK (amount_paid >= 0),
    CONSTRAINT chk_invoice_dates        CHECK (billing_period_end >= billing_period_start)
) ENGINE=InnoDB COMMENT='Customer invoices with tax, discount, late-fee breakdown';

-- ============================================================
-- TABLE: invoice_item
-- Line items within an invoice (plan charge, add-ons, penalties)
-- ============================================================
CREATE TABLE invoice_item (
    item_id         INT           NOT NULL AUTO_INCREMENT,
    invoice_id      INT           NOT NULL,
    description     VARCHAR(255)  NOT NULL,
    quantity        INT           NOT NULL DEFAULT 1,
    unit_price      DECIMAL(12,2) NOT NULL,
    total_price     DECIMAL(12,2) NOT NULL,
    CONSTRAINT pk_invoice_item          PRIMARY KEY (item_id),
    CONSTRAINT fk_item_invoice          FOREIGN KEY (invoice_id)
        REFERENCES invoice(invoice_id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Line items inside each invoice';

-- ============================================================
-- TABLE: payment
-- Records a payment made against an invoice (partial supported).
-- ============================================================
CREATE TABLE payment (
    payment_id      INT           NOT NULL AUTO_INCREMENT,
    invoice_id      INT           NOT NULL,
    amount          DECIMAL(12,2) NOT NULL,
    payment_method  ENUM('CASH','CARD','UPI','BANK_TRANSFER','CHEQUE') NOT NULL,
    transaction_ref VARCHAR(100)  NULL,         -- UPI/bank transaction ID
    payment_date    DATE          NOT NULL,
    recorded_by     INT           NULL,         -- user_id who recorded it
    notes           TEXT          NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_payment               PRIMARY KEY (payment_id),
    CONSTRAINT fk_payment_invoice       FOREIGN KEY (invoice_id)
        REFERENCES invoice(invoice_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_payment_amount       CHECK (amount > 0)
) ENGINE=InnoDB COMMENT='Payment transactions linked to invoices';

-- ============================================================
-- TABLE: usage_record
-- Daily data usage per subscription for billing/cap tracking.
-- ============================================================
CREATE TABLE usage_record (
    usage_id        INT           NOT NULL AUTO_INCREMENT,
    subscription_id INT           NOT NULL,
    usage_date      DATE          NOT NULL,
    data_used_mb    DECIMAL(12,2) NOT NULL DEFAULT 0.00,  -- in Megabytes
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_usage_record          PRIMARY KEY (usage_id),
    CONSTRAINT uq_usage_daily           UNIQUE      (subscription_id, usage_date),
    CONSTRAINT fk_usage_sub             FOREIGN KEY (subscription_id)
        REFERENCES subscription(subscription_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT chk_usage_data           CHECK (data_used_mb >= 0)
) ENGINE=InnoDB COMMENT='Daily data consumption per subscription for cap/billing';

-- ============================================================
-- TABLE: support_ticket
-- Customer-raised issues; assigned to a support agent (technician).
-- ============================================================
CREATE TABLE support_ticket (
    ticket_id       INT           NOT NULL AUTO_INCREMENT,
    customer_id     INT           NOT NULL,
    subscription_id INT           NULL,         -- NULL = account-level issue
    category        ENUM('CONNECTIVITY','BILLING','EQUIPMENT','SPEED','OTHER') NOT NULL,
    priority        ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL DEFAULT 'MEDIUM',
    subject         VARCHAR(255)  NOT NULL,
    description     TEXT          NULL,
    status          ENUM('OPEN','IN_PROGRESS','PENDING_CUSTOMER','RESOLVED','CLOSED') NOT NULL DEFAULT 'OPEN',
    assigned_to     INT           NULL,         -- technician_id
    resolution      TEXT          NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    resolved_at     DATETIME      NULL,
    CONSTRAINT pk_support_ticket        PRIMARY KEY (ticket_id),
    CONSTRAINT fk_ticket_customer       FOREIGN KEY (customer_id)
        REFERENCES customer(customer_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_ticket_sub            FOREIGN KEY (subscription_id)
        REFERENCES subscription(subscription_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_ticket_tech           FOREIGN KEY (assigned_to)
        REFERENCES technician(technician_id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='Customer support tickets with priority and lifecycle';

-- ============================================================
-- TABLE: ticket_update
-- Audit trail of comments/status changes on a ticket.
-- ============================================================
CREATE TABLE ticket_update (
    update_id       INT           NOT NULL AUTO_INCREMENT,
    ticket_id       INT           NOT NULL,
    author_id       INT           NULL,         -- technician_id who wrote the update
    comment         TEXT          NOT NULL,
    new_status      ENUM('OPEN','IN_PROGRESS','PENDING_CUSTOMER','RESOLVED','CLOSED') NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_ticket_update         PRIMARY KEY (update_id),
    CONSTRAINT fk_update_ticket         FOREIGN KEY (ticket_id)
        REFERENCES support_ticket(ticket_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_update_author         FOREIGN KEY (author_id)
        REFERENCES technician(technician_id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='Chronological history of support ticket comments';

-- ============================================================
-- TABLE: network_outage
-- Logged network incidents affecting a service area.
-- ============================================================
CREATE TABLE network_outage (
    outage_id       INT           NOT NULL AUTO_INCREMENT,
    title           VARCHAR(255)  NOT NULL,
    cause           TEXT          NULL,
    area_id         INT           NOT NULL,
    severity        ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL DEFAULT 'MEDIUM',
    start_time      DATETIME      NOT NULL,
    end_time        DATETIME      NULL,         -- NULL = still ongoing
    resolved_by     INT           NULL,         -- technician_id
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_network_outage        PRIMARY KEY (outage_id),
    CONSTRAINT fk_outage_area           FOREIGN KEY (area_id)
        REFERENCES service_area(area_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_outage_tech           FOREIGN KEY (resolved_by)
        REFERENCES technician(technician_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT chk_outage_times         CHECK (end_time IS NULL OR end_time >= start_time)
) ENGINE=InnoDB COMMENT='Network incidents and outages by service area';

-- ============================================================
-- TABLE: user
-- Application login accounts. One-to-one with customer (optional)
-- or staff role (no customer_id).
-- ============================================================
CREATE TABLE `user` (
    user_id         INT           NOT NULL AUTO_INCREMENT,
    username        VARCHAR(100)  NOT NULL,
    password_hash   VARCHAR(255)  NOT NULL,     -- BCrypt hash, NEVER plain text
    role            ENUM('ADMIN','BILLING_CLERK','TECHNICIAN','SUPPORT_AGENT','CUSTOMER') NOT NULL,
    customer_id     INT           NULL,         -- linked only for CUSTOMER role
    technician_id   INT           NULL,         -- linked for TECHNICIAN role
    is_active       TINYINT(1)    NOT NULL DEFAULT 1,
    last_login      DATETIME      NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_user                  PRIMARY KEY (user_id),
    CONSTRAINT uq_user_username         UNIQUE      (username),
    CONSTRAINT fk_user_customer         FOREIGN KEY (customer_id)
        REFERENCES customer(customer_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_user_technician       FOREIGN KEY (technician_id)
        REFERENCES technician(technician_id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='Application users; passwords BCrypt-hashed';

-- ============================================================
-- TABLE: audit_log
-- Written by DB triggers. Records all data changes.
-- ============================================================
CREATE TABLE audit_log (
    log_id          BIGINT        NOT NULL AUTO_INCREMENT,
    table_name      VARCHAR(100)  NOT NULL,
    operation       ENUM('INSERT','UPDATE','DELETE') NOT NULL,
    record_id       VARCHAR(100)  NOT NULL,     -- PK value of changed row (as string)
    old_values      JSON          NULL,         -- JSON snapshot of old row
    new_values      JSON          NULL,         -- JSON snapshot of new row
    changed_by      VARCHAR(100)  NULL,         -- MySQL user@host
    changed_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_audit_log PRIMARY KEY (log_id)
) ENGINE=InnoDB COMMENT='Immutable audit trail written by DB triggers';
