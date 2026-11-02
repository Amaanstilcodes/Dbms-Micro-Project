package com.isp.dao;

import com.isp.Db;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class DashboardDao {
    private static final Logger log = LoggerFactory.getLogger(DashboardDao.class);

    /**
     * Fetches all aggregated metrics, trend histories, and recent activities for the dashboard.
     * Uses PreparedStatements, try-with-resources, and verified Oracle columns.
     * Resilient design: Each of the 6 sections is wrapped in its own try/catch block so that
     * one failure leaves only that widget empty rather than breaking the entire dashboard.
     */
    public static DashboardData fetchDashboardData() throws SQLException {
        int activeCustomers = 0;
        int activeSubscriptions = 0;
        double currentMonthRevenue = 0.0;
        double previousMonthRevenue = 0.0;
        int currentMonthActiveCustomers = 0;
        int previousMonthActiveCustomers = 0;
        int overdueInvoices = 0;
        int openTickets = 0;
        int pendingInstalls = 0;

        Map<String, Double> monthlyRevenueTrend = new LinkedHashMap<>();
        Map<String, Integer> planDistribution = new LinkedHashMap<>();
        List<DashboardData.RecentActivityItem> recentActivities = new ArrayList<>();

        try (Connection conn = Db.getConnection()) {

            // Section 1: Core KPI Counts
            try {
                String countSql = "SELECT " +
                        "  (SELECT COUNT(*) FROM customer WHERE status = 'ACTIVE') AS active_cust, " +
                        "  (SELECT COUNT(*) FROM subscription WHERE status = 'ACTIVE') AS active_sub, " +
                        "  (SELECT COUNT(*) FROM v_overdue_invoices) AS overdue_inv, " +
                        "  (SELECT COUNT(*) FROM support_ticket WHERE status IN ('OPEN','IN_PROGRESS')) AS open_tkt, " +
                        "  (SELECT COUNT(*) FROM installation WHERE status IN ('PENDING','SCHEDULED')) AS pend_inst " +
                        "FROM dual";
                try (PreparedStatement ps = conn.prepareStatement(countSql);
                     ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        activeCustomers = rs.getInt("active_cust");
                        activeSubscriptions = rs.getInt("active_sub");
                        overdueInvoices = rs.getInt("overdue_inv");
                        openTickets = rs.getInt("open_tkt");
                        pendingInstalls = rs.getInt("pend_inst");
                    }
                }
            } catch (SQLException e) {
                log.error("[DashboardDao] Section 1 (Core KPI Counts) failed: {}", e.getMessage(), e);
            }

            // Section 2: Current & Previous Month Revenue
            try {
                String currRevSql = "SELECT NVL(SUM(amount), 0) FROM payment " +
                        "WHERE payment_date >= TRUNC(SYSDATE, 'MM') AND payment_date < ADD_MONTHS(TRUNC(SYSDATE, 'MM'), 1)";
                try (PreparedStatement ps = conn.prepareStatement(currRevSql);
                     ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        currentMonthRevenue = rs.getDouble(1);
                    }
                }

                String prevRevSql = "SELECT NVL(SUM(amount), 0) FROM payment " +
                        "WHERE payment_date >= ADD_MONTHS(TRUNC(SYSDATE, 'MM'), -1) AND payment_date < TRUNC(SYSDATE, 'MM')";
                try (PreparedStatement ps = conn.prepareStatement(prevRevSql);
                     ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        previousMonthRevenue = rs.getDouble(1);
                    }
                }
            } catch (SQLException e) {
                log.error("[DashboardDao] Section 2 (Revenue Calculation) failed: {}", e.getMessage(), e);
            }

            // Section 3: Customer Period Delta
            try {
                String custDeltaSql = "SELECT " +
                        "  (SELECT COUNT(*) FROM customer WHERE status = 'ACTIVE') AS curr_cnt, " +
                        "  (SELECT COUNT(*) FROM customer WHERE status = 'ACTIVE' AND created_at < TRUNC(SYSDATE, 'MM')) AS prev_cnt " +
                        "FROM dual";
                try (PreparedStatement ps = conn.prepareStatement(custDeltaSql);
                     ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        currentMonthActiveCustomers = rs.getInt("curr_cnt");
                        previousMonthActiveCustomers = rs.getInt("prev_cnt");
                    }
                }
            } catch (SQLException e) {
                log.error("[DashboardDao] Section 3 (Customer Delta) failed: {}", e.getMessage(), e);
            }

            // Section 4: Last 6 Months Revenue Trend
            try {
                String trendSql = "SELECT TO_CHAR(TRUNC(payment_date, 'MM'), 'YYYY-MM') AS yr_mo, " +
                        "       NVL(SUM(amount), 0) AS rev " +
                        "FROM payment " +
                        "WHERE payment_date >= ADD_MONTHS(TRUNC(SYSDATE, 'MM'), -5) " +
                        "GROUP BY TRUNC(payment_date, 'MM') " +
                        "ORDER BY TRUNC(payment_date, 'MM') ASC";
                try (PreparedStatement ps = conn.prepareStatement(trendSql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        monthlyRevenueTrend.put(rs.getString("yr_mo"), rs.getDouble("rev"));
                    }
                }
            } catch (SQLException e) {
                log.error("[DashboardDao] Section 4 (Revenue Trend) failed: {}", e.getMessage(), e);
            }

            // Section 5: Plan Distribution Breakdown
            try {
                String planDistSql = "SELECT p.plan_type, COUNT(s.subscription_id) AS sub_count " +
                        "FROM plan p " +
                        "LEFT JOIN subscription s ON p.plan_id = s.plan_id AND s.status = 'ACTIVE' " +
                        "GROUP BY p.plan_type " +
                        "ORDER BY sub_count DESC";
                try (PreparedStatement ps = conn.prepareStatement(planDistSql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        planDistribution.put(rs.getString("plan_type"), rs.getInt("sub_count"));
                    }
                }
            } catch (SQLException e) {
                log.error("[DashboardDao] Section 5 (Plan Distribution) failed: {}", e.getMessage(), e);
            }

            // Section 6: Recent Activity (Verified actual columns: email and phone instead of non-existent city)
            try {
                String activitySql =
                        "SELECT event_type, title, subtitle, event_time " +
                        "FROM ( " +
                        "    SELECT 'PAYMENT' AS event_type, " +
                        "           'Payment of ₹' || TO_CHAR(p.amount, 'FM999,999,990.00') AS title, " +
                        "           c.first_name || ' ' || c.last_name || ' via ' || p.payment_method AS subtitle, " +
                        "           p.payment_date AS event_time " +
                        "    FROM payment p " +
                        "    JOIN invoice i ON p.invoice_id = i.invoice_id " +
                        "    JOIN subscription s ON i.subscription_id = s.subscription_id " +
                        "    JOIN customer c ON s.customer_id = c.customer_id " +
                        "    UNION ALL " +
                        "    SELECT 'TICKET' AS event_type, " +
                        "           'Ticket #' || t.ticket_id || ': ' || SUBSTR(t.subject, 1, 28) AS title, " +
                        "           'Priority: ' || t.priority || ' | ' || t.status AS subtitle, " +
                        "           t.created_at AS event_time " +
                        "    FROM support_ticket t " +
                        "    UNION ALL " +
                        "    SELECT 'CUSTOMER' AS event_type, " +
                        "           'New Customer: ' || first_name || ' ' || last_name AS title, " +
                        "           email || ' | ' || status AS subtitle, " +
                        "           created_at AS event_time " +
                        "    FROM customer " +
                        "    ORDER BY event_time DESC " +
                        ") " +
                        "FETCH FIRST 8 ROWS ONLY";

                try (PreparedStatement ps = conn.prepareStatement(activitySql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String type = rs.getString("event_type");
                        String title = rs.getString("title");
                        String subtitle = rs.getString("subtitle");
                        java.sql.Timestamp ts = rs.getTimestamp("event_time");
                        String timeAgo = formatTimeAgo(ts);

                        String iconLiteral;
                        String iconColorHex;
                        switch (type) {
                            case "PAYMENT" -> {
                                iconLiteral = "fas-credit-card";
                                iconColorHex = "#16A34A";
                            }
                            case "TICKET" -> {
                                iconLiteral = "fas-ticket-alt";
                                iconColorHex = "#7C3AED";
                            }
                            default -> {
                                iconLiteral = "fas-user-plus";
                                iconColorHex = "#2563EB";
                            }
                        }

                        recentActivities.add(new DashboardData.RecentActivityItem(
                                type, title, subtitle, timeAgo, iconLiteral, iconColorHex
                        ));
                    }
                }
            } catch (SQLException e) {
                log.error("[DashboardDao] Section 6 (Recent Activity) failed: {}", e.getMessage(), e);
            }
        }

        return new DashboardData(
                activeCustomers,
                activeSubscriptions,
                currentMonthRevenue,
                previousMonthRevenue,
                currentMonthActiveCustomers,
                previousMonthActiveCustomers,
                overdueInvoices,
                openTickets,
                pendingInstalls,
                monthlyRevenueTrend,
                planDistribution,
                recentActivities
        );
    }

    private static String formatTimeAgo(java.sql.Timestamp ts) {
        if (ts == null) return "recently";
        long diffMs = System.currentTimeMillis() - ts.getTime();
        long diffSec = Math.max(0, diffMs / 1000);
        if (diffSec < 60) return diffSec + "s ago";
        long diffMin = diffSec / 60;
        if (diffMin < 60) return diffMin + "m ago";
        long diffHours = diffMin / 60;
        if (diffHours < 24) return diffHours + "h ago";
        long diffDays = diffHours / 24;
        if (diffDays < 30) return diffDays + "d ago";
        return (diffDays / 30) + "mo ago";
    }
}
