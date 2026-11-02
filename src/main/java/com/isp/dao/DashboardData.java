package com.isp.dao;

import java.util.List;
import java.util.Map;

public record DashboardData(
    int activeCustomers,
    int activeSubscriptions,
    double currentMonthRevenue,
    double previousMonthRevenue,
    int currentMonthActiveCustomers,
    int previousMonthActiveCustomers,
    int overdueInvoices,
    int openTickets,
    int pendingInstalls,
    Map<String, Double> monthlyRevenueTrend, // ordered e.g. "2025-06" -> 12500.0
    Map<String, Integer> planDistribution,   // "FIBER" -> 14
    List<RecentActivityItem> recentActivities
) {
    public record RecentActivityItem(
        String eventType,
        String title,
        String subtitle,
        String timeAgo,
        String iconLiteral,
        String iconColorHex
    ) {}

    public Double calculateRevenueDeltaPercent() {
        if (previousMonthRevenue <= 0.0) return null;
        return ((currentMonthRevenue - previousMonthRevenue) / previousMonthRevenue) * 100.0;
    }

    public Double calculateSubscriberDeltaPercent() {
        if (previousMonthActiveCustomers <= 0) return null;
        return (((double) activeCustomers - previousMonthActiveCustomers) / previousMonthActiveCustomers) * 100.0;
    }
}
