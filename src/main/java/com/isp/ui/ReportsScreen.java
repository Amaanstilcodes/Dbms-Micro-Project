package com.isp.ui;

import com.isp.dao.ReportsDao;
import com.isp.ui.components.EmptyState;
import com.isp.ui.components.StatusBadge;
import com.isp.ui.components.ToastManager;
import com.isp.ui.util.Formatters;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * Modern Analytical Views &amp; Reports dashboard screen.
 *
 * Tab access based on Session role:
 * - ADMIN: all 6 tabs
 * - BILLING_CLERK: Monthly Revenue, Overdue Invoices, Customer Balance, Active Subscriptions
 * - SUPPORT_AGENT: Active Subscriptions, Data Usage Summary
 * - TECHNICIAN: Technician Workload
 */
public class ReportsScreen extends VBox {

    private static final Logger log = LoggerFactory.getLogger(ReportsScreen.class);

    /** Single shared daemon thread for all report data queries. */
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "reports-executor");
        t.setDaemon(true);
        return t;
    });

    private final TabPane tabPane = new TabPane();
    private final Label lastUpdatedLbl = new Label("Last updated: --:--:--");

    // Tabs
    private Tab revTab;
    private Tab overdueTab;
    private Tab balanceTab;
    private Tab workloadTab;
    private Tab usageTab;
    private Tab activeSubsTab;

    // Containers with Loading / Empty / Error / Content states
    private ReportTabContainer revContainer;
    private ReportTabContainer overdueContainer;
    private ReportTabContainer balanceContainer;
    private ReportTabContainer workloadContainer;
    private ReportTabContainer usageContainer;
    private ReportTabContainer activeSubsContainer;

    // Table references for CSV export and data binding
    private TableView<ReportsDao.PlanRevenueRow> revTable;
    private TableView<ReportsDao.OverdueRow> overdueTable;
    private TableView<ReportsDao.CustomerBalanceRow> balanceTable;
    private TableView<ReportsDao.TechnicianWorkloadRow> workloadTable;
    private TableView<ReportsDao.DataUsageRow> usageTable;
    private TableView<ReportsDao.ActiveSubscriptionRow> activeSubsTable;

    // Revenue tab controls
    private ComboBox<String> revMonthCombo;
    private Label revSummaryLbl;
    private LineChart<String, Number> revLineChart;
    private BarChart<String, Number> revPlanBarChart;

    // Workload tab chart
    private BarChart<String, Number> techWorkloadChart;

    // Search fields
    private TextField overdueSearchField;
    private TextField balanceSearchField;

    public ReportsScreen() {
        setSpacing(16);
        setPadding(new Insets(24));
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        initHeader();
        initTabsByRole();
    }

    // ── Header Bar ────────────────────────────────────────────────────────────

    private void initHeader() {
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

        lastUpdatedLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");

        Button refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        FontIcon refreshIcon = new FontIcon("fas-sync");
        refreshIcon.setIconSize(13);
        refreshBtn.setGraphic(refreshIcon);
        refreshBtn.setOnAction(e -> refreshActiveTab());

        Button exportBtn = new Button("Export Current View (CSV)");
        exportBtn.getStyleClass().add("btn-secondary");
        FontIcon exportIcon = new FontIcon("fas-file-csv");
        exportIcon.setIconSize(13);
        exportBtn.setGraphic(exportIcon);
        exportBtn.setOnAction(e -> exportCurrentTabCSV());

        header.getChildren().addAll(titleBox, spacer, lastUpdatedLbl, refreshBtn, exportBtn);
        getChildren().addAll(header, tabPane);
    }

    // ── Role-Based Tab Initialization ─────────────────────────────────────────

    private void initTabsByRole() {
        String role = ReportsDao.getEffectiveRole();
        List<Tab> allowedTabs = new ArrayList<>();

        if ("ADMIN".equals(role) || "BILLING_CLERK".equals(role)) {
            revTab = buildRevenueTab();
            allowedTabs.add(revTab);

            overdueTab = buildOverdueTab();
            allowedTabs.add(overdueTab);

            balanceTab = buildBalanceTab();
            allowedTabs.add(balanceTab);
        }

        if ("ADMIN".equals(role) || "TECHNICIAN".equals(role)) {
            workloadTab = buildWorkloadTab();
            allowedTabs.add(workloadTab);
        }

        if ("ADMIN".equals(role) || "SUPPORT_AGENT".equals(role)) {
            usageTab = buildUsageTab();
            allowedTabs.add(usageTab);
        }

        if ("ADMIN".equals(role) || "BILLING_CLERK".equals(role) || "SUPPORT_AGENT".equals(role)) {
            activeSubsTab = buildActiveSubsTab();
            allowedTabs.add(activeSubsTab);
        }

        tabPane.getTabs().setAll(allowedTabs);

        // If the revenue tab was built, prime the month combo with the most-recent
        // month that actually has data before triggering the first load.
        if (revTab != null && revMonthCombo != null) {
            EXECUTOR.submit(() -> {
                String maxMonth = null;
                try {
                    maxMonth = ReportsDao.maxRevenueMonth();
                } catch (Exception ignored) { /* fall back to current month */ }
                final String resolved = maxMonth;
                Platform.runLater(() -> {
                    if (resolved != null && revMonthCombo.getItems().contains(resolved)) {
                        // Suppress the action listener while we set the value so loadRevenueData
                        // is not triggered prematurely — we call it explicitly below.
                        revMonthCombo.setOnAction(null);
                        revMonthCombo.setValue(resolved);
                        revMonthCombo.setOnAction(e -> loadRevenueData());
                    }
                    loadTab(revTab);
                });
            });
        } else if (!allowedTabs.isEmpty()) {
            loadTab(allowedTabs.get(0));
        }

        // Lazy load on tab change
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                loadTab(newTab);
            }
        });
    }

    private void loadTab(Tab tab) {
        if (tab == revTab) {
            loadRevenueData();
        } else if (tab == overdueTab) {
            loadOverdueData(overdueSearchField != null ? overdueSearchField.getText() : null);
        } else if (tab == balanceTab) {
            loadBalanceData(balanceSearchField != null ? balanceSearchField.getText() : null);
        } else if (tab == workloadTab) {
            loadWorkloadData();
        } else if (tab == usageTab) {
            loadUsageData();
        } else if (tab == activeSubsTab) {
            loadActiveSubsData();
        }
    }

    private void refreshActiveTab() {
        Tab current = tabPane.getSelectionModel().getSelectedItem();
        if (current != null) {
            loadTab(current);
        }
    }

    private void updateLastUpdated() {
        lastUpdatedLbl.setText("Last updated: " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    // ── Tab 1: Monthly Revenue ────────────────────────────────────────────────

    private Tab buildRevenueTab() {
        Tab tab = new Tab("Monthly Revenue");
        tab.setClosable(false);

        VBox contentBox = new VBox(12);
        contentBox.setPadding(new Insets(16, 0, 0, 0));

        // Controls bar: Month selector + Total Revenue summary
        HBox controls = new HBox(16);
        controls.setAlignment(Pos.CENTER_LEFT);

        Label monthLbl = new Label("Billing Month:");
        monthLbl.setStyle("-fx-font-weight: bold;");

        revMonthCombo = new ComboBox<>();
        YearMonth now = YearMonth.now();
        List<String> months = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            months.add(now.minusMonths(i).toString());
        }
        revMonthCombo.setItems(FXCollections.observableArrayList(months));
        revMonthCombo.setValue(now.toString()); // Replaced with DB-max in initTabsByRole after build
        revMonthCombo.setOnAction(e -> loadRevenueData());

        revSummaryLbl = new Label("Total Revenue: ₹0.00 | 0 payments");
        revSummaryLbl.getStyleClass().add("badge");
        revSummaryLbl.setStyle("-fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 6; -fx-background-color: -color-primary-subtle; -fx-text-fill: -color-primary;");

        controls.getChildren().addAll(monthLbl, revMonthCombo, revSummaryLbl);

        // Charts container: (a) LineChart for 6-month trend, (b) BarChart for selected month plan breakdown
        CategoryAxis lineX = new CategoryAxis();
        lineX.setLabel("Month");
        NumberAxis lineY = new NumberAxis();
        lineY.setLabel("Revenue (₹)");
        revLineChart = new LineChart<>(lineX, lineY);
        revLineChart.setTitle("Revenue Trend (Last 6 Months)");
        revLineChart.setPrefHeight(230);
        revLineChart.setLegendVisible(false);
        HBox.setHgrow(revLineChart, Priority.ALWAYS);

        CategoryAxis barX = new CategoryAxis();
        barX.setLabel("Plan");
        NumberAxis barY = new NumberAxis();
        barY.setLabel("Revenue (₹)");
        revPlanBarChart = new BarChart<>(barX, barY);
        revPlanBarChart.setTitle("Revenue by Plan");
        revPlanBarChart.setPrefHeight(230);
        revPlanBarChart.setLegendVisible(false);
        HBox.setHgrow(revPlanBarChart, Priority.ALWAYS);

        HBox chartsBox = new HBox(12, revLineChart, revPlanBarChart);

        // Revenue Table
        revTable = new TableView<>();
        revTable.getColumns().add(makeCol("Month", 100, r -> new SimpleStringProperty(r.month())));
        revTable.getColumns().add(makeCol("Plan Type", 120, r -> new SimpleStringProperty(r.planType())));
        revTable.getColumns().add(makeCol("Plan Name", 200, r -> new SimpleStringProperty(r.planName())));
        revTable.getColumns().add(makeCol("Payments", 100, r -> new SimpleStringProperty(String.valueOf(r.paymentCount()))));
        revTable.getColumns().add(makeCol("Revenue Collected", 160, r -> new SimpleStringProperty(Formatters.formatCurrency(r.revenue()))));
        VBox.setVgrow(revTable, Priority.ALWAYS);

        VBox tableCard = new VBox(revTable);
        tableCard.getStyleClass().add("card");
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        contentBox.getChildren().addAll(controls, chartsBox, tableCard);

        revContainer = new ReportTabContainer(contentBox);
        tab.setContent(revContainer);
        return tab;
    }

    private void loadRevenueData() {
        if (revContainer == null) return;
        revContainer.showLoading();

        String selectedMonth = revMonthCombo.getValue();
        YearMonth curYm = selectedMonth != null ? YearMonth.parse(selectedMonth) : YearMonth.now();
        String fromMonth = curYm.minusMonths(5).toString();
        String toMonth = curYm.toString();

        EXECUTOR.submit(() -> {
            try {
                // (a) 6-month line trend
                List<ReportsDao.MonthRevenueRow> trendList = ReportsDao.revenueByMonth(fromMonth, toMonth);
                // (b) Selected month breakdown by plan
                List<ReportsDao.PlanRevenueRow> planList = ReportsDao.revenueByPlan(selectedMonth);

                Platform.runLater(() -> {
                    // Update LineChart
                    revLineChart.getData().clear();
                    XYChart.Series<String, Number> lineSeries = new XYChart.Series<>();
                    lineSeries.setName("Revenue");
                    for (ReportsDao.MonthRevenueRow row : trendList) {
                        lineSeries.getData().add(new XYChart.Data<>(row.month(), row.revenue()));
                    }
                    revLineChart.getData().add(lineSeries);

                    // Update BarChart
                    revPlanBarChart.getData().clear();
                    XYChart.Series<String, Number> barSeries = new XYChart.Series<>();
                    barSeries.setName("Plan Revenue");
                    double totalRev = 0;
                    int totalPayments = 0;
                    for (ReportsDao.PlanRevenueRow row : planList) {
                        barSeries.getData().add(new XYChart.Data<>(row.planName(), row.revenue()));
                        totalRev += row.revenue();
                        totalPayments += row.paymentCount();
                    }
                    revPlanBarChart.getData().add(barSeries);

                    // Update Summary
                    revSummaryLbl.setText(String.format("Total Revenue for %s: %s | %d payments",
                            selectedMonth, Formatters.formatCurrency(totalRev), totalPayments));

                    // Update Table
                    revTable.setItems(FXCollections.observableArrayList(planList));
                    if (planList.isEmpty()) {
                        // Keep controls + charts visible; show placeholder text in the table only
                        revTable.setPlaceholder(new Label("No payments recorded in " + selectedMonth + "."));
                    }
                    // Only hide the whole content area when BOTH trend and plan lists are empty
                    revContainer.showContent(false, "", "");
                    updateLastUpdated();
                });
            } catch (Exception ex) {
                log.error("Failed to load revenue data", ex);
                Platform.runLater(() -> revContainer.showError(ex.getMessage(), this::loadRevenueData));
            }
        });
    }

    // ── Tab 2: Overdue Invoices ───────────────────────────────────────────────

    private Tab buildOverdueTab() {
        Tab tab = new Tab("Overdue Invoices");
        tab.setClosable(false);

        VBox contentBox = new VBox(12);
        contentBox.setPadding(new Insets(16, 0, 0, 0));

        // Search bar with 300ms debounce
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        overdueSearchField = new TextField();
        overdueSearchField.setPromptText("Search by customer name or email...");
        overdueSearchField.setPrefWidth(300);

        Button clearBtn = new Button("Clear");
        clearBtn.getStyleClass().add("btn-secondary");
        clearBtn.setOnAction(e -> overdueSearchField.clear());

        PauseTransition debounce = new PauseTransition(Duration.millis(300));
        overdueSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            debounce.setOnFinished(ev -> loadOverdueData(newVal));
            debounce.playFromStart();
        });

        filterBar.getChildren().addAll(overdueSearchField, clearBtn);

        // Table
        overdueTable = new TableView<>();
        overdueTable.getColumns().add(makeCol("Invoice ID", 80, r -> new SimpleStringProperty(String.valueOf(r.invoiceId()))));
        overdueTable.getColumns().add(makeCol("Customer Name", 160, r -> new SimpleStringProperty(r.customerName())));
        overdueTable.getColumns().add(makeCol("Email", 180, r -> new SimpleStringProperty(r.email())));
        overdueTable.getColumns().add(makeCol("Phone", 120, r -> new SimpleStringProperty(r.phone())));
        overdueTable.getColumns().add(makeCol("Plan", 140, r -> new SimpleStringProperty(r.planName())));
        overdueTable.getColumns().add(makeCol("Total", 100, r -> new SimpleStringProperty(Formatters.formatCurrency(r.totalAmount()))));
        overdueTable.getColumns().add(makeCol("Paid", 100, r -> new SimpleStringProperty(Formatters.formatCurrency(r.amountPaid()))));
        overdueTable.getColumns().add(makeCol("Outstanding", 110, r -> new SimpleStringProperty(Formatters.formatCurrency(r.outstanding()))));
        overdueTable.getColumns().add(makeCol("Due Date", 100, r -> new SimpleStringProperty(Formatters.formatDate(r.dueDate()))));
        overdueTable.getColumns().add(makeCol("Days Overdue", 100, r -> new SimpleStringProperty(r.daysOverdue() + " days")));
        overdueTable.getColumns().add(makeBadgeCol("Status", 100, ReportsDao.OverdueRow::status));
        VBox.setVgrow(overdueTable, Priority.ALWAYS);

        VBox card = new VBox(overdueTable);
        card.getStyleClass().add("card");
        VBox.setVgrow(card, Priority.ALWAYS);

        contentBox.getChildren().addAll(filterBar, card);

        overdueContainer = new ReportTabContainer(contentBox);
        tab.setContent(overdueContainer);
        return tab;
    }

    private void loadOverdueData(String filter) {
        if (overdueContainer == null) return;
        overdueContainer.showLoading();

        EXECUTOR.submit(() -> {
            try {
                List<ReportsDao.OverdueRow> list = ReportsDao.overdue(filter);
                Platform.runLater(() -> {
                    overdueTable.setItems(FXCollections.observableArrayList(list));
                    overdueContainer.showContent(list.isEmpty(), "No overdue invoices", "All accounts are up to date.");
                    updateLastUpdated();
                });
            } catch (Exception ex) {
                log.error("Failed to load overdue invoices", ex);
                Platform.runLater(() -> overdueContainer.showError(ex.getMessage(), () -> loadOverdueData(filter)));
            }
        });
    }

    // ── Tab 3: Customer Balance ───────────────────────────────────────────────

    private Tab buildBalanceTab() {
        Tab tab = new Tab("Customer Balance");
        tab.setClosable(false);

        VBox contentBox = new VBox(12);
        contentBox.setPadding(new Insets(16, 0, 0, 0));

        // Search bar with 300ms debounce
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        balanceSearchField = new TextField();
        balanceSearchField.setPromptText("Search by customer name or email...");
        balanceSearchField.setPrefWidth(300);

        Button clearBtn = new Button("Clear");
        clearBtn.getStyleClass().add("btn-secondary");
        clearBtn.setOnAction(e -> balanceSearchField.clear());

        PauseTransition debounce = new PauseTransition(Duration.millis(300));
        balanceSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            debounce.setOnFinished(ev -> loadBalanceData(newVal));
            debounce.playFromStart();
        });

        filterBar.getChildren().addAll(balanceSearchField, clearBtn);

        // Table
        balanceTable = new TableView<>();
        balanceTable.getColumns().add(makeCol("Customer ID", 90, r -> new SimpleStringProperty(String.valueOf(r.customerId()))));
        balanceTable.getColumns().add(makeCol("Customer Name", 180, r -> new SimpleStringProperty(r.customerName())));
        balanceTable.getColumns().add(makeCol("Email", 200, r -> new SimpleStringProperty(r.email())));
        balanceTable.getColumns().add(makeCol("Invoices", 90, r -> new SimpleStringProperty(String.valueOf(r.totalInvoices()))));
        balanceTable.getColumns().add(makeCol("Total Billed", 120, r -> new SimpleStringProperty(Formatters.formatCurrency(r.totalBilled()))));
        balanceTable.getColumns().add(makeCol("Total Paid", 120, r -> new SimpleStringProperty(Formatters.formatCurrency(r.totalPaid()))));
        balanceTable.getColumns().add(makeCol("Outstanding Balance", 150, r -> new SimpleStringProperty(Formatters.formatCurrency(r.outstandingBalance()))));
        VBox.setVgrow(balanceTable, Priority.ALWAYS);

        VBox card = new VBox(balanceTable);
        card.getStyleClass().add("card");
        VBox.setVgrow(card, Priority.ALWAYS);

        contentBox.getChildren().addAll(filterBar, card);

        balanceContainer = new ReportTabContainer(contentBox);
        tab.setContent(balanceContainer);
        return tab;
    }

    private void loadBalanceData(String filter) {
        if (balanceContainer == null) return;
        balanceContainer.showLoading();

        EXECUTOR.submit(() -> {
            try {
                List<ReportsDao.CustomerBalanceRow> list = ReportsDao.customerBalance(filter);
                Platform.runLater(() -> {
                    balanceTable.setItems(FXCollections.observableArrayList(list));
                    balanceContainer.showContent(list.isEmpty(), "No balance records found", "No customers match the given query.");
                    updateLastUpdated();
                });
            } catch (Exception ex) {
                log.error("Failed to load customer balances", ex);
                Platform.runLater(() -> balanceContainer.showError(ex.getMessage(), () -> loadBalanceData(filter)));
            }
        });
    }

    // ── Tab 4: Technician Workload ────────────────────────────────────────────

    private Tab buildWorkloadTab() {
        Tab tab = new Tab("Technician Workload");
        tab.setClosable(false);

        VBox contentBox = new VBox(12);
        contentBox.setPadding(new Insets(16, 0, 0, 0));

        // Small BarChart of total workload per technician
        CategoryAxis techX = new CategoryAxis();
        techX.setLabel("Technician");
        NumberAxis techY = new NumberAxis();
        techY.setLabel("Tasks (Installs + Tickets)");
        techWorkloadChart = new BarChart<>(techX, techY);
        techWorkloadChart.setTitle("Technician Workload Distribution");
        techWorkloadChart.setPrefHeight(200);
        techWorkloadChart.setLegendVisible(false);

        // Table
        workloadTable = new TableView<>();
        workloadTable.getColumns().add(makeCol("Tech ID", 80, r -> new SimpleStringProperty(String.valueOf(r.technicianId()))));
        workloadTable.getColumns().add(makeCol("Technician Name", 160, r -> new SimpleStringProperty(r.technicianName())));
        workloadTable.getColumns().add(makeCol("Specialization", 140, r -> new SimpleStringProperty(r.specialization())));
        workloadTable.getColumns().add(makeCol("Service Area", 140, r -> new SimpleStringProperty(r.areaName())));
        workloadTable.getColumns().add(makeCol("Pending Installs", 110, r -> new SimpleStringProperty(String.valueOf(r.pendingInstallations()))));
        workloadTable.getColumns().add(makeCol("Open Tickets", 100, r -> new SimpleStringProperty(String.valueOf(r.openTickets()))));
        workloadTable.getColumns().add(makeCol("Total Workload", 110, r -> new SimpleStringProperty(String.valueOf(r.totalWorkload()))));
        workloadTable.getColumns().add(makeBadgeCol("Status", 120, ReportsDao.TechnicianWorkloadRow::workloadStatus));
        VBox.setVgrow(workloadTable, Priority.ALWAYS);

        VBox card = new VBox(workloadTable);
        card.getStyleClass().add("card");
        VBox.setVgrow(card, Priority.ALWAYS);

        contentBox.getChildren().addAll(techWorkloadChart, card);

        workloadContainer = new ReportTabContainer(contentBox);
        tab.setContent(workloadContainer);
        return tab;
    }

    private void loadWorkloadData() {
        if (workloadContainer == null) return;
        workloadContainer.showLoading();

        EXECUTOR.submit(() -> {
            try {
                List<ReportsDao.TechnicianWorkloadRow> list = ReportsDao.technicianWorkload();
                Platform.runLater(() -> {
                    // Update Chart
                    techWorkloadChart.getData().clear();
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    series.setName("Workload");
                    for (ReportsDao.TechnicianWorkloadRow row : list) {
                        series.getData().add(new XYChart.Data<>(row.technicianName(), row.totalWorkload()));
                    }
                    techWorkloadChart.getData().add(series);

                    // Update Table
                    workloadTable.setItems(FXCollections.observableArrayList(list));
                    workloadContainer.showContent(list.isEmpty(), "No technician workload records", "No active technicians found.");
                    updateLastUpdated();
                });
            } catch (Exception ex) {
                log.error("Failed to load technician workload", ex);
                Platform.runLater(() -> workloadContainer.showError(ex.getMessage(), this::loadWorkloadData));
            }
        });
    }

    // ── Tab 5: Data Usage Summary ─────────────────────────────────────────────

    private Tab buildUsageTab() {
        Tab tab = new Tab("Data Usage");
        tab.setClosable(false);

        VBox contentBox = new VBox(12);
        contentBox.setPadding(new Insets(16, 0, 0, 0));

        usageTable = new TableView<>();
        usageTable.getColumns().add(makeCol("Sub ID", 80, r -> new SimpleStringProperty(String.valueOf(r.subscriptionId()))));
        usageTable.getColumns().add(makeCol("Customer Name", 180, r -> new SimpleStringProperty(r.customerName())));
        usageTable.getColumns().add(makeCol("Plan Name", 160, r -> new SimpleStringProperty(r.planName())));
        usageTable.getColumns().add(makeCol("Cap (GB)", 100, r -> new SimpleStringProperty(
                "UNLIMITED".equalsIgnoreCase(r.capGb()) ? "Unlimited" : r.capGb() + " GB")));
        usageTable.getColumns().add(makeCol("Month", 100, r -> new SimpleStringProperty(r.usageMonth())));
        usageTable.getColumns().add(makeCol("Data Used", 110, r -> new SimpleStringProperty(String.format("%.2f GB", r.dataUsedGb()))));
        usageTable.getColumns().add(makeBadgeCol("Status", 120, ReportsDao.DataUsageRow::usageStatus));
        usageTable.getColumns().add(makeCol("% Used", 90, r -> new SimpleStringProperty(
                r.pctUsed() != null ? String.format("%.1f%%", r.pctUsed()) : "N/A")));
        VBox.setVgrow(usageTable, Priority.ALWAYS);

        VBox card = new VBox(usageTable);
        card.getStyleClass().add("card");
        VBox.setVgrow(card, Priority.ALWAYS);

        contentBox.getChildren().addAll(card);

        usageContainer = new ReportTabContainer(contentBox);
        tab.setContent(usageContainer);
        return tab;
    }

    private void loadUsageData() {
        if (usageContainer == null) return;
        usageContainer.showLoading();

        EXECUTOR.submit(() -> {
            try {
                List<ReportsDao.DataUsageRow> list = ReportsDao.dataUsage();
                Platform.runLater(() -> {
                    usageTable.setItems(FXCollections.observableArrayList(list));
                    usageContainer.showContent(list.isEmpty(), "No data usage records", "No usage logs recorded for current period.");
                    updateLastUpdated();
                });
            } catch (Exception ex) {
                log.error("Failed to load data usage summary", ex);
                Platform.runLater(() -> usageContainer.showError(ex.getMessage(), this::loadUsageData));
            }
        });
    }

    // ── Tab 6: Active Subscriptions ───────────────────────────────────────────

    private Tab buildActiveSubsTab() {
        Tab tab = new Tab("Active Subscriptions");
        tab.setClosable(false);

        VBox contentBox = new VBox(12);
        contentBox.setPadding(new Insets(16, 0, 0, 0));

        activeSubsTable = new TableView<>();
        activeSubsTable.getColumns().add(makeCol("Sub ID", 80, r -> new SimpleStringProperty(String.valueOf(r.subscriptionId()))));
        activeSubsTable.getColumns().add(makeCol("Customer Name", 160, r -> new SimpleStringProperty(r.customerName())));
        activeSubsTable.getColumns().add(makeCol("Email", 180, r -> new SimpleStringProperty(r.email())));
        activeSubsTable.getColumns().add(makeCol("Phone", 120, r -> new SimpleStringProperty(r.phone())));
        activeSubsTable.getColumns().add(makeCol("Plan", 140, r -> new SimpleStringProperty(r.planName())));
        activeSubsTable.getColumns().add(makeCol("Tier", 100, r -> new SimpleStringProperty(r.planType())));
        activeSubsTable.getColumns().add(makeCol("Speed", 100, r -> new SimpleStringProperty(r.speedMbps() + " Mbps")));
        activeSubsTable.getColumns().add(makeCol("Monthly Price", 110, r -> new SimpleStringProperty(Formatters.formatCurrency(r.monthlyPrice()))));
        activeSubsTable.getColumns().add(makeCol("Area", 120, r -> new SimpleStringProperty(r.areaName())));
        activeSubsTable.getColumns().add(makeCol("City", 100, r -> new SimpleStringProperty(r.city())));
        activeSubsTable.getColumns().add(makeCol("Start Date", 100, r -> new SimpleStringProperty(Formatters.formatDate(r.startDate()))));
        activeSubsTable.getColumns().add(makeBadgeCol("Status", 100, ReportsDao.ActiveSubscriptionRow::status));
        VBox.setVgrow(activeSubsTable, Priority.ALWAYS);

        VBox card = new VBox(activeSubsTable);
        card.getStyleClass().add("card");
        VBox.setVgrow(card, Priority.ALWAYS);

        contentBox.getChildren().addAll(card);

        activeSubsContainer = new ReportTabContainer(contentBox);
        tab.setContent(activeSubsContainer);
        return tab;
    }

    private void loadActiveSubsData() {
        if (activeSubsContainer == null) return;
        activeSubsContainer.showLoading();

        EXECUTOR.submit(() -> {
            try {
                List<ReportsDao.ActiveSubscriptionRow> list = ReportsDao.activeSubscriptions();
                Platform.runLater(() -> {
                    activeSubsTable.setItems(FXCollections.observableArrayList(list));
                    activeSubsContainer.showContent(list.isEmpty(), "No active subscriptions", "No active contracts found.");
                    updateLastUpdated();
                });
            } catch (Exception ex) {
                log.error("Failed to load active subscriptions", ex);
                Platform.runLater(() -> activeSubsContainer.showError(ex.getMessage(), this::loadActiveSubsData));
            }
        });
    }

    // ── CSV Export Helper with Full RFC 4180 Escaping & FileChooser ───────────

    private void exportCurrentTabCSV() {
        Tab current = tabPane.getSelectionModel().getSelectedItem();
        if (current == null) {
            ToastManager.showInfo("No report tab selected.");
            return;
        }

        TableView<?> table = getActiveTableView(current);
        if (table == null || table.getItems().isEmpty()) {
            ToastManager.showInfo("No table data ready to export.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export " + current.getText().replaceAll("\\(.*\\)", "").trim() + " CSV");
        String safeName = current.getText().replaceAll("[^a-zA-Z0-9]", "_").replaceAll("_+", "_");
        chooser.setInitialFileName(safeName + "_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));

        Window window = getScene() != null ? getScene().getWindow() : null;
        File file = chooser.showSaveDialog(window);
        if (file == null) return;

        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            // Write column headers
            List<String> headers = new ArrayList<>();
            for (TableColumn<?, ?> col : table.getVisibleLeafColumns()) {
                String title = col.getText() != null ? col.getText() : "";
                headers.add(escapeCsv(title));
            }
            pw.println(String.join(",", headers));

            // Write row cells
            int rowCount = 0;
            for (Object rowObj : table.getItems()) {
                List<String> cells = new ArrayList<>();
                for (TableColumn<?, ?> col : table.getVisibleLeafColumns()) {
                    @SuppressWarnings("unchecked")
                    TableColumn<Object, Object> objCol = (TableColumn<Object, Object>) col;
                    ObservableValue<Object> cellObs = objCol.getCellObservableValue(rowObj);
                    Object val = cellObs != null ? cellObs.getValue() : null;
                    cells.add(escapeCsv(val != null ? val.toString() : ""));
                }
                pw.println(String.join(",", cells));
                rowCount++;
            }
            ToastManager.showSuccess("Exported " + rowCount + " rows to " + file.getName());
        } catch (Exception ex) {
            log.error("Failed to export report CSV", ex);
            ToastManager.showError("Export failed: " + ex.getMessage());
        }
    }

    private TableView<?> getActiveTableView(Tab current) {
        if (current == revTab) return revTable;
        if (current == overdueTab) return overdueTable;
        if (current == balanceTab) return balanceTable;
        if (current == workloadTab) return workloadTable;
        if (current == usageTab) return usageTable;
        if (current == activeSubsTab) return activeSubsTable;
        return null;
    }

    /**
     * RFC 4180 CSV value escaping: wraps value in quotes and doubles internal quotation marks.
     */
    private static String escapeCsv(String value) {
        if (value == null) return "\"\"";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    // ── Table Column Factory Helpers ──────────────────────────────────────────

    private static <S> TableColumn<S, String> makeCol(String title, double width, Function<S, ObservableValue<String>> extractor) {
        TableColumn<S, String> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setCellValueFactory(cellData -> extractor.apply(cellData.getValue()));
        return col;
    }

    private static <S> TableColumn<S, String> makeBadgeCol(String title, double width, Function<S, String> extractor) {
        TableColumn<S, String> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setCellValueFactory(cellData -> new SimpleStringProperty(extractor.apply(cellData.getValue())));
        col.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setGraphic(null);
                    setText(null);
                } else {
                    StatusBadge badge = new StatusBadge(item);
                    setGraphic(badge);
                    setText(null);
                    setAlignment(Pos.CENTER);
                }
            }
        });
        return col;
    }

    // ── Tab State Container Helper ────────────────────────────────────────────

    private static class ReportTabContainer extends StackPane {
        private final Node contentNode;
        private final VBox loadingBox;
        private final StackPane emptyBox;
        private final VBox errorBox;
        private final Label errorMsgLbl;
        private Runnable retryAction;

        public ReportTabContainer(Node contentNode) {
            this.contentNode = contentNode;
            VBox.setVgrow(this, Priority.ALWAYS);

            // Loading overlay
            ProgressIndicator spinner = new ProgressIndicator();
            spinner.setMaxSize(40, 40);
            Label loadLbl = new Label("Loading report data...");
            loadLbl.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 12px;");
            loadingBox = new VBox(12, spinner, loadLbl);
            loadingBox.setAlignment(Pos.CENTER);
            loadingBox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.75);");

            // Empty state container
            emptyBox = new StackPane();
            emptyBox.setVisible(false);

            // Error state container with Retry
            FontIcon errIcon = new FontIcon("fas-exclamation-triangle");
            errIcon.setIconSize(36);
            errIcon.setStyle("-fx-icon-color: -color-danger;");
            Label errTitle = new Label("Unable to load report");
            errTitle.getStyleClass().add("empty-state-title");
            errorMsgLbl = new Label();
            errorMsgLbl.getStyleClass().add("empty-state-subtitle");
            errorMsgLbl.setWrapText(true);
            Button retryBtn = new Button("Retry");
            retryBtn.getStyleClass().add("btn-primary");
            retryBtn.setOnAction(e -> {
                if (retryAction != null) retryAction.run();
            });
            errorBox = new VBox(10, errIcon, errTitle, errorMsgLbl, retryBtn);
            errorBox.setAlignment(Pos.CENTER);
            errorBox.setPadding(new Insets(24));
            errorBox.setVisible(false);

            getChildren().addAll(contentNode, emptyBox, errorBox, loadingBox);
        }

        public void showLoading() {
            loadingBox.setVisible(true);
            errorBox.setVisible(false);
            emptyBox.setVisible(false);
            contentNode.setDisable(true);
        }

        public void showContent(boolean isEmpty, String emptyTitle, String emptySubtitle) {
            loadingBox.setVisible(false);
            errorBox.setVisible(false);
            contentNode.setDisable(false);
            if (isEmpty) {
                emptyBox.getChildren().setAll(new EmptyState(emptyTitle, emptySubtitle, "fas-inbox"));
                emptyBox.setVisible(true);
                contentNode.setVisible(false);
            } else {
                emptyBox.setVisible(false);
                contentNode.setVisible(true);
            }
        }

        public void showError(String msg, Runnable retry) {
            loadingBox.setVisible(false);
            emptyBox.setVisible(false);
            this.retryAction = retry;
            errorMsgLbl.setText(msg != null ? msg : "An unexpected error occurred.");
            errorBox.setVisible(true);
            contentNode.setVisible(false);
        }
    }
}
