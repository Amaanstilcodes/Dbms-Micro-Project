# ISP Management System — Oracle Edition ("ISP Orbit")
> Academic DBMS Project | Java 17 + Oracle Database 23ai Free + Pure JDBC + JavaFX 21

A production-grade desktop Internet Service Provider (ISP) management system integrating a 3NF/BCNF Oracle relational schema, PL/SQL packages, stored procedures, compound triggers, analytical views, and an asynchronous JavaFX frontend built with pure JDBC.

---

## Tech Stack
- **Database**: Oracle Database 23ai Free (Pluggable Database `FREEPDB1`)
- **Backend / Client**: Java 17+ (JDK 21/25 LTS compatible)
- **Database Driver**: Oracle JDBC 23ai (`ojdbc11`)
- **UI Framework**: JavaFX 21 (Controls + Graphics, code-driven layout without FXML)
- **Icons**: FontAwesome 5 via Ikonli (`ikonli-javafx`, `ikonli-fontawesome5-pack`)
- **Security**: jBCrypt 0.4 (salted password hashing, cost factor 12)
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
   Confirm that `FREEPDB1` is registered and `READY`.

2. **Create Application User (`ISP_APP`)**:
   From the project directory:
   ```bash
   sqlplus <admin_user>/<admin_password>@//localhost:1521/FREEPDB1 @database/setup/create_user.sql
   ```

3. **Deploy Schema, Seeds, Views, Packages, and Triggers**:
   ```bash
   sqlplus isp_app/"<ISP_APP_PASSWORD>"@//localhost:1521/FREEPDB1 @database/run_all.sql
   ```
   *(Note: Remember double quotes around passwords containing special characters like `#`)*

---

## How to Run the Application

### ⚡ Quick Start (One Command)
Make sure your terminal is inside the `Dbms-Micro-Project` directory:
```bash
cd c:\Users\Shivam\DBMS\Dbms-Micro-Project
mvn javafx:run
```

### 🛠️ Detailed Setup

1. **Configure Database Credentials**:
   Copy the template and set your Oracle password:
   ```bash
   copy src\main\resources\db.properties.example src\main\resources\db.properties
   ```
   Verify `src/main/resources/db.properties`:
   ```properties
   url=jdbc:oracle:thin:@//localhost:1521/FREEPDB1
   user=isp_app
   password=ISP_Secure#2025
   ```

2. **Inspect & Query Database via SQL\*Plus**:
   Connect directly to the pluggable database (`FREEPDB1`):
   ```bash
   sqlplus isp_app/"ISP_Secure#2025"@//localhost:1521/FREEPDB1
   ```
   Useful verification queries:
   ```sql
   -- View all application tables
   SELECT table_name FROM user_tables ORDER BY table_name;

   -- View active subscribers
   SELECT customer_id, full_name, email, status FROM customer;

   -- View monthly revenue view
   SELECT * FROM v_monthly_revenue;
   ```

3. **(Optional) Rehash Demo Passwords**:
   To update demo users (`admin`, `billing1`, `tech.rajesh`, `support.priya`) with fresh BCrypt hashes of `password123`:
   ```bash
   mvn exec:java -Dexec.mainClass=com.isp.PasswordUtil
   ```

4. **Launch the Application**:
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

## Application Modules & Key Interactions

1. **Dashboard**:
   - Live KPI cards (Active Subscribers, Contracts, Monthly Revenue, Overdue Invoices, Open Tickets, Pending Installs).
   - Dynamic charts: Revenue collection by plan (Bar Chart) and Subscribers by plan type (Pie Chart).
   - "Quick Operations" shortcuts (`New Customer`, `Record Payment`, `Run Invoicing`, `Open Ticket`, `View in Billing`) that route directly to corresponding modules.
   - Live data refresh button (`Refresh Live Data`).
2. **Customers Directory**:
   - Live debounced search by name, email, or phone.
   - Filter by account status (`ACTIVE`, `INACTIVE`, `BLACKLISTED`) and customer type (`INDIVIDUAL`, `BUSINESS`).
   - Register new customers with validation, edit contact details, and soft-delete/deactivate accounts with confirmation dialogs.
3. **Plans Catalog**:
   - Browse broadband, FTTH, and leased-line tiers with speed, upload, data cap, and monthly pricing.
   - Dual-mode view toggle: Cards Grid view and Table view.
4. **Subscriptions**:
   - 3-step Provisioning Wizard (`Select Customer` > `Choose Plan & Area` > `Contract & GST Breakdown`) enforcing ACID atomicity.
   - Live lifecycle control: Terminate or reactivate contracts with `ConfirmDialog` confirmation.
5. **Billing & Revenue Engine**:
   - Tab 1: All Invoices with status filter (`ISSUED`, `PAID`, `PARTIALLY_PAID`, `OVERDUE`, `DRAFT`).
   - Tab 2: Overdue Accounts with live days overdue and outstanding balances from `V_OVERDUE_INVOICES`.
   - Direct execution of `PKG_BILLING.sp_generate_monthly_invoices` (18% GST automatic calculation).
   - Record payments via `PKG_BILLING.sp_record_payment` with instant balance preview.
   - Execute batch account suspensions via `PKG_BILLING.sp_suspend_overdue_accounts`.
6. **Support Tickets**:
   - Dual-mode helpdesk view: Kanban Board (columns by status) and Table view.
   - Status filtering across both Kanban and Table views.
   - Modal ticket creation with populated active customer dropdown and live validated subject field.
   - Ticket detail dialog with quick status/priority updates and ticket timeline commentary logging to `TICKET_UPDATE`.
7. **Analytics & Reports**:
   - Multi-tab Oracle analytical views: Monthly Revenue, Overdue Invoices, Customer Balance, Technician Workload, Data Usage Summary, Active Subscriptions.
   - One-click CSV Export (`Export Current View (CSV)`) that exports active table records directly to disk.
8. **Global Features & Shortcuts**:
   - **`Ctrl+F`**: Global search shortcut that focuses the search bar and performs real-time client-side filtering on the active table.
   - **Sidebar Collapse**: Collapsible navigation menu for maximum data viewing space.
   - **Theme Toggle**: Real-time switch between Dark and Light mode.
   - **Unified Dialogs**: Proper window title bar close (**X**), **Cancel** buttons, and **`ESC` key** dismissal across all modals.

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
│   ├── PROJECT_REPORT.md  # Comprehensive project report & architecture documentation
│   ├── SCHEMA_SUMMARY.md  # Detailed schema, PK/FK, trigger & billing documentation
│   ├── DEMO_SCRIPT.md     # 5-minute walkthrough guide
│   └── oracle-setup-guide.md
├── src/
│   ├── main/
│   │   ├── java/com/isp/
│   │   │   ├── Db.java              # JDBC Connection manager (db.properties)
│   │   │   ├── Session.java         # Current logged-in user & role
│   │   │   ├── PasswordUtil.java    # One-time BCrypt rehashing utility
│   │   │   ├── dao/                 # JDBC prepared & callable DAO queries
│   │   │   ├── ui/                  # Asynchronous JavaFX screens
│   │   │   │   ├── LoginScreen.java
│   │   │   │   ├── MainLayout.java       # Shell, sidebar, search, accelerators
│   │   │   │   ├── DashboardScreen.java  # KPI metrics & quick operations
│   │   │   │   ├── CustomersScreen.java  # Customer directory & modals
│   │   │   │   ├── PlansScreen.java      # Dual grid/table plan catalog
│   │   │   │   ├── SubscriptionsScreen.java # 3-step wizard & lifecycle
│   │   │   │   ├── BillingScreen.java    # Invoices & PKG_BILLING execution
│   │   │   │   ├── TicketsScreen.java    # Kanban board & timeline details
│   │   │   │   ├── ReportsScreen.java    # Tabbed analytical views & CSV export
│   │   │   │   ├── TableViewBuilder.java # Generic async TableView & live filter
│   │   │   │   ├── components/           # Reusable UI widgets & modals
│   │   │   │   │   ├── AppDialog.java    # Unified modal wrapper (ESC, X button)
│   │   │   │   │   ├── ConfirmDialog.java
│   │   │   │   │   ├── FormField.java
│   │   │   │   │   ├── KpiTile.java
│   │   │   │   │   ├── PlanCard.java
│   │   │   │   │   ├── StatusBadge.java
│   │   │   │   │   ├── StepIndicator.java
│   │   │   │   │   └── ToastManager.java
│   │   │   │   └── util/
│   │   │   │       ├── Formatters.java
│   │   │   │       └── ThemeManager.java
│   │   │   └── Main.java            # JavaFX Application entry point
│   │   └── resources/
│   │       ├── css/
│   │       │   ├── app.css          # Core styles
│   │       │   └── isp-theme.css    # Dark & light design system
│   │       ├── logback.xml          # Logging configuration
│   │       ├── db.properties        # Local DB credentials (git-ignored)
│   │       └── db.properties.example
│   └── test/
└── pom.xml
```

---

## Troubleshooting & Oracle Compatibility Notes
- **Pluggable Database (`FREEPDB1`)**: Oracle Database 23ai Free defaults to `FREEPDB1`. All connection strings and session switches must target `FREEPDB1`.
- **Special Characters in Passwords**: When passwords contain `#` or other special characters in SQL*Plus, enclose them in double quotes (e.g. `isp_app/"ISP_Secure#2025"@//localhost:1521/FREEPDB1`).
- **Terminal Working Directory**: Always run `mvn javafx:run` from inside the `Dbms-Micro-Project` directory where `pom.xml` resides.
- **Mutating Table Solution**: Oracle row-level triggers that read/update modifying tables trigger `ORA-04091`. The project uses a **Compound Trigger** (`trg_invoice_status_compound`) to prevent mutating table errors.
