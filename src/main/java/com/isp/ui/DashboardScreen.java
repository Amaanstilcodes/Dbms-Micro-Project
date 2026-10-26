package com.isp.ui;

import com.isp.Db;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DashboardScreen extends ScrollPane {

    public DashboardScreen() {
        setFitToWidth(true);
        setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox content = new VBox(20);
        content.setPadding(new Insets(20));

        Label header = new Label("System Overview");
        header.setFont(Font.font("System", FontWeight.BOLD, 20));

        FlowPane cardsPane = new FlowPane();
        cardsPane.setHgap(16);
        cardsPane.setVgap(16);

        ProgressIndicator spinner = new ProgressIndicator();
        content.getChildren().addAll(header, spinner, cardsPane);
        setContent(content);

        Task<DashboardData> task = new Task<>() {
            @Override
            protected DashboardData call() throws Exception {
                int activeCustomers = 0;
                int activeSubscriptions = 0;
                double monthlyRevenue = 0.0;
                int overdueInvoices = 0;
                int openTickets = 0;

                try (Connection conn = Db.getConnection();
                     Statement st = conn.createStatement()) {

                    try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM customer WHERE status = 'ACTIVE'")) {
                        if (rs.next()) activeCustomers = rs.getInt(1);
                    }
                    try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM subscription WHERE status = 'ACTIVE'")) {
                        if (rs.next()) activeSubscriptions = rs.getInt(1);
                    }
                    try (ResultSet rs = st.executeQuery("SELECT NVL(SUM(revenue_collected), 0) FROM v_monthly_revenue WHERE revenue_month = TO_CHAR(SYSDATE, 'YYYY-MM')")) {
                        if (rs.next()) monthlyRevenue = rs.getDouble(1);
                    }
                    try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM v_overdue_invoices")) {
                        if (rs.next()) overdueInvoices = rs.getInt(1);
                    }
                    try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM support_ticket WHERE status IN ('OPEN','IN_PROGRESS')")) {
                        if (rs.next()) openTickets = rs.getInt(1);
                    }
                }
                return new DashboardData(activeCustomers, activeSubscriptions, monthlyRevenue, overdueInvoices, openTickets);
            }
        };

        task.setOnSucceeded(e -> {
            content.getChildren().remove(spinner);
            DashboardData d = task.getValue();
            cardsPane.getChildren().addAll(
                createCard("Active Customers", String.valueOf(d.activeCustomers), "#2563eb"),
                createCard("Active Subscriptions", String.valueOf(d.activeSubscriptions), "#059669"),
                createCard("This Month's Revenue", "₹" + String.format("%.2f", d.monthlyRevenue), "#d97706"),
                createCard("Overdue Invoices", String.valueOf(d.overdueInvoices), "#dc2626"),
                createCard("Open Support Tickets", String.valueOf(d.openTickets), "#7c3aed")
            );

            // Add Recent Overdue Invoices quick view
            Label overdueTitle = new Label("Overdue Invoices Alert");
            overdueTitle.setFont(Font.font("System", FontWeight.SEMI_BOLD, 16));
            StackPane overdueTable = TableViewBuilder.buildAsyncTable(
                "SELECT invoice_id, customer_name, plan_name, outstanding, days_overdue, status FROM v_overdue_invoices FETCH FIRST 10 ROWS ONLY"
            );
            overdueTable.setPrefHeight(250);
            content.getChildren().addAll(overdueTitle, overdueTable);
        });

        task.setOnFailed(e -> {
            content.getChildren().remove(spinner);
            TableViewBuilder.showError("Failed to load dashboard data", task.getException().getMessage());
        });

        new Thread(task).start();
    }

    private VBox createCard(String title, String value, String accentColor) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setPrefWidth(220);
        card.setStyle(
            "-fx-background-color: white; -fx-background-radius: 8px; " +
            "-fx-border-color: #e2e8f0; -fx-border-radius: 8px; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 8, 0, 0, 2);"
        );

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px; -fx-font-weight: 600;");

        Label valLbl = new Label(value);
        valLbl.setStyle("-fx-text-fill: " + accentColor + "; -fx-font-size: 22px; -fx-font-weight: bold;");

        card.getChildren().addAll(titleLbl, valLbl);
        return card;
    }

    private record DashboardData(int activeCustomers, int activeSubscriptions, double monthlyRevenue, int overdueInvoices, int openTickets) {}
}
