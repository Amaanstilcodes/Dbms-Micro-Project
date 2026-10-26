package com.isp.ui;

import com.isp.Db;
import com.isp.Session;
import com.isp.ui.components.KpiTile;
import com.isp.ui.components.ToastManager;
import com.isp.ui.util.Formatters;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DashboardScreen extends ScrollPane {

    public DashboardScreen() {
        setFitToWidth(true);
        setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox content = new VBox(24);
        content.setPadding(new Insets(24));
        content.setFillWidth(true);
        content.setMaxWidth(Double.MAX_VALUE);

        // Top Alert Banner for Overdue Invoices
        HBox alertBanner = new HBox(12);
        alertBanner.setAlignment(Pos.CENTER_LEFT);
        alertBanner.setStyle(
            "-fx-background-color: -color-warning-bg; -fx-padding: 12 16; -fx-background-radius: 10; -fx-border-color: -color-warning; -fx-border-radius: 10;"
        );

        FontIcon alertIcon = new FontIcon("fas-exclamation-triangle");
        alertIcon.setIconSize(18);
        alertIcon.setStyle("-fx-icon-color: -color-warning;");

        Label alertText = new Label("Notice: Billing reconciliation cycle detected overdue subscriber invoices.");
        alertText.setStyle("-fx-font-weight: 600; -fx-text-fill: -color-text;");

        Region alertSpacer = new Region();
        HBox.setHgrow(alertSpacer, Priority.ALWAYS);

        Button refreshBtn = new Button(" Refresh Live Data");
        refreshBtn.getStyleClass().add("btn-secondary");
        FontIcon syncIcon = new FontIcon("fas-sync-alt");
        syncIcon.setIconSize(14);
        refreshBtn.setGraphic(syncIcon);

        alertBanner.getChildren().addAll(alertIcon, alertText, alertSpacer, refreshBtn);

        // Row of KPI Tiles: 6 Tiles spread evenly with HGrow ALWAYS
        HBox kpiGrid = new HBox(12);
        kpiGrid.setAlignment(Pos.CENTER_LEFT);
        kpiGrid.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(kpiGrid, Priority.ALWAYS);

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(36, 36);

        // Charts Row using GridPane for 50/50 balanced layout
        GridPane chartsGrid = new GridPane();
        chartsGrid.setHgap(16);
        chartsGrid.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(chartsGrid, Priority.ALWAYS);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(58);
        col1.setHgrow(Priority.ALWAYS);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(42);
        col2.setHgrow(Priority.ALWAYS);

        chartsGrid.getColumnConstraints().addAll(col1, col2);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Plan Name");
        yAxis.setLabel("Revenue (₹)");
        BarChart<String, Number> revenueChart = new BarChart<>(xAxis, yAxis);
        revenueChart.setTitle("Revenue Collection by Plan (V_MONTHLY_REVENUE)");
        revenueChart.setLegendVisible(false);
        revenueChart.setMinHeight(280);
        revenueChart.setPrefHeight(300);
        revenueChart.getStyleClass().add("card");
        GridPane.setHgrow(revenueChart, Priority.ALWAYS);

        PieChart planPieChart = new PieChart();
        planPieChart.setTitle("Subscribers by Plan Type");
        planPieChart.setMinHeight(280);
        planPieChart.setPrefHeight(300);
        planPieChart.getStyleClass().add("card");
        GridPane.setHgrow(planPieChart, Priority.ALWAYS);

        chartsGrid.add(revenueChart, 0, 0);
        chartsGrid.add(planPieChart, 1, 0);

        // Quick Actions Card
        VBox quickActionsCard = new VBox(12);
        quickActionsCard.getStyleClass().add("card");
        quickActionsCard.setFillWidth(true);

        Label qaTitle = new Label("Quick Operations");
        qaTitle.getStyleClass().add("section-title");

        HBox actionsRow = new HBox(12);
        Button addCustBtn = new Button("New Customer");
        addCustBtn.getStyleClass().add("btn-primary");
        FontIcon userPlusIcon = new FontIcon("fas-user-plus");
        userPlusIcon.setIconSize(14);
        addCustBtn.setGraphic(userPlusIcon);
        addCustBtn.setOnAction(e -> ToastManager.showInfo("Navigate to Customers to register a new account."));

        Button recordPayBtn = new Button("Record Payment");
        recordPayBtn.getStyleClass().add("btn-secondary");
        FontIcon payIcon = new FontIcon("fas-credit-card");
        payIcon.setIconSize(14);
        recordPayBtn.setGraphic(payIcon);
        recordPayBtn.setOnAction(e -> ToastManager.showInfo("Navigate to Billing to record an invoice payment."));

        Button genInvBtn = new Button("Run Invoicing");
        genInvBtn.getStyleClass().add("btn-secondary");
        FontIcon boltIcon = new FontIcon("fas-bolt");
        boltIcon.setIconSize(14);
        genInvBtn.setGraphic(boltIcon);
        genInvBtn.setOnAction(e -> ToastManager.showInfo("Navigate to Billing to execute PL/SQL invoice generation."));

        Button newTicketBtn = new Button("Open Ticket");
        newTicketBtn.getStyleClass().add("btn-secondary");
        FontIcon ticketIcon = new FontIcon("fas-ticket-alt");
        ticketIcon.setIconSize(14);
        newTicketBtn.setGraphic(ticketIcon);
        newTicketBtn.setOnAction(e -> ToastManager.showInfo("Navigate to Tickets to open a support inquiry."));

        actionsRow.getChildren().addAll(addCustBtn, recordPayBtn, genInvBtn, newTicketBtn);
        quickActionsCard.getChildren().addAll(qaTitle, actionsRow);

        // Overdue Invoices Quick View Table
        VBox overdueCard = new VBox(12);
        overdueCard.getStyleClass().add("card");
        overdueCard.setFillWidth(true);

        HBox overdueHeader = new HBox(12);
        overdueHeader.setAlignment(Pos.CENTER_LEFT);

        Label overdueTitle = new Label("Critical Overdue Accounts (V_OVERDUE_INVOICES)");
        overdueTitle.getStyleClass().add("section-title");

        Region overdueSpacer = new Region();
        HBox.setHgrow(overdueSpacer, Priority.ALWAYS);

        Button viewAllOverdueBtn = new Button("View in Billing");
        viewAllOverdueBtn.getStyleClass().add("btn-ghost");
        FontIcon arrowIcon = new FontIcon("fas-arrow-right");
        arrowIcon.setIconSize(12);
        viewAllOverdueBtn.setGraphic(arrowIcon);
        viewAllOverdueBtn.setOnAction(e -> ToastManager.showInfo("Navigate to Billing tab to view all overdue accounts."));

        overdueHeader.getChildren().addAll(overdueTitle, overdueSpacer, viewAllOverdueBtn);

        TableViewBuilder.TableContainer overdueTable = TableViewBuilder.buildAsyncTable(
            "SELECT invoice_id, customer_name, plan_name, outstanding, days_overdue, due_date, status FROM v_overdue_invoices FETCH FIRST 10 ROWS ONLY"
        );
        overdueTable.setPrefHeight(260);
        overdueCard.getChildren().addAll(overdueHeader, overdueTable);

        content.getChildren().addAll(alertBanner, spinner, kpiGrid, chartsGrid, quickActionsCard, overdueCard);
        setContent(content);

        // Background Data Fetching Task
        Runnable loadDashboard = () -> {
            spinner.setVisible(true);
            Task<DashboardData> task = new Task<>() {
                @Override
                protected DashboardData call() throws Exception {
                    int activeCustomers = 0;
                    int activeSubscriptions = 0;
                    double monthlyRevenue = 0.0;
                    int overdueInvoices = 0;
                    int openTickets = 0;
                    int pendingInstalls = 0;

                    XYChart.Series<String, Number> revSeries = new XYChart.Series<>();
                    ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

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
                        try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM installation WHERE status IN ('PENDING','SCHEDULED')")) {
                            if (rs.next()) pendingInstalls = rs.getInt(1);
                        }

                        // Chart 1: Revenue by plan
                        try (ResultSet rs = st.executeQuery("SELECT plan_name, revenue_collected FROM v_monthly_revenue WHERE ROWNUM <= 6")) {
                            while (rs.next()) {
                                revSeries.getData().add(new XYChart.Data<>(rs.getString(1), rs.getDouble(2)));
                            }
                        }

                        // Chart 2: Plan types
                        try (ResultSet rs = st.executeQuery("SELECT p.plan_type, COUNT(s.subscription_id) FROM plan p LEFT JOIN subscription s ON p.plan_id = s.plan_id GROUP BY p.plan_type")) {
                            while (rs.next()) {
                                pieData.add(new PieChart.Data(rs.getString(1) + " (" + rs.getInt(2) + ")", rs.getInt(2)));
                            }
                        }
                    }
                    return new DashboardData(activeCustomers, activeSubscriptions, monthlyRevenue,
                                             overdueInvoices, openTickets, pendingInstalls, revSeries, pieData);
                }
            };

            task.setOnSucceeded(e -> {
                content.getChildren().remove(spinner);
                DashboardData d = task.getValue();

                KpiTile t1 = new KpiTile("Active Subscribers", String.valueOf(d.activeCustomers), "Live accounts", "fas-users", "#2563EB");
                KpiTile t2 = new KpiTile("Active Contracts", String.valueOf(d.activeSubscriptions), "Subscriptions", "fas-receipt", "#06B6D4");
                KpiTile t3 = new KpiTile("Monthly Revenue", Formatters.formatCurrency(d.monthlyRevenue), "Current cycle", "fas-wallet", "#16A34A");
                KpiTile t4 = new KpiTile("Overdue Invoices", String.valueOf(d.overdueInvoices), "Past due date", "fas-file-invoice-dollar", "#DC2626");
                KpiTile t5 = new KpiTile("Open Tickets", String.valueOf(d.openTickets), "Requires action", "fas-headset", "#7C3AED");
                KpiTile t6 = new KpiTile("Pending Installs", String.valueOf(d.pendingInstalls), "Field crew", "fas-tools", "#F59E0B");

                for (KpiTile tile : new KpiTile[]{t1, t2, t3, t4, t5, t6}) {
                    HBox.setHgrow(tile, Priority.ALWAYS);
                    tile.setMaxWidth(Double.MAX_VALUE);
                }

                kpiGrid.getChildren().setAll(t1, t2, t3, t4, t5, t6);

                revenueChart.getData().clear();
                revenueChart.getData().add(d.revenueSeries);

                planPieChart.setData(d.pieData);
            });

            task.setOnFailed(e -> {
                content.getChildren().remove(spinner);
                ToastManager.showError("Failed to load dashboard metrics: " + task.getException().getMessage());
            });

            new Thread(task).start();
        };

        refreshBtn.setOnAction(e -> loadDashboard.run());
        loadDashboard.run();
    }

    private static class DashboardData {
        final int activeCustomers;
        final int activeSubscriptions;
        final double monthlyRevenue;
        final int overdueInvoices;
        final int openTickets;
        final int pendingInstalls;
        final XYChart.Series<String, Number> revenueSeries;
        final ObservableList<PieChart.Data> pieData;

        DashboardData(int activeCustomers, int activeSubscriptions, double monthlyRevenue,
                      int overdueInvoices, int openTickets, int pendingInstalls,
                      XYChart.Series<String, Number> revenueSeries, ObservableList<PieChart.Data> pieData) {
            this.activeCustomers = activeCustomers;
            this.activeSubscriptions = activeSubscriptions;
            this.monthlyRevenue = monthlyRevenue;
            this.overdueInvoices = overdueInvoices;
            this.openTickets = openTickets;
            this.pendingInstalls = pendingInstalls;
            this.revenueSeries = revenueSeries;
            this.pieData = pieData;
        }
    }
}
