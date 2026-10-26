package com.isp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class Db {
    private static final Logger log = LoggerFactory.getLogger(Db.class);
    private static final Properties props = new Properties();

    static {
        try (InputStream in = Db.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
            } else {
                log.warn("db.properties not found on classpath!");
            }
        } catch (Exception e) {
            log.error("Failed to load db.properties", e);
        }
    }

    private Db() {}

    public static Connection getConnection() throws SQLException {
        String url = props.getProperty("url", "jdbc:oracle:thin:@//localhost:1521/FREEPDB1");
        String user = props.getProperty("user", "isp_app");
        String password = props.getProperty("password", "");
        return DriverManager.getConnection(url, user, password);
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn.isValid(3);
        } catch (SQLException e) {
            log.error("Database connection test failed: {}", e.getMessage());
            return false;
        }
    }

    public static void main(String[] args) {
        boolean ok = testConnection();
        System.out.println("==========================================");
        System.out.println("DATABASE CONNECTION STATUS: " + (ok ? "CONNECTED SUCCESSFULLY" : "CONNECTION FAILED"));
        System.out.println("==========================================");
    }
}
