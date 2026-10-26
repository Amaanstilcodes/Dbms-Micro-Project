package com.isp.controller;

import com.isp.model.Customer;
import com.isp.util.SceneManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;

/**
 * Controller for Customers.fxml.
 * Lists all customers with search; opens an Add/Edit dialog.
 *
 * TODO: inject CustomerDAO to load/save data.
 */
public class CustomersController {

    // --- Table ---
    @FXML private TableView<Customer>       customerTable;
    @FXML private TableColumn<Customer,Integer> colId;
    @FXML private TableColumn<Customer,String>  colName;
    @FXML private TableColumn<Customer,String>  colType;
    @FXML private TableColumn<Customer,String>  colEmail;
    @FXML private TableColumn<Customer,String>  colPhone;
    @FXML private TableColumn<Customer,String>  colArea;
    @FXML private TableColumn<Customer,String>  colStatus;

    // --- Toolbar ---
    @FXML private TextField searchField;
    @FXML private Button    addButton;
    @FXML private Button    editButton;
    @FXML private Button    deleteButton;

    // --- Status bar ---
    @FXML private Label statusLabel;

    private final ObservableList<Customer> customers = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        // Bind columns to model properties
        colId.setCellValueFactory(new PropertyValueFactory<>("customerId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colType.setCellValueFactory(new PropertyValueFactory<>("customerType"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colArea.setCellValueFactory(new PropertyValueFactory<>("areaName"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        customerTable.setItems(customers);

        // Disable edit/delete when nothing selected
        editButton.disableProperty().bind(
                customerTable.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.disableProperty().bind(
                customerTable.getSelectionModel().selectedItemProperty().isNull());

        loadCustomers();
    }

    private void loadCustomers() {
        customers.clear();
        // TODO: customers.addAll(customerDAO.findAll());
        statusLabel.setText(customers.size() + " customers loaded.");
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            loadCustomers();
        } else {
            customers.clear();
            // TODO: customers.addAll(customerDAO.search(query));
        }
    }

    @FXML
    private void handleAdd() {
        // TODO: open CustomerFormDialog in ADD mode, then reload
        showInfo("Add Customer dialog — TODO");
    }

    @FXML
    private void handleEdit() {
        Customer selected = customerTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        // TODO: open CustomerFormDialog in EDIT mode with selected, then reload
        showInfo("Edit Customer dialog — TODO for ID " + selected.getCustomerId());
    }

    @FXML
    private void handleDelete() {
        Customer selected = customerTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete customer '" + selected.getFullName() + "'?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                // TODO: customerDAO.deleteById(selected.getCustomerId());
                customers.remove(selected);
                statusLabel.setText("Deleted customer " + selected.getCustomerId());
            }
        });
    }

    @FXML private void goBack() throws IOException { SceneManager.switchTo("Dashboard.fxml"); }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }
}
