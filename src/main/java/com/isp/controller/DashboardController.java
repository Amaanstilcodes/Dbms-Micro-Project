package com.isp.controller;

import com.isp.service.MockDataService;
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
 * Phase A: KPIs populated from MockDataService.
 * Phase B: replace each MockDataService call with the real DAO:
 *   activeCustomersLabel.setText(String.valueOf(DAOFactory.getCustomerDAO().countActive()));
 *   activeSubscriptionsLabel.setText(String.valueOf(DAOFactory.getSubscriptionDAO().countActive()));
 *   monthlyRevenueLabel.setText("₹ " + DAOFactory.getInvoiceDAO().getMonthlyRevenue());
 *   overdueInvoicesLabel.setText(String.valueOf(DAOFactory.getInvoiceDAO().countOverdue()));
 *   openTicketsLabel.setText(String.valueOf(DAOFactory.getSupportTicketDAO().countOpen()));
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

        // Phase A ── mock KPI values (realistic numbers from seed data)
        activeCustomersLabel.setText(String.valueOf(MockDataService.countActiveCustomers()));
        activeSubscriptionsLabel.setText(String.valueOf(MockDataService.getActiveSubscriptionsCount()));
        monthlyRevenueLabel.setText("₹ " + MockDataService.getMonthlyRevenue());
        overdueInvoicesLabel.setText(String.valueOf(MockDataService.getOverdueInvoicesCount()));
        openTicketsLabel.setText(String.valueOf(MockDataService.getOpenTicketsCount()));
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
