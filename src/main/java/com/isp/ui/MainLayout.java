package com.isp.ui;

import com.isp.Main;
import com.isp.Session;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class MainLayout extends BorderPane {
    private final StackPane contentArea;

    public MainLayout() {
        VBox sidebar = new VBox(10);
        sidebar.setPrefWidth(220);
        sidebar.setStyle("-fx-background-color: #1e293b;");
        sidebar.setPadding(new Insets(20, 12, 20, 12));

        Label brand = new Label("ISP Manager");
        brand.setFont(Font.font("System", FontWeight.BOLD, 18));
        brand.setStyle("-fx-text-fill: white;");

        Label roleLbl = new Label("Logged in as: " + Session.getUsername() + " (" + Session.getRole() + ")");
        roleLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

        VBox navItems = new VBox(6);
        navItems.setPadding(new Insets(16, 0, 0, 0));

        String role = Session.getRole();

        // Role-based sidebar menu items
        // ADMIN sees everything
        // BILLING_CLERK: Billing, Customers, Plans
        // TECHNICIAN: Subscriptions (Installations), Tickets
        // SUPPORT_AGENT: Customers, Tickets
        if ("ADMIN".equals(role)) {
            addNav(navItems, "Dashboard", () -> setScreen(new DashboardScreen()));
            addNav(navItems, "Customers", () -> setScreen(new CustomersScreen()));
            addNav(navItems, "Plans", () -> setScreen(new PlansScreen()));
            addNav(navItems, "Subscriptions", () -> setScreen(new SubscriptionsScreen()));
            addNav(navItems, "Billing", () -> setScreen(new BillingScreen()));
            addNav(navItems, "Support Tickets", () -> setScreen(new TicketsScreen()));
            addNav(navItems, "Analytics & Reports", () -> setScreen(new ReportsScreen()));
        } else if ("BILLING_CLERK".equals(role)) {
            addNav(navItems, "Billing", () -> setScreen(new BillingScreen()));
            addNav(navItems, "Customers", () -> setScreen(new CustomersScreen()));
            addNav(navItems, "Plans", () -> setScreen(new PlansScreen()));
            addNav(navItems, "Reports", () -> setScreen(new ReportsScreen()));
        } else if ("TECHNICIAN".equals(role)) {
            addNav(navItems, "Installations / Subs", () -> setScreen(new SubscriptionsScreen()));
            addNav(navItems, "Assigned Tickets", () -> setScreen(new TicketsScreen()));
        } else if ("SUPPORT_AGENT".equals(role)) {
            addNav(navItems, "Customers", () -> setScreen(new CustomersScreen()));
            addNav(navItems, "Support Tickets", () -> setScreen(new TicketsScreen()));
        } else {
            addNav(navItems, "Dashboard", () -> setScreen(new DashboardScreen()));
        }

        Region vSpacer = new Region();
        VBox.setVgrow(vSpacer, Priority.ALWAYS);

        Button logoutBtn = new Button("Logout");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: #cbd5e1; -fx-cursor: hand;");
        logoutBtn.setOnAction(e -> {
            Session.logout();
            Main.showLoginScreen();
        });

        sidebar.getChildren().addAll(brand, roleLbl, navItems, vSpacer, logoutBtn);
        setLeft(sidebar);

        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: #f8fafc;");
        setCenter(contentArea);

        // Show default screen
        if ("ADMIN".equals(role)) {
            setScreen(new DashboardScreen());
        } else if ("BILLING_CLERK".equals(role)) {
            setScreen(new BillingScreen());
        } else if ("TECHNICIAN".equals(role)) {
            setScreen(new SubscriptionsScreen());
        } else if ("SUPPORT_AGENT".equals(role)) {
            setScreen(new CustomersScreen());
        }
    }

    private void addNav(VBox container, String text, Runnable onClick) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #cbd5e1; -fx-font-size: 13px; -fx-padding: 8 12; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 12; -fx-cursor: hand; -fx-background-radius: 6;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #cbd5e1; -fx-font-size: 13px; -fx-padding: 8 12; -fx-cursor: hand;"));
        btn.setOnAction(e -> onClick.run());
        container.getChildren().add(btn);
    }

    public void setScreen(Node node) {
        contentArea.getChildren().setAll(node);
    }
}
