package com.isp.controller;

import com.isp.model.Subscription;
import com.isp.util.SceneManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Controller for Subscriptions.fxml.
 * Manage customer-plan subscription contracts.
 *
 * TODO: inject SubscriptionDAO and CustomerDAO/PlanDAO for combo boxes.
 */
public class SubscriptionsController {

    @FXML private TableView<Subscription>               subTable;
    @FXML private TableColumn<Subscription,Integer>     colId;
    @FXML private TableColumn<Subscription,String>      colCustomer;
    @FXML private TableColumn<Subscription,String>      colPlan;
    @FXML private TableColumn<Subscription,String>      colArea;
    @FXML private TableColumn<Subscription,LocalDate>   colStart;
    @FXML private TableColumn<Subscription,LocalDate>   colEnd;
    @FXML private TableColumn<Subscription,String>      colStatus;

    @FXML private ComboBox<String> statusFilter;
    @FXML private Button           addButton;
    @FXML private Button           editButton;
    @FXML private Button           changeStatusButton;
    @FXML private Label            statusLabel;

    private final ObservableList<Subscription> subscriptions = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("subscriptionId"));
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colPlan.setCellValueFactory(new PropertyValueFactory<>("planName"));
        colArea.setCellValueFactory(new PropertyValueFactory<>("areaName"));
        colStart.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        colEnd.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        subTable.setItems(subscriptions);

        statusFilter.setItems(FXCollections.observableArrayList(
                "ALL", "PENDING", "ACTIVE", "SUSPENDED", "TERMINATED"));
        statusFilter.setValue("ALL");

        editButton.disableProperty().bind(
                subTable.getSelectionModel().selectedItemProperty().isNull());
        changeStatusButton.disableProperty().bind(
                subTable.getSelectionModel().selectedItemProperty().isNull());

        loadSubscriptions();
    }

    private void loadSubscriptions() {
        subscriptions.clear();
        // TODO: subscriptions.addAll(subscriptionDAO.findAll());
        statusLabel.setText(subscriptions.size() + " subscriptions loaded.");
    }

    @FXML
    private void handleFilterChange() {
        // TODO: filter in-memory or re-query DAO by status
        loadSubscriptions();
    }

    @FXML
    private void handleAdd() {
        showInfo("New Subscription dialog — TODO");
    }

    @FXML
    private void handleEdit() {
        Subscription s = subTable.getSelectionModel().getSelectedItem();
        if (s == null) return;
        showInfo("Edit Subscription dialog — TODO for ID " + s.getSubscriptionId());
    }

    @FXML
    private void handleChangeStatus() {
        Subscription s = subTable.getSelectionModel().getSelectedItem();
        if (s == null) return;
        ChoiceDialog<String> dialog = new ChoiceDialog<>(s.getStatus(),
                "PENDING", "ACTIVE", "SUSPENDED", "TERMINATED");
        dialog.setTitle("Change Status");
        dialog.setHeaderText("New status for subscription " + s.getSubscriptionId());
        dialog.showAndWait().ifPresent(newStatus -> {
            // TODO: subscriptionDAO.updateStatus(s.getSubscriptionId(), newStatus);
            s.setStatus(newStatus);
            subTable.refresh();
        });
    }

    @FXML private void goBack() throws IOException { SceneManager.switchTo("Dashboard.fxml"); }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }
}
