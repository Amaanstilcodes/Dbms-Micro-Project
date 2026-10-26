package com.isp;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PasswordUtil {
    private static final Logger log = LoggerFactory.getLogger(PasswordUtil.class);

    public static void main(String[] args) {
        String defaultPw = "password123";
        String adminHash = BCrypt.hashpw(defaultPw, BCrypt.gensalt(10));
        String billingHash = BCrypt.hashpw(defaultPw, BCrypt.gensalt(10));
        String techHash = BCrypt.hashpw(defaultPw, BCrypt.gensalt(10));
        String suppHash = BCrypt.hashpw(defaultPw, BCrypt.gensalt(10));

        System.out.println("Updating APP_USER password hashes to match BCrypt for '" + defaultPw + "'...");

        try (Connection conn = Db.getConnection()) {
            updateUser(conn, "admin", adminHash);
            updateUser(conn, "billing1", billingHash);
            updateUser(conn, "tech.rajesh", techHash);
            updateUser(conn, "support.priya", suppHash);
            System.out.println("Successfully updated all seeded APP_USER hashes!");
        } catch (Exception e) {
            log.error("Failed to update passwords", e);
        }
    }

    private static void updateUser(Connection conn, String username, String hash) throws Exception {
        String sql = "UPDATE app_user SET password_hash = ? WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setString(2, username);
            int rows = ps.executeUpdate();
            System.out.println("Updated " + username + " (rows: " + rows + ")");
        }
    }
}
