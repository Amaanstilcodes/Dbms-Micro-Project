package com.isp.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class ReportsScreen extends VBox {

    public ReportsScreen() {
        setSpacing(16);
        setPadding(new Insets(20));

        Label title = new Label("Analytics & Reports");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));

        TabPane tabPane = new TabPane();
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        Tab revTab = new Tab("Monthly Revenue (V_MONTHLY_REVENUE)",
            TableViewBuilder.buildAsyncTable("SELECT revenue_month, plan_type, plan_name, payment_count, '₹' || revenue_collected AS revenue FROM v_monthly_revenue"));
        revTab.setClosable(false);

        Tab overdueTab = new Tab("Overdue Invoices (V_OVERDUE_INVOICES)",
            TableViewBuilder.buildAsyncTable("SELECT invoice_id, customer_name, email, phone, plan_name, total_amount, amount_paid, outstanding, days_overdue, status FROM v_overdue_invoices"));
        overdueTab.setClosable(false);

        Tab balTab = new Tab("Customer Balance (V_CUSTOMER_BALANCE)",
            TableViewBuilder.buildAsyncTable("SELECT customer_id, customer_name, email, total_invoices, total_billed, total_paid, outstanding_balance FROM v_customer_balance ORDER BY outstanding_balance DESC"));
        balTab.setClosable(false);

        Tab workTab = new Tab("Technician Workload (V_TECHNICIAN_WORKLOAD)",
            TableViewBuilder.buildAsyncTable("SELECT technician_id, technician_name, specialization, area_name, pending_installations, open_tickets, total_workload, workload_status FROM v_technician_workload"));
        workTab.setClosable(false);

        Tab usageTab = new Tab("Data Usage Summary (V_DATA_USAGE_SUMMARY)",
            TableViewBuilder.buildAsyncTable("SELECT subscription_id, customer_name, plan_name, NVL(TO_CHAR(data_cap_gb), 'UNLIMITED') AS cap_gb, usage_month, data_used_gb, cap_status FROM v_data_usage_summary ORDER BY data_used_gb DESC"));
        usageTab.setClosable(false);

        tabPane.getTabs().addAll(revTab, overdueTab, balTab, workTab, usageTab);

        getChildren().addAll(title, tabPane);
    }
}
