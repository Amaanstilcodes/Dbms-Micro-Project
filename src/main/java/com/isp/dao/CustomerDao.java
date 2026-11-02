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

public class CustomerDao {
    private static final Logger log = LoggerFactory.getLogger(CustomerDao.class);

    private static void enforcePermission(String action) {
        String role = Session.getRole();
        if (role == null) role = "SUPPORT_AGENT";

        boolean isAdmin = "ADMIN".equals(role);
        boolean isSupport = "SUPPORT_AGENT".equals(role);
        boolean isBilling = "BILLING_CLERK".equals(role);

        switch (action) {
            case "CREATE", "UPDATE" -> {
                if (!isAdmin && !isSupport) {
                    throw new SecurityException("Role " + role + " is not authorized to " + action + " customer accounts.");
                }
            }
            case "DEACTIVATE" -> {
                if (!isAdmin) {
                    throw new SecurityException("Role " + role + " is not authorized to deactivate customer accounts. Admin privileges required.");
                }
            }
            case "READ" -> {
                if (!isAdmin && !isSupport && !isBilling) {
                    throw new SecurityException("Role " + role + " is not authorized to access customer records.");
                }
            }
            default -> throw new SecurityException("Unauthorized action: " + action);
        }
    }

    public static List<CustomerModels.CustomerRow> search(String query, String status, String type) throws SQLException {
        enforcePermission("READ");
        List<CustomerModels.CustomerRow> list = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
            "SELECT c.customer_id, c.first_name || ' ' || c.last_name AS full_name, " +
            "       c.email, c.phone, c.customer_type, NVL(sa.area_name, '-') AS area_name, c.status, " +
            "       TO_CHAR(c.created_at, 'YYYY-MM-DD') AS reg_date " +
            "FROM customer c " +
            "LEFT JOIN service_area sa ON c.area_id = sa.area_id " +
            "WHERE 1=1"
        );

        List<Object> params = new ArrayList<>();

        if (status != null && !status.isBlank() && !"All Statuses".equalsIgnoreCase(status)) {
            sql.append(" AND c.status = ?");
            params.add(status.trim());
        }
        if (type != null && !type.isBlank() && !"All Types".equalsIgnoreCase(type)) {
            sql.append(" AND c.customer_type = ?");
            params.add(type.trim());
        }
        if (query != null && !query.trim().isEmpty()) {
            sql.append(" AND (UPPER(c.first_name) LIKE UPPER(?) " +
                       " OR UPPER(c.last_name) LIKE UPPER(?) " +
                       " OR UPPER(c.first_name || ' ' || c.last_name) LIKE UPPER(?) " +
                       " OR UPPER(c.email) LIKE UPPER(?) " +
                       " OR c.phone LIKE ?)");
            String p = "%" + query.trim() + "%";
            params.add(p);
            params.add(p);
            params.add(p);
            params.add(p);
            params.add(p);
        }

        sql.append(" ORDER BY c.customer_id DESC");

        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new CustomerModels.CustomerRow(
                        rs.getInt("customer_id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("customer_type"),
                        rs.getString("area_name"),
                        rs.getString("status"),
                        rs.getString("reg_date")
                    ));
                }
            }
        }
        return list;
    }

    public static List<CustomerModels.ServiceAreaItem> loadAreas() throws SQLException {
        List<CustomerModels.ServiceAreaItem> areas = new ArrayList<>();
        String sql = "SELECT area_id, area_name, city, pin_code FROM service_area WHERE is_active = 'Y' ORDER BY area_name ASC";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                areas.add(new CustomerModels.ServiceAreaItem(
                    rs.getInt("area_id"),
                    rs.getString("area_name"),
                    rs.getString("city"),
                    rs.getString("pin_code")
                ));
            }
        }
        return areas;
    }

    public static boolean existsByEmail(String email, Integer excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM customer WHERE LOWER(email) = LOWER(?)" +
                     (excludeId != null ? " AND customer_id <> ?" : "");
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            if (excludeId != null) {
                ps.setInt(2, excludeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public static boolean existsByPhone(String phone, Integer excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM customer WHERE phone = ?" +
                     (excludeId != null ? " AND customer_id <> ?" : "");
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone.trim());
            if (excludeId != null) {
                ps.setInt(2, excludeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public static CustomerModels.CustomerDetails findById(int customerId) throws SQLException {
        enforcePermission("READ");
        String sql = "SELECT c.customer_id, c.customer_type, c.first_name, c.last_name, c.company_name, " +
                     "       c.email, c.phone, c.alternate_phone, c.area_id, NVL(sa.area_name, '-') AS area_name, c.status, " +
                     "       c.id_proof_type, c.id_proof_number, c.gst_number, " +
                     "       TO_CHAR(c.created_at, 'YYYY-MM-DD HH24:MI') AS reg_date, " +
                     "       TO_CHAR(c.updated_at, 'YYYY-MM-DD HH24:MI') AS mod_date " +
                     "FROM customer c " +
                     "LEFT JOIN service_area sa ON c.area_id = sa.area_id " +
                     "WHERE c.customer_id = ?";

        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Subscriptions
                    List<CustomerModels.CustomerDetails.SubInfo> subs = new ArrayList<>();
                    String subSql = "SELECT s.subscription_id, p.plan_name, p.plan_type, s.status, " +
                                    "       TO_CHAR(s.start_date, 'YYYY-MM-DD') AS s_date, " +
                                    "       TO_CHAR(s.end_date, 'YYYY-MM-DD') AS e_date " +
                                    "FROM subscription s " +
                                    "JOIN plan p ON s.plan_id = p.plan_id " +
                                    "WHERE s.customer_id = ? ORDER BY s.subscription_id DESC";
                    try (PreparedStatement subPs = conn.prepareStatement(subSql)) {
                        subPs.setInt(1, customerId);
                        try (ResultSet subRs = subPs.executeQuery()) {
                            while (subRs.next()) {
                                subs.add(new CustomerModels.CustomerDetails.SubInfo(
                                    subRs.getInt("subscription_id"),
                                    subRs.getString("plan_name"),
                                    subRs.getString("plan_type"),
                                    subRs.getString("status"),
                                    subRs.getString("s_date"),
                                    subRs.getString("e_date") != null ? subRs.getString("e_date") : "Ongoing"
                                ));
                            }
                        }
                    }

                    // Last 5 Invoices
                    List<CustomerModels.CustomerDetails.InvInfo> invs = new ArrayList<>();
                    String invSql = "SELECT i.invoice_id, i.total_amount, i.amount_paid, i.status, " +
                                    "       TO_CHAR(i.due_date, 'YYYY-MM-DD') AS d_date " +
                                    "FROM invoice i " +
                                    "JOIN subscription s ON i.subscription_id = s.subscription_id " +
                                    "WHERE s.customer_id = ? " +
                                    "ORDER BY i.invoice_id DESC FETCH FIRST 5 ROWS ONLY";
                    try (PreparedStatement invPs = conn.prepareStatement(invSql)) {
                        invPs.setInt(1, customerId);
                        try (ResultSet invRs = invPs.executeQuery()) {
                            while (invRs.next()) {
                                invs.add(new CustomerModels.CustomerDetails.InvInfo(
                                    invRs.getInt("invoice_id"),
                                    invRs.getDouble("total_amount"),
                                    invRs.getDouble("amount_paid"),
                                    invRs.getString("status"),
                                    invRs.getString("d_date")
                                ));
                            }
                        }
                    }

                    // Tickets
                    List<CustomerModels.CustomerDetails.TicketInfo> tkts = new ArrayList<>();
                    String tktSql = "SELECT ticket_id, subject, priority, status, " +
                                    "       TO_CHAR(created_at, 'YYYY-MM-DD') AS c_date " +
                                    "FROM support_ticket " +
                                    "WHERE customer_id = ? ORDER BY ticket_id DESC FETCH FIRST 5 ROWS ONLY";
                    try (PreparedStatement tktPs = conn.prepareStatement(tktSql)) {
                        tktPs.setInt(1, customerId);
                        try (ResultSet tktRs = tktPs.executeQuery()) {
                            while (tktRs.next()) {
                                tkts.add(new CustomerModels.CustomerDetails.TicketInfo(
                                    tktRs.getInt("ticket_id"),
                                    tktRs.getString("subject"),
                                    tktRs.getString("priority"),
                                    tktRs.getString("status"),
                                    tktRs.getString("c_date")
                                ));
                            }
                        }
                    }

                    return new CustomerModels.CustomerDetails(
                        rs.getInt("customer_id"),
                        rs.getString("customer_type"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("company_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("alternate_phone"),
                        rs.getInt("area_id"),
                        rs.getString("area_name"),
                        rs.getString("status"),
                        rs.getString("id_proof_type"),
                        rs.getString("id_proof_number"),
                        rs.getString("gst_number"),
                        rs.getString("reg_date"),
                        rs.getString("mod_date"),
                        subs,
                        invs,
                        tkts
                    );
                }
            }
        }
        return null;
    }

    public static void insert(
        String customerType,
        String firstName,
        String lastName,
        String companyName,
        String email,
        String phone,
        String alternatePhone,
        int areaId,
        String idProofType,
        String idProofNumber,
        String gstNumber,
        String status
    ) throws SQLException {
        enforcePermission("CREATE");

        String sql = "INSERT INTO customer (customer_type, first_name, last_name, company_name, email, phone, " +
                     "alternate_phone, area_id, id_proof_type, id_proof_number, gst_number, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerType);
            ps.setString(2, firstName);
            ps.setString(3, lastName);
            ps.setString(4, (companyName != null && !companyName.isBlank()) ? companyName.trim() : null);
            ps.setString(5, email.trim());
            ps.setString(6, phone.trim());
            ps.setString(7, (alternatePhone != null && !alternatePhone.isBlank()) ? alternatePhone.trim() : null);
            ps.setInt(8, areaId);
            ps.setString(9, (idProofType != null && !idProofType.isBlank()) ? idProofType.trim() : null);
            ps.setString(10, (idProofNumber != null && !idProofNumber.isBlank()) ? idProofNumber.trim() : null);
            ps.setString(11, (gstNumber != null && !gstNumber.isBlank()) ? gstNumber.trim() : null);
            ps.setString(12, (status != null && !status.isBlank()) ? status.trim() : "ACTIVE");

            ps.executeUpdate();
        }
    }

    public static void update(
        int customerId,
        String customerType,
        String firstName,
        String lastName,
        String companyName,
        String email,
        String phone,
        String alternatePhone,
        int areaId,
        String idProofType,
        String idProofNumber,
        String gstNumber,
        String status
    ) throws SQLException {
        enforcePermission("UPDATE");

        String sql = "UPDATE customer SET " +
                     "  customer_type = ?, first_name = ?, last_name = ?, company_name = ?, " +
                     "  email = ?, phone = ?, alternate_phone = ?, area_id = ?, " +
                     "  id_proof_type = ?, id_proof_number = ?, gst_number = ?, status = ? " +
                     "WHERE customer_id = ?";

        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerType);
            ps.setString(2, firstName);
            ps.setString(3, lastName);
            ps.setString(4, (companyName != null && !companyName.isBlank()) ? companyName.trim() : null);
            ps.setString(5, email.trim());
            ps.setString(6, phone.trim());
            ps.setString(7, (alternatePhone != null && !alternatePhone.isBlank()) ? alternatePhone.trim() : null);
            ps.setInt(8, areaId);
            ps.setString(9, (idProofType != null && !idProofType.isBlank()) ? idProofType.trim() : null);
            ps.setString(10, (idProofNumber != null && !idProofNumber.isBlank()) ? idProofNumber.trim() : null);
            ps.setString(11, (gstNumber != null && !gstNumber.isBlank()) ? gstNumber.trim() : null);
            ps.setString(12, status);
            ps.setInt(13, customerId);

            ps.executeUpdate();
        }
    }

    public static void deactivate(int customerId) throws SQLException {
        enforcePermission("DEACTIVATE");

        String sql = "UPDATE customer SET status = 'INACTIVE' WHERE customer_id = ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.executeUpdate();
        }
    }
}
