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
import java.sql.Statement;

public class SubscriptionsScreen extends VBox {
    private final StackPane tableContainer;

    public SubscriptionsScreen() {
        setSpacing(16);
        setPadding(new Insets(20));

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Customer Subscriptions");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button newSubBtn = new Button("+ New Subscription");
        newSubBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold;");
        newSubBtn.setOnAction(e -> openNewSubscriptionDialog());

        header.getChildren().addAll(title, spacer, newSubBtn);

        tableContainer = new StackPane();
        VBox.setVgrow(tableContainer, Priority.ALWAYS);

        getChildren().addAll(header, tableContainer);
        refreshTable();
    }

    private void refreshTable() {
        String sql = "SELECT subscription_id, customer_name, plan_name, monthly_price, area_name, city, subscription_status, start_date FROM v_active_subscriptions ORDER BY subscription_id DESC";
        StackPane table = TableViewBuilder.buildAsyncTable(sql);
        tableContainer.getChildren().setAll(table);
    }

    private void openNewSubscriptionDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Create Subscription");
        dialog.setHeaderText("Create Subscription (Atomic Single Transaction)");

        ButtonType createBtnType = new ButtonType("Create Subscription", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<IdNameItem> customerBox = new ComboBox<>();
        ComboBox<IdNameItem> planBox = new ComboBox<>();
        ComboBox<IdNameItem> areaBox = new ComboBox<>();

        grid.add(new Label("Customer:"), 0, 0);     grid.add(customerBox, 1, 0);
        grid.add(new Label("Plan:"), 0, 1);         grid.add(planBox, 1, 1);
        grid.add(new Label("Service Area:"), 0, 2); grid.add(areaBox, 1, 2);

        // Load dropdown options asynchronously
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try (Connection conn = Db.getConnection();
                     Statement st = conn.createStatement()) {

                    try (ResultSet rs = st.executeQuery("SELECT customer_id, first_name || ' ' || last_name || ' (' || email || ')' FROM customer WHERE status = 'ACTIVE' ORDER BY first_name")) {
                        while (rs.next()) {
                            customerBox.getItems().add(new IdNameItem(rs.getInt(1), rs.getString(2)));
                        }
                    }
                    try (ResultSet rs = st.executeQuery("SELECT plan_id, plan_name || ' (₹' || price || ')' FROM plan WHERE is_active = 'Y' ORDER BY price")) {
                        while (rs.next()) {
                            planBox.getItems().add(new IdNameItem(rs.getInt(1), rs.getString(2)));
                        }
                    }
                    try (ResultSet rs = st.executeQuery("SELECT area_id, area_name || ' - ' || city || ' (' || pin_code || ')' FROM service_area WHERE is_active = 'Y' ORDER BY area_name")) {
                        while (rs.next()) {
                            areaBox.getItems().add(new IdNameItem(rs.getInt(1), rs.getString(2)));
                        }
                    }
                }
                return null;
            }
        };
        loadTask.setOnSucceeded(e -> {
            if (!customerBox.getItems().isEmpty()) customerBox.setValue(customerBox.getItems().get(0));
            if (!planBox.getItems().isEmpty()) planBox.setValue(planBox.getItems().get(0));
            if (!areaBox.getItems().isEmpty()) areaBox.setValue(areaBox.getItems().get(0));
        });
        new Thread(loadTask).start();

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == createBtnType) {
                IdNameItem selCust = customerBox.getValue();
                IdNameItem selPlan = planBox.getValue();
                IdNameItem selArea = areaBox.getValue();

                if (selCust == null || selPlan == null || selArea == null) {
                    TableViewBuilder.showError("Missing Fields", "Please select customer, plan, and service area.");
                    return null;
                }

                Task<Void> txTask = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        try (Connection conn = Db.getConnection()) {
                            conn.setAutoCommit(false); // ATOMIC TRANSACTION
                            try {
                                String sql = "INSERT INTO subscription (customer_id, plan_id, area_id, start_date, status) " +
                                             "VALUES (?, ?, ?, TRUNC(SYSDATE), 'ACTIVE')";
                                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                                    ps.setInt(1, selCust.id);
                                    ps.setInt(2, selPlan.id);
                                    ps.setInt(3, selArea.id);
                                    ps.executeUpdate();
                                }
                                conn.commit();
                            } catch (Exception ex) {
                                conn.rollback();
                                throw ex;
                            } finally {
                                conn.setAutoCommit(true);
                            }
                        }
                        return null;
                    }
                };

                txTask.setOnSucceeded(ev -> {
                    TableViewBuilder.showInfo("Subscription Created", "Subscription for " + selCust.name + " active.");
                    refreshTable();
                });

                txTask.setOnFailed(ev -> TableViewBuilder.showError("Failed to Create Subscription", txTask.getException().getMessage()));

                new Thread(txTask).start();
                return true;
            }
            return null;
        });

        dialog.showAndWait();
    }

    private static class IdNameItem {
        final int id;
        final String name;
        IdNameItem(int id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }
}
