package com.isp;

public final class Session {
    private static Integer userId;
    private static String username;
    private static String role;

    private Session() {}

    public static void login(Integer id, String name, String userRole) {
        userId = id;
        username = name;
        role = userRole;
    }

    public static void logout() {
        userId = null;
        username = null;
        role = null;
    }

    public static boolean isLoggedIn() {
        return username != null;
    }

    public static Integer getUserId() {
        return userId;
    }

    public static String getUsername() {
        return username;
    }

    public static String getRole() {
        return role;
    }
}
