package com.isp.controller;

import com.isp.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.io.IOException;

/**
 * Controller for Reports.fxml.
 * Provides summary views backed by DB views:
 *  - v_active_subscriptions
 *  - v_overdue_invoices
 *  - v_monthly_revenue
 *  - v_customer_balance
 *  - v_technician_workload
 *  - v_data_usage_summary
 *
 * TODO: inject DAOs / run named SQL views and populate tables.
 */
public class ReportsController {

    @FXML private Label reportStatusLabel;

    @FXML
    private void initialize() {
        reportStatusLabel.setText("Select a report to generate.");
    }

    @FXML private void runActiveSubscriptionsReport() {
        reportStatusLabel.setText("Running v_active_subscriptions — TODO");
    }

    @FXML private void runOverdueInvoicesReport() {
        reportStatusLabel.setText("Running v_overdue_invoices — TODO");
    }

    @FXML private void runMonthlyRevenueReport() {
        reportStatusLabel.setText("Running v_monthly_revenue — TODO");
    }

    @FXML private void runCustomerBalanceReport() {
        reportStatusLabel.setText("Running v_customer_balance — TODO");
    }

    @FXML private void runTechnicianWorkloadReport() {
        reportStatusLabel.setText("Running v_technician_workload — TODO");
    }

    @FXML private void runDataUsageSummaryReport() {
        reportStatusLabel.setText("Running v_data_usage_summary — TODO");
    }

    @FXML private void goBack() throws IOException { SceneManager.switchTo("Dashboard.fxml"); }
}
