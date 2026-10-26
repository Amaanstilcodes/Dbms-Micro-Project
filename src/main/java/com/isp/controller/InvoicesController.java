package com.isp.controller;

import com.isp.model.Invoice;
import com.isp.util.SceneManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Controller for Invoices.fxml.
 * View invoices; record payments; change status.
 *
 * TODO: inject InvoiceDAO.
 */
public class InvoicesController {

    @FXML private TableView<Invoice>                  invoiceTable;
    @FXML private TableColumn<Invoice,Integer>        colId;
    @FXML private TableColumn<Invoice,String>         colCustomer;
    @FXML private TableColumn<Invoice,String>         colPlan;
    @FXML private TableColumn<Invoice,LocalDate>      colDue;
    @FXML private TableColumn<Invoice,BigDecimal>     colTotal;
    @FXML private TableColumn<Invoice,BigDecimal>     colPaid;
    @FXML private TableColumn<Invoice,BigDecimal>     colOutstanding;
    @FXML private TableColumn<Invoice,String>         colStatus;

    @FXML private ComboBox<String> statusFilter;
    @FXML private Button           recordPaymentButton;
    @FXML private Label            summaryLabel;

    private final ObservableList<Invoice> invoices = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("invoiceId"));
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colPlan.setCellValueFactory(new PropertyValueFactory<>("planName"));
        colDue.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        colPaid.setCellValueFactory(new PropertyValueFactory<>("amountPaid"));
        colOutstanding.setCellValueFactory(new PropertyValueFactory<>("outstanding"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        invoiceTable.setItems(invoices);

        statusFilter.setItems(FXCollections.observableArrayList(
                "ALL", "DRAFT", "ISSUED", "PARTIALLY_PAID", "PAID", "OVERDUE", "CANCELLED"));
        statusFilter.setValue("ALL");

        recordPaymentButton.disableProperty().bind(
                invoiceTable.getSelectionModel().selectedItemProperty().isNull());

        loadInvoices();
    }

    private void loadInvoices() {
        invoices.clear();
        // TODO: invoices.addAll(invoiceDAO.findAll());
        summaryLabel.setText("Showing " + invoices.size() + " invoices.");
    }

    @FXML
    private void handleFilterChange() {
        loadInvoices(); // TODO: pass status filter to DAO
    }

    @FXML
    private void handleRecordPayment() {
        Invoice inv = invoiceTable.getSelectionModel().getSelectedItem();
        if (inv == null) return;

        TextInputDialog dlg = new TextInputDialog("0.00");
        dlg.setTitle("Record Payment");
        dlg.setHeaderText("Invoice #" + inv.getInvoiceId()
                + " | Outstanding: ₹" + inv.getOutstanding());
        dlg.setContentText("Amount to record (₹):");

        dlg.showAndWait().ifPresent(amtStr -> {
            try {
                BigDecimal amount = new BigDecimal(amtStr.trim());
                if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
                // TODO: invoiceDAO.recordPayment(inv.getInvoiceId(), amount);
                showInfo("Payment of ₹" + amount + " recorded for invoice " + inv.getInvoiceId());
                loadInvoices();
            } catch (NumberFormatException e) {
                showError("Enter a valid positive amount.");
            }
        });
    }

    @FXML private void goBack() throws IOException { SceneManager.switchTo("Dashboard.fxml"); }

    private void showInfo(String msg)  { new Alert(Alert.AlertType.INFORMATION, msg).showAndWait(); }
    private void showError(String msg) { new Alert(Alert.AlertType.ERROR, msg).showAndWait(); }
}
