# Demo Script: ISP Management System

This document provides a fast 5-minute practical walkthrough of the ISP Management System demo application.

---

## Prerequisites Before Launching
1. Ensure Oracle Database 23ai Free is running on `//localhost:1521/FREEPDB1`.
2. Ensure `src/main/resources/db.properties` has your password configured:
   ```properties
   url=jdbc:oracle:thin:@//localhost:1521/FREEPDB1
   user=isp_app
   password=<YOUR_ISP_APP_PASSWORD>
   ```
3. Run the application:
   ```bash
   mvn javafx:run
   ```

---

## 1. Login as Admin
- **Username**: `admin`
- **Password**: `password123`
- Click **Sign In**.
- **Observation**: The full sidebar navigation appears with:
  `Dashboard`, `Customers`, `Plans`, `Subscriptions`, `Billing`, `Support Tickets`, `Analytics & Reports`.

---

## 2. Dashboard KPIs
- Click on **Dashboard**.
- **KPI Metrics Displayed**:
  - **Active Customers**: 20
  - **Active Subscriptions**: 20
  - **This Month's Revenue**: Current month revenue dynamically calculated from `v_monthly_revenue`.
  - **Overdue Invoices**: Live count from `v_overdue_invoices`.
  - **Open Support Tickets**: Live count of unresolved tickets.
- Bottom panel displays an instant quick-view table of overdue invoices.

---

## 3. Manage Customers
- Navigate to **Customers**.
- **Search**:
  - Type `Arjun` or `techventures` in the search box and click **Search**. The table filters instantly. Clear the search box to view all 20 records.
- **Add Customer**:
  - Click **+ Add Customer**.
  - Fill in:
    - Customer Type: `INDIVIDUAL`
    - First Name: `Rohan`
    - Last Name: `Verma`
    - Email: `rohan.verma@demo.com`
    - Phone: `9899988877`
    - Service Area ID: `1`
  - Click **Save**. Observe the success alert and instant table reload.
- **Edit / Deactivate**:
  - Click **Edit Status / Phone** to update contact details.
  - Click **Deactivate** to soft-delete a subscriber by marking their status `INACTIVE`.

---

## 4. Create an Atomic Subscription
- Navigate to **Subscriptions**.
- Click **+ New Subscription**.
- Select:
  - Customer: Any active customer (e.g. `Arjun Mehta`)
  - Plan: `Gold 200 (₹999)`
  - Service Area: `Koramangala - Bangalore (560034)`
- Click **Create Subscription**.
- **Observation**: The record is inserted inside an atomic database transaction (`conn.setAutoCommit(false)` with explicit commit/rollback).

---

## 5. Billing & Invoices: Package Execution & Payments
- Navigate to **Billing**.
- **Filter**: Toggle the status dropdown between `ALL`, `PAID`, `OVERDUE`, and `ISSUED`.
- **Generate Monthly Invoices via PL/SQL**:
  - Click **⚡ Generate Monthly Invoices**.
  - Provide Year: `2026`, Month: `10` (or target billing cycle).
  - Click **Generate**.
  - **Observation**: Calls `PKG_BILLING.sp_generate_monthly_invoices` via JDBC `CallableStatement`. A dialog shows the exact count of generated invoices with 18% GST auto-calculated.
- **Record a Payment**:
  - Click **💳 Record Payment**.
  - Invoice ID: `7` (or any unpaid invoice).
  - Payment Amount: `470.82`
  - Payment Method: `UPI`
  - Transaction Ref: `UPI-DEMO-999`
  - Click **Submit Payment**.
  - **Observation**: Calls `PKG_BILLING.sp_record_payment`. The procedure locks the invoice row (`SELECT ... FOR UPDATE NOWAIT`), updates `amount_paid`, and transitions the invoice status from `OVERDUE` to `PAID`.

---

## 6. Analytics & Reports
- Navigate to **Analytics & Reports**.
- Browse across the 5 view tabs:
  1. **Monthly Revenue (`V_MONTHLY_REVENUE`)**: Summarizes total revenue collected broken down by plan type and month.
  2. **Overdue Invoices (`V_OVERDUE_INVOICES`)**: Calculates `days_overdue` and remaining balance.
  3. **Customer Balance (`V_CUSTOMER_BALANCE`)**: Summarizes lifetime billed vs paid vs outstanding balance per subscriber.
  4. **Technician Workload (`V_TECHNICIAN_WORKLOAD`)**: Combines pending installations and open tickets into an `AVAILABLE`, `BUSY`, or `OVERLOADED` metric.
  5. **Data Usage Summary (`V_DATA_USAGE_SUMMARY`)**: Flags subscribers as `UNLIMITED`, `NEAR_CAP`, or `CAP_EXCEEDED`.

---

## 7. Role-Based Access Control Demo
- Click **Logout** at the bottom of the sidebar.
- **Login as Billing Clerk**:
  - Username: `billing1`
  - Password: `password123`
  - **Observation**: Sidebar shows only `Billing`, `Customers`, `Plans`, and `Reports`. Technical and support administration options are restricted.
- **Login as Support Agent**:
  - Username: `support.priya`
  - Password: `password123`
  - **Observation**: Sidebar is filtered to only `Customers` and `Support Tickets`.
- **Login as Technician**:
  - Username: `tech.rajesh`
  - Password: `password123`
  - **Observation**: Sidebar is filtered to `Installations / Subs` and `Assigned Tickets`.
