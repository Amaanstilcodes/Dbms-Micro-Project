# ER Diagram — ISP Management System (Oracle Edition)

## Mermaid Source (paste at https://mermaid.live)

```mermaid
erDiagram
    SERVICE_AREA { number area_id PK; varchar2 area_name; varchar2 city; varchar2 pin_code; char is_active }
    CUSTOMER { number customer_id PK; varchar2 customer_type; varchar2 first_name; varchar2 email; number area_id FK; varchar2 status }
    ADDRESS { number address_id PK; number customer_id FK; varchar2 address_type; varchar2 street_line1; varchar2 pin_code }
    PLAN { number plan_id PK; varchar2 plan_name; varchar2 plan_type; number speed_mbps; number data_cap_gb; number price; varchar2 billing_cycle }
    SUBSCRIPTION { number subscription_id PK; number customer_id FK; number plan_id FK; number area_id FK; date start_date; varchar2 status }
    EQUIPMENT { number equipment_id PK; varchar2 equipment_type; varchar2 serial_number; varchar2 mac_address; varchar2 status; number subscription_id FK }
    TECHNICIAN { number technician_id PK; varchar2 first_name; varchar2 email; number area_id FK; char is_active }
    INSTALLATION { number installation_id PK; number subscription_id FK; number technician_id FK; date scheduled_date; varchar2 status }
    INVOICE { number invoice_id PK; number subscription_id FK; date billing_period_start; number total_amount; number amount_paid; date due_date; varchar2 status }
    INVOICE_ITEM { number item_id PK; number invoice_id FK; varchar2 description; number unit_price; number total_price }
    PAYMENT { number payment_id PK; number invoice_id FK; number amount; varchar2 payment_method; varchar2 transaction_ref; date payment_date }
    USAGE_RECORD { number usage_id PK; number subscription_id FK; date usage_date; number data_used_mb }
    SUPPORT_TICKET { number ticket_id PK; number customer_id FK; number subscription_id FK; varchar2 category; varchar2 priority; varchar2 status; number assigned_to FK }
    TICKET_UPDATE { number update_id PK; number ticket_id FK; number author_id FK; clob comment_txt; date created_at }
    NETWORK_OUTAGE { number outage_id PK; number area_id FK; varchar2 title; varchar2 severity; date start_time; date end_time }
    APP_USER { number user_id PK; varchar2 username; varchar2 password_hash; varchar2 role; number customer_id FK; number technician_id FK }
    AUDIT_LOG { number log_id PK; varchar2 table_name; varchar2 operation; varchar2 record_id; clob old_values; clob new_values }

    SERVICE_AREA ||--o{ CUSTOMER : "serves"
    SERVICE_AREA ||--o{ TECHNICIAN : "employs"
    SERVICE_AREA ||--o{ SUBSCRIPTION : "covers"
    SERVICE_AREA ||--o{ NETWORK_OUTAGE : "affects"
    CUSTOMER ||--o{ ADDRESS : "has"
    CUSTOMER ||--o{ SUBSCRIPTION : "has"
    CUSTOMER ||--o{ SUPPORT_TICKET : "raises"
    CUSTOMER ||--o| APP_USER : "may login as"
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
    TECHNICIAN ||--o| APP_USER : "may login as"
    SUPPORT_TICKET ||--o{ TICKET_UPDATE : "has"
```

## dbdiagram.io Source

```
Table service_area { area_id number [pk] area_name varchar2 city varchar2 pin_code varchar2 [unique] }
Table customer { customer_id number [pk] email varchar2 [unique] area_id number [ref: > service_area.area_id] status varchar2 }
Table plan { plan_id number [pk] plan_name varchar2 [unique] plan_type varchar2 price number }
Table subscription { subscription_id number [pk] customer_id number [ref: > customer.customer_id] plan_id number [ref: > plan.plan_id] area_id number [ref: > service_area.area_id] status varchar2 }
Table invoice { invoice_id number [pk] subscription_id number [ref: > subscription.subscription_id] total_amount number due_date date status varchar2 }
Table payment { payment_id number [pk] invoice_id number [ref: > invoice.invoice_id] amount number payment_method varchar2 }
Table equipment { equipment_id number [pk] serial_number varchar2 [unique] status varchar2 subscription_id number [ref: > subscription.subscription_id] }
Table installation { installation_id number [pk] subscription_id number [ref: - subscription.subscription_id] technician_id number [ref: > technician.technician_id] }
Table technician { technician_id number [pk] email varchar2 [unique] area_id number [ref: > service_area.area_id] }
Table support_ticket { ticket_id number [pk] customer_id number [ref: > customer.customer_id] assigned_to number [ref: > technician.technician_id] }
Table app_user { user_id number [pk] username varchar2 [unique] role varchar2 customer_id number [ref: > customer.customer_id] }
```
