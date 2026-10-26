package com.isp.util;

/**
 * Holds the currently authenticated user's session data.
 * Set on successful login; cleared on logout.
 */
public class SessionManager {

    private static int    userId;
    private static String username;
    private static String role;      // ADMIN | BILLING_CLERK | TECHNICIAN | SUPPORT_AGENT | CUSTOMER
    private static Integer customerId;
    private static Integer technicianId;

    public static void login(int userId, String username, String role,
                              Integer customerId, Integer technicianId) {
        SessionManager.userId       = userId;
        SessionManager.username     = username;
        SessionManager.role         = role;
        SessionManager.customerId   = customerId;
        SessionManager.technicianId = technicianId;
    }

    public static void logout() {
        userId = 0;
        username = null;
        role = null;
        customerId = null;
        technicianId = null;
    }

    public static int     getUserId()       { return userId; }
    public static String  getUsername()     { return username; }
    public static String  getRole()         { return role; }
    public static Integer getCustomerId()   { return customerId; }
    public static Integer getTechnicianId() { return technicianId; }

    public static boolean isAdmin()          { return "ADMIN".equals(role); }
    public static boolean isBillingClerk()   { return "BILLING_CLERK".equals(role); }
    public static boolean isTechnician()     { return "TECHNICIAN".equals(role); }
    public static boolean isSupportAgent()   { return "SUPPORT_AGENT".equals(role); }
    public static boolean isCustomer()       { return "CUSTOMER".equals(role); }
}
