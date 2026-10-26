package com.isp.controller;

import com.isp.model.Plan;
import com.isp.util.SceneManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Controller for Plans.fxml.
 * Manage internet service plans (CRUD).
 *
 * TODO: inject PlanDAO.
 */
public class PlansController {

    @FXML private TableView<Plan>             planTable;
    @FXML private TableColumn<Plan,Integer>   colId;
    @FXML private TableColumn<Plan,String>    colName;
    @FXML private TableColumn<Plan,String>    colType;
    @FXML private TableColumn<Plan,Integer>   colSpeed;
    @FXML private TableColumn<Plan,Integer>   colCap;
    @FXML private TableColumn<Plan,BigDecimal> colPrice;
    @FXML private TableColumn<Plan,String>    colCycle;
    @FXML private TableColumn<Plan,Boolean>   colActive;

    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button toggleActiveButton;
    @FXML private Label  statusLabel;

    private final ObservableList<Plan> plans = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("planId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("planName"));
        colType.setCellValueFactory(new PropertyValueFactory<>("planType"));
        colSpeed.setCellValueFactory(new PropertyValueFactory<>("speedMbps"));
        colCap.setCellValueFactory(new PropertyValueFactory<>("dataCapGb"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colCycle.setCellValueFactory(new PropertyValueFactory<>("billingCycle"));
        colActive.setCellValueFactory(new PropertyValueFactory<>("active"));

        planTable.setItems(plans);

        editButton.disableProperty().bind(
                planTable.getSelectionModel().selectedItemProperty().isNull());
        toggleActiveButton.disableProperty().bind(
                planTable.getSelectionModel().selectedItemProperty().isNull());

        loadPlans();
    }

    private void loadPlans() {
        plans.clear();
        // TODO: plans.addAll(planDAO.findAll());
        statusLabel.setText(plans.size() + " plans loaded.");
    }

    @FXML
    private void handleAdd() {
        showInfo("Add Plan dialog — TODO");
    }

    @FXML
    private void handleEdit() {
        Plan selected = planTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        showInfo("Edit Plan dialog — TODO for plan ID " + selected.getPlanId());
    }

    @FXML
    private void handleToggleActive() {
        Plan selected = planTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        boolean newState = !selected.isActive();
        // TODO: planDAO.setActive(selected.getPlanId(), newState);
        selected.setActive(newState);
        planTable.refresh();
        statusLabel.setText("Plan " + selected.getPlanId() + " is now " + (newState ? "active" : "inactive"));
    }

    @FXML private void goBack() throws IOException { SceneManager.switchTo("Dashboard.fxml"); }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }
}
