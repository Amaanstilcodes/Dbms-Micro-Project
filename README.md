# ISP Management System — Oracle Edition
> Academic DBMS Project | Java 17 + Oracle Database 23ai Free + Pure JDBC + JavaFX

A complete desktop Internet Service Provider (ISP) management application integrating a robust Oracle relational schema, PL/SQL packages, stored procedures, triggers, views, and an asynchronous JavaFX frontend built with pure JDBC.

---

## Tech Stack
- **Database**: Oracle Database 23ai Free (Pluggable Database `FREEPDB1`)
- **Backend / Client**: Java 17+ (JDK 25 LTS compatible)
- **Database Driver**: Oracle JDBC 23ai (`ojdbc11`)
- **UI Framework**: JavaFX 21 (Controls + Graphics, no FXML)
- **Security**: jBCrypt 0.4 (salted password hashing)
- **Logging**: SLF4J + Logback Classic

---

## Prerequisites
- **Java 17+**: Ensure `java -version` returns 17 or higher.
- **Maven**: Ensure `mvn` is installed (`mvn -v`).
- **Oracle Database 23ai Free**: Running with listener registered on port 1521 for service `FREEPDB1`.
- **SQL*Plus**: Command-line utility for executing SQL setup scripts.

---

## Database Setup

1. **Verify Listener and Service**:
   ```bash
   lsnrctl status
   ```
   Confirm that `FREEPDB1` is registered and READY.

2. **Create Application User (`ISP_APP`)**:
   From the project root:
   ```bash
   sqlplus <admin_user>/<admin_password>@//localhost:1521/FREEPDB1 @database/setup/create_user.sql
   ```

3. **Deploy Schema, Seeds, Views, Packages, and Triggers**:
   ```bash
   sqlplus isp_app/"<ISP_APP_PASSWORD>"@//localhost:1521/FREEPDB1 @database/run_all.sql
   ```

---

## How to Run the Application

1. **Configure Credentials**:
   Copy the template and set your Oracle password:
   ```bash
   copy src/main/resources/db.properties.example src/main/resources/db.properties
   ```
   Edit `src/main/resources/db.properties`:
   ```properties
   url=jdbc:oracle:thin:@//localhost:1521/FREEPDB1
   user=isp_app
   password=<YOUR_ISP_APP_PASSWORD>
   ```

2. **(Optional) Run Password Utility**:
   To update demo users (`admin`, `billing1`, `tech.rajesh`, `support.priya`) with fresh BCrypt hashes of `password123`:
   ```bash
   mvn exec:java -Dexec.mainClass=com.isp.PasswordUtil
   ```

3. **Launch the JavaFX Application**:
   ```bash
   mvn javafx:run
   ```

---

## Seeded Demo Logins
All demo accounts use password: `password123`

| Username | Role | Accessible Features |
|---|---|---|
| `admin` | `ADMIN` | Full access: Dashboard, Customers, Plans, Subscriptions, Billing, Tickets, Reports |
| `billing1` | `BILLING_CLERK` | Billing, Invoices, Payments, Customers, Plans, Reports |
| `tech.rajesh` | `TECHNICIAN` | Assigned installations and technical support tickets |
| `support.priya` | `SUPPORT_AGENT` | Customer lookup and support tickets |

---

## Project Structure
```text
isp-management-system/
├── database/
│   ├── setup/             # User creation (create_user.sql) & teardown (drop_all.sql)
│   ├── schema/            # Core DDL tables (V001__create_core_tables.sql)
│   ├── migrations/        # Performance indexes (V002__indexes.sql)
│   ├── seeds/             # Sample datasets (V003__seed_data.sql)
│   ├── views/             # Analytical views (V004__views.sql)
│   ├── procedures/        # PL/SQL billing package (V005__pkg_billing.sql)
│   ├── triggers/          # Audit & compound triggers (V006__triggers.sql)
│   ├── security/          # Roles and permissions (V009__security.sql)
│   └── run_all.sql        # Master deployment script
├── docs/
│   ├── SCHEMA_SUMMARY.md  # Detailed schema, PK/FK, trigger & billing documentation
│   ├── DEMO_SCRIPT.md     # 5-minute walkthrough guide
│   └── oracle-setup-guide.md
├── src/
│   ├── main/
│   │   ├── java/com/isp/
│   │   │   ├── Db.java              # JDBC Connection manager
│   │   │   ├── Session.java         # Current logged-in user & role
│   │   │   ├── PasswordUtil.java    # One-time BCrypt rehashing utility
│   │   │   ├── dao/                 # JDBC prepared & callable DAO queries
│   │   │   ├── ui/                  # Asynchronous JavaFX screens
│   │   │   │   ├── LoginScreen.java
│   │   │   │   ├── DashboardScreen.java
│   │   │   │   ├── CustomersScreen.java
│   │   │   │   ├── PlansScreen.java
│   │   │   │   ├── SubscriptionsScreen.java
│   │   │   │   ├── BillingScreen.java
│   │   │   │   ├── ReportsScreen.java
│   │   │   │   ├── TicketsScreen.java
│   │   │   │   └── TableViewBuilder.java # Generic async TableView helper
│   │   │   └── Main.java            # JavaFX Application entry point
│   │   └── resources/
│   │       ├── css/app.css          # UI styles
│   │       ├── logback.xml          # Logging configuration
│   │       ├── db.properties        # Local DB credentials (git-ignored)
│   │       └── db.properties.example
└── pom.xml
```

---

## Features
- **Role-Based Authentication**: Custom navigation sidebars tailored to specific employee roles.
- **Asynchronous Execution**: All database queries run in background `Task` worker threads, keeping the UI responsive.
- **Clean Oracle Error Handling**: Application catches `ORA-20xxx` PL/SQL application errors and presents user-friendly alert dialogs.
- **Atomic Multi-Table Transactions**: Subscriptions are provisioned with transaction rollback safety.
- **PL/SQL Package Integration**: Direct execution of `PKG_BILLING.sp_record_payment` and `PKG_BILLING.sp_generate_monthly_invoices` via `CallableStatement`.

---

## Progress Checklist
- [x] Oracle Database XE / 23ai Free configured and running
- [x] All 17 tables, 6 views, 8 triggers, and PKG_BILLING deployed and valid
- [x] Schema reference guide documented in `docs/SCHEMA_SUMMARY.md`
- [x] Maven build configured (`pom.xml`) with JavaFX 21 and Oracle JDBC 23ai
- [x] Application skeleton and generic `TableViewBuilder` implemented
- [x] Login screen with BCrypt authentication and role-based routing
- [x] Dashboard with live view KPI cards
- [x] Customers management screen (search, add, edit, soft-delete)
- [x] Plans catalog view
- [x] Subscriptions creation screen with atomic transactions
- [x] Billing screen with payment recording and batch invoice generation
- [x] Support ticket viewing and status filtering
- [x] Analytics and reporting tabbed interface
- [x] Complete walkthrough demo script in `docs/DEMO_SCRIPT.md`

---

## Troubleshooting & Oracle Compatibility Notes
- **Pluggable Database (FREEPDB1 vs XEPDB1)**: Oracle Database 23ai Free defaults to `FREEPDB1` rather than XE's `XEPDB1`. All connection strings and `ALTER SESSION SET CONTAINER` directives target `FREEPDB1`.
- **Foreign Key `ON DELETE RESTRICT` (ORA-02000)**: In Oracle SQL, `ON DELETE RESTRICT` is not valid syntax. Restrict is Oracle's default referential action when no `ON DELETE` clause is specified.
- **Redundant Index on UNIQUE Columns (ORA-01408)**: Columns defined with `UNIQUE` constraints (e.g. `customer(email)` and `usage_record(subscription_id, usage_date)`) are already indexed automatically by Oracle; creating an explicit index on the same column set raises `ORA-01408`.
- **Seed Data Payment Foreign Key (ORA-02291)**: In `V003__seed_data.sql`, a payment was seeded with `invoice_id = 10`, but only 9 invoices were inserted. Pointing the payment at `invoice_id = 9` aligns it with the corresponding 5898.82 invoice.
- **Password Identifiers with Special Characters**: When using passwords containing special characters like `#` in SQL*Plus, enclose them in double quotes (e.g. `isp_app/"<password>"`).
