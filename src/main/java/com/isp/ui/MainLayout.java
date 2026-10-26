package com.isp.ui;

import com.isp.Main;
import com.isp.Session;
import com.isp.ui.components.ToastManager;
import com.isp.ui.util.ThemeManager;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MainLayout extends BorderPane {
    private final StackPane contentArea;
    private final VBox sidebar;
    private boolean sidebarCollapsed = false;
    private final List<Button> navButtons = new ArrayList<>();
    private final Label pageTitleLbl;
    private final Label breadcrumbLbl;

    public MainLayout() {
        // --- 1. SIDEBAR ---
        sidebar = new VBox(8);
        sidebar.setPrefWidth(240);
        sidebar.setMinWidth(64);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(16, 10, 16, 10));

        // Brand Row
        HBox brandRow = new HBox(10);
        brandRow.setAlignment(Pos.CENTER_LEFT);
        brandRow.setPadding(new Insets(6, 6, 16, 6));

        FontIcon brandIcon = new FontIcon("fas-bolt");
        brandIcon.setIconSize(20);
        brandIcon.setStyle("-fx-icon-color: -color-accent;");

        VBox brandTextBox = new VBox(2);
        Label brand = new Label("ISP Orbit");
        brand.getStyleClass().add("sidebar-brand-title");
        Label brandTag = new Label("Oracle 23ai Telecom");
        brandTag.getStyleClass().add("sidebar-brand-tag");
        brandTextBox.getChildren().addAll(brand, brandTag);

        brandRow.getChildren().addAll(brandIcon, brandTextBox);

        // Sidebar Navigation Container
        VBox navItems = new VBox(4);
        navItems.setPadding(new Insets(8, 0, 0, 0));

        String role = Session.getRole();
        if ("ADMIN".equals(role)) {
            addNav(navItems, "fas-chart-pie", "Dashboard", () -> navigate("Dashboard", "System Overview", new DashboardScreen()));
            addNav(navItems, "fas-users", "Customers", () -> navigate("Customers", "Subscriber Directory", new CustomersScreen()));
            addNav(navItems, "fas-layer-group", "Plans Catalog", () -> navigate("Plans", "Broadband & Fiber Tiers", new PlansScreen()));
            addNav(navItems, "fas-receipt", "Subscriptions", () -> navigate("Subscriptions", "Contracts & Installations", new SubscriptionsScreen()));
            addNav(navItems, "fas-file-invoice-dollar", "Billing & Payments", () -> navigate("Billing", "Invoices & Revenue Engine", new BillingScreen()));
            addNav(navItems, "fas-headset", "Support Tickets", () -> navigate("Support Tickets", "Customer Inquiries & SLA", new TicketsScreen()));
            addNav(navItems, "fas-chart-line", "Analytics & Reports", () -> navigate("Analytics", "Views & Fiscal Metrics", new ReportsScreen()));
        } else if ("BILLING_CLERK".equals(role)) {
            addNav(navItems, "fas-file-invoice-dollar", "Billing", () -> navigate("Billing", "Invoices & Payments", new BillingScreen()));
            addNav(navItems, "fas-users", "Customers", () -> navigate("Customers", "Subscriber Directory", new CustomersScreen()));
            addNav(navItems, "fas-layer-group", "Plans", () -> navigate("Plans", "Service Offerings", new PlansScreen()));
            addNav(navItems, "fas-chart-line", "Reports", () -> navigate("Reports", "Financial Analytics", new ReportsScreen()));
        } else if ("TECHNICIAN".equals(role)) {
            addNav(navItems, "fas-tools", "Installations / Subs", () -> navigate("Subscriptions", "Field Assignments", new SubscriptionsScreen()));
            addNav(navItems, "fas-headset", "Assigned Tickets", () -> navigate("Tickets", "Work Orders", new TicketsScreen()));
        } else if ("SUPPORT_AGENT".equals(role)) {
            addNav(navItems, "fas-users", "Customers", () -> navigate("Customers", "Subscriber Accounts", new CustomersScreen()));
            addNav(navItems, "fas-headset", "Support Tickets", () -> navigate("Tickets", "Helpdesk Desk", new TicketsScreen()));
        } else {
            addNav(navItems, "fas-chart-pie", "Dashboard", () -> navigate("Dashboard", "System Overview", new DashboardScreen()));
        }

        Region vSpacer = new Region();
        VBox.setVgrow(vSpacer, Priority.ALWAYS);

        // Sidebar Collapse Toggle Button
        Button toggleSidebarBtn = new Button();
        toggleSidebarBtn.getStyleClass().add("sidebar-item");
        toggleSidebarBtn.setMaxWidth(Double.MAX_VALUE);
        FontIcon toggleIcon = new FontIcon("fas-bars");
        toggleIcon.setIconSize(16);
        toggleIcon.setStyle("-fx-icon-color: #94A3B8;");
        Label toggleLabel = new Label(" Collapse Menu");
        toggleLabel.setStyle("-fx-text-fill: #94A3B8;");
        toggleSidebarBtn.setGraphic(new HBox(10, toggleIcon, toggleLabel));
        toggleSidebarBtn.setOnAction(e -> toggleSidebar(brandTextBox, toggleLabel));

        sidebar.getChildren().addAll(brandRow, navItems, vSpacer, toggleSidebarBtn);
        setLeft(sidebar);

        // --- 2. TOP HEADER BAR ---
        HBox header = new HBox(16);
        header.getStyleClass().add("header-bar");
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        breadcrumbLbl = new Label("App / Dashboard");
        breadcrumbLbl.getStyleClass().add("page-subtitle");
        breadcrumbLbl.setStyle("-fx-font-size: 11px;");

        pageTitleLbl = new Label("Dashboard");
        pageTitleLbl.getStyleClass().add("page-title");
        titleBox.getChildren().addAll(breadcrumbLbl, pageTitleLbl);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        // Search Field
        TextField globalSearch = new TextField();
        globalSearch.setPromptText("Quick search (Ctrl+F)...");
        globalSearch.setPrefWidth(220);

        // Dark / Light Theme Toggle Button
        Button themeBtn = new Button();
        themeBtn.getStyleClass().add("btn-icon");
        FontIcon themeIcon = new FontIcon("fas-moon");
        themeIcon.setIconSize(16);
        themeIcon.setStyle("-fx-icon-color: -color-text;");
        themeBtn.setGraphic(themeIcon);
        themeBtn.setTooltip(new Tooltip("Toggle Dark / Light Theme"));
        themeBtn.setOnAction(e -> {
            ThemeManager.toggleTheme(getScene());
            themeIcon.setIconLiteral(ThemeManager.isDark() ? "fas-sun" : "fas-moon");
        });

        // Overdue Notification Bell Chip
        Button notifyBtn = new Button();
        notifyBtn.getStyleClass().add("btn-icon");
        FontIcon bellIcon = new FontIcon("fas-bell");
        bellIcon.setIconSize(16);
        bellIcon.setStyle("-fx-icon-color: -color-text;");
        notifyBtn.setGraphic(bellIcon);
        notifyBtn.setTooltip(new Tooltip("Overdue Notifications"));
        notifyBtn.setOnAction(e -> ToastManager.showInfo("System alert checks are running on schedule."));

        // User Avatar Chip
        HBox userChip = new HBox(8);
        userChip.setAlignment(Pos.CENTER_LEFT);
        userChip.setStyle("-fx-background-color: -color-surface-hover; -fx-padding: 4 10; -fx-background-radius: 20; -fx-border-color: -color-border; -fx-border-radius: 20;");

        FontIcon userIcon = new FontIcon("fas-user-circle");
        userIcon.setIconSize(18);
        userIcon.setStyle("-fx-icon-color: -color-primary;");

        Label usernameLbl = new Label(Session.getUsername());
        usernameLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text;");

        Label roleBadge = new Label(Session.getRole());
        roleBadge.getStyleClass().addAll("badge", "badge-info");

        Button logoutBtn = new Button();
        logoutBtn.getStyleClass().add("btn-icon");
        FontIcon logoutIcon = new FontIcon("fas-sign-out-alt");
        logoutIcon.setIconSize(14);
        logoutIcon.setStyle("-fx-icon-color: -color-danger;");
        logoutBtn.setGraphic(logoutIcon);
        logoutBtn.setTooltip(new Tooltip("Sign Out"));
        logoutBtn.setOnAction(e -> {
            Session.logout();
            Main.showLoginScreen();
        });

        userChip.getChildren().addAll(userIcon, usernameLbl, roleBadge, logoutBtn);

        header.getChildren().addAll(titleBox, headerSpacer, globalSearch, themeBtn, notifyBtn, userChip);
        setTop(header);

        // --- 3. MAIN CONTENT CONTAINER ---
        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: -color-bg;");
        setCenter(contentArea);

        // Bind toast overlay
        ToastManager.bindContainer(contentArea);

        // --- 4. BOTTOM STATUS BAR ---
        HBox statusBar = new HBox(20);
        statusBar.getStyleClass().add("status-bar");
        statusBar.setAlignment(Pos.CENTER_LEFT);

        HBox dbStatus = new HBox(6);
        dbStatus.setAlignment(Pos.CENTER_LEFT);
        FontIcon dotIcon = new FontIcon("fas-circle");
        dotIcon.setIconSize(8);
        dotIcon.setStyle("-fx-icon-color: #16A34A;");
        Label dbLbl = new Label("Connected to FREEPDB1 (Oracle 23ai)");
        dbStatus.getChildren().addAll(dotIcon, dbLbl);

        Region statusSpacer = new Region();
        HBox.setHgrow(statusSpacer, Priority.ALWAYS);

        Label sessionLbl = new Label("Active Session: " + Session.getUsername() + " [" + Session.getRole() + "]");
        Label timeLbl = new Label(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));

        statusBar.getChildren().addAll(dbStatus, statusSpacer, sessionLbl, timeLbl);
        setBottom(statusBar);

        // Default screen navigation
        if ("ADMIN".equals(role)) {
            navigate("Dashboard", "System Overview", new DashboardScreen());
        } else if ("BILLING_CLERK".equals(role)) {
            navigate("Billing", "Invoices & Payments", new BillingScreen());
        } else if ("TECHNICIAN".equals(role)) {
            navigate("Subscriptions", "Field Assignments", new SubscriptionsScreen());
        } else if ("SUPPORT_AGENT".equals(role)) {
            navigate("Customers", "Subscriber Directory", new CustomersScreen());
        }
    }

    private void addNav(VBox container, String iconCode, String text, Runnable onClick) {
        Button btn = new Button();
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.getStyleClass().add("sidebar-item");

        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(15);
        icon.setStyle("-fx-icon-color: #94A3B8;");

        Label label = new Label("  " + text);
        label.setStyle("-fx-text-fill: inherit; -fx-font-weight: 600;");

        HBox graphic = new HBox(8, icon, label);
        graphic.setAlignment(Pos.CENTER_LEFT);
        btn.setGraphic(graphic);

        btn.setOnAction(e -> {
            for (Button b : navButtons) {
                b.getStyleClass().remove("sidebar-item-active");
            }
            btn.getStyleClass().add("sidebar-item-active");
            onClick.run();
        });

        navButtons.add(btn);
        container.getChildren().add(btn);
    }

    private void toggleSidebar(Node brandText, Label toggleLabel) {
        sidebarCollapsed = !sidebarCollapsed;
        double targetWidth = sidebarCollapsed ? 68 : 240;

        Timeline timeline = new Timeline(
            new KeyFrame(Duration.millis(180),
                new KeyValue(sidebar.prefWidthProperty(), targetWidth),
                new KeyValue(sidebar.maxWidthProperty(), targetWidth)
            )
        );
        timeline.setOnFinished(ev -> {
            brandText.setVisible(!sidebarCollapsed);
            brandText.setManaged(!sidebarCollapsed);
            toggleLabel.setVisible(!sidebarCollapsed);
            toggleLabel.setManaged(!sidebarCollapsed);
            for (Button b : navButtons) {
                HBox g = (HBox) b.getGraphic();
                Label lbl = (Label) g.getChildren().get(1);
                lbl.setVisible(!sidebarCollapsed);
                lbl.setManaged(!sidebarCollapsed);
            }
        });
        timeline.play();
    }

    public void navigate(String title, String subtitle, Node screen) {
        pageTitleLbl.setText(title);
        breadcrumbLbl.setText("App / " + title);

        screen.setOpacity(0.0);
        contentArea.getChildren().setAll(screen);
        ToastManager.bindContainer(contentArea);

        FadeTransition ft = new FadeTransition(Duration.millis(200), screen);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    public void setScreen(Node screen) {
        navigate("ISP Manager", "Telecom Dashboard", screen);
    }
}
