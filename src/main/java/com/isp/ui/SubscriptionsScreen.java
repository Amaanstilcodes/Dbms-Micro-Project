package com.isp.ui;

import com.isp.Db;
import com.isp.ui.components.PlanCard;
import com.isp.ui.components.StepIndicator;
import com.isp.ui.components.ToastManager;
import com.isp.ui.util.Formatters;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SubscriptionsScreen extends VBox {
    private final StackPane tableContainer;

    public SubscriptionsScreen() {
        setSpacing(16);
        setPadding(new Insets(24));

        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("Customer Subscriptions");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Broadband contracts, service area coverage, and active status tracking");
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button newSubBtn = new Button("+ New Subscription Wizard");
        newSubBtn.getStyleClass().add("btn-primary");
        newSubBtn.setOnAction(e -> openWizardDialog());

        header.getChildren().addAll(titleBox, spacer, newSubBtn);

        VBox tableCard = new VBox();
        tableCard.getStyleClass().add("card");
        tableCard.setFillWidth(true);
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        tableContainer = new StackPane();
        tableContainer.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(tableContainer, Priority.ALWAYS);
        tableCard.getChildren().add(tableContainer);

        getChildren().addAll(header, tableCard);
        refreshTable();
    }

    private void refreshTable() {
        String sql = "SELECT subscription_id, customer_name, plan_name, monthly_price, area_name, city, start_date, subscription_status FROM v_active_subscriptions ORDER BY subscription_id DESC";
        TableViewBuilder.TableContainer table = TableViewBuilder.buildAsyncTable(
            row -> ToastManager.showInfo("Subscription #" + row.get(0) + " (" + row.get(1) + " - " + row.get(2) + ")"),
            sql
        );
        tableContainer.getChildren().setAll(table);
    }

    private void openWizardDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Subscription Provisioning Wizard");
        dialog.setHeaderText(null);

        VBox wizardLayout = new VBox(16);
        wizardLayout.getStyleClass().add("card");
        wizardLayout.setPrefWidth(550);
        wizardLayout.setPrefHeight(480);

        StepIndicator steps = new StepIndicator("Select Customer", "Choose Plan & Area", "Review & Provision");

        StackPane stepContent = new StackPane();
        VBox.setVgrow(stepContent, Priority.ALWAYS);

        // State variables
        final int[] currentStep = {1};
        final IdNameItem[] selectedCustomer = {null};
        final PlanOption[] selectedPlan = {null};
        final IdNameItem[] selectedArea = {null};

        // UI Controls for Steps
        // Step 1: Customer combo
        VBox step1View = new VBox(14);
        step1View.setAlignment(Pos.CENTER_LEFT);
        Label s1Title = new Label("Step 1: Choose Active Customer");
        s1Title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        ComboBox<IdNameItem> customerBox = new ComboBox<>();
        customerBox.setMaxWidth(Double.MAX_VALUE);
        customerBox.setPromptText("Loading active subscribers...");
        step1View.getChildren().addAll(s1Title, new Label("Select the registered customer account:"), customerBox);

        // Step 2: Plan & Area
        VBox step2View = new VBox(14);
        step2View.setAlignment(Pos.CENTER_LEFT);
        Label s2Title = new Label("Step 2: Select Plan Tier & Service Area");
        s2Title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        ComboBox<PlanOption> planBox = new ComboBox<>();
        planBox.setMaxWidth(Double.MAX_VALUE);
        ComboBox<IdNameItem> areaBox = new ComboBox<>();
        areaBox.setMaxWidth(Double.MAX_VALUE);
        step2View.getChildren().addAll(s2Title, new Label("Plan Package:"), planBox, new Label("Service Area Zone:"), areaBox);

        // Step 3: Review & Summary
        VBox step3View = new VBox(12);
        step3View.setAlignment(Pos.CENTER_LEFT);
        Label s3Title = new Label("Step 3: Contract Summary & GST Breakdown");
        s3Title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        VBox summaryCard = new VBox(8);
        summaryCard.setStyle("-fx-background-color: -color-surface-hover; -fx-padding: 14; -fx-background-radius: 8; -fx-border-color: -color-border; -fx-border-radius: 8;");
        Label summaryCust = new Label();
        Label summaryPlan = new Label();
        Label summaryArea = new Label();
        Label summaryTax = new Label("GST Tax: 18% Applicable on Monthly Recurring Charge");
        summaryTax.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-muted;");
        Label summaryTotal = new Label();
        summaryTotal.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: -color-primary;");
        summaryCard.getChildren().addAll(summaryCust, summaryPlan, summaryArea, new Separator(), summaryTax, summaryTotal);
        step3View.getChildren().addAll(s3Title, summaryCard);

        stepContent.getChildren().setAll(step1View);

        // Navigation Buttons
        HBox navRow = new HBox(10);
        navRow.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> dialog.close());

        Button backBtn = new Button("Back");
        backBtn.getStyleClass().add("btn-secondary");
        backBtn.setDisable(true);

        Button nextBtn = new Button("Next Step");
        nextBtn.getStyleClass().add("btn-primary");

        navRow.getChildren().addAll(cancelBtn, backBtn, nextBtn);
        wizardLayout.getChildren().addAll(steps, stepContent, navRow);

        dialog.getDialogPane().setContent(wizardLayout);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/isp-theme.css").toExternalForm());

        // Async Load Data for Dropdowns
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
                    try (ResultSet rs = st.executeQuery("SELECT plan_id, plan_name, speed_mbps, price FROM plan WHERE is_active = 'Y' ORDER BY price")) {
                        while (rs.next()) {
                            planBox.getItems().add(new PlanOption(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getDouble(4)));
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

        // Wizard navigation handlers
        backBtn.setOnAction(e -> {
            if (currentStep[0] > 1) {
                currentStep[0]--;
                steps.setStep(currentStep[0]);
                if (currentStep[0] == 1) {
                    stepContent.getChildren().setAll(step1View);
                    backBtn.setDisable(true);
                    nextBtn.setText("Next Step");
                } else if (currentStep[0] == 2) {
                    stepContent.getChildren().setAll(step2View);
                    nextBtn.setText("Next Step");
                }
            }
        });

        nextBtn.setOnAction(e -> {
            if (currentStep[0] == 1) {
                if (customerBox.getValue() == null) {
                    ToastManager.showError("Please select a customer.");
                    return;
                }
                selectedCustomer[0] = customerBox.getValue();
                currentStep[0] = 2;
                steps.setStep(2);
                stepContent.getChildren().setAll(step2View);
                backBtn.setDisable(false);
            } else if (currentStep[0] == 2) {
                if (planBox.getValue() == null || areaBox.getValue() == null) {
                    ToastManager.showError("Please select both a plan and service area.");
                    return;
                }
                selectedPlan[0] = planBox.getValue();
                selectedArea[0] = areaBox.getValue();
                currentStep[0] = 3;
                steps.setStep(3);

                // Populate summary
                summaryCust.setText("Subscriber: " + selectedCustomer[0].name());
                summaryPlan.setText("Chosen Plan: " + selectedPlan[0].name + " (" + selectedPlan[0].speed + " Mbps @ " + Formatters.formatCurrency(selectedPlan[0].price) + "/mo)");
                summaryArea.setText("Service Area: " + selectedArea[0].name());
                double base = selectedPlan[0].price;
                double gst = base * 0.18;
                summaryTotal.setText("Total First Month (Incl 18% GST): " + Formatters.formatCurrency(base + gst));

                stepContent.getChildren().setAll(step3View);
                nextBtn.setText("Confirm & Provision");
            } else if (currentStep[0] == 3) {
                // Execute atomic transaction
                nextBtn.setDisable(true);
                Task<Void> subTask = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        String insert = "INSERT INTO subscription (customer_id, plan_id, area_id, start_date, status) VALUES (?, ?, ?, SYSDATE, 'ACTIVE')";
                        try (Connection conn = Db.getConnection()) {
                            conn.setAutoCommit(false);
                            try (PreparedStatement ps = conn.prepareStatement(insert)) {
                                ps.setInt(1, selectedCustomer[0].id());
                                ps.setInt(2, selectedPlan[0].id);
                                ps.setInt(3, selectedArea[0].id());
                                ps.executeUpdate();
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

                subTask.setOnSucceeded(ev -> {
                    dialog.close();
                    ToastManager.showSuccess("Subscription activated successfully!");
                    refreshTable();
                });

                subTask.setOnFailed(ev -> {
                    nextBtn.setDisable(false);
                    ToastManager.showError("Provisioning failed: " + TableViewBuilder.cleanOracleMessage(subTask.getException().getMessage()));
                });

                new Thread(subTask).start();
            }
        });

        dialog.showAndWait();
    }

    private static class PlanOption {
        final int id;
        final String name;
        final int speed;
        final double price;

        PlanOption(int id, String name, int speed, double price) {
            this.id = id;
            this.name = name;
            this.speed = speed;
            this.price = price;
        }

        @Override
        public String toString() {
            return name + " (" + speed + " Mbps) - ₹" + String.format("%.0f", price);
        }
    }

    private record IdNameItem(int id, String name) {
        @Override
        public String toString() {
            return name;
        }
    }
}
