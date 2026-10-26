package com.isp.ui;

import com.isp.Db;
import com.isp.ui.components.EmptyState;
import com.isp.ui.components.StatusBadge;
import com.isp.ui.components.ToastManager;
import com.isp.ui.util.Formatters;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TableViewBuilder {

    public static class TableContainer extends VBox {
        private final TableView<ObservableList<String>> tableView;
        private final Label footerLabel;
        private final ProgressIndicator spinner;
        private final StackPane stack;

        public TableContainer(TableView<ObservableList<String>> tableView, Label footerLabel, ProgressIndicator spinner, StackPane stack) {
            this.tableView = tableView;
            this.footerLabel = footerLabel;
            this.spinner = spinner;
            this.stack = stack;
            getChildren().addAll(stack, footerLabel);
            VBox.setVgrow(stack, Priority.ALWAYS);
            VBox.setVgrow(this, Priority.ALWAYS);
            setFillWidth(true);
            setMaxWidth(Double.MAX_VALUE);
        }

        public TableView<ObservableList<String>> getTableView() {
            return tableView;
        }

        public Label getFooterLabel() {
            return footerLabel;
        }

        public ProgressIndicator getSpinner() {
            return spinner;
        }
    }

    public static TableContainer buildAsyncTable(String sql, Object... params) {
        return buildAsyncTable(null, null, sql, params);
    }

    public static TableContainer buildAsyncTable(Consumer<ObservableList<String>> onRowDoubleClicked, String sql, Object... params) {
        return buildAsyncTable(onRowDoubleClicked, null, sql, params);
    }

    public static TableContainer buildAsyncTable(Consumer<ObservableList<String>> onRowDoubleClicked,
                                                Consumer<ObservableList<String>> onRowSelected,
                                                String sql, Object... params) {
        StackPane stack = new StackPane();
        stack.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(stack, Priority.ALWAYS);

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(36, 36);

        TableView<ObservableList<String>> tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableView.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        Label footerLabel = new Label("Loading records...");
        footerLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-muted; -fx-padding: 8 4 0 4;");

        stack.getChildren().addAll(tableView, spinner);
        TableContainer container = new TableContainer(tableView, footerLabel, spinner, stack);

        Task<TableData> task = new Task<>() {
            @Override
            protected TableData call() throws Exception {
                try (Connection conn = Db.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (int i = 0; i < params.length; i++) {
                        ps.setObject(i + 1, params[i]);
                    }
                    try (ResultSet rs = ps.executeQuery()) {
                        ResultSetMetaData md = rs.getMetaData();
                        int colCount = md.getColumnCount();
                        List<String> headers = new ArrayList<>();
                        for (int i = 1; i <= colCount; i++) {
                            headers.add(md.getColumnLabel(i));
                        }
                        List<ObservableList<String>> rows = new ArrayList<>();
                        while (rs.next()) {
                            ObservableList<String> row = FXCollections.observableArrayList();
                            for (int i = 1; i <= colCount; i++) {
                                Object val = rs.getObject(i);
                                row.add(val != null ? val.toString() : "");
                            }
                            rows.add(row);
                        }
                        return new TableData(headers, rows);
                    }
                }
            }
        };

        task.setOnSucceeded(e -> {
            stack.getChildren().remove(spinner);
            TableData data = task.getValue();
            tableView.getColumns().clear();

            for (int i = 0; i < data.headers.size(); i++) {
                final int colIndex = i;
                String rawHeader = data.headers.get(i);
                TableColumn<ObservableList<String>, String> col = configureColumn(rawHeader, colIndex);
                tableView.getColumns().add(col);
            }

            tableView.getItems().setAll(data.rows);
            int rowCount = data.rows.size();
            footerLabel.setText("Showing " + rowCount + (rowCount == 1 ? " record" : " records"));

            if (data.rows.isEmpty()) {
                tableView.setPlaceholder(new EmptyState("No records found", "Try clearing search or filters.", "fas-inbox"));
            }

            // Context Menu & Double Click
            ContextMenu contextMenu = new ContextMenu();
            MenuItem copyRow = new MenuItem("Copy Row Data");
            copyRow.setOnAction(ev -> {
                ObservableList<String> sel = tableView.getSelectionModel().getSelectedItem();
                if (sel != null) {
                    ClipboardContent cc = new ClipboardContent();
                    cc.putString(String.join("\t", sel));
                    Clipboard.getSystemClipboard().setContent(cc);
                    ToastManager.showInfo("Copied row to clipboard");
                }
            });
            contextMenu.getItems().add(copyRow);
            tableView.setContextMenu(contextMenu);

            if (onRowDoubleClicked != null) {
                tableView.setRowFactory(tv -> {
                    TableRow<ObservableList<String>> row = new TableRow<>();
                    row.setOnMouseClicked(event -> {
                        if (event.getClickCount() == 2 && (!row.isEmpty())) {
                            onRowDoubleClicked.accept(row.getItem());
                        }
                    });
                    return row;
                });
            }

            if (onRowSelected != null) {
                tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                    onRowSelected.accept(newVal);
                });
            }
        });

        task.setOnFailed(e -> {
            stack.getChildren().remove(spinner);
            Throwable ex = task.getException();
            footerLabel.setText("Error loading data");
            showError("Database Query Error", ex != null ? ex.getMessage() : "Unknown error");
        });

        new Thread(task).start();
        return container;
    }

    public static TableColumn<ObservableList<String>, String> addColumn(
            String title, final int colIndex, double prefWidth, double minWidth, double maxWidth, Pos alignment) {
        TableColumn<ObservableList<String>, String> col = new TableColumn<>(title);
        col.setMinWidth(minWidth);
        col.setPrefWidth(prefWidth);
        if (maxWidth > 0) {
            col.setMaxWidth(maxWidth);
        }

        col.setCellValueFactory(param -> {
            ObservableList<String> row = param.getValue();
            if (row != null && colIndex < row.size()) {
                return new SimpleStringProperty(row.get(colIndex));
            }
            return new SimpleStringProperty("");
        });

        String tLower = title.toLowerCase();
        if (tLower.contains("status")) {
            col.setCellFactory(tc -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null || item.isBlank()) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        StatusBadge badge = new StatusBadge(item);
                        setGraphic(badge);
                        setText(null);
                        setAlignment(Pos.CENTER);
                    }
                }
            });
        } else if (tLower.contains("price") || tLower.contains("amount") ||
                   tLower.contains("balance") || tLower.contains("revenue") ||
                   tLower.contains("outstanding") || tLower.contains("total") ||
                   tLower.contains("subtotal") || tLower.contains("fee") || tLower.contains("gst")) {
            col.setCellFactory(tc -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null || item.isBlank()) {
                        setText(null);
                    } else {
                        setText(Formatters.formatCurrency(item));
                        setAlignment(Pos.CENTER_RIGHT);
                    }
                }
            });
        } else if (tLower.contains("date") || tLower.contains("created") || tLower.contains("start") || tLower.contains("due")) {
            col.setCellFactory(tc -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null || item.isBlank()) {
                        setText(null);
                        setTooltip(null);
                    } else {
                        setText(Formatters.formatDate(item));
                        setAlignment(alignment != null ? alignment : Pos.CENTER_LEFT);
                        setTooltip(new Tooltip(item));
                    }
                }
            });
        } else {
            col.setCellFactory(tc -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null || item.isBlank()) {
                        setText(null);
                        setTooltip(null);
                    } else {
                        setText(item);
                        setAlignment(alignment != null ? alignment : Pos.CENTER_LEFT);
                        if (item.length() > 20) {
                            setTextOverrun(OverrunStyle.ELLIPSIS);
                            setTooltip(new Tooltip(item));
                        } else {
                            setTooltip(null);
                        }
                    }
                }
            });
        }

        return col;
    }

    private static TableColumn<ObservableList<String>, String> configureColumn(String rawHeader, int colIndex) {
        String h = rawHeader.toLowerCase();
        String title = formatHeader(rawHeader);

        // ID columns: pref 70, max 90, center-aligned
        if (h.endsWith("_id") || h.equals("id") || h.contains("item_id")) {
            String shortTitle = title.replace(" Id", "").replace("ID", "").trim();
            if (shortTitle.isEmpty() || shortTitle.equalsIgnoreCase("customer") || shortTitle.equalsIgnoreCase("subscription") || shortTitle.equalsIgnoreCase("invoice") || shortTitle.equalsIgnoreCase("ticket")) {
                shortTitle = "ID";
            } else {
                shortTitle = shortTitle + " ID";
            }
            return addColumn(shortTitle, colIndex, 75, 65, 95, Pos.CENTER);
        }

        // Status columns: pref 125, min 105, center-aligned
        if (h.contains("status")) {
            return addColumn("Status", colIndex, 125, 105, 150, Pos.CENTER);
        }

        // Dates: pref 135, min 115
        if (h.contains("date") || h.contains("created_at") || h.contains("due_date")) {
            String dateTitle = title.replace("Date", "").trim();
            if (dateTitle.isEmpty()) dateTitle = "Date";
            else dateTitle = dateTitle + " Date";
            if (h.contains("created_at")) dateTitle = "Created On";
            return addColumn(dateTitle, colIndex, 135, 115, 170, Pos.CENTER_LEFT);
        }

        // Phone: pref 130, min 110
        if (h.contains("phone")) {
            return addColumn("Phone", colIndex, 130, 110, 150, Pos.CENTER_LEFT);
        }

        // Customer Type: pref 115, min 95
        if (h.contains("type") && h.contains("cust")) {
            return addColumn("Type", colIndex, 115, 95, 140, Pos.CENTER);
        }

        // Currency / Money columns: pref 120, min 100, right-aligned
        if (h.contains("price") || h.contains("amount") || h.contains("balance") ||
            h.contains("revenue") || h.contains("outstanding") || h.contains("subtotal")) {
            return addColumn(title, colIndex, 125, 105, 160, Pos.CENTER_RIGHT);
        }

        // Email: pref 240, min 180, grows
        if (h.contains("email")) {
            return addColumn("Email", colIndex, 240, 180, -1, Pos.CENTER_LEFT);
        }

        // Name / Plan / Description / Company / City / Area columns: grow with the table
        if (h.contains("name") || h.contains("company") || h.contains("subject") || h.contains("plan")) {
            return addColumn(title, colIndex, 200, 150, -1, Pos.CENTER_LEFT);
        }

        // General fallback
        return addColumn(title, colIndex, 140, 100, -1, Pos.CENTER_LEFT);
    }

    public static void exportToCSV(TableView<ObservableList<String>> table, String filename) {
        try {
            File file = new File(System.getProperty("user.home"), filename + ".csv");
            try (PrintWriter pw = new PrintWriter(file)) {
                List<String> headers = new ArrayList<>();
                for (TableColumn<ObservableList<String>, ?> col : table.getColumns()) {
                    headers.add("\"" + col.getText() + "\"");
                }
                pw.println(String.join(",", headers));

                for (ObservableList<String> row : table.getItems()) {
                    List<String> items = new ArrayList<>();
                    for (String val : row) {
                        items.add("\"" + val.replace("\"", "\"\"") + "\"");
                    }
                    pw.println(String.join(",", items));
                }
            }
            ToastManager.showSuccess("Exported " + table.getItems().size() + " rows to " + file.getName());
        } catch (Exception e) {
            showError("Export Error", e.getMessage());
        }
    }

    private static String formatHeader(String label) {
        if (label == null) return "";
        String[] parts = label.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) {
                sb.append(Character.toUpperCase(p.charAt(0)))
                  .append(p.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }

    public static void showError(String title, String rawMessage) {
        String msg = cleanOracleMessage(rawMessage);
        ToastManager.showError(title + ": " + msg);
    }

    public static void showInfo(String title, String message) {
        ToastManager.showInfo(title + ": " + message);
    }

    public static String cleanOracleMessage(String raw) {
        if (raw == null) return "An error occurred.";
        int oraIdx = raw.indexOf("ORA-");
        if (oraIdx != -1) {
            String substring = raw.substring(oraIdx);
            int newline = substring.indexOf("\n");
            if (newline != -1) {
                substring = substring.substring(0, newline);
            }
            if (substring.matches("^ORA-20\\d{3}:\\s*.*")) {
                return substring.replaceFirst("^ORA-20\\d{3}:\\s*", "");
            }
            return substring;
        }
        return raw;
    }

    private static class TableData {
        final List<String> headers;
        final List<ObservableList<String>> rows;
        TableData(List<String> headers, List<ObservableList<String>> rows) {
            this.headers = headers;
            this.rows = rows;
        }
    }
}
