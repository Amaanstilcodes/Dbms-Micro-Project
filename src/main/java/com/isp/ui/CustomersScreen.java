package com.isp.ui;

import com.isp.Db;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class CustomersScreen extends VBox {
    private final StackPane tableContainer;
    private final TextField searchField;

    public CustomersScreen() {
        setSpacing(16);
        setPadding(new Insets(20));

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Customers");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        searchField = new TextField();
        searchField.setPromptText("Search by name, email, phone...");
        searchField.setPrefWidth(260);

        Button searchBtn = new Button("Search");
        searchBtn.setOnAction(e -> refreshTable());

        Button addBtn = new Button("+ Add Customer");
        addBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold;");
        addBtn.setOnAction(e -> openAddDialog());

        Button editBtn = new Button("Edit Status / Phone");
        editBtn.setOnAction(e -> openEditDialog());

        Button deactivateBtn = new Button("Deactivate");
        deactivateBtn.setStyle("-fx-text-fill: #dc2626;");
        deactivateBtn.setOnAction(e -> openDeactivateDialog());

        header.getChildren().addAll(title, spacer, searchField, searchBtn, addBtn, editBtn, deactivateBtn);

        tableContainer = new StackPane();
        VBox.setVgrow(tableContainer, Priority.ALWAYS);

        getChildren().addAll(header, tableContainer);
        refreshTable();
    }

    private void refreshTable() {
        String q = searchField.getText().trim();
        String sql;
        StackPane table;
        if (q.isEmpty()) {
            sql = "SELECT customer_id, customer_type, first_name || ' ' || last_name AS name, company_name, email, phone, status, created_at FROM customer ORDER BY customer_id DESC";
            table = TableViewBuilder.buildAsyncTable(sql);
        } else {
            sql = "SELECT customer_id, customer_type, first_name || ' ' || last_name AS name, company_name, email, phone, status, created_at FROM customer " +
                  "WHERE LOWER(first_name) LIKE ? OR LOWER(last_name) LIKE ? OR LOWER(email) LIKE ? OR phone LIKE ? ORDER BY customer_id DESC";
            String pattern = "%" + q.toLowerCase() + "%";
            table = TableViewBuilder.buildAsyncTable(sql, pattern, pattern, pattern, "%" + q + "%");
        }
        tableContainer.getChildren().setAll(table);
    }

    private void openAddDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Add New Customer");
        dialog.setHeaderText("Enter Customer Details");

        ButtonType saveBtnType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("INDIVIDUAL", "BUSINESS");
        typeBox.setValue("INDIVIDUAL");

        TextField fName = new TextField();
        TextField lName = new TextField();
        TextField comp = new TextField();
        TextField email = new TextField();
        TextField phone = new TextField();
        TextField areaId = new TextField("1");

        grid.add(new Label("Customer Type:"), 0, 0); grid.add(typeBox, 1, 0);
        grid.add(new Label("First Name:"), 0, 1);    grid.add(fName, 1, 1);
        grid.add(new Label("Last Name:"), 0, 2);     grid.add(lName, 1, 2);
        grid.add(new Label("Company Name:"), 0, 3);  grid.add(comp, 1, 3);
        grid.add(new Label("Email:"), 0, 4);         grid.add(email, 1, 4);
        grid.add(new Label("Phone:"), 0, 5);         grid.add(phone, 1, 5);
        grid.add(new Label("Service Area ID:"), 0, 6);grid.add(areaId, 1, 6);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveBtnType) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        String sql = "INSERT INTO customer (customer_type, first_name, last_name, company_name, email, phone, area_id) " +
                                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
                        try (Connection conn = Db.getConnection();
                             PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setString(1, typeBox.getValue());
                            ps.setString(2, fName.getText().trim());
                            ps.setString(3, lName.getText().trim());
                            ps.setString(4, comp.getText().trim().isEmpty() ? null : comp.getText().trim());
                            ps.setString(5, email.getText().trim());
                            ps.setString(6, phone.getText().trim());
                            ps.setInt(7, Integer.parseInt(areaId.getText().trim()));
                            ps.executeUpdate();
                        }
                        return null;
                    }
                };
                task.setOnSucceeded(ev -> {
                    TableViewBuilder.showInfo("Success", "Customer created successfully!");
                    refreshTable();
                });
                task.setOnFailed(ev -> TableViewBuilder.showError("Customer Creation Failed", task.getException().getMessage()));
                new Thread(task).start();
                return true;
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void openEditDialog() {
        TextInputDialog idDialog = new TextInputDialog();
        idDialog.setTitle("Edit Customer");
        idDialog.setHeaderText("Enter Customer ID to update");
        idDialog.setContentText("Customer ID:");
        idDialog.showAndWait().ifPresent(idStr -> {
            try {
                int cid = Integer.parseInt(idStr.trim());
                Dialog<Void> d = new Dialog<>();
                d.setTitle("Edit Customer #" + cid);
                ButtonType updateBtn = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
                d.getDialogPane().getButtonTypes().addAll(updateBtn, ButtonType.CANCEL);

                GridPane grid = new GridPane();
                grid.setHgap(10); grid.setVgap(10);
                TextField phoneField = new TextField();
                ComboBox<String> statusBox = new ComboBox<>();
                statusBox.getItems().addAll("ACTIVE", "INACTIVE", "BLACKLISTED");
                statusBox.setValue("ACTIVE");

                grid.add(new Label("New Phone:"), 0, 0); grid.add(phoneField, 1, 0);
                grid.add(new Label("Status:"), 0, 1);    grid.add(statusBox, 1, 1);
                d.getDialogPane().setContent(grid);

                d.setResultConverter(bt -> {
                    if (bt == updateBtn) {
                        Task<Void> task = new Task<>() {
                            @Override
                            protected Void call() throws Exception {
                                String sql = "UPDATE customer SET phone = NVL(NULLIF(?,''), phone), status = ? WHERE customer_id = ?";
                                try (Connection conn = Db.getConnection();
                                     PreparedStatement ps = conn.prepareStatement(sql)) {
                                    ps.setString(1, phoneField.getText().trim());
                                    ps.setString(2, statusBox.getValue());
                                    ps.setInt(3, cid);
                                    ps.executeUpdate();
                                }
                                return null;
                            }
                        };
                        task.setOnSucceeded(ev -> {
                            TableViewBuilder.showInfo("Success", "Customer updated.");
                            refreshTable();
                        });
                        task.setOnFailed(ev -> TableViewBuilder.showError("Update Failed", task.getException().getMessage()));
                        new Thread(task).start();
                    }
                    return null;
                });
                d.showAndWait();
            } catch (Exception e) {
                TableViewBuilder.showError("Invalid Input", "Customer ID must be a number.");
            }
        });
    }

    private void openDeactivateDialog() {
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Deactivate Customer");
        d.setHeaderText("Soft-delete customer (marks status as INACTIVE)");
        d.setContentText("Enter Customer ID:");
        d.showAndWait().ifPresent(idStr -> {
            try {
                int cid = Integer.parseInt(idStr.trim());
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        String sql = "UPDATE customer SET status = 'INACTIVE' WHERE customer_id = ?";
                        try (Connection conn = Db.getConnection();
                             PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setInt(1, cid);
                            int rows = ps.executeUpdate();
                            if (rows == 0) throw new Exception("Customer " + cid + " not found.");
                        }
                        return null;
                    }
                };
                task.setOnSucceeded(e -> {
                    TableViewBuilder.showInfo("Deactivated", "Customer #" + cid + " marked INACTIVE.");
                    refreshTable();
                });
                task.setOnFailed(e -> TableViewBuilder.showError("Deactivation Failed", task.getException().getMessage()));
                new Thread(task).start();
            } catch (Exception e) {
                TableViewBuilder.showError("Invalid Input", "Please provide a valid Customer ID number.");
            }
        });
    }
}
