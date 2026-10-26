package com.isp.controller;

import com.isp.model.Customer;
import com.isp.service.MockDataService;
import com.isp.util.SceneManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller for Customers.fxml.
 * Lists all customers with search; opens CustomerForm.fxml for Add/Edit.
 *
 * ── Phase A (current): all data comes from {@link MockDataService} (in-memory).
 * ── Phase B: replace each MockDataService call with DAOFactory.getCustomerDAO().xxx().
 *             All form, validation and navigation code stays identical.
 */
public class CustomersController {

    // ── Table ────────────────────────────────────────────────────────────
    @FXML private TableView<Customer>          customerTable;
    @FXML private TableColumn<Customer,Integer> colId;
    @FXML private TableColumn<Customer,String>  colName;
    @FXML private TableColumn<Customer,String>  colType;
    @FXML private TableColumn<Customer,String>  colEmail;
    @FXML private TableColumn<Customer,String>  colPhone;
    @FXML private TableColumn<Customer,String>  colArea;
    @FXML private TableColumn<Customer,String>  colStatus;

    // ── Toolbar ──────────────────────────────────────────────────────────
    @FXML private TextField searchField;
    @FXML private Button    addButton;
    @FXML private Button    editButton;
    @FXML private Button    deleteButton;

    // ── Status bar ───────────────────────────────────────────────────────
    @FXML private Label statusLabel;

    private final ObservableList<Customer> customers = FXCollections.observableArrayList();

    // ────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ────────────────────────────────────────────────────────────────────

    @FXML
    private void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("customerId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colType.setCellValueFactory(new PropertyValueFactory<>("customerType"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colArea.setCellValueFactory(new PropertyValueFactory<>("areaName"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        customerTable.setItems(customers);

        // Edit and Delete only active when a row is selected
        editButton.disableProperty().bind(
                customerTable.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.disableProperty().bind(
                customerTable.getSelectionModel().selectedItemProperty().isNull());

        // Double-click a row → Edit
        customerTable.setOnMouseClicked(evt -> {
            if (evt.getClickCount() == 2
                    && customerTable.getSelectionModel().getSelectedItem() != null) {
                handleEdit();
            }
        });

        loadCustomers();
    }

    // ────────────────────────────────────────────────────────────────────
    // Data
    // ────────────────────────────────────────────────────────────────────

    private void loadCustomers() {
        customers.clear();
        // Phase A ── mock
        customers.addAll(MockDataService.getCustomers());
        // Phase B ── real (one-line swap):
        // customers.addAll(DAOFactory.getCustomerDAO().findAll());
        statusLabel.setText(customers.size() + " customers loaded.");
    }

    // ────────────────────────────────────────────────────────────────────
    // Toolbar handlers
    // ────────────────────────────────────────────────────────────────────

    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim();
        customers.clear();
        if (query.isEmpty()) {
            customers.addAll(MockDataService.getCustomers());
            // Phase B: customers.addAll(DAOFactory.getCustomerDAO().findAll());
            statusLabel.setText(customers.size() + " customers loaded.");
        } else {
            customers.addAll(MockDataService.searchCustomers(query));
            // Phase B: customers.addAll(DAOFactory.getCustomerDAO().search(query));
            statusLabel.setText(customers.size() + " result(s) for \"" + query + "\".");
        }
    }

    @FXML
    private void handleAdd() {
        openCustomerForm(null);
    }

    @FXML
    private void handleEdit() {
        Customer selected = customerTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        openCustomerForm(selected);
    }

    @FXML
    private void handleDelete() {
        Customer selected = customerTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete customer: " + selected.getFullName() + "?");
        confirm.setContentText(
                "This action cannot be undone.\n" +
                "Customers with outstanding invoices cannot be deleted.");
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        confirm.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.YES) return;
            try {
                // Phase A: MockDataService
                // Phase B: DAOFactory.getCustomerDAO().deleteById(selected.getCustomerId());
                MockDataService.deleteCustomer(selected.getCustomerId());
                loadCustomers();
                statusLabel.setText("Customer '" + selected.getFullName() + "' deleted.");
            } catch (IllegalStateException ex) {
                // Business rule / DB trigger blocked deletion — show the reason
                new Alert(Alert.AlertType.WARNING,
                        "Cannot delete customer:\n\n" + ex.getMessage(),
                        ButtonType.OK).showAndWait();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR,
                        "Unexpected error during delete:\n" + ex.getMessage(),
                        ButtonType.OK).showAndWait();
                ex.printStackTrace();
            }
        });
    }

    @FXML
    private void goBack() throws IOException {
        SceneManager.switchTo("Dashboard.fxml");
    }

    // ────────────────────────────────────────────────────────────────────
    // Form dialog
    // ────────────────────────────────────────────────────────────────────

    /**
     * Opens CustomerForm.fxml as a modal dialog.
     *
     * @param customer {@code null} for ADD mode; a {@link Customer} for EDIT mode.
     */
    private void openCustomerForm(Customer customer) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/isp/views/CustomerForm.fxml"));
            Parent root = loader.load();

            CustomerFormController formCtrl = loader.getController();

            if (customer == null) {
                formCtrl.initAdd(saved -> {
                    loadCustomers();
                    statusLabel.setText("✔ Customer '" + saved.getFullName() + "' added.");
                });
            } else {
                formCtrl.initEdit(customer, saved -> {
                    loadCustomers();
                    statusLabel.setText("✔ Customer '" + saved.getFullName() + "' updated.");
                });
            }

            Stage dialog = new Stage();
            dialog.setTitle(customer == null ? "Add Customer" : "Edit Customer");
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(customerTable.getScene().getWindow());
            dialog.setScene(new Scene(root));
            dialog.setResizable(true);
            dialog.setMinWidth(540);
            dialog.setMinHeight(540);

            // Inherit app stylesheet so the form matches the rest of the UI
            dialog.getScene().getStylesheets().add(
                    getClass().getResource("/com/isp/css/style.css").toExternalForm());

            dialog.showAndWait(); // blocks; table refresh happens via onSave callback

        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR,
                    "Could not open the customer form:\n" + ex.getMessage(),
                    ButtonType.OK).showAndWait();
            ex.printStackTrace();
        }
    }
}
