package com.isp.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class TicketsScreen extends VBox {
    private final StackPane tableContainer;
    private final ComboBox<String> filterBox;

    public TicketsScreen() {
        setSpacing(16);
        setPadding(new Insets(20));

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Support Tickets");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        filterBox = new ComboBox<>();
        filterBox.getItems().addAll("ALL", "OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED");
        filterBox.setValue("ALL");
        filterBox.setOnAction(e -> refreshTable());

        header.getChildren().addAll(title, spacer, new Label("Status:"), filterBox);

        tableContainer = new StackPane();
        VBox.setVgrow(tableContainer, Priority.ALWAYS);

        getChildren().addAll(header, tableContainer);
        refreshTable();
    }

    private void refreshTable() {
        String status = filterBox.getValue();
        String sql;
        StackPane table;
        if ("ALL".equals(status)) {
            sql = "SELECT t.ticket_id, c.first_name || ' ' || c.last_name AS customer_name, t.category, t.priority, " +
                  "t.subject, t.status, NVL(tech.first_name || ' ' || tech.last_name, 'UNASSIGNED') AS assigned_tech, t.created_at " +
                  "FROM support_ticket t " +
                  "JOIN customer c ON t.customer_id = c.customer_id " +
                  "LEFT JOIN technician tech ON t.assigned_to = tech.technician_id " +
                  "ORDER BY t.ticket_id DESC";
            table = TableViewBuilder.buildAsyncTable(sql);
        } else {
            sql = "SELECT t.ticket_id, c.first_name || ' ' || c.last_name AS customer_name, t.category, t.priority, " +
                  "t.subject, t.status, NVL(tech.first_name || ' ' || tech.last_name, 'UNASSIGNED') AS assigned_tech, t.created_at " +
                  "FROM support_ticket t " +
                  "JOIN customer c ON t.customer_id = c.customer_id " +
                  "LEFT JOIN technician tech ON t.assigned_to = tech.technician_id " +
                  "WHERE t.status = ? " +
                  "ORDER BY t.ticket_id DESC";
            table = TableViewBuilder.buildAsyncTable(sql, status);
        }
        tableContainer.getChildren().setAll(table);
    }
}
