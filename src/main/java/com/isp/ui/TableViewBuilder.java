package com.isp.ui;

import com.isp.Db;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

public class TableViewBuilder {

    public static StackPane buildAsyncTable(String sql, Object... params) {
        StackPane root = new StackPane();
        ProgressIndicator spinner = new ProgressIndicator();
        TableView<ObservableList<String>> tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        root.getChildren().addAll(tableView, spinner);

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
            TableData data = task.getValue();
            tableView.getColumns().clear();
            for (int i = 0; i < data.headers.size(); i++) {
                final int colIndex = i;
                TableColumn<ObservableList<String>, String> col = new TableColumn<>(data.headers.get(i));
                col.setCellValueFactory(param -> {
                    ObservableList<String> row = param.getValue();
                    if (row != null && colIndex < row.size()) {
                        return new SimpleStringProperty(row.get(colIndex));
                    }
                    return new SimpleStringProperty("");
                });
                tableView.getColumns().add(col);
            }
            tableView.getItems().setAll(data.rows);
            root.getChildren().remove(spinner);
        });

        task.setOnFailed(e -> {
            root.getChildren().remove(spinner);
            Throwable ex = task.getException();
            showError("Database Query Error", ex != null ? ex.getMessage() : "Unknown error");
        });

        new Thread(task).start();
        return root;
    }

    public static void showError(String title, String rawMessage) {
        String msg = cleanOracleMessage(rawMessage);
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }

    public static void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
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
            // Strip code if ORA-20xxx
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
