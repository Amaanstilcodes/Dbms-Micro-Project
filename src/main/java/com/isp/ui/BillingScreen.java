package com.isp.ui;

import com.isp.Session;
import com.isp.dao.BillingDao;
import com.isp.dao.BillingDao.*;
import com.isp.ui.components.ConfirmDialog;
import com.isp.ui.components.EmptyState;
import com.isp.ui.components.StatusBadge;
import com.isp.ui.components.ToastManager;
import com.isp.ui.util.Formatters;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Billing &amp; Payments screen.
 *
 * Role gates (enforced here AND in BillingDao):
 *   ADMIN         – full access
 *   BILLING_CLERK – generate, record payment, view (no suspend)
 *   SUPPORT_AGENT – read-only (Generate, Suspend, Record Payment hidden)
 *   TECHNICIAN    – excluded by MainLayout (no Billing menu item)
 */
public class BillingScreen extends VBox {

    /** Single-thread daemon executor shared across ALL tasks in this screen. */
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Billing-Worker");
        t.setDaemon(true);
        return t;
    });

    // ── Role flags ────────────────────────────────────────────────────────────
    private final boolean isAdmin;
    private final boolean canWrite;

    // ── All Invoices tab ──────────────────────────────────────────────────────
    private final TextField searchField;
    private final ComboBox<String> statusFilter;
    private final TableView<InvoiceRow> invoiceTable = new TableView<>();
    private final ObservableList<InvoiceRow> invoiceRows = FXCollections.observableArrayList();
    private final StackPane invTableContainer;
    private final ProgressIndicator invSpinner;
    private final EmptyState invEmptyState;
    private final VBox invErrorBox;
    private final Label invErrorLbl;
    private final Button recordPayBtn;
    private InvoiceRow selectedInvoice = null;

    // ── Overdue tab ───────────────────────────────────────────────────────────
    private final TableView<OverdueRow> overdueTable = new TableView<>();
    private final ObservableList<OverdueRow> overdueRows = FXCollections.observableArrayList();
    private final StackPane ovdTableContainer;
    private final ProgressIndicator ovdSpinner;
    private final EmptyState ovdEmptyState;
    private final VBox ovdErrorBox;
    private final Label ovdErrorLbl;

    // ── Root TabPane ──────────────────────────────────────────────────────────
    private final TabPane tabPane;

    public BillingScreen() {
        setSpacing(16);
        setPadding(new Insets(24));
        setFillWidth(true);

        String role = Session.getRole();
        if (role == null) role = "SUPPORT_AGENT";
        isAdmin  = "ADMIN".equals(role);
        canWrite = isAdmin || "BILLING_CLERK".equals(role);

        // ── Header ────────────────────────────────────────────────────────────
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("Billing & Revenue Engine");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("PL/SQL pkg_billing · 18% GST invoicing · live payments");
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button genBtn = new Button("⚡ Generate Monthly");
        genBtn.getStyleClass().add("btn-secondary");
        genBtn.setOnAction(e -> openGenerateDialog());
        genBtn.setVisible(canWrite);
        genBtn.setManaged(canWrite);

        Button suspendBtn = new Button("⚠ Suspend Overdue");
        suspendBtn.getStyleClass().add("btn-danger");
        suspendBtn.setOnAction(e -> openSuspendDialog());
        suspendBtn.setVisible(isAdmin);
        suspendBtn.setManaged(isAdmin);

        recordPayBtn = new Button("💳 Record Payment");
        recordPayBtn.getStyleClass().add("btn-primary");
        recordPayBtn.setOnAction(e -> openPaymentDialog(selectedInvoice));
        recordPayBtn.setDisable(true);
        recordPayBtn.setVisible(canWrite);
        recordPayBtn.setManaged(canWrite);

        header.getChildren().addAll(titleBox, spacer, genBtn, suspendBtn, recordPayBtn);

        // ══════════════════════════════════════════════════════════════════════
        // TAB 1 — ALL INVOICES
        // ══════════════════════════════════════════════════════════════════════
        Tab invTab = new Tab("All Invoices");
        invTab.setClosable(false);

        // toolbar
        HBox invToolbar = new HBox(10);
        invToolbar.getStyleClass().add("toolbar-card");
        invToolbar.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("Search invoice ID, customer or plan…");
        searchField.setPrefWidth(260);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("ALL", "ISSUED", "PARTIALLY_PAID", "PAID", "OVERDUE", "DRAFT", "CANCELLED");
        statusFilter.setValue("ALL");

        Button clearBtn = new Button("Clear");
        clearBtn.getStyleClass().add("btn-secondary");
        clearBtn.setOnAction(e -> { searchField.clear(); statusFilter.setValue("ALL"); });

        invToolbar.getChildren().addAll(
            new Label("Search:"), searchField,
            new Label("Status:"), statusFilter,
            clearBtn
        );

        // table
        buildInvoiceTable();

        invSpinner = new ProgressIndicator();
        invSpinner.setPrefSize(48, 48);
        invSpinner.setVisible(false);

        invEmptyState = new EmptyState("No Invoices Found",
            "Try adjusting your search or filter.", "fas-file-invoice");
        invEmptyState.setVisible(false);

        invErrorLbl = new Label();
        invErrorLbl.setWrapText(true);
        invErrorLbl.getStyleClass().add("page-subtitle");
        Button invRetryBtn = new Button("Retry");
        invRetryBtn.getStyleClass().add("btn-secondary");
        invRetryBtn.setOnAction(e -> refreshInvoices());
        FontIcon invErrIcon = new FontIcon("fas-exclamation-circle");
        invErrIcon.setIconSize(36);
        invErrIcon.setStyle("-fx-icon-color: -color-danger;");
        invErrorBox = new VBox(10, invErrIcon, invErrorLbl, invRetryBtn);
        invErrorBox.setAlignment(Pos.CENTER);
        invErrorBox.setVisible(false);

        invTableContainer = new StackPane(invoiceTable, invSpinner, invEmptyState, invErrorBox);
        VBox.setVgrow(invTableContainer, Priority.ALWAYS);

        VBox invContent = new VBox(12, invToolbar, invTableContainer);
        invContent.setPadding(new Insets(12, 0, 0, 0));
        VBox.setVgrow(invTableContainer, Priority.ALWAYS);
        invTab.setContent(invContent);

        // ══════════════════════════════════════════════════════════════════════
        // TAB 2 — OVERDUE ACCOUNTS
        // ══════════════════════════════════════════════════════════════════════
        Tab ovdTab = new Tab("Overdue Accounts (v_overdue_invoices)");
        ovdTab.setClosable(false);

        buildOverdueTable();

        ovdSpinner = new ProgressIndicator();
        ovdSpinner.setPrefSize(48, 48);
        ovdSpinner.setVisible(false);

        ovdEmptyState = new EmptyState("No Overdue Invoices",
            "All accounts are current — no overdue invoices.", "fas-check-circle");
        ovdEmptyState.setVisible(false);

        ovdErrorLbl = new Label();
        ovdErrorLbl.setWrapText(true);
        ovdErrorLbl.getStyleClass().add("page-subtitle");
        Button ovdRetryBtn = new Button("Retry");
        ovdRetryBtn.getStyleClass().add("btn-secondary");
        ovdRetryBtn.setOnAction(e -> refreshOverdue());
        FontIcon ovdErrIcon = new FontIcon("fas-exclamation-circle");
        ovdErrIcon.setIconSize(36);
        ovdErrIcon.setStyle("-fx-icon-color: -color-danger;");
        ovdErrorBox = new VBox(10, ovdErrIcon, ovdErrorLbl, ovdRetryBtn);
        ovdErrorBox.setAlignment(Pos.CENTER);
        ovdErrorBox.setVisible(false);

        ovdTableContainer = new StackPane(overdueTable, ovdSpinner, ovdEmptyState, ovdErrorBox);
        VBox.setVgrow(ovdTableContainer, Priority.ALWAYS);

        VBox ovdContent = new VBox(12, ovdTableContainer);
        ovdContent.setPadding(new Insets(12, 0, 0, 0));
        VBox.setVgrow(ovdTableContainer, Priority.ALWAYS);
        ovdTab.setContent(ovdContent);

        // ══════════════════════════════════════════════════════════════════════
        // Assemble
        // ══════════════════════════════════════════════════════════════════════
        tabPane = new TabPane(invTab, ovdTab);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        getChildren().addAll(header, tabPane);

        // ── Listeners ─────────────────────────────────────────────────────────
        PauseTransition debounce = new PauseTransition(Duration.millis(300));
        searchField.textProperty().addListener((obs, old, val) -> {
            debounce.setOnFinished(e -> refreshInvoices());
            debounce.playFromStart();
        });
        statusFilter.valueProperty().addListener((obs, old, val) -> refreshInvoices());

        invoiceTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            selectedInvoice = sel;
            updateRecordPayBtn();
        });

        refreshInvoices();
        refreshOverdue();
    }

    // ── Invoice table columns ─────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private void buildInvoiceTable() {
        TableColumn<InvoiceRow, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(r -> new SimpleObjectProperty<>(r.getValue().invoiceId()));
        idCol.setPrefWidth(60);

        TableColumn<InvoiceRow, String> custCol = new TableColumn<>("Customer");
        custCol.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().customerName()));
        custCol.setPrefWidth(170);

        TableColumn<InvoiceRow, String> planCol = new TableColumn<>("Plan");
        planCol.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().planName()));
        planCol.setPrefWidth(140);

        TableColumn<InvoiceRow, String> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(r -> new SimpleStringProperty(
            Formatters.formatCurrency(r.getValue().totalAmount())));
        totalCol.setPrefWidth(100);

        TableColumn<InvoiceRow, String> paidCol = new TableColumn<>("Paid");
        paidCol.setCellValueFactory(r -> new SimpleStringProperty(
            Formatters.formatCurrency(r.getValue().amountPaid())));
        paidCol.setPrefWidth(100);

        TableColumn<InvoiceRow, String> outCol = new TableColumn<>("Outstanding");
        outCol.setCellValueFactory(r -> new SimpleStringProperty(
            Formatters.formatCurrency(r.getValue().outstanding())));
        outCol.setPrefWidth(110);

        TableColumn<InvoiceRow, String> dueCol = new TableColumn<>("Due Date");
        dueCol.setCellValueFactory(r -> new SimpleStringProperty(
            Formatters.formatDate(r.getValue().dueDate())));
        dueCol.setPrefWidth(100);

        TableColumn<InvoiceRow, StatusBadge> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(r -> new SimpleObjectProperty<>(
            new StatusBadge(r.getValue().status())));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(StatusBadge badge, boolean empty) {
                super.updateItem(badge, empty);
                setGraphic(empty || badge == null ? null : badge);
            }
        });
        statusCol.setPrefWidth(120);

        invoiceTable.getColumns().addAll(
            idCol, custCol, planCol, totalCol, paidCol, outCol, dueCol, statusCol);
        invoiceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        invoiceTable.setItems(invoiceRows);
        invoiceTable.setPlaceholder(new Label(""));

        // Double-click → detail dialog
        invoiceTable.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2
                    && selectedInvoice != null) {
                openDetailDialog(selectedInvoice);
            }
        });
    }

    // ── Overdue table columns ─────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private void buildOverdueTable() {
        TableColumn<OverdueRow, Integer> idCol = new TableColumn<>("Invoice ID");
        idCol.setCellValueFactory(r -> new SimpleObjectProperty<>(r.getValue().invoiceId()));
        idCol.setPrefWidth(80);

        TableColumn<OverdueRow, String> custCol = new TableColumn<>("Customer");
        custCol.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().customerName()));
        custCol.setPrefWidth(160);

        TableColumn<OverdueRow, String> planCol = new TableColumn<>("Plan");
        planCol.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().planName()));
        planCol.setPrefWidth(130);

        TableColumn<OverdueRow, String> outCol = new TableColumn<>("Outstanding");
        outCol.setCellValueFactory(r -> new SimpleStringProperty(
            Formatters.formatCurrency(r.getValue().outstanding())));
        outCol.setPrefWidth(110);

        TableColumn<OverdueRow, String> dueCol = new TableColumn<>("Due Date");
        dueCol.setCellValueFactory(r -> new SimpleStringProperty(
            Formatters.formatDate(r.getValue().dueDate())));
        dueCol.setPrefWidth(100);

        TableColumn<OverdueRow, Integer> daysCol = new TableColumn<>("Days Overdue");
        daysCol.setCellValueFactory(r -> new SimpleObjectProperty<>(r.getValue().daysOverdue()));
        daysCol.setPrefWidth(100);
        daysCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(String.valueOf(v));
                setStyle(v > 60 ? "-fx-text-fill: #DC2626; -fx-font-weight:bold;"
                               : "-fx-text-fill: #D97706;");
            }
        });

        TableColumn<OverdueRow, StatusBadge> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(r -> new SimpleObjectProperty<>(
            new StatusBadge(r.getValue().status())));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(StatusBadge badge, boolean empty) {
                super.updateItem(badge, empty);
                setGraphic(empty || badge == null ? null : badge);
            }
        });
        statusCol.setPrefWidth(110);

        overdueTable.getColumns().addAll(idCol, custCol, planCol, outCol, dueCol, daysCol, statusCol);
        overdueTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        overdueTable.setItems(overdueRows);
        overdueTable.setPlaceholder(new Label(""));

        // Double-click → open payment dialog from overdue row (if canWrite)
        overdueTable.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && canWrite) {
                OverdueRow row = overdueTable.getSelectionModel().getSelectedItem();
                if (row != null) openPaymentDialogById(row.invoiceId(), row.outstanding());
            }
        });
    }

    // ── Update Record Payment button state ────────────────────────────────────
    private void updateRecordPayBtn() {
        if (!canWrite) { recordPayBtn.setDisable(true); return; }
        if (selectedInvoice == null) { recordPayBtn.setDisable(true); return; }
        String s = selectedInvoice.status();
        recordPayBtn.setDisable("PAID".equals(s) || "CANCELLED".equals(s));
    }

    // ── Async refresh — invoices ──────────────────────────────────────────────
    private void refreshInvoices() {
        String text   = searchField.getText();
        String status = statusFilter.getValue();
        showInvLoading(true);

        Task<List<InvoiceRow>> task = new Task<>() {
            @Override protected List<InvoiceRow> call() throws Exception {
                return BillingDao.searchInvoices(text, status);
            }
        };
        task.setOnSucceeded(e -> {
            showInvLoading(false);
            List<InvoiceRow> rows = task.getValue();
            invoiceRows.setAll(rows);
            invoiceTable.setVisible(!rows.isEmpty());
            invEmptyState.setVisible(rows.isEmpty());
            invErrorBox.setVisible(false);
            selectedInvoice = null;
            updateRecordPayBtn();
        });
        task.setOnFailed(e -> {
            showInvLoading(false);
            invoiceTable.setVisible(false);
            invEmptyState.setVisible(false);
            invErrorBox.setVisible(true);
            invErrorLbl.setText("Failed to load invoices: " + task.getException().getMessage());
        });
        EXECUTOR.submit(task);
    }

    private void showInvLoading(boolean on) {
        invSpinner.setVisible(on);
        if (on) { invoiceTable.setVisible(false); invEmptyState.setVisible(false); invErrorBox.setVisible(false); }
    }

    // ── Async refresh — overdue ───────────────────────────────────────────────
    private void refreshOverdue() {
        showOvdLoading(true);
        Task<List<OverdueRow>> task = new Task<>() {
            @Override protected List<OverdueRow> call() throws Exception {
                return BillingDao.loadOverdue();
            }
        };
        task.setOnSucceeded(e -> {
            showOvdLoading(false);
            List<OverdueRow> rows = task.getValue();
            overdueRows.setAll(rows);
            overdueTable.setVisible(!rows.isEmpty());
            ovdEmptyState.setVisible(rows.isEmpty());
            ovdErrorBox.setVisible(false);
        });
        task.setOnFailed(e -> {
            showOvdLoading(false);
            overdueTable.setVisible(false);
            ovdEmptyState.setVisible(false);
            ovdErrorBox.setVisible(true);
            ovdErrorLbl.setText("Failed to load overdue: " + task.getException().getMessage());
        });
        EXECUTOR.submit(task);
    }

    private void showOvdLoading(boolean on) {
        ovdSpinner.setVisible(on);
        if (on) { overdueTable.setVisible(false); ovdEmptyState.setVisible(false); ovdErrorBox.setVisible(false); }
    }

    private void refreshBoth() {
        refreshInvoices();
        refreshOverdue();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DIALOG: Generate Monthly Invoices
    // ══════════════════════════════════════════════════════════════════════════
    private void openGenerateDialog() {
        LocalDate now = LocalDate.now();
        Dialog<Boolean> d = buildDialog("Generate Monthly Invoices");

        VBox content = new VBox(16);
        content.getStyleClass().add("card");
        content.setPrefWidth(420);

        Label h = new Label("Execute Monthly Invoicing");
        h.setStyle("-fx-font-size:16px; -fx-font-weight:bold;");

        Label desc = new Label(
            "Calls PKG_BILLING.sp_generate_monthly_invoices — generates ISSUED invoices " +
            "with 18% GST for all ACTIVE subscriptions not yet billed this month.");
        desc.setWrapText(true);
        desc.getStyleClass().add("page-subtitle");

        TextField yrField = new TextField(String.valueOf(now.getYear()));
        TextField moField = new TextField(String.valueOf(now.getMonthValue()));
        Label yrLbl = new Label("Billing Year"); yrLbl.getStyleClass().add("form-label");
        Label moLbl = new Label("Billing Month (1-12)"); moLbl.getStyleClass().add("form-label");

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> { d.setResult(false); d.close(); });
        Button runBtn = new Button("Generate");
        runBtn.getStyleClass().add("btn-primary");
        btnRow.getChildren().addAll(cancelBtn, runBtn);

        content.getChildren().addAll(h, desc, yrLbl, yrField, moLbl, moField, btnRow);
        d.getDialogPane().setContent(content);
        applyCss(d);

        runBtn.setOnAction(e -> {
            int y, m;
            try {
                y = Integer.parseInt(yrField.getText().trim());
                m = Integer.parseInt(moField.getText().trim());
                if (m < 1 || m > 12) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                ToastManager.showError("Enter a valid year and month (1-12).");
                return;
            }
            runBtn.setDisable(true);
            Task<Integer> task = new Task<>() {
                @Override protected Integer call() throws Exception {
                    return BillingDao.generateMonthly(y, m);
                }
            };
            task.setOnSucceeded(ev -> {
                d.close();
                ToastManager.showSuccess("Generated " + task.getValue() + " invoice(s) for " + y + "-" + String.format("%02d", m) + ".");
                refreshBoth();
            });
            task.setOnFailed(ev -> {
                runBtn.setDisable(false);
                Throwable ex = task.getException();
                ToastManager.showError(ex instanceof SecurityException
                    ? "Permission denied: " + ex.getMessage()
                    : ex.getMessage());
            });
            EXECUTOR.submit(task);
        });

        d.showAndWait();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DIALOG: Suspend Overdue Accounts (with preview count)
    // ══════════════════════════════════════════════════════════════════════════
    private void openSuspendDialog() {
        // First, count how many accounts would be affected (off FX thread)
        Task<Integer> countTask = new Task<>() {
            @Override protected Integer call() throws Exception {
                return BillingDao.previewSuspendCount();
            }
        };
        countTask.setOnSucceeded(e -> {
            int count = countTask.getValue();
            if (count == 0) {
                ToastManager.showInfo("No accounts qualify for suspension right now " +
                    "(no subscriptions have invoices overdue > 30 days).");
                return;
            }
            ConfirmDialog confirm = new ConfirmDialog(
                "Suspend Overdue Accounts",
                count + " active subscription(s) will be suspended because they have " +
                "OVERDUE invoices past 30 days.\n\n" +
                "This calls PKG_BILLING.sp_suspend_overdue_accounts. Proceed?",
                "Suspend " + count + " Account(s)",
                true
            );
            confirm.showAndWait().ifPresent(confirmed -> {
                if (!confirmed) return;
                Task<Integer> suspTask = new Task<>() {
                    @Override protected Integer call() throws Exception {
                        return BillingDao.suspendOverdue();
                    }
                };
                suspTask.setOnSucceeded(ev -> {
                    ToastManager.showSuccess(suspTask.getValue() + " account(s) suspended.");
                    refreshBoth();
                });
                suspTask.setOnFailed(ev -> {
                    Throwable ex = suspTask.getException();
                    ToastManager.showError(ex instanceof SecurityException
                        ? "Permission denied: " + ex.getMessage()
                        : ex.getMessage());
                });
                EXECUTOR.submit(suspTask);
            });
        });
        countTask.setOnFailed(e ->
            ToastManager.showError("Could not compute preview: " + countTask.getException().getMessage()));
        EXECUTOR.submit(countTask);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DIALOG: Invoice Detail (double-click on All Invoices row)
    // ══════════════════════════════════════════════════════════════════════════
    private void openDetailDialog(InvoiceRow row) {
        Task<InvoiceDetail> task = new Task<>() {
            @Override protected InvoiceDetail call() throws Exception {
                return BillingDao.findInvoice(row.invoiceId());
            }
        };
        task.setOnSucceeded(e -> {
            InvoiceDetail d = task.getValue();
            if (d == null) { ToastManager.showError("Invoice #" + row.invoiceId() + " not found."); return; }
            showDetailDialog(d);
        });
        task.setOnFailed(e ->
            ToastManager.showError("Load failed: " + task.getException().getMessage()));
        EXECUTOR.submit(task);
    }

    @SuppressWarnings("unchecked")
    private void showDetailDialog(InvoiceDetail d) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Invoice #" + d.invoiceId() + " — Detail");
        dlg.setHeaderText(null);
        dlg.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Node bar = dlg.getDialogPane().lookup(".button-bar");
        if (bar != null) { bar.setVisible(false); bar.setManaged(false); }
        dlg.getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode() == KeyCode.ESCAPE) { dlg.close(); }
        });

        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setPrefWidth(560);

        // Header card
        VBox headerCard = new VBox(6);
        headerCard.getStyleClass().add("card");
        headerCard.setPadding(new Insets(12));

        Label invTitle = new Label("Invoice #" + d.invoiceId());
        invTitle.setStyle("-fx-font-size:16px; -fx-font-weight:bold;");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(6);
        addDetailRow(grid, 0, "Customer", d.customerName() + " <" + d.email() + ">");
        addDetailRow(grid, 1, "Plan", d.planName());
        addDetailRow(grid, 2, "Billing Period",
            Formatters.formatDate(d.billingPeriodStart()) + " → " + Formatters.formatDate(d.billingPeriodEnd()));
        addDetailRow(grid, 3, "Subtotal", Formatters.formatCurrency(d.subtotal()));
        addDetailRow(grid, 4, "Tax (18% GST)", Formatters.formatCurrency(d.taxAmount()));
        if (d.lateFee() > 0)
            addDetailRow(grid, 5, "Late Fee (2%/mo)", Formatters.formatCurrency(d.lateFee()));
        if (d.discount() > 0)
            addDetailRow(grid, 6, "Discount", "-" + Formatters.formatCurrency(d.discount()));
        addDetailRow(grid, 7, "Total Amount", Formatters.formatCurrency(d.totalAmount()));
        addDetailRow(grid, 8, "Amount Paid", Formatters.formatCurrency(d.amountPaid()));
        // Outstanding: build styled label and pass it as a Node so addDetailRow adds it ONCE at row 9
        Label outLbl = new Label(Formatters.formatCurrency(d.outstanding()));
        outLbl.setStyle(d.outstanding() > 0
            ? "-fx-text-fill:#DC2626; -fx-font-weight:bold;"
            : "-fx-text-fill:#16A34A; -fx-font-weight:bold;");
        addDetailRow(grid, 9, "Outstanding", outLbl);
        addDetailRow(grid, 10, "Due Date", Formatters.formatDate(d.dueDate()));
        addDetailRow(grid, 11, "Status", new StatusBadge(d.status()));

        headerCard.getChildren().addAll(invTitle, grid);

        // Payment history
        Label payTitle = new Label("Payment History");
        payTitle.setStyle("-fx-font-size:14px; -fx-font-weight:bold;");

        TableView<PaymentRow> payTable = new TableView<>();
        payTable.setPrefHeight(160);
        payTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<PaymentRow, Integer> pidCol = new TableColumn<>("Pay #");
        pidCol.setCellValueFactory(r -> new SimpleObjectProperty<>(r.getValue().paymentId()));
        pidCol.setPrefWidth(60);

        TableColumn<PaymentRow, String> pamtCol = new TableColumn<>("Amount");
        pamtCol.setCellValueFactory(r -> new SimpleStringProperty(Formatters.formatCurrency(r.getValue().amount())));

        TableColumn<PaymentRow, String> pmthCol = new TableColumn<>("Method");
        pmthCol.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().method()));

        TableColumn<PaymentRow, String> prefCol = new TableColumn<>("Reference");
        prefCol.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().transactionRef()));

        TableColumn<PaymentRow, String> pdtCol = new TableColumn<>("Date");
        pdtCol.setCellValueFactory(r -> new SimpleStringProperty(Formatters.formatDate(r.getValue().paymentDate())));

        payTable.getColumns().addAll(pidCol, pamtCol, pmthCol, prefCol, pdtCol);

        HBox closeRow = new HBox();
        closeRow.setAlignment(Pos.CENTER_RIGHT);
        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("btn-secondary");
        closeBtn.setOnAction(e -> dlg.close());
        closeRow.getChildren().add(closeBtn);

        root.getChildren().addAll(headerCard, payTitle, payTable, closeRow);
        dlg.getDialogPane().setContent(root);
        applyCss(dlg);

        // Load payments async
        Task<List<PaymentRow>> payTask = new Task<>() {
            @Override protected List<PaymentRow> call() throws Exception {
                return BillingDao.loadPayments(d.invoiceId());
            }
        };
        payTask.setOnSucceeded(e -> {
            List<PaymentRow> rows = payTask.getValue();
            payTable.getItems().setAll(rows);
            if (rows.isEmpty()) payTable.setPlaceholder(new Label("No payments recorded."));
        });
        payTask.setOnFailed(e ->
            payTable.setPlaceholder(new Label("Failed to load payments: " + payTask.getException().getMessage())));
        EXECUTOR.submit(payTask);

        dlg.showAndWait();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DIALOG: Record Payment
    // ══════════════════════════════════════════════════════════════════════════
    /** Open from selected row in All Invoices tab */
    private void openPaymentDialog(InvoiceRow row) {
        if (row == null) return;
        openPaymentDialogById(row.invoiceId(), row.outstanding());
    }

    /** Open from double-click on Overdue row (loads detail first) */
    private void openPaymentDialogById(int invoiceId, double knownOutstanding) {
        // Load full detail to get current balance
        Task<InvoiceDetail> task = new Task<>() {
            @Override protected InvoiceDetail call() throws Exception {
                return BillingDao.findInvoice(invoiceId);
            }
        };
        task.setOnSucceeded(e -> {
            InvoiceDetail detail = task.getValue();
            if (detail == null) { ToastManager.showError("Invoice #" + invoiceId + " not found."); return; }
            if ("PAID".equals(detail.status()) || "CANCELLED".equals(detail.status())) {
                ToastManager.showInfo("Invoice #" + invoiceId + " is already " + detail.status() + ".");
                return;
            }
            showPaymentDialog(detail);
        });
        task.setOnFailed(e -> ToastManager.showError("Load failed: " + task.getException().getMessage()));
        EXECUTOR.submit(task);
    }

    private void showPaymentDialog(InvoiceDetail detail) {
        Dialog<Boolean> d = buildDialog("Record Payment — Invoice #" + detail.invoiceId());

        VBox content = new VBox(14);
        content.getStyleClass().add("card");
        content.setPrefWidth(480);

        Label h = new Label("Invoice #" + detail.invoiceId() + " — " + detail.customerName());
        h.setStyle("-fx-font-size:15px; -fx-font-weight:bold;");

        // Live balance preview card (pre-filled from loaded detail)
        VBox previewCard = new VBox(4);
        previewCard.setStyle("-fx-background-color:-color-surface-hover; -fx-padding:10; -fx-background-radius:8;");
        Label previewTotal = new Label("Total: " + Formatters.formatCurrency(detail.totalAmount()));
        Label previewPaid  = new Label("Paid:  " + Formatters.formatCurrency(detail.amountPaid()));
        Label previewOut   = new Label("Outstanding: " + Formatters.formatCurrency(detail.outstanding()));
        previewOut.setStyle("-fx-font-weight:bold; -fx-text-fill:-color-danger;");
        previewCard.getChildren().addAll(previewTotal, previewPaid, previewOut);

        // Amount field
        Label amtLbl = new Label("Payment Amount (₹) *");
        amtLbl.getStyleClass().add("form-label");
        TextField amtField = new TextField();
        amtField.setPromptText("Max: " + String.format("%.2f", detail.outstanding()));

        // Payment method chips
        Label methodLbl = new Label("Payment Method *");
        methodLbl.getStyleClass().add("form-label");
        HBox methodBox = new HBox(8);
        ToggleGroup tg = new ToggleGroup();
        for (String m : BillingDao.PAYMENT_METHODS) {
            ToggleButton tb = new ToggleButton(m);
            tb.setToggleGroup(tg);
            tb.getStyleClass().add("btn-secondary");
            if ("UPI".equals(m)) tb.setSelected(true);
            methodBox.getChildren().add(tb);
        }

        // Reference field
        Label refLbl = new Label("Transaction Reference");
        refLbl.getStyleClass().add("form-label");
        TextField refField = new TextField("TXN-" + System.currentTimeMillis() % 100000);

        // Ref required notice
        Label refNotice = new Label("Required for UPI, CARD, BANK_TRANSFER, CHEQUE.");
        refNotice.setStyle("-fx-font-size:11px; -fx-text-fill:-color-muted;");

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> { d.setResult(false); d.close(); });
        Button submitBtn = new Button("Submit Payment");
        submitBtn.getStyleClass().add("btn-primary");
        btnRow.getChildren().addAll(cancelBtn, submitBtn);

        content.getChildren().addAll(h, previewCard, amtLbl, amtField,
            methodLbl, methodBox, refLbl, refField, refNotice, btnRow);
        d.getDialogPane().setContent(content);
        applyCss(d);

        submitBtn.setOnAction(e -> {
            // ── Validation ────────────────────────────────────────────────────
            double amt;
            try {
                amt = Double.parseDouble(amtField.getText().trim());
            } catch (NumberFormatException ex) {
                ToastManager.showError("Enter a valid payment amount.");
                return;
            }
            if (amt <= 0) { ToastManager.showError("Amount must be greater than zero."); return; }
            if (amt > detail.outstanding() + 0.005) {
                ToastManager.showError("Amount exceeds outstanding balance of "
                    + Formatters.formatCurrency(detail.outstanding()) + ".");
                return;
            }

            ToggleButton sel = (ToggleButton) tg.getSelectedToggle();
            if (sel == null) { ToastManager.showError("Select a payment method."); return; }
            String method = sel.getText();

            String ref = refField.getText().trim();
            // Reference required for non-cash methods
            if (ref.isEmpty() && !("CASH".equals(method))) {
                ToastManager.showError("Transaction reference is required for " + method + ".");
                return;
            }

            submitBtn.setDisable(true);
            final double finalAmt = amt;
            final String finalMethod = method;
            final String finalRef = ref;

            Task<String> task = new Task<>() {
                @Override protected String call() throws Exception {
                    return BillingDao.recordPayment(
                        detail.invoiceId(), finalAmt, finalMethod, finalRef, detail.outstanding());
                }
            };
            // Capture receipt values NOW, before the dialog closes and fields are cleared
            final String rcptCustomer = detail.customerName();
            final int    rcptInvId    = detail.invoiceId();
            final double rcptBalance  = Math.max(0, detail.outstanding() - finalAmt);
            final String rcptDate     = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));

            task.setOnSucceeded(ev -> {
                String resultMsg = task.getValue();
                d.setResult(true);
                d.close();
                ToastManager.showSuccess("Payment recorded.");
                // Open receipt dialog on next FX pulse, after payment dialog has fully closed
                Platform.runLater(() -> showReceiptDialog(
                    rcptCustomer, rcptInvId, finalAmt, finalMethod, finalRef,
                    rcptDate, rcptBalance));
                refreshBoth();
            });
            task.setOnFailed(ev -> {
                submitBtn.setDisable(false);
                Throwable ex = task.getException();
                ToastManager.showError(ex instanceof SecurityException
                    ? "Permission denied: " + ex.getMessage()
                    : ex.getMessage());
            });
            EXECUTOR.submit(task);
        });

        d.showAndWait();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Receipt Dialog (shown after successful payment)
    // ══════════════════════════════════════════════════════════════════════════
    private void showReceiptDialog(String customer, int invoiceId,
                                   double amount, String method, String ref,
                                   String date, double balanceLeft) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Payment Receipt");
        dlg.setHeaderText(null);
        dlg.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        // ── Content ───────────────────────────────────────────────────────────
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));
        vbox.setPrefWidth(420);

        Label header = new Label("\u2705 Payment Successful");
        header.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:-color-success;");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(6);
        addDetailRow(grid, 0, "Customer",    customer);
        addDetailRow(grid, 1, "Invoice #",   String.valueOf(invoiceId));
        addDetailRow(grid, 2, "Amount Paid", Formatters.formatCurrency(amount));
        addDetailRow(grid, 3, "Method",      method);
        addDetailRow(grid, 4, "Reference",   ref.isEmpty() ? "N/A" : ref);
        addDetailRow(grid, 5, "Date",        date);
        Label balLbl = new Label(Formatters.formatCurrency(balanceLeft));
        balLbl.setStyle(balanceLeft > 0
            ? "-fx-text-fill:#DC2626; -fx-font-weight:bold;"
            : "-fx-text-fill:#16A34A; -fx-font-weight:bold;");
        addDetailRow(grid, 6, "Balance Left", balLbl);

        vbox.getChildren().addAll(header, new Separator(), grid);

        // ── Buttons ───────────────────────────────────────────────────────────
        ButtonType saveReceiptType = new ButtonType("Save Receipt", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(saveReceiptType, ButtonType.CLOSE);
        dlg.getDialogPane().setContent(vbox);
        dlg.getDialogPane().getStylesheets().add(
            getClass().getResource("/css/isp-theme.css").toExternalForm());

        // "Save Receipt" button handler
        Button saveBtn = (Button) dlg.getDialogPane().lookupButton(saveReceiptType);
        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            ev.consume(); // prevent dialog from closing automatically
            FileChooser fc = new FileChooser();
            fc.setTitle("Save Payment Receipt");
            fc.setInitialFileName("receipt_inv" + invoiceId + "_" + LocalDate.now() + ".txt");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
            Stage stage = (Stage) dlg.getDialogPane().getScene().getWindow();
            File file = fc.showSaveDialog(stage);
            if (file == null) return;
            try (PrintWriter pw = new PrintWriter(file)) {
                String line = "=".repeat(50);
                pw.println(line);
                pw.println("         ISP ORBIT \u2014 PAYMENT RECEIPT");
                pw.println(line);
                pw.println("Date         : " + date);
                pw.println("Invoice #    : " + invoiceId);
                pw.println("Customer     : " + customer);
                pw.println(line);
                pw.println("Amount Paid  : " + Formatters.formatCurrency(amount));
                pw.println("Method       : " + method);
                pw.println("Ref/UTR      : " + (ref.isEmpty() ? "N/A" : ref));
                pw.println("Balance Left : " + Formatters.formatCurrency(balanceLeft));
                pw.println(line);
                pw.println("Thank you for your payment.");
                ToastManager.showSuccess("Receipt saved: " + file.getName());
            } catch (Exception ex) {
                ToastManager.showError("Could not save receipt: " + ex.getMessage());
            }
        });

        dlg.showAndWait();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Dialog<Boolean> buildDialog(String title) {
        Dialog<Boolean> d = new Dialog<>();
        d.setTitle(title);
        d.setHeaderText(null);
        d.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Node bar = d.getDialogPane().lookup(".button-bar");
        if (bar != null) { bar.setVisible(false); bar.setManaged(false); }
        d.getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode() == KeyCode.ESCAPE) { d.setResult(false); d.close(); }
        });
        return d;
    }

    private void applyCss(Dialog<?> d) {
        d.getDialogPane().getStylesheets().add(
            getClass().getResource("/css/isp-theme.css").toExternalForm());
    }

    private static void addDetailRow(GridPane grid, int row, String label, Object value) {
        Label k = new Label(label + ":");
        k.setStyle("-fx-font-weight:bold; -fx-text-fill:-color-text-secondary;");
        k.setMinWidth(130);
        grid.add(k, 0, row);
        if (value instanceof Node n) {
            grid.add(n, 1, row);
        } else {
            Label v = new Label(value != null ? value.toString() : "-");
            v.setStyle("-fx-text-fill:-color-text;");
            grid.add(v, 1, row);
        }
    }
}
