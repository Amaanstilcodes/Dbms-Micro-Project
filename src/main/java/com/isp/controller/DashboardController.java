package com.isp.controller;

import com.isp.util.SceneManager;
import com.isp.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.io.IOException;

/**
 * Controller for Dashboard.fxml.
 * Shows KPI cards: active customers, active subscriptions,
 * monthly revenue, overdue invoices, open tickets.
 *
 * TODO: inject DAOs and populate KPI labels from DB.
 */
public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label activeCustomersLabel;
    @FXML private Label activeSubscriptionsLabel;
    @FXML private Label monthlyRevenueLabel;
    @FXML private Label overdueInvoicesLabel;
    @FXML private Label openTicketsLabel;

    @FXML
    private void initialize() {
        welcomeLabel.setText("Welcome, " + SessionManager.getUsername()
                + "  [" + SessionManager.getRole() + "]");

        // --- TODO: replace stubs with DAO calls ---
        activeCustomersLabel.setText("—");
        activeSubscriptionsLabel.setText("—");
        monthlyRevenueLabel.setText("₹ —");
        overdueInvoicesLabel.setText("—");
        openTicketsLabel.setText("—");
    }

    // ---- Sidebar navigation ----

    @FXML private void goCustomers()     throws IOException { SceneManager.switchTo("Customers.fxml"); }
    @FXML private void goPlans()         throws IOException { SceneManager.switchTo("Plans.fxml"); }
    @FXML private void goSubscriptions() throws IOException { SceneManager.switchTo("Subscriptions.fxml"); }
    @FXML private void goInvoices()      throws IOException { SceneManager.switchTo("Invoices.fxml"); }
    @FXML private void goPayments()      throws IOException { SceneManager.switchTo("Payments.fxml"); }
    @FXML private void goTickets()       throws IOException { SceneManager.switchTo("SupportTickets.fxml"); }
    @FXML private void goReports()       throws IOException { SceneManager.switchTo("Reports.fxml"); }

    @FXML
    private void handleLogout() throws IOException {
        SessionManager.logout();
        SceneManager.switchTo("Login.fxml");
    }
}
