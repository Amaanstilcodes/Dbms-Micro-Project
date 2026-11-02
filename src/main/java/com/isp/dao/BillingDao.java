package com.isp.dao;

import com.isp.Db;
import com.isp.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for Billing &amp; Payments screen.
 *
 * Role permissions (enforced here AND in BillingScreen):
 *   ADMIN          – full access: generate, suspend, record payment, view all
 *   BILLING_CLERK  – generate, record payment, view all  (no suspendOverdue)
 *   SUPPORT_AGENT  – read-only
 *   TECHNICIAN     – no Billing menu (MainLayout excludes it)
 *
 * Payment method values MUST match PAYMENT.payment_method CHECK constraint:
 *   CASH, CARD, UPI, BANK_TRANSFER, CHEQUE
 */
public class BillingDao {

    private static final Logger log = LoggerFactory.getLogger(BillingDao.class);

    /** Canonical payment methods matching CHECK constraint in V001. */
    public static final String[] PAYMENT_METHODS = {"UPI", "CARD", "CASH", "BANK_TRANSFER", "CHEQUE"};

    // ── Model records ─────────────────────────────────────────────────────────

    public record InvoiceRow(
            int    invoiceId,
            int    subscriptionId,
            String customerName,
            String planName,
            double subtotal,
            double taxAmount,
            double lateFee,
            double totalAmount,
            double amountPaid,
            double outstanding,
            String dueDate,
            String status
    ) {}

    public record OverdueRow(
            int    invoiceId,
            int    subscriptionId,
            String customerName,
            String email,
            String phone,
            String planName,
            double totalAmount,
            double amountPaid,
            double outstanding,
            String dueDate,
            int    daysOverdue,
            String status
    ) {}

    public record PaymentRow(
            int    paymentId,
            double amount,
            String method,
            String transactionRef,
            String paymentDate
    ) {}

    public record InvoiceDetail(
            int    invoiceId,
            int    subscriptionId,
            String customerName,
            String email,
            String planName,
            String billingPeriodStart,
            String billingPeriodEnd,
            double subtotal,
            double taxAmount,
            double lateFee,
            double discount,
            double totalAmount,
            double amountPaid,
            double outstanding,
            String dueDate,
            String status,
            String issuedAt
    ) {}

    // ── Permission helpers ────────────────────────────────────────────────────

    private static String safeRole() {
        String r = Session.getRole();
        return r != null ? r : "SUPPORT_AGENT";
    }

    private static void requireWrite(String action) {
        String role = safeRole();
        switch (action) {
            case "GENERATE", "RECORD_PAYMENT" -> {
                if (!"ADMIN".equals(role) && !"BILLING_CLERK".equals(role))
                    throw new SecurityException("Role " + role + " cannot perform: " + action);
            }
            case "SUSPEND" -> {
                if (!"ADMIN".equals(role))
                    throw new SecurityException("Only ADMIN can suspend overdue accounts.");
            }
            default -> throw new SecurityException("Unknown action: " + action);
        }
    }

    // ── Friendly error messages ───────────────────────────────────────────────

    /**
     * Strips the "ORA-20nnn: " prefix from RAISE_APPLICATION_ERROR messages.
     * Also converts ORA-00054 (resource busy) to a clear lock message.
     */
    public static String friendlyMessage(SQLException ex) {
        String msg = ex.getMessage();
        if (msg == null) return "Database error (no details).";
        if (msg.contains("ORA-00054"))
            return "Invoice is locked by another session. Please try again in a moment.";
        String firstLine = msg.split("\n")[0].trim();
        return firstLine.replaceFirst("ORA-20\\d+:\\s*", "");
    }

    // ── searchInvoices ────────────────────────────────────────────────────────
    // Joins INVOICE → SUBSCRIPTION → CUSTOMER, PLAN so we can show names.
    // Searches: invoice_id (numeric prefix match), customer name (UPPER LIKE), plan name.

    public static List<InvoiceRow> searchInvoices(String text, String status) throws SQLException {
        List<InvoiceRow> list = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
            "SELECT i.invoice_id, i.subscription_id, " +
            "       c.first_name || ' ' || c.last_name AS customer_name, " +
            "       p.plan_name, " +
            "       i.subtotal, i.tax_amount, " +
            "       NVL(i.late_fee, 0) AS late_fee, " +
            "       i.total_amount, i.amount_paid, " +
            "       (i.total_amount - i.amount_paid) AS outstanding, " +
            "       TO_CHAR(i.due_date, 'YYYY-MM-DD') AS due_date, " +
            "       i.status " +
            "FROM   invoice i " +
            "JOIN   subscription s ON i.subscription_id = s.subscription_id " +
            "JOIN   customer c     ON s.customer_id     = c.customer_id " +
            "JOIN   plan p         ON s.plan_id         = p.plan_id " +
            "WHERE  1 = 1"
        );

        List<Object> params = new ArrayList<>();

        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            sql.append(" AND i.status = ?");
            params.add(status.trim());
        }

        if (text != null && !text.trim().isEmpty()) {
            String t = text.trim();
            // If purely numeric, match invoice_id exactly; otherwise match names
            if (t.matches("\\d+")) {
                sql.append(" AND i.invoice_id = ?");
                params.add(Integer.parseInt(t));
            } else {
                sql.append(" AND (UPPER(c.first_name || ' ' || c.last_name) LIKE UPPER(?)" +
                           "  OR  UPPER(p.plan_name) LIKE UPPER(?))");
                String p = "%" + t + "%";
                params.add(p);
                params.add(p);
            }
        }

        sql.append(" ORDER BY i.invoice_id DESC");

        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new InvoiceRow(
                        rs.getInt("invoice_id"),
                        rs.getInt("subscription_id"),
                        rs.getString("customer_name"),
                        rs.getString("plan_name"),
                        rs.getDouble("subtotal"),
                        rs.getDouble("tax_amount"),
                        rs.getDouble("late_fee"),
                        rs.getDouble("total_amount"),
                        rs.getDouble("amount_paid"),
                        rs.getDouble("outstanding"),
                        rs.getString("due_date"),
                        rs.getString("status")
                    ));
                }
            }
        }
        return list;
    }

    // ── loadOverdue ───────────────────────────────────────────────────────────

    public static List<OverdueRow> loadOverdue() throws SQLException {
        List<OverdueRow> list = new ArrayList<>();
        String sql =
            "SELECT invoice_id, subscription_id, customer_name, email, phone, " +
            "       plan_name, total_amount, amount_paid, outstanding, " +
            "       TO_CHAR(due_date, 'YYYY-MM-DD') AS due_date, days_overdue, status " +
            "FROM   v_overdue_invoices";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new OverdueRow(
                    rs.getInt("invoice_id"),
                    rs.getInt("subscription_id"),
                    rs.getString("customer_name"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getString("plan_name"),
                    rs.getDouble("total_amount"),
                    rs.getDouble("amount_paid"),
                    rs.getDouble("outstanding"),
                    rs.getString("due_date"),
                    rs.getInt("days_overdue"),
                    rs.getString("status")
                ));
            }
        }
        return list;
    }

    // ── findInvoice ───────────────────────────────────────────────────────────

    public static InvoiceDetail findInvoice(int invoiceId) throws SQLException {
        String sql =
            "SELECT i.invoice_id, i.subscription_id, " +
            "       c.first_name || ' ' || c.last_name AS customer_name, " +
            "       c.email, " +
            "       p.plan_name, " +
            "       TO_CHAR(i.billing_period_start, 'YYYY-MM-DD') AS period_start, " +
            "       TO_CHAR(i.billing_period_end,   'YYYY-MM-DD') AS period_end, " +
            "       i.subtotal, i.tax_amount, " +
            "       NVL(i.late_fee, 0) AS late_fee, " +
            "       NVL(i.discount_amount, 0) AS discount_amount, " +
            "       i.total_amount, i.amount_paid, " +
            "       (i.total_amount - i.amount_paid) AS outstanding, " +
            "       TO_CHAR(i.due_date, 'YYYY-MM-DD') AS due_date, " +
            "       i.status, " +
            "       TO_CHAR(i.issued_at, 'YYYY-MM-DD') AS issued_at " +
            "FROM   invoice i " +
            "JOIN   subscription s ON i.subscription_id = s.subscription_id " +
            "JOIN   customer c     ON s.customer_id     = c.customer_id " +
            "JOIN   plan p         ON s.plan_id         = p.plan_id " +
            "WHERE  i.invoice_id = ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new InvoiceDetail(
                        rs.getInt("invoice_id"),
                        rs.getInt("subscription_id"),
                        rs.getString("customer_name"),
                        rs.getString("email"),
                        rs.getString("plan_name"),
                        rs.getString("period_start"),
                        rs.getString("period_end"),
                        rs.getDouble("subtotal"),
                        rs.getDouble("tax_amount"),
                        rs.getDouble("late_fee"),
                        rs.getDouble("discount_amount"),
                        rs.getDouble("total_amount"),
                        rs.getDouble("amount_paid"),
                        rs.getDouble("outstanding"),
                        rs.getString("due_date"),
                        rs.getString("status"),
                        rs.getString("issued_at")
                    );
                }
            }
        }
        return null;
    }

    // ── loadPayments ──────────────────────────────────────────────────────────

    public static List<PaymentRow> loadPayments(int invoiceId) throws SQLException {
        List<PaymentRow> list = new ArrayList<>();
        String sql =
            "SELECT payment_id, amount, payment_method, " +
            "       NVL(transaction_ref, '-') AS transaction_ref, " +
            "       TO_CHAR(payment_date, 'YYYY-MM-DD') AS payment_date " +
            "FROM   payment " +
            "WHERE  invoice_id = ? " +
            "ORDER BY payment_date DESC, payment_id DESC";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new PaymentRow(
                        rs.getInt("payment_id"),
                        rs.getDouble("amount"),
                        rs.getString("payment_method"),
                        rs.getString("transaction_ref"),
                        rs.getString("payment_date")
                    ));
                }
            }
        }
        return list;
    }

    // ── recordPayment ─────────────────────────────────────────────────────────
    // Calls pkg_billing.sp_record_payment.
    // Java-side pre-validation: amount > 0 AND amount <= outstanding.
    // sp_record_payment returns a result message via OUT param; if it starts
    // with "ERROR:" we surface it as IllegalStateException.

    public static String recordPayment(int invoiceId, double amount,
                                       String method, String transactionRef,
                                       double outstanding) throws SQLException {
        requireWrite("RECORD_PAYMENT");

        if (amount <= 0)
            throw new IllegalStateException("Payment amount must be greater than zero.");
        if (amount > outstanding + 0.005) // small epsilon for floating point
            throw new IllegalStateException(
                "Payment amount (" + String.format("%.2f", amount) +
                ") exceeds outstanding balance (" + String.format("%.2f", outstanding) + ").");

        String call = "{call pkg_billing.sp_record_payment(?, ?, ?, ?, ?, ?)}";
        try (Connection conn = Db.getConnection();
             CallableStatement cs = conn.prepareCall(call)) {
            cs.setInt(1, invoiceId);
            cs.setDouble(2, amount);
            cs.setString(3, method);
            cs.setString(4, transactionRef != null ? transactionRef : "");
            cs.setInt(5, Session.getUserId());
            cs.registerOutParameter(6, Types.VARCHAR);
            cs.execute();
            String result = cs.getString(6);
            log.info("sp_record_payment invoice={} amount={} result={}", invoiceId, amount, result);
            if (result != null && result.startsWith("ERROR:")) {
                throw new IllegalStateException(result.substring(6).trim());
            }
            return result != null ? result : "Payment recorded.";
        } catch (SQLException ex) {
            throw new IllegalStateException(friendlyMessage(ex), ex);
        }
    }

    // ── generateMonthly ───────────────────────────────────────────────────────

    public static int generateMonthly(int year, int month) throws SQLException {
        requireWrite("GENERATE");
        String call = "{call pkg_billing.sp_generate_monthly_invoices(?, ?, ?)}";
        try (Connection conn = Db.getConnection();
             CallableStatement cs = conn.prepareCall(call)) {
            cs.setInt(1, year);
            cs.setInt(2, month);
            cs.registerOutParameter(3, Types.NUMERIC);
            cs.execute();
            int count = cs.getInt(3);
            log.info("sp_generate_monthly_invoices year={} month={} created={}", year, month, count);
            return count;
        } catch (SQLException ex) {
            throw new IllegalStateException(friendlyMessage(ex), ex);
        }
    }

    // ── previewSuspendCount ───────────────────────────────────────────────────
    // Uses the same conditions as sp_suspend_overdue_accounts to count
    // how many ACTIVE subscriptions would be suspended.

    public static int previewSuspendCount() throws SQLException {
        String sql =
            "SELECT COUNT(*) FROM subscription s " +
            "WHERE  s.status = 'ACTIVE' " +
            "AND EXISTS (" +
            "  SELECT 1 FROM invoice i " +
            "  WHERE  i.subscription_id = s.subscription_id " +
            "    AND  i.status = 'OVERDUE' " +
            "    AND  i.due_date < TRUNC(SYSDATE) - 30" +
            ")";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ── suspendOverdue ────────────────────────────────────────────────────────

    public static int suspendOverdue() throws SQLException {
        requireWrite("SUSPEND");
        String call = "{call pkg_billing.sp_suspend_overdue_accounts(?)}";
        try (Connection conn = Db.getConnection();
             CallableStatement cs = conn.prepareCall(call)) {
            cs.registerOutParameter(1, Types.NUMERIC);
            cs.execute();
            int count = cs.getInt(1);
            log.info("sp_suspend_overdue_accounts suspended={}", count);
            return count;
        } catch (SQLException ex) {
            throw new IllegalStateException(friendlyMessage(ex), ex);
        }
    }
}
