# Oracle XE 21c Setup Guide

## 1. Install Oracle XE 21c
- Windows: Download installer from https://www.oracle.com/database/technologies/xe-downloads.html
- Docker (recommended): `docker-compose up -d` (see docker-compose.yml)

## 2. Start the Listener
```bash
lsnrctl start      # start listener
lsnrctl status     # verify it shows FREEPDB1 as registered service
lsnrctl stop       # stop listener
```

## 3. Create Application User (as SYSTEM)
```sql
sqlplus system/oracle@//localhost:1521/FREEPDB1
@database/setup/create_user.sql
```

## 4. Run All Scripts
```bash
sqlplus isp_app/"ISP_Secure#2025"@//localhost:1521/FREEPDB1 @database/run_all.sql
```

## 5. Verify Tables
```sql
SELECT table_name FROM user_tables ORDER BY 1;
SELECT object_name, status FROM user_objects WHERE status != 'VALID';
```

## 6. Build and Run (Maven)
```bash
mvn clean compile          # compile Java
mvn clean test             # run all tests
mvn javafx:run             # launch the JavaFX app
```

## Troubleshooting Common ORA- Errors

| Error | Meaning | Fix |
|---|---|---|
| ORA-12541 | Listener not running | Run `lsnrctl start` |
| ORA-12514 | Service name not registered | Check `lsnrctl status`; verify service_name in URL |
| ORA-01017 | Invalid username/password | Check db.properties credentials; check CAPS (Oracle usernames case-insensitive in 12c+ with default settings) |
| ORA-28000 | Account locked (too many failures) | `ALTER USER isp_app ACCOUNT UNLOCK;` as SYSTEM |
| ORA-01400 | Cannot insert NULL | Java side: add NOT NULL validation before INSERT |
| ORA-00001 | Unique constraint violated | Duplicate email/serial_number; catch in Java |
| ORA-02291 | Parent key not found | Foreign key value does not exist in parent table |
| ORA-02292 | Child record exists | Cannot DELETE parent while child rows exist |
| ORA-04091 | Mutating table | Use compound trigger (see V006__triggers.sql) |
| ORA-08177 | Cannot serialize | Serializable tx conflict; retry the transaction |

## Connection URL Formats
```
# Service name (recommended for PDB):
jdbc:oracle:thin:@//localhost:1521/FREEPDB1

# SID (legacy for non-CDB):
jdbc:oracle:thin:@localhost:1521:XE

# With TNS alias (if tnsnames.ora configured):
jdbc:oracle:thin:@FREEPDB1
```
