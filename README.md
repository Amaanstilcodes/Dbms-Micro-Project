# ISP Management System
> Academic DBMS Project — Java 17 + MySQL 8 + JavaFX + Pure JDBC

## Setup and Run Commands

### Step 1: Create the Database
```sql
-- Run in MySQL Workbench or mysql CLI as root:
CREATE DATABASE isp_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE isp_db;
```

### Step 2: Run SQL Scripts (in order)
```bash
mysql -u root -p isp_db < database/schema/V001__create_core_tables.sql
mysql -u root -p isp_db < database/migrations/V002__indexes.sql
mysql -u root -p isp_db < database/seeds/V003__seed_data.sql
mysql -u root -p isp_db < database/views/V004__views.sql
mysql -u root -p isp_db < database/procedures/V005__procedures.sql
mysql -u root -p isp_db < database/triggers/V006__triggers.sql
mysql -u root -p isp_db < database/security/V009__security.sql
```

### Step 3: Configure Database Connection
```bash
# Copy example and edit:
copy .env.example .env
# Edit src/main/resources/db.properties with your MySQL password
```

### Step 4: Build with Maven
```bash
mvn clean compile          # compile only
mvn clean test             # run all tests
mvn clean package          # build JAR
```

### Step 5: Run the App
```bash
mvn javafx:run             # run via JavaFX plugin
# OR:
mvn exec:java              # via exec plugin
```

### Step 6: Docker (optional — skip if MySQL already installed)
```bash
docker-compose up -d       # starts MySQL on port 3306
```

## Project Structure
See Part 1 deliverable for full ASCII tree.

## Tech Stack
- Java 17, Maven, MySQL 8
- Pure JDBC (PreparedStatement, CallableStatement, batch updates)
- HikariCP connection pool
- JavaFX 21 + FXML frontend
- SLF4J + Logback logging
- JUnit 5 + H2 (in-memory) for tests
- BCrypt password hashing

## Design Patterns Used
| Pattern | Location |
|---|---|
| DAO | dao/ — separates SQL from business logic |
| Singleton | ConnectionManager — one HikariCP pool |
| Factory | DAOFactory — creates DAO instances |
| Strategy | Billing/discount calculation strategies |
| Observer | UsageAlertService — 80%/100% cap alerts |
| Template Method | BaseDAO — common CRUD skeleton |
