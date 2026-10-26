package com.isp.ui;

import com.isp.Db;
import com.isp.ui.components.EmptyState;
import com.isp.ui.components.FormField;
import com.isp.ui.components.StatusBadge;
import com.isp.ui.components.ToastManager;
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

public class TicketsScreen extends VBox {
    private final StackPane viewContainer;
    private final ComboBox<String> filterBox;
    private boolean isKanbanView = true;
    private final Button toggleViewBtn;

    public TicketsScreen() {
        setSpacing(16);
        setPadding(new Insets(24));

        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("Customer Support Tickets");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Helpdesk SLA, technical fault resolution, and field technician work logs");
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        filterBox = new ComboBox<>();
        filterBox.getItems().addAll("ALL", "OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED");
        filterBox.setValue("ALL");
        filterBox.setOnAction(e -> refreshView());

        toggleViewBtn = new Button("Switch to Table View");
        toggleViewBtn.getStyleClass().add("btn-secondary");
        FontIcon toggleIcon = new FontIcon("fas-table");
        toggleIcon.setIconSize(13);
        toggleViewBtn.setGraphic(toggleIcon);

        toggleViewBtn.setOnAction(e -> {
            isKanbanView = !isKanbanView;
            if (isKanbanView) {
                toggleViewBtn.setText("Switch to Table View");
                toggleIcon.setIconLiteral("fas-table");
            } else {
                toggleViewBtn.setText("Switch to Kanban Board");
                toggleIcon.setIconLiteral("fas-columns");
            }
            refreshView();
        });

        Button newTicketBtn = new Button("+ New Ticket");
        newTicketBtn.getStyleClass().add("btn-primary");
        newTicketBtn.setOnAction(e -> openNewTicketDialog());

        header.getChildren().addAll(titleBox, spacer, new Label("Status:"), filterBox, toggleViewBtn, newTicketBtn);

        viewContainer = new StackPane();
        VBox.setVgrow(viewContainer, Priority.ALWAYS);

        getChildren().addAll(header, viewContainer);
        refreshView();
    }

    private void refreshView() {
        if (isKanbanView) {
            loadKanbanBoard();
        } else {
            loadTableView();
        }
    }

    private void loadTableView() {
        String status = filterBox.getValue();
        String sql;
        TableViewBuilder.TableContainer table;
        if ("ALL".equals(status)) {
            sql = "SELECT t.ticket_id, c.first_name || ' ' || c.last_name AS customer_name, t.category, t.priority, " +
                  "t.subject, NVL(tech.first_name || ' ' || tech.last_name, 'UNASSIGNED') AS assigned_tech, t.created_at, t.status " +
                  "FROM support_ticket t " +
                  "JOIN customer c ON t.customer_id = c.customer_id " +
                  "LEFT JOIN technician tech ON t.assigned_to = tech.technician_id " +
                  "ORDER BY t.ticket_id DESC";
            table = TableViewBuilder.buildAsyncTable(
                row -> openTicketDetail(Integer.parseInt(row.get(0))),
                sql
            );
        } else {
            sql = "SELECT t.ticket_id, c.first_name || ' ' || c.last_name AS customer_name, t.category, t.priority, " +
                  "t.subject, NVL(tech.first_name || ' ' || tech.last_name, 'UNASSIGNED') AS assigned_tech, t.created_at, t.status " +
                  "FROM support_ticket t " +
                  "JOIN customer c ON t.customer_id = c.customer_id " +
                  "LEFT JOIN technician tech ON t.assigned_to = tech.technician_id " +
                  "WHERE t.status = ? " +
                  "ORDER BY t.ticket_id DESC";
            table = TableViewBuilder.buildAsyncTable(
                row -> openTicketDetail(Integer.parseInt(row.get(0))),
                sql, status
            );
        }
        VBox tableCard = new VBox(table);
        tableCard.getStyleClass().add("card");
        tableCard.setFillWidth(true);
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        viewContainer.getChildren().setAll(tableCard);
    }

    private void loadKanbanBoard() {
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(36, 36);
        viewContainer.getChildren().setAll(spinner);

        Task<List<TicketCardModel>> task = new Task<>() {
            @Override
            protected List<TicketCardModel> call() throws Exception {
                List<TicketCardModel> list = new ArrayList<>();
                String sql = "SELECT t.ticket_id, c.first_name || ' ' || c.last_name, t.category, t.priority, t.subject, t.status, NVL(tech.first_name, 'Unassigned') " +
                             "FROM support_ticket t JOIN customer c ON t.customer_id = c.customer_id " +
                             "LEFT JOIN technician tech ON t.assigned_to = tech.technician_id ORDER BY t.ticket_id DESC";
                try (Connection conn = Db.getConnection();
                     Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(sql)) {
                    while (rs.next()) {
                        list.add(new TicketCardModel(
                            rs.getInt(1), rs.getString(2), rs.getString(3),
                            rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7)
                        ));
                    }
                }
                return list;
            }
        };

        task.setOnSucceeded(e -> {
            List<TicketCardModel> tickets = task.getValue();

            HBox board = new HBox(16);
            board.setAlignment(Pos.TOP_LEFT);

            VBox openCol = createKanbanColumn("OPEN", "#38BDF8");
            VBox inProgCol = createKanbanColumn("IN_PROGRESS", "#F59E0B");
            VBox resolvedCol = createKanbanColumn("RESOLVED", "#16A34A");
            VBox closedCol = createKanbanColumn("CLOSED", "#64748B");

            for (TicketCardModel t : tickets) {
                VBox card = createTicketCard(t);
                if ("OPEN".equals(t.status)) openCol.getChildren().add(card);
                else if ("IN_PROGRESS".equals(t.status)) inProgCol.getChildren().add(card);
                else if ("RESOLVED".equals(t.status)) resolvedCol.getChildren().add(card);
                else closedCol.getChildren().add(card);
            }

            board.getChildren().addAll(openCol, inProgCol, resolvedCol, closedCol);
            ScrollPane scroll = new ScrollPane(board);
            scroll.setFitToHeight(true);
            scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            viewContainer.getChildren().setAll(scroll);
        });

        task.setOnFailed(e -> {
            viewContainer.getChildren().setAll(new EmptyState("Failed to load tickets", task.getException().getMessage(), "fas-exclamation-circle"));
        });

        new Thread(task).start();
    }

    private VBox createKanbanColumn(String title, String accentHex) {
        VBox col = new VBox(10);
        col.setPrefWidth(280);
        col.setMinWidth(260);
        col.getStyleClass().add("card");
        col.setStyle("-fx-border-color: " + accentHex + "44; -fx-border-width: 2px 0 0 0;");

        HBox colHeader = new HBox(8);
        colHeader.setAlignment(Pos.CENTER_LEFT);

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: -color-text;");

        colHeader.getChildren().add(titleLbl);
        col.getChildren().add(colHeader);
        return col;
    }

    private VBox createTicketCard(TicketCardModel t) {
        VBox card = new VBox(6);
        card.setStyle("-fx-background-color: -color-surface-hover; -fx-padding: 10; -fx-background-radius: 8; -fx-cursor: hand; -fx-border-color: -color-border; -fx-border-radius: 8;");

        HBox top = new HBox(6);
        top.setAlignment(Pos.CENTER_LEFT);

        Label idLbl = new Label("#" + t.id);
        idLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: -color-muted;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label prioBadge = new Label(t.priority);
        prioBadge.getStyleClass().add("badge");
        if ("CRITICAL".equals(t.priority) || "HIGH".equals(t.priority)) prioBadge.getStyleClass().add("badge-danger");
        else if ("MEDIUM".equals(t.priority)) prioBadge.getStyleClass().add("badge-warning");
        else prioBadge.getStyleClass().add("badge-neutral");

        top.getChildren().addAll(idLbl, sp, prioBadge);

        Label subLbl = new Label(t.subject);
        subLbl.setStyle("-fx-font-weight: 600; -fx-font-size: 12px; -fx-text-fill: -color-text;");
        subLbl.setWrapText(true);

        Label custLbl = new Label(t.customerName + " • " + t.category);
        custLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-muted;");

        card.getChildren().addAll(top, subLbl, custLbl);
        card.setOnMouseClicked(e -> openTicketDetail(t.id));
        return card;
    }

    private void openTicketDetail(int ticketId) {
        Dialog<Boolean> d = new Dialog<>();
        d.setTitle("Ticket #" + ticketId + " Timeline & Details");

        VBox content = new VBox(14);
        content.getStyleClass().add("card");
        content.setPrefWidth(480);

        Label title = new Label("Ticket #" + ticketId + " Activity Timeline");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        VBox timelineBox = new VBox(8);
        timelineBox.setStyle("-fx-background-color: -color-surface-hover; -fx-padding: 12; -fx-background-radius: 8; -fx-max-height: 200px;");
        ScrollPane scroll = new ScrollPane(timelineBox);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(180);

        // Fetch updates
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                List<String> logs = new ArrayList<>();
                String q = "SELECT comment_txt, new_status, created_at FROM ticket_update WHERE ticket_id = ? ORDER BY update_id ASC";
                try (Connection conn = Db.getConnection();
                     PreparedStatement ps = conn.prepareStatement(q)) {
                    ps.setInt(1, ticketId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            logs.add("[" + rs.getString(3) + "] (" + rs.getString(2) + "): " + rs.getString(1));
                        }
                    }
                }
                return logs;
            }
        };
        task.setOnSucceeded(e -> {
            List<String> list = task.getValue();
            if (list.isEmpty()) {
                timelineBox.getChildren().add(new Label("No timeline commentary posted yet."));
            } else {
                for (String item : list) {
                    Label l = new Label(item);
                    l.setWrapText(true);
                    l.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text;");
                    timelineBox.getChildren().add(l);
                }
            }
        });
        new Thread(task).start();

        TextField commentInput = new TextField();
        commentInput.setPromptText("Enter timeline update / progress note...");

        ComboBox<String> newStatusBox = new ComboBox<>();
        newStatusBox.getItems().addAll("IN_PROGRESS", "RESOLVED", "CLOSED");
        newStatusBox.setValue("IN_PROGRESS");

        HBox addRow = new HBox(8, commentInput, newStatusBox);
        HBox.setHgrow(commentInput, Priority.ALWAYS);

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("btn-secondary");
        closeBtn.setOnAction(e -> d.close());

        Button postBtn = new Button("Post Update");
        postBtn.getStyleClass().add("btn-primary");

        btnRow.getChildren().addAll(closeBtn, postBtn);
        content.getChildren().addAll(title, scroll, new Label("Add Ticket Comment:"), addRow, btnRow);

        d.getDialogPane().setContent(content);
        d.getDialogPane().getStylesheets().add(getClass().getResource("/css/isp-theme.css").toExternalForm());

        postBtn.setOnAction(e -> {
            String note = commentInput.getText().trim();
            if (note.isEmpty()) return;
            postBtn.setDisable(true);

            Task<Void> postTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    String sql = "INSERT INTO ticket_update (ticket_id, comment_txt, new_status, created_at) VALUES (?, ?, ?, SYSDATE)";
                    try (Connection conn = Db.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, ticketId);
                        ps.setString(2, note);
                        ps.setString(3, newStatusBox.getValue());
                        ps.executeUpdate();
                    }
                    String updateTicket = "UPDATE support_ticket SET status = ? WHERE ticket_id = ?";
                    try (Connection conn = Db.getConnection();
                         PreparedStatement ps = conn.prepareStatement(updateTicket)) {
                        ps.setString(1, newStatusBox.getValue());
                        ps.setInt(2, ticketId);
                        ps.executeUpdate();
                    }
                    return null;
                }
            };
            postTask.setOnSucceeded(ev -> {
                d.close();
                ToastManager.showSuccess("Timeline comment logged!");
                refreshView();
            });
            postTask.setOnFailed(ev -> {
                postBtn.setDisable(false);
                ToastManager.showError("Error posting update: " + postTask.getException().getMessage());
            });
            new Thread(postTask).start();
        });

        d.showAndWait();
    }

    private void openNewTicketDialog() {
        Dialog<Boolean> d = new Dialog<>();
        d.setTitle("Open Support Ticket");

        VBox content = new VBox(14);
        content.getStyleClass().add("card");
        content.setPrefWidth(420);

        Label title = new Label("Create Incident / Inquiry Ticket");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        TextField custId = new TextField("1");
        FormField custField = new FormField("Customer ID", custId, true);

        ComboBox<String> catBox = new ComboBox<>();
        catBox.getItems().addAll("CONNECTIVITY", "BILLING", "EQUIPMENT", "SPEED", "OTHER");
        catBox.setValue("CONNECTIVITY");
        catBox.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> prioBox = new ComboBox<>();
        prioBox.getItems().addAll("LOW", "MEDIUM", "HIGH", "CRITICAL");
        prioBox.setValue("MEDIUM");
        prioBox.setMaxWidth(Double.MAX_VALUE);

        TextField subj = new TextField();
        FormField subjField = new FormField("Subject Summary", subj, true);

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("btn-secondary");
        cancel.setOnAction(e -> d.close());

        Button save = new Button("Create Ticket");
        save.getStyleClass().add("btn-primary");
        btnRow.getChildren().addAll(cancel, save);

        content.getChildren().addAll(
            title, custField,
            new Label("Category *"), catBox,
            new Label("Priority *"), prioBox,
            subjField,
            btnRow
        );

        d.getDialogPane().setContent(content);
        d.getDialogPane().getStylesheets().add(getClass().getResource("/css/isp-theme.css").toExternalForm());

        save.setOnAction(e -> {
            if (subj.getText().trim().isEmpty()) {
                subjField.setError("Subject summary is required");
                return;
            }
            save.setDisable(true);
            Task<Void> t = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    String insert = "INSERT INTO support_ticket (customer_id, category, priority, subject, status, created_at) VALUES (?, ?, ?, ?, 'OPEN', SYSDATE)";
                    try (Connection conn = Db.getConnection();
                         PreparedStatement ps = conn.prepareStatement(insert)) {
                        ps.setInt(1, Integer.parseInt(custId.getText().trim()));
                        ps.setString(2, catBox.getValue());
                        ps.setString(3, prioBox.getValue());
                        ps.setString(4, subj.getText().trim());
                        ps.executeUpdate();
                    }
                    return null;
                }
            };
            t.setOnSucceeded(ev -> {
                d.close();
                ToastManager.showSuccess("Support ticket opened!");
                refreshView();
            });
            t.setOnFailed(ev -> {
                save.setDisable(false);
                ToastManager.showError("Creation failed: " + TableViewBuilder.cleanOracleMessage(t.getException().getMessage()));
            });
            new Thread(t).start();
        });

        d.showAndWait();
    }

    private static class TicketCardModel {
        final int id;
        final String customerName;
        final String category;
        final String priority;
        final String subject;
        final String status;
        final String assignedTech;

        TicketCardModel(int id, String customerName, String category, String priority, String subject, String status, String assignedTech) {
            this.id = id;
            this.customerName = customerName;
            this.category = category;
            this.priority = priority;
            this.subject = subject;
            this.status = status;
            this.assignedTech = assignedTech;
        }
    }
}
