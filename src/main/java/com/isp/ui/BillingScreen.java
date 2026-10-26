package com.isp.ui;

import com.isp.Db;
import com.isp.Session;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.time.LocalDate;

public class BillingScreen extends VBox {
    private final StackPane tableContainer;
    private final ComboBox<String> statusFilter;

    public BillingScreen() {
        setSpacing(16);
        setPadding(new Insets(20));

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Billing & Invoices");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("ALL", "ISSUED", "PAID", "PARTIALLY_PAID", "OVERDUE", "DRAFT");
        statusFilter.setValue("ALL");
        statusFilter.setOnAction(e -> refreshTable());

        Button generateBtn = new Button("⚡ Generate Monthly Invoices");
        generateBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold;");
        generateBtn.setOnAction(e -> openGenerateInvoicesDialog());

        Button payBtn = new Button("💳 Record Payment");
        payBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold;");
        payBtn.setOnAction(e -> openRecordPaymentDialog());

        header.getChildren().addAll(title, spacer, new Label("Status:"), statusFilter, generateBtn, payBtn);

        tableContainer = new StackPane();
        VBox.setVgrow(tableContainer, Priority.ALWAYS);

        getChildren().addAll(header, tableContainer);
        refreshTable();
    }

    private void refreshTable() {
        String filter = statusFilter.getValue();
        String sql;
        StackPane table;
        if ("ALL".equals(filter)) {
            sql = "SELECT invoice_id, subscription_id, total_amount, amount_paid, (total_amount - amount_paid) AS balance, " +
                  "due_date, status, billing_period_start, billing_period_end FROM invoice ORDER BY invoice_id DESC";
            table = TableViewBuilder.buildAsyncTable(sql);
        } else {
            sql = "SELECT invoice_id, subscription_id, total_amount, amount_paid, (total_amount - amount_paid) AS balance, " +
                  "due_date, status, billing_period_start, billing_period_end FROM invoice WHERE status = ? ORDER BY invoice_id DESC";
            table = TableViewBuilder.buildAsyncTable(sql, filter);
        }
        tableContainer.getChildren().setAll(table);
    }

    private void openGenerateInvoicesDialog() {
        LocalDate now = LocalDate.now();
        Dialog<Boolean> d = new Dialog<>();
        d.setTitle("Generate Monthly Invoices");
        d.setHeaderText("Call PKG_BILLING.sp_generate_monthly_invoices");

        ButtonType runBtn = new ButtonType("Generate", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(runBtn, ButtonType.CANCEL);

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10);
        TextField yr = new TextField(String.valueOf(now.getYear()));
        TextField mo = new TextField(String.valueOf(now.getMonthValue()));

        g.add(new Label("Year:"), 0, 0);  g.add(yr, 1, 0);
        g.add(new Label("Month:"), 0, 1); g.add(mo, 1, 1);
        d.getDialogPane().setContent(g);

        d.setResultConverter(bt -> {
            if (bt == runBtn) {
                int y = Integer.parseInt(yr.getText().trim());
                int m = Integer.parseInt(mo.getText().trim());

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
                    int count = task.getValue();
                    TableViewBuilder.showInfo("Invoices Generated", "Created " + count + " new invoice(s) for " + y + "-" + m + ".");
                    refreshTable();
                });
                task.setOnFailed(ev -> TableViewBuilder.showError("Generation Failed", task.getException().getMessage()));

                new Thread(task).start();
                return true;
            }
            return null;
        });

        d.showAndWait();
    }

    private void openRecordPaymentDialog() {
        Dialog<Boolean> d = new Dialog<>();
        d.setTitle("Record Payment");
        d.setHeaderText("Calls PKG_BILLING.sp_record_payment (Updates amount_paid & status)");

        ButtonType payBtnType = new ButtonType("Submit Payment", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(payBtnType, ButtonType.CANCEL);

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10);
        g.setPadding(new Insets(20));

        TextField invField = new TextField();
        invField.setPromptText("e.g. 7, 8");
        TextField amtField = new TextField();
        amtField.setPromptText("e.g. 470.82");

        ComboBox<String> methodBox = new ComboBox<>();
        methodBox.getItems().addAll("UPI", "CARD", "BANK_TRANSFER", "CASH", "CHEQUE");
        methodBox.setValue("UPI");

        TextField refField = new TextField();
        refField.setPromptText("Transaction / Ref Number");

        g.add(new Label("Invoice ID:"), 0, 0);      g.add(invField, 1, 0);
        g.add(new Label("Payment Amount:"), 0, 1);  g.add(amtField, 1, 1);
        g.add(new Label("Payment Method:"), 0, 2);  g.add(methodBox, 1, 2);
        g.add(new Label("Transaction Ref:"), 0, 3); g.add(refField, 1, 3);

        d.getDialogPane().setContent(g);

        d.setResultConverter(bt -> {
            if (bt == payBtnType) {
                try {
                    int invoiceId = Integer.parseInt(invField.getText().trim());
                    double amount = Double.parseDouble(amtField.getText().trim());
                    String method = methodBox.getValue();
                    String ref = refField.getText().trim();
                    Integer userId = Session.getUserId();

                    Task<String> task = new Task<>() {
                        @Override
                        protected String call() throws Exception {
                            String call = "{call pkg_billing.sp_record_payment(?, ?, ?, ?, ?, ?)}";
                            try (Connection conn = Db.getConnection();
                                 CallableStatement cs = conn.prepareCall(call)) {
                                cs.setInt(1, invoiceId);
                                cs.setDouble(2, amount);
                                cs.setString(3, method);
                                cs.setString(4, ref.isEmpty() ? "REF-" + System.currentTimeMillis() : ref);
                                if (userId != null) cs.setInt(5, userId); else cs.setNull(5, java.sql.Types.NUMERIC);
                                cs.registerOutParameter(6, java.sql.Types.VARCHAR);
                                cs.execute();
                                return cs.getString(6);
                            }
                        }
                    };

                    task.setOnSucceeded(ev -> {
                        String msg = task.getValue();
                        TableViewBuilder.showInfo("Payment Processed", msg != null ? msg : "Payment recorded successfully.");
                        refreshTable();
                    });

                    task.setOnFailed(ev -> TableViewBuilder.showError("Payment Failed", task.getException().getMessage()));

                    new Thread(task).start();
                    return true;
                } catch (Exception ex) {
                    TableViewBuilder.showError("Invalid Input", "Please enter valid invoice ID and amount.");
                }
            }
            return null;
        });

        d.showAndWait();
    }
}
