package com.isp.ui;

import com.isp.Db;
import com.isp.ui.components.ToastManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class ReportsScreen extends VBox {

    public ReportsScreen() {
        setSpacing(16);
        setPadding(new Insets(24));

        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("Oracle Analytical Views & Reports");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Real-time aggregated fiscal, operational, and technician workload telemetry");
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button exportBtn = new Button("Export Current View (CSV)");
        exportBtn.getStyleClass().add("btn-secondary");
        FontIcon exportIcon = new FontIcon("fas-file-csv");
        exportIcon.setIconSize(13);
        exportBtn.setGraphic(exportIcon);

        header.getChildren().addAll(titleBox, spacer, exportBtn);

        TabPane tabPane = new TabPane();
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // Tab 1: Monthly Revenue
        Tab revTab = new Tab("Monthly Revenue (V_MONTHLY_REVENUE)");
        revTab.setClosable(false);
        VBox revBox = new VBox(12);
        revBox.setPadding(new Insets(16, 0, 0, 0));

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> revChart = new BarChart<>(xAxis, yAxis);
        revChart.setTitle("Revenue Collection Progression");
        revChart.setPrefHeight(220);
        revChart.setLegendVisible(false);

        loadRevenueChart(revChart);

        TableViewBuilder.TableContainer revTable = TableViewBuilder.buildAsyncTable("SELECT revenue_month, plan_type, plan_name, payment_count, revenue_collected FROM v_monthly_revenue");
        VBox revTableCard = new VBox(revTable);
        revTableCard.getStyleClass().add("card");
        revTableCard.setFillWidth(true);
        VBox.setVgrow(revTableCard, Priority.ALWAYS);

        revBox.getChildren().addAll(revChart, revTableCard);
        revTab.setContent(revBox);

        // Tab 2: Overdue Invoices
        Tab overdueTab = new Tab("Overdue Invoices (V_OVERDUE_INVOICES)");
        overdueTab.setClosable(false);
        VBox overdueBox = new VBox(12);
        overdueBox.setPadding(new Insets(16, 0, 0, 0));
        TableViewBuilder.TableContainer overdueTable = TableViewBuilder.buildAsyncTable("SELECT invoice_id, customer_name, email, phone, plan_name, total_amount, amount_paid, outstanding, due_date, days_overdue, status FROM v_overdue_invoices");
        VBox overdueCard = new VBox(overdueTable);
        overdueCard.getStyleClass().add("card");
        overdueCard.setFillWidth(true);
        VBox.setVgrow(overdueCard, Priority.ALWAYS);
        overdueBox.getChildren().add(overdueCard);
        overdueTab.setContent(overdueBox);

        // Tab 3: Customer Balance
        Tab balTab = new Tab("Customer Balance (V_CUSTOMER_BALANCE)");
        balTab.setClosable(false);
        VBox balBox = new VBox(12);
        balBox.setPadding(new Insets(16, 0, 0, 0));
        TableViewBuilder.TableContainer balTable = TableViewBuilder.buildAsyncTable("SELECT customer_id, customer_name, email, total_invoices, total_billed, total_paid, outstanding_balance FROM v_customer_balance ORDER BY outstanding_balance DESC");
        VBox balCard = new VBox(balTable);
        balCard.getStyleClass().add("card");
        balCard.setFillWidth(true);
        VBox.setVgrow(balCard, Priority.ALWAYS);
        balBox.getChildren().add(balCard);
        balTab.setContent(balBox);

        // Tab 4: Technician Workload
        Tab workTab = new Tab("Technician Workload (V_TECHNICIAN_WORKLOAD)");
        workTab.setClosable(false);
        VBox workBox = new VBox(12);
        workBox.setPadding(new Insets(16, 0, 0, 0));
        TableViewBuilder.TableContainer workTable = TableViewBuilder.buildAsyncTable("SELECT technician_id, technician_name, specialization, area_name, pending_installations, open_tickets, total_workload, workload_status FROM v_technician_workload");
        VBox workCard = new VBox(workTable);
        workCard.getStyleClass().add("card");
        workCard.setFillWidth(true);
        VBox.setVgrow(workCard, Priority.ALWAYS);
        workBox.getChildren().add(workCard);
        workTab.setContent(workBox);

        // Tab 5: Data Usage Summary
        Tab usageTab = new Tab("Data Usage Summary (V_DATA_USAGE_SUMMARY)");
        usageTab.setClosable(false);
        VBox usageBox = new VBox(12);
        usageBox.setPadding(new Insets(16, 0, 0, 0));
        TableViewBuilder.TableContainer usageTable = TableViewBuilder.buildAsyncTable("SELECT subscription_id, customer_name, plan_name, NVL(TO_CHAR(data_cap_gb), 'UNLIMITED') AS cap_gb, usage_month, data_used_gb, cap_status FROM v_data_usage_summary ORDER BY data_used_gb DESC");
        VBox usageCard = new VBox(usageTable);
        usageCard.getStyleClass().add("card");
        usageCard.setFillWidth(true);
        VBox.setVgrow(usageCard, Priority.ALWAYS);
        usageBox.getChildren().add(usageCard);
        usageTab.setContent(usageBox);

        // Tab 6: Active Subscriptions
        Tab activeSubTab = new Tab("Active Subscriptions (V_ACTIVE_SUBSCRIPTIONS)");
        activeSubTab.setClosable(false);
        VBox subBox = new VBox(12);
        subBox.setPadding(new Insets(16, 0, 0, 0));
        TableViewBuilder.TableContainer activeSubTable = TableViewBuilder.buildAsyncTable("SELECT subscription_id, customer_name, email, phone, plan_name, plan_type, speed_mbps, monthly_price, area_name, city, start_date, subscription_status FROM v_active_subscriptions");
        VBox subCard = new VBox(activeSubTable);
        subCard.getStyleClass().add("card");
        subCard.setFillWidth(true);
        VBox.setVgrow(subCard, Priority.ALWAYS);
        subBox.getChildren().add(subCard);
        activeSubTab.setContent(subBox);

        tabPane.getTabs().addAll(revTab, overdueTab, balTab, workTab, usageTab, activeSubTab);

        exportBtn.setOnAction(e -> {
            Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
            if (selectedTab != null) {
                TableViewBuilder.TableContainer tc = null;
                if (selectedTab == revTab) tc = revTable;
                else if (selectedTab == overdueTab) tc = overdueTable;
                else if (selectedTab == balTab) tc = balTable;
                else if (selectedTab == workTab) tc = workTable;
                else if (selectedTab == usageTab) tc = usageTable;
                else if (selectedTab == activeSubTab) tc = activeSubTable;

                if (tc != null && tc.getTableView() != null) {
                    String cleanName = selectedTab.getText().replaceAll("[^a-zA-Z0-9_]", "_");
                    TableViewBuilder.exportToCSV(tc.getTableView(), cleanName);
                } else {
                    ToastManager.showInfo("No table data ready to export.");
                }
            }
        });

        getChildren().addAll(header, tabPane);
    }

    private void loadRevenueChart(BarChart<String, Number> chart) {
        Task<XYChart.Series<String, Number>> task = new Task<>() {
            @Override
            protected XYChart.Series<String, Number> call() throws Exception {
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                String q = "SELECT plan_name, revenue_collected FROM v_monthly_revenue WHERE ROWNUM <= 8";
                try (Connection conn = Db.getConnection();
                     Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(q)) {
                    while (rs.next()) {
                        series.getData().add(new XYChart.Data<>(rs.getString(1), rs.getDouble(2)));
                    }
                }
                return series;
            }
        };

        task.setOnSucceeded(e -> chart.getData().add(task.getValue()));
        new Thread(task).start();
    }
}
