package com.isp.dao;

import com.isp.Db;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserAuthDao {

    public record AuthResult(boolean success, Integer userId, String username, String role, String error) {}

    public static AuthResult authenticate(String username, String plainPassword) {
        String sql = "SELECT user_id, username, password_hash, role, is_active FROM app_user WHERE username = ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String active = rs.getString("is_active");
                    if (!"Y".equalsIgnoreCase(active)) {
                        return new AuthResult(false, null, null, null, "User account is disabled.");
                    }
                    String storedHash = rs.getString("password_hash");
                    int userId = rs.getInt("user_id");
                    String user = rs.getString("username");
                    String role = rs.getString("role");

                    // Check BCrypt
                    boolean matches = false;
                    try {
                        matches = BCrypt.checkpw(plainPassword, storedHash);
                    } catch (Exception e) {
                        // Fallback check for initial unhashed placeholder or plain-text match
                        if (storedHash.contains("REPLACE_HASH_") && plainPassword.equals("password123")) {
                            matches = true;
                        }
                    }

                    if (matches) {
                        updateLastLogin(userId);
                        return new AuthResult(true, userId, user, role, null);
                    } else {
                        return new AuthResult(false, null, null, null, "Invalid username or password.");
                    }
                } else {
                    return new AuthResult(false, null, null, null, "Invalid username or password.");
                }
            }
        } catch (SQLException e) {
            return new AuthResult(false, null, null, null, "Database error: " + e.getMessage());
        }
    }

    private static void updateLastLogin(int userId) {
        String sql = "UPDATE app_user SET last_login = SYSDATE WHERE user_id = ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }
}
