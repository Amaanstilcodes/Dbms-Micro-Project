package com.isp.dao;

import com.isp.Db;
import com.isp.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for SUBSCRIPTION table.
 *
 * Role permissions (enforced here AND in the UI):
 *   ADMIN          – create, suspend, resume, terminate, read
 *   BILLING_CLERK  – create, suspend, resume, read  (no terminate)
 *   SUPPORT_AGENT  – read only
 *   TECHNICIAN     – read only
 *
 * All write methods throw SecurityException on violation before touching the DB.
 *
 * GST_RATE = 0.18  (matches pkg_billing.v_tax_rate, line 208 of V005)
 */
public class SubscriptionDao {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionDao.class);

    /** Must match pkg_billing.v_tax_rate constant = 0.18 */
    public static final double GST_RATE = 0.18;

    // ── Model records ─────────────────────────────────────────────────────────

    public record SubscriptionRow(
            int    subscriptionId,
            String customerName,
            String planName,
            double price,
            String areaName,
            String startDate,
            String status
    ) {}

    public record CustomerItem(int id, String display) {
        @Override public String toString() { return display; }
    }

    public record PlanItem(int id, String name, int speedMbps, double price, String planType) {
        @Override public String toString() {
            return name + " (" + speedMbps + " Mbps) \u2013 \u20b9" + String.format("%.0f", price);
        }
    }

    public record AreaItem(int id, String display) {
        @Override public String toString() { return display; }
    }

    // ── Permission helpers ───────────────────────────────────────────────────

    private static String safeRole() {
        String r = Session.getRole();
        return r != null ? r : "SUPPORT_AGENT";
    }

    /**
     * Strips the "ORA-20nnn: " prefix that Oracle JDBC leaves on
     * RAISE_APPLICATION_ERROR messages so the user sees only the plain text
     * written by the trigger/procedure (e.g. "Cannot delete customer …").
     * If the message is not an ORA-20xxx, it is returned unchanged.
     */
    private static String friendlyMessage(SQLException ex) {
        String msg = ex.getMessage();
        if (msg == null) return "Database error (no details).";
        // JDBC wraps it as: "ORA-20nnn: <trigger message>\nORA-06512: ..."
        // Keep only the first line and strip the ORA-20nnn: prefix.
        String firstLine = msg.split("\n")[0].trim();
        return firstLine.replaceFirst("ORA-20\\d+:\\s*", "");
    }

    private static void requireWrite(String action) {
        String role = safeRole();
        switch (action) {
            case "CREATE", "SUSPEND", "RESUME" -> {
                if (!"ADMIN".equals(role) && !"BILLING_CLERK".equals(role))
                    throw new SecurityException("Role " + role + " cannot perform: " + action);
            }
            case "TERMINATE" -> {
                if (!"ADMIN".equals(role))
                    throw new SecurityException("Only ADMIN can terminate subscriptions.");
            }
            default -> throw new SecurityException("Unknown action: " + action);
        }
    }

    // ── List query ───────────────────────────────────────────────────────────
    // Direct JOIN on SUBSCRIPTION (NOT the view) so ALL statuses are returned.
    // LEFT JOIN service_area so subscriptions without an area still appear.

    public static List<SubscriptionRow> search(String text, String status) throws SQLException {
        List<SubscriptionRow> list = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
            "SELECT s.subscription_id, " +
            "       c.first_name || ' ' || c.last_name AS customer_name, " +
            "       p.plan_name, p.price, " +
            "       NVL(sa.area_name, '-') AS area_name, " +
            "       TO_CHAR(s.start_date, 'YYYY-MM-DD') AS start_date, " +
            "       s.status " +
            "FROM   subscription s " +
            "JOIN   customer      c  ON s.customer_id = c.customer_id " +
            "JOIN   plan          p  ON s.plan_id     = p.plan_id " +
            "LEFT JOIN service_area sa ON s.area_id   = sa.area_id " +
            "WHERE  1 = 1"
        );

        List<Object> params = new ArrayList<>();

        if (status != null && !status.isBlank() && !"All".equalsIgnoreCase(status)) {
            sql.append(" AND s.status = ?");
            params.add(status.trim());
        }

        if (text != null && !text.trim().isEmpty()) {
            sql.append(" AND (UPPER(c.first_name || ' ' || c.last_name) LIKE UPPER(?)" +
                       "  OR  UPPER(p.plan_name) LIKE UPPER(?))");
            String p = "%" + text.trim() + "%";
            params.add(p);
            params.add(p);
        }

        sql.append(" ORDER BY s.subscription_id DESC");

        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new SubscriptionRow(
                        rs.getInt("subscription_id"),
                        rs.getString("customer_name"),
                        rs.getString("plan_name"),
                        rs.getDouble("price"),
                        rs.getString("area_name"),
                        rs.getString("start_date"),
                        rs.getString("status")
                    ));
                }
            }
        }
        return list;
    }

    // ── Wizard dropdowns ─────────────────────────────────────────────────────

    public static List<CustomerItem> loadActiveCustomers() throws SQLException {
        List<CustomerItem> list = new ArrayList<>();
        String sql = "SELECT customer_id, " +
                     "       first_name || ' ' || last_name || ' (' || email || ')' AS display " +
                     "FROM   customer WHERE status = 'ACTIVE' ORDER BY first_name, last_name";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new CustomerItem(rs.getInt("customer_id"), rs.getString("display")));
            }
        }
        return list;
    }

    public static List<PlanItem> loadActivePlans() throws SQLException {
        List<PlanItem> list = new ArrayList<>();
        String sql = "SELECT plan_id, plan_name, speed_mbps, price, plan_type " +
                     "FROM   plan WHERE is_active = 'Y' ORDER BY price";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new PlanItem(
                    rs.getInt("plan_id"),
                    rs.getString("plan_name"),
                    rs.getInt("speed_mbps"),
                    rs.getDouble("price"),
                    rs.getString("plan_type")
                ));
            }
        }
        return list;
    }

    public static List<AreaItem> loadAreas() throws SQLException {
        List<AreaItem> list = new ArrayList<>();
        String sql = "SELECT area_id, " +
                     "       area_name || ' \u2013 ' || city || ' (' || pin_code || ')' AS display " +
                     "FROM   service_area WHERE is_active = 'Y' ORDER BY area_name";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new AreaItem(rs.getInt("area_id"), rs.getString("display")));
            }
        }
        return list;
    }

    // ── Duplicate guard ──────────────────────────────────────────────────────
    // Blocks creating a second PENDING/ACTIVE/SUSPENDED subscription for the
    // same customer+plan combination.

    public static boolean existsActive(int customerId, int planId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM subscription " +
                     "WHERE  customer_id = ? AND plan_id = ? " +
                     "AND    status IN ('PENDING','ACTIVE','SUSPENDED')";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.setInt(2, planId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // ── Create ───────────────────────────────────────────────────────────────
    // No PL/SQL procedure exists for subscription creation (V005 confirmed).
    // Uses plain INSERT with explicit transaction + rollback.
    // Default status = 'PENDING' per schema constraint default.

    public static void create(int customerId, int planId, int areaId) throws SQLException {
        requireWrite("CREATE");

        // Duplicate check first (throws friendly IllegalStateException, not SQL)
        if (existsActive(customerId, planId)) {
            throw new IllegalStateException(
                "This customer already has an active, pending, or suspended subscription for that plan.");
        }

        String sql = "INSERT INTO subscription (customer_id, plan_id, area_id, start_date, status) " +
                     "VALUES (?, ?, ?, SYSDATE, 'PENDING')";

        try (Connection conn = Db.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, customerId);
                ps.setInt(2, planId);
                ps.setInt(3, areaId);
                ps.executeUpdate();
                conn.commit();
                log.info("Subscription created: customer={} plan={} area={}", customerId, planId, areaId);
            } catch (SQLException ex) {
                conn.rollback();
                // Surface ORA-20xxx trigger messages as friendly text
                throw new IllegalStateException(friendlyMessage(ex), ex);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // ── Status transitions ───────────────────────────────────────────────────
    // DB CHECK allows any of: PENDING, ACTIVE, SUSPENDED, TERMINATED.
    // No trigger blocks TERMINATED -> ACTIVE at DB level, but the UI
    // never exposes "Reactivate" for TERMINATED rows (per requirement).
    //
    // Allowed transitions exposed to the UI:
    //   PENDING/SUSPENDED -> ACTIVE    (Resume)
    //   ACTIVE            -> SUSPENDED (Suspend)
    //   ACTIVE/SUSPENDED  -> TERMINATED (Terminate, ADMIN only)

    public static void changeStatus(int subscriptionId, String newStatus) throws SQLException {
        switch (newStatus) {
            case "ACTIVE"     -> requireWrite("RESUME");
            case "SUSPENDED"  -> requireWrite("SUSPEND");
            case "TERMINATED" -> requireWrite("TERMINATE");
            default           -> throw new IllegalArgumentException("Unknown target status: " + newStatus);
        }

        // trg_subscription_updated_at will stamp updated_at automatically on UPDATE
        String sql = "UPDATE subscription SET status = ? WHERE subscription_id = ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, subscriptionId);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new IllegalStateException("Subscription #" + subscriptionId + " not found.");
            log.info("Subscription #{} -> {}", subscriptionId, newStatus);
        } catch (SQLException ex) {
            // Surface ORA-20xxx trigger messages as friendly text
            throw new IllegalStateException(friendlyMessage(ex), ex);
        }
    }
}
