package com.isp.ui;

import com.isp.Db;
import com.isp.Session;
import com.isp.ui.components.ConfirmDialog;
import com.isp.ui.components.FormField;
import com.isp.ui.components.ToastManager;
import com.isp.ui.util.Formatters;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

public class BillingScreen extends VBox {
    private final TabPane tabPane;
    private final StackPane invoicesContainer;
    private final StackPane overdueContainer;
    private final ComboBox<String> statusFilter;

    public BillingScreen() {
        setSpacing(16);
        setPadding(new Insets(24));

        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("Billing & Revenue Engine");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("PL/SQL package procedures, 18% GST invoice generation, and live payments");
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button genInvoicesBtn = new Button("⚡ Generate Monthly Invoices");
        genInvoicesBtn.getStyleClass().add("btn-secondary");
        genInvoicesBtn.setOnAction(e -> openGenerateInvoicesDialog());

        Button suspendBtn = new Button("⚠ Suspend Overdue Accounts");
        suspendBtn.getStyleClass().add("btn-danger");
        suspendBtn.setOnAction(e -> openSuspendAccountsDialog());

        Button payBtn = new Button("💳 Record Payment");
        payBtn.getStyleClass().add("btn-primary");
        payBtn.setOnAction(e -> openRecordPaymentDialog());

        header.getChildren().addAll(titleBox, spacer, genInvoicesBtn, suspendBtn, payBtn);

        tabPane = new TabPane();
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // Tab 1: All Invoices
        Tab invTab = new Tab("All Invoices");
        invTab.setClosable(false);

        VBox invBox = new VBox(12);
        invBox.setPadding(new Insets(16, 0, 0, 0));

        HBox invToolbar = new HBox(12);
        invToolbar.getStyleClass().add("toolbar-card");
        invToolbar.setAlignment(Pos.CENTER_LEFT);

        statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("ALL", "ISSUED", "PAID", "PARTIALLY_PAID", "OVERDUE", "DRAFT");
        statusFilter.setValue("ALL");
        statusFilter.setOnAction(e -> refreshInvoices());

        invToolbar.getChildren().addAll(new Label("Filter Invoice Status:"), statusFilter);

        VBox tableCard = new VBox();
        tableCard.getStyleClass().add("card");
        tableCard.setFillWidth(true);
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        invoicesContainer = new StackPane();
        invoicesContainer.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(invoicesContainer, Priority.ALWAYS);
        tableCard.getChildren().add(invoicesContainer);

        invBox.getChildren().addAll(invToolbar, tableCard);
        invTab.setContent(invBox);

        // Tab 2: Overdue Invoices View
        Tab overdueTab = new Tab("Overdue Accounts (V_OVERDUE_INVOICES)");
        overdueTab.setClosable(false);

        VBox overdueBox = new VBox(12);
        overdueBox.setPadding(new Insets(16, 0, 0, 0));

        VBox overdueCard = new VBox();
        overdueCard.getStyleClass().add("card");
        overdueCard.setFillWidth(true);
        VBox.setVgrow(overdueCard, Priority.ALWAYS);

        overdueContainer = new StackPane();
        overdueContainer.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(overdueContainer, Priority.ALWAYS);
        overdueCard.getChildren().add(overdueContainer);

        overdueBox.getChildren().add(overdueCard);
        overdueTab.setContent(overdueBox);

        tabPane.getTabs().addAll(invTab, overdueTab);

        getChildren().addAll(header, tabPane);
        refreshInvoices();
        refreshOverdue();
    }

    private void refreshInvoices() {
        String filter = statusFilter.getValue();
        String sql;
        TableViewBuilder.TableContainer table;
        if ("ALL".equals(filter)) {
            sql = "SELECT invoice_id, subscription_id, billing_period_start, billing_period_end, total_amount, amount_paid, (total_amount - amount_paid) AS outstanding_balance, " +
                  "due_date, status FROM invoice ORDER BY invoice_id DESC";
            table = TableViewBuilder.buildAsyncTable(
                row -> ToastManager.showInfo("Invoice #" + row.get(0) + " - Status: " + row.get(8)),
                sql
            );
        } else {
            sql = "SELECT invoice_id, subscription_id, billing_period_start, billing_period_end, total_amount, amount_paid, (total_amount - amount_paid) AS outstanding_balance, " +
                  "due_date, status FROM invoice WHERE status = ? ORDER BY invoice_id DESC";
            table = TableViewBuilder.buildAsyncTable(
                row -> ToastManager.showInfo("Invoice #" + row.get(0) + " - Status: " + row.get(8)),
                sql, filter
            );
        }
        invoicesContainer.getChildren().setAll(table);
    }

    private void refreshOverdue() {
        String sql = "SELECT invoice_id, subscription_id, customer_name, email, phone, plan_name, total_amount, amount_paid, outstanding, due_date, days_overdue, status FROM v_overdue_invoices";
        TableViewBuilder.TableContainer table = TableViewBuilder.buildAsyncTable(sql);
        overdueContainer.getChildren().setAll(table);
    }

    private void openGenerateInvoicesDialog() {
        LocalDate now = LocalDate.now();
        Dialog<Boolean> d = new Dialog<>();
        d.setTitle("Generate Monthly Invoices");
        d.setHeaderText(null);

        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Node defaultBar = d.getDialogPane().lookup(".button-bar");
        if (defaultBar != null) {
            defaultBar.setVisible(false);
            defaultBar.setManaged(false);
        }

        d.getDialogPane().addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                d.setResult(false);
                d.close();
                event.consume();
            }
        });

        VBox content = new VBox(16);
        content.getStyleClass().add("card");
        content.setPrefWidth(420);

        Label title = new Label("Execute Monthly Invoicing");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label desc = new Label("Calls PKG_BILLING.sp_generate_monthly_invoices to generate billing statements with 18% GST tax.");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: -color-muted;");

        TextField yr = new TextField(String.valueOf(now.getYear()));
        FormField yrField = new FormField("Billing Cycle Year", yr, true);

        TextField mo = new TextField(String.valueOf(now.getMonthValue()));
        FormField moField = new FormField("Billing Cycle Month (1-12)", mo, true);

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> {
            d.setResult(false);
            d.close();
        });

        Button runBtn = new Button("Generate Invoices");
        runBtn.getStyleClass().add("btn-primary");

        btnRow.getChildren().addAll(cancelBtn, runBtn);
        content.getChildren().addAll(title, desc, yrField, moField, btnRow);

        d.getDialogPane().setContent(content);
        d.getDialogPane().getStylesheets().add(getClass().getResource("/css/isp-theme.css").toExternalForm());

        runBtn.setOnAction(e -> {
            try {
                int y = Integer.parseInt(yr.getText().trim());
                int m = Integer.parseInt(mo.getText().trim());

                runBtn.setDisable(true);
                Task<Integer> task = new Task<>() {
                    @Override
                    protected Integer call() throws Exception {
                        String call = "{call pkg_billing.sp_generate_monthly_invoices(?, ?, ?)}";
                        try (Connection conn = Db.getConnection();
                             CallableStatement cs = conn.prepareCall(call)) {
                            cs.setInt(1, y);
                            cs.setInt(2, m);
                            cs.registerOutParameter(3, java.sql.Types.NUMERIC);
                            cs.execute();
                            return cs.getInt(3);
                        }
                    }
                };

                task.setOnSucceeded(ev -> {
                    d.close();
                    int count = task.getValue();
                    ToastManager.showSuccess("Successfully generated " + count + " invoice(s) for " + y + "-" + m);
                    refreshInvoices();
                    refreshOverdue();
                });

                task.setOnFailed(ev -> {
                    runBtn.setDisable(false);
                    ToastManager.showError("Generation failed: " + TableViewBuilder.cleanOracleMessage(task.getException().getMessage()));
                });

                new Thread(task).start();
            } catch (Exception ex) {
                ToastManager.showError("Invalid year/month input.");
            }
        });

        d.showAndWait();
    }

    private void openSuspendAccountsDialog() {
        ConfirmDialog confirm = new ConfirmDialog(
            "Suspend Overdue Accounts",
            "This invokes PKG_BILLING.sp_suspend_overdue_accounts. All subscriptions with unpaid invoices overdue past 30 days will transition to SUSPENDED. Proceed?",
            "Execute Suspension",
            true
        );

        confirm.showAndWait().ifPresent(confirmed -> {
            if (confirmed) {
                Task<Integer> task = new Task<>() {
                    @Override
                    protected Integer call() throws Exception {
                        String call = "{call pkg_billing.sp_suspend_overdue_accounts(?)}";
                        try (Connection conn = Db.getConnection();
                             CallableStatement cs = conn.prepareCall(call)) {
                            cs.registerOutParameter(1, java.sql.Types.NUMERIC);
                            cs.execute();
                            return cs.getInt(1);
                        }
                    }
                };

                task.setOnSucceeded(e -> {
                    ToastManager.showSuccess("Suspension routine completed: " + task.getValue() + " account(s) suspended.");
                    refreshInvoices();
                    refreshOverdue();
                });

                task.setOnFailed(e -> ToastManager.showError("Error: " + task.getException().getMessage()));
                new Thread(task).start();
            }
        });
    }

    private void openRecordPaymentDialog() {
        Dialog<Boolean> d = new Dialog<>();
        d.setTitle("Record Invoice Payment");
        d.setHeaderText(null);

        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Node defaultBar = d.getDialogPane().lookup(".button-bar");
        if (defaultBar != null) {
            defaultBar.setVisible(false);
            defaultBar.setManaged(false);
        }

        d.getDialogPane().addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                d.setResult(false);
                d.close();
                event.consume();
            }
        });

        VBox content = new VBox(14);
        content.getStyleClass().add("card");
        content.setPrefWidth(460);

        Label title = new Label("Record Payment via PKG_BILLING");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        TextField invIdField = new TextField("7");
        FormField invIdForm = new FormField("Target Invoice ID", invIdField, true);

        // Live balance info card
        VBox previewCard = new VBox(4);
        previewCard.setStyle("-fx-background-color: -color-surface-hover; -fx-padding: 10; -fx-background-radius: 8;");
        Label previewTotal = new Label("Total: --");
        Label previewPaid = new Label("Paid: --");
        Label previewOutstanding = new Label("Outstanding: --");
        previewOutstanding.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-danger;");
        previewCard.getChildren().addAll(previewTotal, previewPaid, previewOutstanding);

        // Auto load invoice balance when ID changes
        Runnable updateBalancePreview = () -> {
            try {
                int id = Integer.parseInt(invIdField.getText().trim());
                Task<double[]> checkTask = new Task<>() {
                    @Override
                    protected double[] call() throws Exception {
                        String q = "SELECT total_amount, amount_paid FROM invoice WHERE invoice_id = ?";
                        try (Connection c = Db.getConnection();
                             PreparedStatement ps = c.prepareStatement(q)) {
                            ps.setInt(1, id);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    return new double[]{rs.getDouble(1), rs.getDouble(2)};
                                }
                            }
                        }
                        return null;
                    }
                };
                checkTask.setOnSucceeded(ev -> {
                    double[] res = checkTask.getValue();
                    if (res != null) {
                        previewTotal.setText("Total Invoice Amount: " + Formatters.formatCurrency(res[0]));
                        previewPaid.setText("Already Paid: " + Formatters.formatCurrency(res[1]));
                        previewOutstanding.setText("Outstanding Balance: " + Formatters.formatCurrency(res[0] - res[1]));
                    } else {
                        previewTotal.setText("Total: Not found");
                        previewPaid.setText("");
                        previewOutstanding.setText("");
                    }
                });
                new Thread(checkTask).start();
            } catch (Exception ignored) {}
        };

        invIdField.textProperty().addListener((o, ov, nv) -> updateBalancePreview.run());
        updateBalancePreview.run();

        TextField amountField = new TextField();
        FormField amountForm = new FormField("Payment Amount (₹)", amountField, true);

        // Payment Method Chips / Buttons
        Label methodLbl = new Label("Payment Method *");
        methodLbl.getStyleClass().add("form-label");
        HBox methodBox = new HBox(8);
        ToggleGroup tg = new ToggleGroup();
        String[] methods = {"UPI", "CARD", "CASH", "NET_BANKING"};
        for (String m : methods) {
            ToggleButton tb = new ToggleButton(m);
            tb.setToggleGroup(tg);
            tb.getStyleClass().add("btn-secondary");
            if ("UPI".equals(m)) tb.setSelected(true);
            methodBox.getChildren().add(tb);
        }

        TextField refField = new TextField("TXN-PAY-" + System.currentTimeMillis() % 100000);
        FormField refForm = new FormField("Transaction Reference / UTR", refField, true);

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> {
            d.setResult(false);
            d.close();
        });

        Button submitBtn = new Button("Submit Payment");
        submitBtn.getStyleClass().add("btn-primary");

        btnRow.getChildren().addAll(cancelBtn, submitBtn);

        content.getChildren().addAll(title, invIdForm, previewCard, amountForm, methodLbl, methodBox, refForm, btnRow);

        d.getDialogPane().setContent(content);
        d.getDialogPane().getStylesheets().add(getClass().getResource("/css/isp-theme.css").toExternalForm());

        submitBtn.setOnAction(e -> {
            try {
                int invId = Integer.parseInt(invIdField.getText().trim());
                double amt = Double.parseDouble(amountField.getText().trim());
                ToggleButton sel = (ToggleButton) tg.getSelectedToggle();
                String method = sel != null ? sel.getText() : "UPI";
                String ref = refField.getText().trim();

                submitBtn.setDisable(true);
                Task<String> task = new Task<>() {
                    @Override
                    protected String call() throws Exception {
                        String call = "{call pkg_billing.sp_record_payment(?, ?, ?, ?, ?, ?)}";
                        try (Connection conn = Db.getConnection();
                             CallableStatement cs = conn.prepareCall(call)) {
                            cs.setInt(1, invId);
                            cs.setDouble(2, amt);
                            cs.setString(3, method);
                            cs.setString(4, ref);
                            cs.setInt(5, Session.getUserId());
                            cs.registerOutParameter(6, java.sql.Types.VARCHAR);
                            cs.execute();
                            return cs.getString(6);
                        }
                    }
                };

                task.setOnSucceeded(ev -> {
                    d.close();
                    ToastManager.showSuccess("Payment Recorded: " + task.getValue());
                    refreshInvoices();
                    refreshOverdue();
                });

                task.setOnFailed(ev -> {
                    submitBtn.setDisable(false);
                    ToastManager.showError("Payment Failed: " + TableViewBuilder.cleanOracleMessage(task.getException().getMessage()));
                });

                new Thread(task).start();
            } catch (Exception ex) {
                ToastManager.showError("Please enter valid numeric invoice ID and payment amount.");
            }
        });

        d.showAndWait();
    }
}
