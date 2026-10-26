# ISP Management System — Oracle Edition
> Java 17 + Oracle XE 21c + Pure JDBC + JavaFX | Academic DBMS Project

## Quick Start

### Step 1: Start Oracle (Docker — recommended)
```bash
docker-compose up -d
# Wait ~2 minutes for Oracle to fully initialize
```

### Step 2: Create Schema (as SYSTEM)
```bash
sqlplus system/oracle@//localhost:1521/XEPDB1 @database/setup/create_user.sql
```

### Step 3: Run All Scripts
```bash
sqlplus isp_app/"ISP_Secure#2025"@//localhost:1521/XEPDB1 @database/run_all.sql
```

### Step 4: Verify
```sql
-- In SQL*Plus as isp_app:
SELECT table_name FROM user_tables ORDER BY 1;
SELECT object_name, status FROM user_objects WHERE status != 'VALID';
```

### Step 5: Configure Java
```bash
copy .env.example .env
# Edit src/main/resources/db.properties with your password
```

### Step 6: Build and Run
```bash
mvn clean compile          # compile
mvn clean test             # run tests
mvn javafx:run             # launch app
```

## Connection String
```
Service URL: jdbc:oracle:thin:@//localhost:1521/XEPDB1
SID URL:     jdbc:oracle:thin:@localhost:1521:XE
User:        isp_app
Password:    ISP_Secure#2025
```

## Design Patterns
| Pattern | Where |
|---|---|
| DAO | dao/impl/*.java — all DB access isolated here |
| Singleton | ConnectionManager — one HikariCP pool |
| Factory | DAOFactory — creates DAO instances |
| Strategy | billing/discount rules in service layer |
| Observer | UsageAlertService — 80%/100% cap alerts |
| Compound Trigger | trg_invoice_status_compound — avoids ORA-04091 |

## DBMS Concepts Checklist
| Concept | File |
|---|---|
| PK/FK/CHECK/UNIQUE/NOT NULL | V001__create_core_tables.sql |
| IDENTITY vs SEQUENCE | V001 — equipment uses SEQ, others use IDENTITY |
| Normalization (1NF→BCNF) | docs/normalization.md |
| Indexes + EXPLAIN PLAN | V002__indexes.sql |
| Views (5 views) | V004__views.sql |
| PL/SQL Package + Procedures | V005__pkg_billing.sql |
| Cursors (explicit + FOR loop) | V005 — sp_generate_monthly_invoices |
| SYS_REFCURSOR | V005 — sp_get_active_subscriptions |
| Exception Handling (RAISE_APPLICATION_ERROR) | V005, V006 |
| Triggers (BEFORE/AFTER/Compound) | V006__triggers.sql |
| Mutating Table Solution | V006 — trg_invoice_status_compound |
| Transactions + SAVEPOINT | V008__transactions_demo.sql |
| Isolation Levels (READ COMMITTED, SERIALIZABLE) | V008 |
| SELECT FOR UPDATE | V008 |
| All Join Types | V007__demo_queries.sql queries 1-5 |
| Subqueries (correlated + non-correlated) | V007 queries 6-7 |
| ROLLUP / CUBE | V007 queries 9-10 |
| Analytic Functions | V007 query 11 |
| CTEs (regular + recursive) | V007 queries 12-13 |
| UNION / INTERSECT / MINUS | V007 query 14 |
| CASE/DECODE/NVL/COALESCE | V007 query 15 |
| Date Functions (TRUNC/ADD_MONTHS/TO_CHAR) | V007 query 16 |
| FETCH FIRST / OFFSET (pagination) | V007 queries 17-18 |
| Security: Roles, GRANT/REVOKE | V009__security.sql |
| Data Pump Backup | database/backup/ |
| ORA- Error Mapping | db.properties comments |
