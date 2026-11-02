package com.isp.dao;

import com.isp.Db;
import com.isp.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Analytical Views &amp; Reports screen.
 *
 * All queries execute against Oracle views defined in V004__views.sql:
 * - V_MONTHLY_REVENUE
 * - V_OVERDUE_INVOICES
 * - V_CUSTOMER_BALANCE
 * - V_TECHNICIAN_WORKLOAD
 * - V_DATA_USAGE_SUMMARY
 * - V_ACTIVE_SUBSCRIPTIONS
 *
 * Role permissions:
 * - ADMIN: all 6 reports
 * - BILLING_CLERK: Monthly Revenue, Overdue Invoices, Customer Balance, Active Subscriptions
 * - SUPPORT_AGENT: Active Subscriptions, Data Usage Summary
 * - TECHNICIAN: Technician Workload
 * If Session role is null, defaults to least privileged role (TECHNICIAN).
 */
public class ReportsDao {

    private static final Logger log = LoggerFactory.getLogger(ReportsDao.class);

    // ── Model Records ─────────────────────────────────────────────────────────

    public record MonthRevenueRow(
            String month,
            int    paymentCount,
            double revenue
    ) {}

    public record PlanRevenueRow(
            String month,
            String planType,
            String planName,
            int    paymentCount,
            double revenue
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

    public record CustomerBalanceRow(
            int    customerId,
            String customerName,
            String email,
            int    totalInvoices,
            double totalBilled,
            double totalPaid,
            double outstandingBalance
    ) {}

    public record TechnicianWorkloadRow(
            int    technicianId,
            String technicianName,
            String specialization,
            String areaName,
            int    pendingInstallations,
            int    openTickets,
            int    totalWorkload,
            String workloadStatus
    ) {}

    public record DataUsageRow(
            int    subscriptionId,
            String customerName,
            String planName,
            String capGb,
            String usageMonth,
            double dataUsedGb,
            String usageStatus,
            Double pctUsed
    ) {}

    public record ActiveSubscriptionRow(
            int    subscriptionId,
            String customerName,
            String email,
            String phone,
            String planName,
            String planType,
            int    speedMbps,
            double monthlyPrice,
            String areaName,
            String city,
            String startDate,
            String status
    ) {}

    // ── Security & Role Gates ─────────────────────────────────────────────────

    public static String getEffectiveRole() {
        String role = Session.getRole();
        if (role == null || role.isBlank()) {
            return "TECHNICIAN"; // Default to least-privileged role
        }
        return role.toUpperCase().trim();
    }

    public static void checkPermission(String reportType) {
        String role = getEffectiveRole();
        boolean allowed = switch (reportType) {
            case "REVENUE"     -> "ADMIN".equals(role) || "BILLING_CLERK".equals(role);
            case "OVERDUE"     -> "ADMIN".equals(role) || "BILLING_CLERK".equals(role);
            case "BALANCE"     -> "ADMIN".equals(role) || "BILLING_CLERK".equals(role);
            case "WORKLOAD"    -> "ADMIN".equals(role) || "TECHNICIAN".equals(role);
            case "USAGE"       -> "ADMIN".equals(role) || "SUPPORT_AGENT".equals(role);
            case "ACTIVE_SUBS" -> "ADMIN".equals(role) || "BILLING_CLERK".equals(role) || "SUPPORT_AGENT".equals(role);
            default            -> false;
        };

        if (!allowed) {
            log.warn("Security rejection: role '{}' attempted to access report '{}'", role, reportType);
            throw new SecurityException("Access Denied: Role '" + role + "' is not permitted to access " + reportType + " report.");
        }
    }

    // ── Report Methods ────────────────────────────────────────────────────────

    /**
     * Returns the most recent revenue_month present in v_monthly_revenue,
     * or null if the view contains no rows. No permission check — read-only
     * metadata query used only to seed the month ComboBox default.
     */
    public static String maxRevenueMonth() throws SQLException {
        String sql = "SELECT MAX(revenue_month) FROM v_monthly_revenue";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    /**
     * Total revenue aggregated per month within a month range [fromMonth, toMonth].
     * If either param is null or blank, returns all months.
     */
    public static List<MonthRevenueRow> revenueByMonth(String fromMonth, String toMonth) throws SQLException {
        checkPermission("REVENUE");
        boolean hasFilter = fromMonth != null && !fromMonth.isBlank() && toMonth != null && !toMonth.isBlank();

        String sql = "SELECT revenue_month, " +
                     "       SUM(payment_count) AS payment_count, " +
                     "       SUM(revenue_collected) AS revenue_collected " +
                     "FROM   v_monthly_revenue " +
                     (hasFilter ? "WHERE revenue_month BETWEEN ? AND ? " : "") +
                     "GROUP BY revenue_month " +
                     "ORDER BY revenue_month ASC";

        List<MonthRevenueRow> list = new ArrayList<>();
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (hasFilter) {
                ps.setString(1, fromMonth.trim());
                ps.setString(2, toMonth.trim());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new MonthRevenueRow(
                            rs.getString("revenue_month"),
                            rs.getInt("payment_count"),
                            rs.getDouble("revenue_collected")
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Revenue broken down by plan for a specific month (e.g. '2026-11').
     * If month is null or blank, returns all available plan revenues.
     */
    public static List<PlanRevenueRow> revenueByPlan(String month) throws SQLException {
        checkPermission("REVENUE");
        boolean hasFilter = month != null && !month.isBlank();

        String sql = "SELECT revenue_month, plan_type, plan_name, payment_count, revenue_collected " +
                     "FROM   v_monthly_revenue " +
                     (hasFilter ? "WHERE revenue_month = ? " : "") +
                     "ORDER BY revenue_collected DESC";

        List<PlanRevenueRow> list = new ArrayList<>();
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (hasFilter) {
                ps.setString(1, month.trim());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new PlanRevenueRow(
                            rs.getString("revenue_month"),
                            rs.getString("plan_type"),
                            rs.getString("plan_name"),
                            rs.getInt("payment_count"),
                            rs.getDouble("revenue_collected")
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Overdue invoices with optional customer name / email text filter.
     */
    public static List<OverdueRow> overdue(String text) throws SQLException {
        checkPermission("OVERDUE");
        boolean hasFilter = text != null && !text.isBlank();

        String sql = "SELECT invoice_id, subscription_id, customer_name, email, phone, plan_name, " +
                     "       total_amount, amount_paid, outstanding, " +
                     "       TO_CHAR(due_date, 'YYYY-MM-DD') AS due_date_str, " +
                     "       days_overdue, status " +
                     "FROM   v_overdue_invoices " +
                     (hasFilter ? "WHERE LOWER(customer_name) LIKE ? OR LOWER(email) LIKE ? " : "") +
                     "ORDER BY days_overdue DESC";

        List<OverdueRow> list = new ArrayList<>();
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (hasFilter) {
                String pattern = "%" + text.toLowerCase().trim() + "%";
                ps.setString(1, pattern);
                ps.setString(2, pattern);
            }

            try (ResultSet rs = ps.executeQuery()) {
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
                            rs.getString("due_date_str"),
                            rs.getInt("days_overdue"),
                            rs.getString("status")
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Customer balances aggregated across invoices with optional text filter.
     */
    public static List<CustomerBalanceRow> customerBalance(String text) throws SQLException {
        checkPermission("BALANCE");
        boolean hasFilter = text != null && !text.isBlank();

        String sql = "SELECT customer_id, customer_name, email, total_invoices, " +
                     "       total_billed, total_paid, outstanding_balance " +
                     "FROM   v_customer_balance " +
                     (hasFilter ? "WHERE LOWER(customer_name) LIKE ? OR LOWER(email) LIKE ? " : "") +
                     "ORDER BY outstanding_balance DESC";

        List<CustomerBalanceRow> list = new ArrayList<>();
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (hasFilter) {
                String pattern = "%" + text.toLowerCase().trim() + "%";
                ps.setString(1, pattern);
                ps.setString(2, pattern);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new CustomerBalanceRow(
                            rs.getInt("customer_id"),
                            rs.getString("customer_name"),
                            rs.getString("email"),
                            rs.getInt("total_invoices"),
                            rs.getDouble("total_billed"),
                            rs.getDouble("total_paid"),
                            rs.getDouble("outstanding_balance")
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Operational technician workload metrics and status.
     */
    public static List<TechnicianWorkloadRow> technicianWorkload() throws SQLException {
        checkPermission("WORKLOAD");

        String sql = "SELECT technician_id, technician_name, specialization, area_name, " +
                     "       pending_installations, open_tickets, total_workload, workload_status " +
                     "FROM   v_technician_workload " +
                     "ORDER BY total_workload DESC, technician_name ASC";

        List<TechnicianWorkloadRow> list = new ArrayList<>();
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new TechnicianWorkloadRow(
                        rs.getInt("technician_id"),
                        rs.getString("technician_name"),
                        rs.getString("specialization"),
                        rs.getString("area_name"),
                        rs.getInt("pending_installations"),
                        rs.getInt("open_tickets"),
                        rs.getInt("total_workload"),
                        rs.getString("workload_status")
                ));
            }
        }
        return list;
    }

    /**
     * Data usage summary per subscription with quota status and percentage.
     * Note: View column is USAGE_STATUS (not cap_status).
     */
    public static List<DataUsageRow> dataUsage() throws SQLException {
        checkPermission("USAGE");

        String sql = "SELECT subscription_id, customer_name, plan_name, " +
                     "       NVL(TO_CHAR(data_cap_gb), 'UNLIMITED') AS cap_gb, " +
                     "       usage_month, data_used_gb, usage_status, pct_used " +
                     "FROM   v_data_usage_summary " +
                     "ORDER BY usage_month DESC, data_used_gb DESC";

        List<DataUsageRow> list = new ArrayList<>();
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Double pct = rs.getObject("pct_used") != null ? rs.getDouble("pct_used") : null;
                list.add(new DataUsageRow(
                        rs.getInt("subscription_id"),
                        rs.getString("customer_name"),
                        rs.getString("plan_name"),
                        rs.getString("cap_gb"),
                        rs.getString("usage_month"),
                        rs.getDouble("data_used_gb"),
                        rs.getString("usage_status"),
                        pct
                ));
            }
        }
        return list;
    }

    /**
     * All active customer subscriptions and plan assignments.
     */
    public static List<ActiveSubscriptionRow> activeSubscriptions() throws SQLException {
        checkPermission("ACTIVE_SUBS");

        String sql = "SELECT subscription_id, customer_name, email, phone, " +
                     "       plan_name, plan_type, speed_mbps, monthly_price, " +
                     "       area_name, city, " +
                     "       TO_CHAR(start_date, 'YYYY-MM-DD') AS start_date_str, " +
                     "       subscription_status " +
                     "FROM   v_active_subscriptions " +
                     "ORDER BY start_date DESC";

        List<ActiveSubscriptionRow> list = new ArrayList<>();
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new ActiveSubscriptionRow(
                        rs.getInt("subscription_id"),
                        rs.getString("customer_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("plan_name"),
                        rs.getString("plan_type"),
                        rs.getInt("speed_mbps"),
                        rs.getDouble("monthly_price"),
                        rs.getString("area_name"),
                        rs.getString("city"),
                        rs.getString("start_date_str"),
                        rs.getString("subscription_status")
                ));
            }
        }
        return list;
    }
}
