package com.isp.ui;

import com.isp.Db;
import com.isp.ui.components.ConfirmDialog;
import com.isp.ui.components.FormField;
import com.isp.ui.components.ToastManager;
import javafx.animation.PauseTransition;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.function.Consumer;

public class CustomersScreen extends VBox {
    private final StackPane tableContainer;
    private final TextField searchField;
    private final ComboBox<String> statusFilter;
    private final ComboBox<String> typeFilter;

    public CustomersScreen() {
        setSpacing(16);
        setPadding(new Insets(24));

        // Header Row: Title & Action Button
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("Customer Directory");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Manage active broadband subscribers and corporate client accounts");
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Button addBtn = new Button("+ Add Customer");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> openAddDialog());

        header.getChildren().addAll(titleBox, headerSpacer, addBtn);

        // Toolbar Card: Search, Filters, Clear, Export
        HBox toolbar = new HBox(12);
        toolbar.getStyleClass().add("toolbar-card");
        toolbar.setAlignment(Pos.CENTER_LEFT);

        FontIcon searchIcon = new FontIcon("fas-search");
        searchIcon.setIconSize(13);
        searchIcon.setStyle("-fx-icon-color: -color-muted;");

        searchField = new TextField();
        searchField.setPromptText("Search name, email, phone (live)...");
        searchField.setPrefWidth(280);

        // Debounced search on typing
        PauseTransition debounce = new PauseTransition(Duration.millis(300));
        debounce.setOnFinished(e -> refreshTable());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> debounce.playFromStart());

        statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("All Statuses", "ACTIVE", "INACTIVE", "BLACKLISTED");
        statusFilter.setValue("All Statuses");
        statusFilter.setOnAction(e -> refreshTable());

        typeFilter = new ComboBox<>();
        typeFilter.getItems().addAll("All Types", "INDIVIDUAL", "BUSINESS");
        typeFilter.setValue("All Types");
        typeFilter.setOnAction(e -> refreshTable());

        Button clearBtn = new Button("Clear Filters");
        clearBtn.getStyleClass().add("btn-ghost");
        clearBtn.setOnAction(e -> {
            searchField.clear();
            statusFilter.setValue("All Statuses");
            typeFilter.setValue("All Types");
            refreshTable();
        });

        Region toolbarSpacer = new Region();
        HBox.setHgrow(toolbarSpacer, Priority.ALWAYS);

        Button editBtn = new Button("Edit Contact");
        editBtn.getStyleClass().add("btn-secondary");
        editBtn.setDisable(true);
        editBtn.setOnAction(e -> openEditDialog());

        Button deactivateBtn = new Button("Deactivate");
        deactivateBtn.getStyleClass().add("btn-danger");
        deactivateBtn.setDisable(true);
        deactivateBtn.setOnAction(e -> openDeactivateDialog());

        toolbar.getChildren().addAll(searchIcon, searchField, statusFilter, typeFilter, clearBtn, toolbarSpacer, editBtn, deactivateBtn);

        // Table Container Card
        VBox tableCard = new VBox();
        tableCard.getStyleClass().add("card");
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        tableCard.setFillWidth(true);

        tableContainer = new StackPane();
        tableContainer.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(tableContainer, Priority.ALWAYS);
        tableCard.getChildren().add(tableContainer);

        getChildren().addAll(header, toolbar, tableCard);
        refreshTable();
    }

    private void refreshTable() {
        String q = searchField.getText().trim();
        String status = statusFilter.getValue();
        String type = typeFilter.getValue();

        StringBuilder sql = new StringBuilder(
            "SELECT customer_id, first_name || ' ' || last_name AS name, email, phone, customer_type, status, created_at FROM customer WHERE 1=1"
        );

        if (!"All Statuses".equals(status)) {
            sql.append(" AND status = '").append(status).append("'");
        }
        if (!"All Types".equals(type)) {
            sql.append(" AND customer_type = '").append(type).append("'");
        }

        TableViewBuilder.TableContainer table;
        Consumer<ObservableList<String>> onSelect = row -> {
            boolean hasSel = row != null;
            // Lookup buttons in toolbar
            HBox tb = (HBox) getChildren().get(1);
            Button edit = (Button) tb.getChildren().get(6);
            Button deact = (Button) tb.getChildren().get(7);
            edit.setDisable(!hasSel);
            deact.setDisable(!hasSel);
            if (hasSel) {
                selectedCustomerId = Integer.parseInt(row.get(0));
            }
        };

        if (q.isEmpty()) {
            sql.append(" ORDER BY customer_id DESC");
            table = TableViewBuilder.buildAsyncTable(
                row -> ToastManager.showInfo("Selected Customer ID #" + row.get(0) + " (" + row.get(1) + ")"),
                onSelect,
                sql.toString()
            );
        } else {
            sql.append(" AND (LOWER(first_name) LIKE ? OR LOWER(last_name) LIKE ? OR LOWER(email) LIKE ? OR phone LIKE ?) ORDER BY customer_id DESC");
            String pattern = "%" + q.toLowerCase() + "%";
            table = TableViewBuilder.buildAsyncTable(
                row -> ToastManager.showInfo("Selected Customer ID #" + row.get(0) + " (" + row.get(1) + ")"),
                onSelect,
                sql.toString(), pattern, pattern, pattern, "%" + q + "%"
            );
        }
        tableContainer.getChildren().setAll(table);
    }

    private Integer selectedCustomerId = null;

    private void openAddDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Add New Customer");
        dialog.setHeaderText(null);

        VBox content = new VBox(16);
        content.getStyleClass().add("card");
        content.setPrefWidth(450);

        Label titleLbl = new Label("Register Customer");
        titleLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-text;");

        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("INDIVIDUAL", "BUSINESS");
        typeBox.setValue("INDIVIDUAL");
        typeBox.setMaxWidth(Double.MAX_VALUE);

        TextField fName = new TextField();
        FormField fNameField = new FormField("First Name", fName, true);

        TextField lName = new TextField();
        FormField lNameField = new FormField("Last Name", lName, true);

        TextField comp = new TextField();
        FormField compField = new FormField("Company Name (Business accounts)", comp, false);

        TextField email = new TextField();
        FormField emailField = new FormField("Email Address", email, true);

        TextField phone = new TextField();
        FormField phoneField = new FormField("Phone Number", phone, true);

        TextField areaId = new TextField("1");
        FormField areaField = new FormField("Service Area ID", areaId, true);

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> dialog.setResult(false));

        Button saveBtn = new Button("Create Customer");
        saveBtn.getStyleClass().add("btn-primary");

        btnRow.getChildren().addAll(cancelBtn, saveBtn);

        content.getChildren().addAll(
            titleLbl,
            new Label("Customer Type *"), typeBox,
            new HBox(10, fNameField, lNameField),
            compField,
            emailField,
            new HBox(10, phoneField, areaField),
            btnRow
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/isp-theme.css").toExternalForm());

        saveBtn.setOnAction(e -> {
            boolean valid = true;
            if (fName.getText().trim().isEmpty()) { fNameField.setError("First name is required"); valid = false; } else fNameField.clearError();
            if (lName.getText().trim().isEmpty()) { lNameField.setError("Last name is required"); valid = false; } else lNameField.clearError();
            if (!email.getText().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) { emailField.setError("Enter a valid email address"); valid = false; } else emailField.clearError();
            if (phone.getText().trim().length() < 7) { phoneField.setError("Valid phone required"); valid = false; } else phoneField.clearError();

            if (!valid) return;

            saveBtn.setDisable(true);
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    String insert = "INSERT INTO customer (customer_type, first_name, last_name, company_name, email, phone, area_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
                    try (Connection conn = Db.getConnection();
                         PreparedStatement ps = conn.prepareStatement(insert)) {
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
                dialog.setResult(true);
                dialog.close();
                ToastManager.showSuccess("Customer created successfully!");
                refreshTable();
            });

            task.setOnFailed(ev -> {
                saveBtn.setDisable(false);
                ToastManager.showError("Save error: " + TableViewBuilder.cleanOracleMessage(task.getException().getMessage()));
            });

            new Thread(task).start();
        });

        dialog.showAndWait();
    }

    private void openEditDialog() {
        if (selectedCustomerId != null) {
            editCustomerById(selectedCustomerId);
            return;
        }
        TextInputDialog idDialog = new TextInputDialog();
        idDialog.setTitle("Edit Customer");
        idDialog.setHeaderText("Update Customer Contact");
        idDialog.setContentText("Enter Customer ID to edit:");
        idDialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/isp-theme.css").toExternalForm());

        idDialog.showAndWait().ifPresent(idStr -> {
            try {
                int cId = Integer.parseInt(idStr.trim());
                editCustomerById(cId);
            } catch (NumberFormatException e) {
                ToastManager.showError("Invalid customer ID entered.");
            }
        });
    }

    private void editCustomerById(int cId) {
        Dialog<Boolean> editDialog = new Dialog<>();
        editDialog.setTitle("Edit Customer #" + cId);

        VBox content = new VBox(16);
        content.getStyleClass().add("card");
        content.setPrefWidth(400);

        TextField phoneField = new TextField();
        FormField phoneForm = new FormField("New Phone Number", phoneField, true);

        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("ACTIVE", "INACTIVE", "BLACKLISTED");
        statusBox.setValue("ACTIVE");
        statusBox.setMaxWidth(Double.MAX_VALUE);

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> editDialog.close());

        Button saveBtn = new Button("Update Customer");
        saveBtn.getStyleClass().add("btn-primary");

        btnRow.getChildren().addAll(cancelBtn, saveBtn);

        content.getChildren().addAll(
            new Label("Edit Contact for Customer #" + cId),
            phoneForm,
            new Label("Status *"), statusBox,
            btnRow
        );

        editDialog.getDialogPane().setContent(content);
        editDialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/isp-theme.css").toExternalForm());

        saveBtn.setOnAction(e -> {
            if (phoneField.getText().trim().isEmpty()) {
                phoneForm.setError("Phone is required");
                return;
            }
            saveBtn.setDisable(true);

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    String sql = "UPDATE customer SET phone = ?, status = ? WHERE customer_id = ?";
                    try (Connection conn = Db.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, phoneField.getText().trim());
                        ps.setString(2, statusBox.getValue());
                        ps.setInt(3, cId);
                        ps.executeUpdate();
                    }
                    return null;
                }
            };

            task.setOnSucceeded(ev -> {
                editDialog.close();
                ToastManager.showSuccess("Customer #" + cId + " updated successfully!");
                refreshTable();
            });

            task.setOnFailed(ev -> {
                saveBtn.setDisable(false);
                ToastManager.showError("Update error: " + TableViewBuilder.cleanOracleMessage(task.getException().getMessage()));
            });

            new Thread(task).start();
        });

        editDialog.showAndWait();
    }

    private void openDeactivateDialog() {
        if (selectedCustomerId != null) {
            deactivateCustomerById(selectedCustomerId);
            return;
        }
        TextInputDialog idDialog = new TextInputDialog();
        idDialog.setTitle("Deactivate Customer");
        idDialog.setHeaderText("Soft Deactivation");
        idDialog.setContentText("Enter Customer ID to mark INACTIVE:");
        idDialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/isp-theme.css").toExternalForm());

        idDialog.showAndWait().ifPresent(idStr -> {
            try {
                int cId = Integer.parseInt(idStr.trim());
                deactivateCustomerById(cId);
            } catch (NumberFormatException e) {
                ToastManager.showError("Invalid Customer ID.");
            }
        });
    }

    private void deactivateCustomerById(int cId) {
        ConfirmDialog confirm = new ConfirmDialog(
            "Confirm Deactivation",
            "Are you sure you want to deactivate Customer #" + cId + "? This marks status to INACTIVE without destroying past billing history.",
            "Deactivate Account",
            true
        );

        confirm.showAndWait().ifPresent(confirmed -> {
            if (confirmed) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        String sql = "UPDATE customer SET status = 'INACTIVE' WHERE customer_id = ?";
                        try (Connection conn = Db.getConnection();
                             PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setInt(1, cId);
                            ps.executeUpdate();
                        }
                        return null;
                    }
                };
                task.setOnSucceeded(ev -> {
                    ToastManager.showSuccess("Customer #" + cId + " deactivated.");
                    refreshTable();
                });
                task.setOnFailed(ev -> ToastManager.showError("Error: " + task.getException().getMessage()));
                new Thread(task).start();
            }
        });
    }
}
