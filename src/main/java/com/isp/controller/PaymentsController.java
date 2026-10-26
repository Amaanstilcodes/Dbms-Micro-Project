package com.isp.controller;

import com.isp.model.Payment;
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
 * Controller for Payments.fxml.
 * Read-only list of all payment transactions with export capability.
 *
 * TODO: inject PaymentDAO.
 */
public class PaymentsController {

    @FXML private TableView<Payment>               paymentTable;
    @FXML private TableColumn<Payment,Integer>     colId;
    @FXML private TableColumn<Payment,Integer>     colInvoiceId;
    @FXML private TableColumn<Payment,String>      colCustomer;
    @FXML private TableColumn<Payment,BigDecimal>  colAmount;
    @FXML private TableColumn<Payment,String>      colMethod;
    @FXML private TableColumn<Payment,String>      colRef;
    @FXML private TableColumn<Payment,LocalDate>   colDate;

    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private ComboBox<String> methodFilter;
    @FXML private Button    filterButton;
    @FXML private Label     totalLabel;

    private final ObservableList<Payment> payments = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("paymentId"));
        colInvoiceId.setCellValueFactory(new PropertyValueFactory<>("invoiceId"));
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colMethod.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        colRef.setCellValueFactory(new PropertyValueFactory<>("transactionRef"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("paymentDate"));

        paymentTable.setItems(payments);

        methodFilter.setItems(FXCollections.observableArrayList(
                "ALL", "CASH", "CARD", "UPI", "BANK_TRANSFER", "CHEQUE"));
        methodFilter.setValue("ALL");

        // Default date range: current month
        fromDatePicker.setValue(LocalDate.now().withDayOfMonth(1));
        toDatePicker.setValue(LocalDate.now());

        loadPayments();
    }

    private void loadPayments() {
        payments.clear();
        // TODO: payments.addAll(paymentDAO.findByDateRange(fromDate, toDate));
        BigDecimal total = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalLabel.setText("Total collected: ₹" + total);
    }

    @FXML
    private void handleFilter() {
        loadPayments();
    }

    @FXML private void goBack() throws IOException { SceneManager.switchTo("Dashboard.fxml"); }
}
