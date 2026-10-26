package com.isp.controller;

import com.isp.model.SupportTicket;
import com.isp.util.SceneManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Controller for SupportTickets.fxml.
 * Manage support tickets: view, create, update status.
 *
 * TODO: inject SupportTicketDAO.
 */
public class SupportTicketsController {

    @FXML private TableView<SupportTicket>               ticketTable;
    @FXML private TableColumn<SupportTicket,Integer>     colId;
    @FXML private TableColumn<SupportTicket,String>      colCustomer;
    @FXML private TableColumn<SupportTicket,String>      colCategory;
    @FXML private TableColumn<SupportTicket,String>      colPriority;
    @FXML private TableColumn<SupportTicket,String>      colSubject;
    @FXML private TableColumn<SupportTicket,String>      colTechnician;
    @FXML private TableColumn<SupportTicket,String>      colStatus;
    @FXML private TableColumn<SupportTicket,LocalDateTime> colCreated;

    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> priorityFilter;
    @FXML private Button           addButton;
    @FXML private Button           editButton;
    @FXML private Button           resolveButton;
    @FXML private Label            statusLabel;

    private final ObservableList<SupportTicket> tickets = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("ticketId"));
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colPriority.setCellValueFactory(new PropertyValueFactory<>("priority"));
        colSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        colTechnician.setCellValueFactory(new PropertyValueFactory<>("technicianName"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colCreated.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        ticketTable.setItems(tickets);

        statusFilter.setItems(FXCollections.observableArrayList(
                "ALL", "OPEN", "IN_PROGRESS", "PENDING_CUSTOMER", "RESOLVED", "CLOSED"));
        statusFilter.setValue("ALL");

        priorityFilter.setItems(FXCollections.observableArrayList(
                "ALL", "LOW", "MEDIUM", "HIGH", "CRITICAL"));
        priorityFilter.setValue("ALL");

        editButton.disableProperty().bind(
                ticketTable.getSelectionModel().selectedItemProperty().isNull());
        resolveButton.disableProperty().bind(
                ticketTable.getSelectionModel().selectedItemProperty().isNull());

        loadTickets();
    }

    private void loadTickets() {
        tickets.clear();
        // TODO: tickets.addAll(supportTicketDAO.findAll());
        statusLabel.setText(tickets.size() + " tickets loaded.");
    }

    @FXML
    private void handleFilterChange() {
        loadTickets(); // TODO: filter by status & priority
    }

    @FXML
    private void handleAdd() {
        showInfo("New Support Ticket dialog — TODO");
    }

    @FXML
    private void handleEdit() {
        SupportTicket t = ticketTable.getSelectionModel().getSelectedItem();
        if (t == null) return;
        showInfo("Edit Ticket dialog — TODO for ticket #" + t.getTicketId());
    }

    @FXML
    private void handleResolve() {
        SupportTicket t = ticketTable.getSelectionModel().getSelectedItem();
        if (t == null) return;
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Resolve Ticket #" + t.getTicketId());
        dlg.setHeaderText("Enter resolution notes:");
        dlg.showAndWait().ifPresent(resolution -> {
            t.setResolution(resolution);
            t.setStatus("RESOLVED");
            t.setResolvedAt(LocalDateTime.now());
            // TODO: supportTicketDAO.update(t);
            ticketTable.refresh();
            statusLabel.setText("Ticket #" + t.getTicketId() + " resolved.");
        });
    }

    @FXML private void goBack() throws IOException { SceneManager.switchTo("Dashboard.fxml"); }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }
}
