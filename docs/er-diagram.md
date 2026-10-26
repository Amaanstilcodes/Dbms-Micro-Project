# ER Diagram — ISP Management System

## Mermaid Source (paste into https://mermaid.live)

```mermaid
erDiagram
    SERVICE_AREA {
        int area_id PK
        string area_name
        string city
        string state
        string pin_code
    }
    CUSTOMER {
        int customer_id PK
        enum customer_type
        string first_name
        string last_name
        string company_name
        string email
        string phone
        int area_id FK
        enum status
    }
    ADDRESS {
        int address_id PK
        int customer_id FK
        enum address_type
        string street_line1
        string city
        string pin_code
        bool is_primary
    }
    PLAN {
        int plan_id PK
        string plan_name
        enum plan_type
        int speed_mbps
        int data_cap_gb
        decimal price
        enum billing_cycle
    }
    SUBSCRIPTION {
        int subscription_id PK
        int customer_id FK
        int plan_id FK
        int area_id FK
        date start_date
        date end_date
        enum status
    }
    EQUIPMENT {
        int equipment_id PK
        enum equipment_type
        string serial_number
        string mac_address
        enum status
        int subscription_id FK
    }
    TECHNICIAN {
        int technician_id PK
        string first_name
        string last_name
        string email
        int area_id FK
    }
    INSTALLATION {
        int installation_id PK
        int subscription_id FK
        int technician_id FK
        datetime scheduled_date
        enum status
    }
    INVOICE {
        int invoice_id PK
        int subscription_id FK
        date billing_period_start
        date billing_period_end
        decimal total_amount
        decimal amount_paid
        date due_date
        enum status
    }
    INVOICE_ITEM {
        int item_id PK
        int invoice_id FK
        string description
        decimal unit_price
        decimal total_price
    }
    PAYMENT {
        int payment_id PK
        int invoice_id FK
        decimal amount
        enum payment_method
        string transaction_ref
        date payment_date
    }
    USAGE_RECORD {
        int usage_id PK
        int subscription_id FK
        date usage_date
        decimal data_used_mb
    }
    SUPPORT_TICKET {
        int ticket_id PK
        int customer_id FK
        int subscription_id FK
        enum category
        enum priority
        enum status
        int assigned_to FK
    }
    TICKET_UPDATE {
        int update_id PK
        int ticket_id FK
        int author_id FK
        text comment
        datetime created_at
    }
    NETWORK_OUTAGE {
        int outage_id PK
        int area_id FK
        string title
        enum severity
        datetime start_time
        datetime end_time
    }
    USER {
        int user_id PK
        string username
        string password_hash
        enum role
        int customer_id FK
        int technician_id FK
    }
    AUDIT_LOG {
        bigint log_id PK
        string table_name
        enum operation
        string record_id
        json old_values
        json new_values
    }

    SERVICE_AREA ||--o{ CUSTOMER : "serves"
    SERVICE_AREA ||--o{ TECHNICIAN : "employs"
    SERVICE_AREA ||--o{ SUBSCRIPTION : "covers"
    SERVICE_AREA ||--o{ NETWORK_OUTAGE : "affects"
    CUSTOMER ||--o{ ADDRESS : "has"
    CUSTOMER ||--o{ SUBSCRIPTION : "has"
    CUSTOMER ||--o{ SUPPORT_TICKET : "raises"
    CUSTOMER ||--o| USER : "may login as"
    PLAN ||--o{ SUBSCRIPTION : "used in"
    SUBSCRIPTION ||--o{ INVOICE : "generates"
    SUBSCRIPTION ||--o{ USAGE_RECORD : "tracks"
    SUBSCRIPTION ||--|| INSTALLATION : "requires"
    SUBSCRIPTION ||--o| EQUIPMENT : "assigned"
    INVOICE ||--o{ INVOICE_ITEM : "contains"
    INVOICE ||--o{ PAYMENT : "receives"
    TECHNICIAN ||--o{ INSTALLATION : "performs"
    TECHNICIAN ||--o{ SUPPORT_TICKET : "handles"
    TECHNICIAN ||--o{ TICKET_UPDATE : "writes"
    TECHNICIAN ||--o| USER : "may login as"
    SUPPORT_TICKET ||--o{ TICKET_UPDATE : "has"
```

## dbdiagram.io Source (paste into https://dbdiagram.io)

```
Table service_area { area_id int [pk, increment] area_name varchar city varchar state varchar pin_code varchar [unique] is_active bool }
Table customer { customer_id int [pk, increment] customer_type enum email varchar [unique] phone varchar area_id int [ref: > service_area.area_id] status enum }
Table plan { plan_id int [pk, increment] plan_name varchar [unique] plan_type enum speed_mbps int price decimal billing_cycle enum }
Table subscription { subscription_id int [pk, increment] customer_id int [ref: > customer.customer_id] plan_id int [ref: > plan.plan_id] area_id int [ref: > service_area.area_id] start_date date status enum }
Table invoice { invoice_id int [pk, increment] subscription_id int [ref: > subscription.subscription_id] total_amount decimal amount_paid decimal due_date date status enum }
Table payment { payment_id int [pk, increment] invoice_id int [ref: > invoice.invoice_id] amount decimal payment_method enum payment_date date }
Table equipment { equipment_id int [pk, increment] serial_number varchar [unique] mac_address varchar [unique] status enum subscription_id int [ref: > subscription.subscription_id] }
Table installation { installation_id int [pk, increment] subscription_id int [ref: - subscription.subscription_id] technician_id int [ref: > technician.technician_id] status enum }
Table technician { technician_id int [pk, increment] first_name varchar last_name varchar email varchar [unique] area_id int [ref: > service_area.area_id] }
Table support_ticket { ticket_id int [pk, increment] customer_id int [ref: > customer.customer_id] assigned_to int [ref: > technician.technician_id] priority enum status enum }
Table user { user_id int [pk, increment] username varchar [unique] password_hash varchar role enum customer_id int [ref: > customer.customer_id] }
```
