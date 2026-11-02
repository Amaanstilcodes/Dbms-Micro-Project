package com.isp.ui;

import com.isp.Session;
import com.isp.dao.DashboardDao;
import com.isp.dao.DashboardData;
import com.isp.ui.components.EmptyState;
import com.isp.ui.components.KpiTile;
import com.isp.ui.components.ToastManager;
import com.isp.ui.util.Formatters;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardScreen extends ScrollPane {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Dashboard-Background-Worker");
        t.setDaemon(true);
        return t;
    });

    private final MainLayout parentLayout;
    private final Timeline autoRefreshTimeline;

    // UI Widgets
    private final HBox alertBanner;
    private final FontIcon alertIcon;
    private final Label alertText;
    private final Label lastUpdatedLbl;
    private final Button refreshBtn;

    // KPI Tiles
    private final GridPane kpiGrid;
    private final KpiTile tileSubscribers;
    private final KpiTile tileContracts;
    private final KpiTile tileRevenue;
    private final KpiTile tileOverdue;
    private final KpiTile tileOpenTickets;
    private final KpiTile tilePendingInstalls;

    // Charts & Middle Row
    private final GridPane chartsGrid;
    private final LineChart<String, Number> revenueTrendChart;
    private final PieChart planPieChart;
    private final StackPane chart1Stack;
    private final StackPane chart2Stack;
    private final EmptyState chart1Empty;
    private final EmptyState chart2Empty;

    // Quick Actions
    private final VBox quickActionsCard;
    private final Button addCustBtn;
    private final Button recordPayBtn;
    private final Button genInvBtn;
    private final Button newTicketBtn;

    // Recent Activity & Overdue Table
    private final VBox recentActivityCard;
    private final VBox recentActivityList;
    private final VBox overdueCard;

    // Error State Box
    private final VBox errorBox;
    private final Label errorMsgLbl;
    private final Button retryBtn;

    public DashboardScreen() {
        this(null);
    }

    public DashboardScreen(MainLayout parentLayout) {
        this.parentLayout = parentLayout;

        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setFillWidth(true);
        content.setMaxWidth(Double.MAX_VALUE);

        // --- 1. Top Alert Banner & Refresh Bar ---
        alertBanner = new HBox(12);
        alertBanner.setAlignment(Pos.CENTER_LEFT);
        alertBanner.setStyle(
            "-fx-background-color: -color-success-bg; -fx-padding: 10 16; -fx-background-radius: 10; -fx-border-color: -color-success; -fx-border-radius: 10;"
        );

        alertIcon = new FontIcon("fas-check-circle");
        alertIcon.setIconSize(16);
        alertIcon.setStyle("-fx-icon-color: -color-success;");

        alertText = new Label("All subscriber accounts are current and up to date.");
        alertText.setStyle("-fx-font-weight: 600; -fx-text-fill: -color-text;");

        Region alertSpacer = new Region();
        HBox.setHgrow(alertSpacer, Priority.ALWAYS);

        lastUpdatedLbl = new Label("Updated: --:--:--");
        lastUpdatedLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-muted;");

        refreshBtn = new Button(" Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        FontIcon syncIcon = new FontIcon("fas-sync-alt");
        syncIcon.setIconSize(13);
        refreshBtn.setGraphic(syncIcon);

        alertBanner.getChildren().addAll(alertIcon, alertText, alertSpacer, lastUpdatedLbl, refreshBtn);

        // --- Error Banner (Hidden by default) ---
        errorBox = new VBox(10);
        errorBox.setAlignment(Pos.CENTER);
        errorBox.setStyle("-fx-background-color: -color-danger-bg; -fx-padding: 14; -fx-background-radius: 10; -fx-border-color: -color-danger; -fx-border-radius: 10;");
        errorBox.setVisible(false);
        errorBox.setManaged(false);

        errorMsgLbl = new Label();
        errorMsgLbl.setStyle("-fx-text-fill: -color-danger; -fx-font-weight: 600;");

        retryBtn = new Button(" Retry Now");
        retryBtn.getStyleClass().add("btn-danger");
        FontIcon retryIcon = new FontIcon("fas-redo");
        retryIcon.setIconSize(12);
        retryBtn.setGraphic(retryIcon);
        errorBox.getChildren().addAll(errorMsgLbl, retryBtn);

        // --- 2. KPI Grid (Columns dynamically sized per role) ---
        kpiGrid = new GridPane();
        kpiGrid.setHgap(12);
        kpiGrid.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(kpiGrid, Priority.ALWAYS);

        tileSubscribers = new KpiTile("Active Subscribers", "-", "Live accounts", null, "fas-users", "#2563EB");
        tileContracts = new KpiTile("Active Contracts", "-", "Subscriptions", null, "fas-receipt", "#06B6D4");
        tileRevenue = new KpiTile("Monthly Revenue", "-", "Current cycle", null, "fas-wallet", "#16A34A");
        tileOverdue = new KpiTile("Overdue Invoices", "-", "Past due date", null, "fas-file-invoice-dollar", "#DC2626");
        tileOpenTickets = new KpiTile("Open Tickets", "-", "Requires action", null, "fas-headset", "#7C3AED");
        tilePendingInstalls = new KpiTile("Pending Installs", "-", "Field crew", null, "fas-tools", "#F59E0B");

        // --- 3. Charts Grid (LineChart Revenue Trend + PieChart Plan Breakdown) ---
        chartsGrid = new GridPane();
        chartsGrid.setHgap(16);
        chartsGrid.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(chartsGrid, Priority.ALWAYS);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(58);
        c1.setHgrow(Priority.ALWAYS);

        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(42);
        c2.setHgrow(Priority.ALWAYS);
        chartsGrid.getColumnConstraints().addAll(c1, c2);

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Billing Month");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Revenue (₹)");

        revenueTrendChart = new LineChart<>(xAxis, yAxis);
        revenueTrendChart.setTitle("Revenue Collection Trend (Last 6 Months)");
        revenueTrendChart.setLegendVisible(false);
        revenueTrendChart.setMinHeight(290);
        revenueTrendChart.setPrefHeight(310);
        revenueTrendChart.setAnimated(false);
        revenueTrendChart.getStyleClass().add("card");

        chart1Empty = new EmptyState("No Revenue Records", "Payments recorded in Oracle will chart here.", "fas-chart-line");
        chart1Empty.setVisible(false);
        chart1Stack = new StackPane(revenueTrendChart, chart1Empty);
        GridPane.setHgrow(chart1Stack, Priority.ALWAYS);

        planPieChart = new PieChart();
        planPieChart.setTitle("Active Subscribers by Plan Type");
        planPieChart.setMinHeight(290);
        planPieChart.setPrefHeight(310);
        planPieChart.setLegendVisible(true);
        planPieChart.setAnimated(false);
        planPieChart.getStyleClass().add("card");

        chart2Empty = new EmptyState("No Plan Distribution", "No active subscriber plans found.", "fas-chart-pie");
        chart2Empty.setVisible(false);
        chart2Stack = new StackPane(planPieChart, chart2Empty);
        GridPane.setHgrow(chart2Stack, Priority.ALWAYS);

        chartsGrid.add(chart1Stack, 0, 0);
        chartsGrid.add(chart2Stack, 1, 0);

        // --- 4. Operations & Recent Activity Row ---
        GridPane midRow = new GridPane();
        midRow.setHgap(16);
        midRow.setMaxWidth(Double.MAX_VALUE);

        ColumnConstraints midCol1 = new ColumnConstraints();
        midCol1.setPercentWidth(50);
        midCol1.setHgrow(Priority.ALWAYS);

        ColumnConstraints midCol2 = new ColumnConstraints();
        midCol2.setPercentWidth(50);
        midCol2.setHgrow(Priority.ALWAYS);
        midRow.getColumnConstraints().addAll(midCol1, midCol2);

        // Quick Operations Card
        quickActionsCard = new VBox(14);
        quickActionsCard.getStyleClass().add("card");
        quickActionsCard.setFillWidth(true);

        Label qaTitle = new Label("Quick Operations");
        qaTitle.getStyleClass().add("section-title");

        HBox actionsRow = new HBox(10);
        addCustBtn = new Button("New Customer");
        addCustBtn.getStyleClass().add("btn-primary");
        FontIcon userPlusIcon = new FontIcon("fas-user-plus");
        userPlusIcon.setIconSize(13);
        addCustBtn.setGraphic(userPlusIcon);

        recordPayBtn = new Button("Record Payment");
        recordPayBtn.getStyleClass().add("btn-secondary");
        FontIcon payIcon = new FontIcon("fas-credit-card");
        payIcon.setIconSize(13);
        recordPayBtn.setGraphic(payIcon);

        genInvBtn = new Button("Run Invoicing");
        genInvBtn.getStyleClass().add("btn-secondary");
        FontIcon boltIcon = new FontIcon("fas-bolt");
        boltIcon.setIconSize(13);
        genInvBtn.setGraphic(boltIcon);

        newTicketBtn = new Button("Open Ticket");
        newTicketBtn.getStyleClass().add("btn-secondary");
        FontIcon ticketIcon = new FontIcon("fas-ticket-alt");
        ticketIcon.setIconSize(13);
        newTicketBtn.setGraphic(ticketIcon);

        actionsRow.getChildren().addAll(addCustBtn, recordPayBtn, genInvBtn, newTicketBtn);

        Label qaSub = new Label("Perform direct operational actions permitted by your role.");
        qaSub.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-muted;");

        quickActionsCard.getChildren().addAll(qaTitle, actionsRow, qaSub);

        // Wire Action Routes
        addCustBtn.setOnAction(e -> {
            if (parentLayout != null) parentLayout.navigate("Customers", "Subscriber Directory", new CustomersScreen());
            else ToastManager.showInfo("Navigating to Customers...");
        });
        recordPayBtn.setOnAction(e -> {
            if (parentLayout != null) parentLayout.navigate("Billing", "Invoices & Revenue Engine", new BillingScreen());
            else ToastManager.showInfo("Navigating to Billing...");
        });
        genInvBtn.setOnAction(e -> {
            if (parentLayout != null) parentLayout.navigate("Billing", "Invoices & Revenue Engine", new BillingScreen());
            else ToastManager.showInfo("Navigating to Billing...");
        });
        newTicketBtn.setOnAction(e -> {
            if (parentLayout != null) parentLayout.navigate("Support Tickets", "Customer Inquiries & SLA", new TicketsScreen());
            else ToastManager.showInfo("Navigating to Support Tickets...");
        });

        // Recent Activity Card
        recentActivityCard = new VBox(12);
        recentActivityCard.getStyleClass().add("card");
        recentActivityCard.setFillWidth(true);

        Label activityTitle = new Label("Recent Activity (Oracle Live Feed)");
        activityTitle.getStyleClass().add("section-title");

        recentActivityList = new VBox(8);
        recentActivityList.setFillWidth(true);

        recentActivityCard.getChildren().addAll(activityTitle, recentActivityList);

        midRow.add(quickActionsCard, 0, 0);
        midRow.add(recentActivityCard, 1, 0);

        // --- 5. Overdue Accounts Quick View Table ---
        overdueCard = new VBox(12);
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
        viewAllOverdueBtn.setOnAction(e -> {
            if (parentLayout != null) parentLayout.navigate("Billing", "Invoices & Revenue Engine", new BillingScreen());
            else ToastManager.showInfo("Opening Billing...");
        });

        overdueHeader.getChildren().addAll(overdueTitle, overdueSpacer, viewAllOverdueBtn);

        TableViewBuilder.TableContainer overdueTable = TableViewBuilder.buildAsyncTable(
            "SELECT invoice_id, customer_name, plan_name, outstanding, days_overdue, due_date, status FROM v_overdue_invoices FETCH FIRST 10 ROWS ONLY"
        );
        overdueTable.setPrefHeight(240);
        overdueCard.getChildren().addAll(overdueHeader, overdueTable);

        // Add everything to content
        content.getChildren().addAll(alertBanner, errorBox, kpiGrid, chartsGrid, midRow, overdueCard);
        setContent(content);

        // Apply Role Visibility
        applyRoleVisibility();

        // Wire Actions & Timers
        refreshBtn.setOnAction(e -> loadDashboard());
        retryBtn.setOnAction(e -> loadDashboard());

        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(60), e -> loadDashboard()));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);

        // Auto-refresh leak fix: stop timeline when removed from scene, start when attached
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                autoRefreshTimeline.stop();
            } else {
                autoRefreshTimeline.play();
            }
        });

        // Initial Load
        loadDashboard();
    }

    private void applyRoleVisibility() {
        String role = Session.getRole();
        if (role == null) role = "SUPPORT_AGENT"; // Least privilege default

        boolean isAdmin = "ADMIN".equals(role);
        boolean isBilling = "BILLING_CLERK".equals(role);
        boolean isTech = "TECHNICIAN".equals(role);
        boolean isSupport = "SUPPORT_AGENT".equals(role);

        // 1. Alert Banner & Overdue Card: ADMIN and BILLING_CLERK only
        boolean showOverdueOps = isAdmin || isBilling;
        setWidgetVisible(alertBanner, showOverdueOps);
        setWidgetVisible(overdueCard, showOverdueOps);

        // 2. Financials (Revenue tile, charts): ADMIN and BILLING_CLERK only
        boolean showFinancials = isAdmin || isBilling;
        setWidgetVisible(chartsGrid, showFinancials);

        // 3. Tile Visibility flags
        boolean showSubs = isAdmin || isBilling || isSupport;
        boolean showRevenue = showFinancials;
        boolean showOverdueTile = showOverdueOps;
        boolean showOps = isAdmin || isTech || isSupport;

        setWidgetVisible(tileSubscribers, showSubs);
        setWidgetVisible(tileContracts, showSubs);
        setWidgetVisible(tileRevenue, showRevenue);
        setWidgetVisible(tileOverdue, showOverdueTile);
        setWidgetVisible(tileOpenTickets, showOps);
        setWidgetVisible(tilePendingInstalls, showOps);

        // Rebuild kpiGrid so only visible tiles are attached without empty gaps
        kpiGrid.getChildren().clear();
        kpiGrid.getColumnConstraints().clear();

        java.util.List<KpiTile> visibleTiles = new java.util.ArrayList<>();
        if (showSubs) {
            visibleTiles.add(tileSubscribers);
            visibleTiles.add(tileContracts);
        }
        if (showRevenue) {
            visibleTiles.add(tileRevenue);
        }
        if (showOverdueTile) {
            visibleTiles.add(tileOverdue);
        }
        if (showOps) {
            visibleTiles.add(tileOpenTickets);
            visibleTiles.add(tilePendingInstalls);
        }

        int count = visibleTiles.size();
        if (count > 0) {
            double percent = 100.0 / count;
            for (int i = 0; i < count; i++) {
                ColumnConstraints col = new ColumnConstraints();
                col.setPercentWidth(percent);
                col.setHgrow(Priority.ALWAYS);
                kpiGrid.getColumnConstraints().add(col);
                kpiGrid.add(visibleTiles.get(i), i, 0);
            }
        }

        // 4. Quick Buttons:
        // - ADMIN: all 4
        // - BILLING_CLERK: Record Payment, Run Invoicing
        // - TECHNICIAN: Open Ticket
        // - SUPPORT_AGENT: New Customer, Open Ticket
        setWidgetVisible(addCustBtn, isAdmin || isSupport);
        setWidgetVisible(recordPayBtn, isAdmin || isBilling);
        setWidgetVisible(genInvBtn, isAdmin || isBilling);
        setWidgetVisible(newTicketBtn, isAdmin || isTech || isSupport);
    }

    private void setWidgetVisible(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void loadDashboard() {
        refreshBtn.setDisable(true);
        errorBox.setVisible(false);
        errorBox.setManaged(false);

        Task<DashboardData> task = new Task<>() {
            @Override
            protected DashboardData call() throws Exception {
                return DashboardDao.fetchDashboardData();
            }
        };

        task.setOnSucceeded(e -> {
            refreshBtn.setDisable(false);
            DashboardData d = task.getValue();
            updateUi(d);
        });

        task.setOnFailed(e -> {
            refreshBtn.setDisable(false);
            Throwable ex = task.getException();
            errorMsgLbl.setText("Failed to load dashboard metrics: " + (ex != null ? ex.getMessage() : "Unknown error"));
            errorBox.setVisible(true);
            errorBox.setManaged(true);
            ToastManager.showError("Dashboard sync failed: " + (ex != null ? ex.getMessage() : "Database offline"));
        });

        EXECUTOR.submit(task);
    }

    private void updateUi(DashboardData d) {
        // Last updated time
        lastUpdatedLbl.setText("Updated: " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

        // Top Banner Condition
        if (d.overdueInvoices() > 0) {
            alertBanner.setStyle(
                "-fx-background-color: -color-warning-bg; -fx-padding: 10 16; -fx-background-radius: 10; -fx-border-color: -color-warning; -fx-border-radius: 10;"
            );
            alertIcon.setIconLiteral("fas-exclamation-triangle");
            alertIcon.setStyle("-fx-icon-color: -color-warning;");
            alertText.setText("Action Required: " + d.overdueInvoices() + " overdue invoice(s) pending payment reconciliation.");
        } else {
            alertBanner.setStyle(
                "-fx-background-color: -color-success-bg; -fx-padding: 10 16; -fx-background-radius: 10; -fx-border-color: -color-success; -fx-border-radius: 10;"
            );
            alertIcon.setIconLiteral("fas-check-circle");
            alertIcon.setStyle("-fx-icon-color: -color-success;");
            alertText.setText("All subscriber accounts are current and up to date.");
        }

        // Active Subscribers Delta
        tileSubscribers.setValue(String.valueOf(d.activeCustomers()));
        Double subDelta = d.calculateSubscriberDeltaPercent();
        if (subDelta != null) {
            String sign = subDelta >= 0 ? "+" : "";
            tileSubscribers.setDelta(String.format("%s%.1f%% vs last mo", sign, subDelta));
        } else {
            tileSubscribers.setDelta("-");
        }

        // Contracts
        tileContracts.setValue(String.valueOf(d.activeSubscriptions()));

        // Revenue & Revenue Delta
        tileRevenue.setValue(Formatters.formatCurrency(d.currentMonthRevenue()));
        Double revDelta = d.calculateRevenueDeltaPercent();
        if (revDelta != null) {
            String sign = revDelta >= 0 ? "+" : "";
            tileRevenue.setDelta(String.format("%s%.1f%% vs last mo", sign, revDelta));
        } else {
            tileRevenue.setDelta("-");
        }

        // Overdue, Tickets, Installs
        tileOverdue.setValue(String.valueOf(d.overdueInvoices()));
        tileOpenTickets.setValue(String.valueOf(d.openTickets()));
        tilePendingInstalls.setValue(String.valueOf(d.pendingInstalls()));

        // Revenue Trend Line Chart
        revenueTrendChart.getData().clear();
        Map<String, Double> trend = d.monthlyRevenueTrend();
        if (trend == null || trend.isEmpty()) {
            chart1Empty.setVisible(true);
            revenueTrendChart.setVisible(false);
        } else {
            chart1Empty.setVisible(false);
            revenueTrendChart.setVisible(true);
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Monthly Collections");
            for (Map.Entry<String, Double> entry : trend.entrySet()) {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }
            revenueTrendChart.getData().add(series);
        }

        // Plan Distribution Pie Chart
        Map<String, Integer> dist = d.planDistribution();
        if (dist == null || dist.isEmpty()) {
            chart2Empty.setVisible(true);
            planPieChart.setVisible(false);
        } else {
            chart2Empty.setVisible(false);
            planPieChart.setVisible(true);
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            for (Map.Entry<String, Integer> entry : dist.entrySet()) {
                pieData.add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
            }
            planPieChart.setData(pieData);
        }

        // Recent Activity List
        recentActivityList.getChildren().clear();
        if (d.recentActivities().isEmpty()) {
            Label noAct = new Label("No recent activities logged in system.");
            noAct.setStyle("-fx-text-fill: -color-muted; -fx-padding: 8;");
            recentActivityList.getChildren().add(noAct);
        } else {
            for (DashboardData.RecentActivityItem item : d.recentActivities()) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-padding: 6 8; -fx-background-color: -color-surface-hover; -fx-background-radius: 6;");

                FontIcon icon = new FontIcon(item.iconLiteral());
                icon.setIconSize(13);
                icon.setStyle("-fx-icon-color: " + item.iconColorHex() + ";");

                HBox iconWrap = new HBox(icon);
                iconWrap.setAlignment(Pos.CENTER);
                iconWrap.setStyle("-fx-background-color: " + item.iconColorHex() + "18; -fx-padding: 6; -fx-background-radius: 6;");

                VBox textBox = new VBox(2);
                Label tLbl = new Label(item.title());
                tLbl.setStyle("-fx-font-weight: 600; -fx-font-size: 12px; -fx-text-fill: -color-text;");
                Label sLbl = new Label(item.subtitle());
                sLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-muted;");
                textBox.getChildren().addAll(tLbl, sLbl);

                Region sp = new Region();
                HBox.setHgrow(sp, Priority.ALWAYS);

                Label timeLbl = new Label(item.timeAgo());
                timeLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-muted; -fx-font-weight: 600;");

                row.getChildren().addAll(iconWrap, textBox, sp, timeLbl);
                recentActivityList.getChildren().add(row);
            }
        }
    }
}
