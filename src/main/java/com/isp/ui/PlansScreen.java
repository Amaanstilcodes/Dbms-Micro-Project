package com.isp.ui;

import com.isp.Db;
import com.isp.ui.components.EmptyState;
import com.isp.ui.components.PlanCard;
import com.isp.ui.components.ToastManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PlansScreen extends VBox {
    private final StackPane viewContainer;
    private boolean isGridView = true;
    private final Button toggleViewBtn;

    public PlansScreen() {
        setSpacing(16);
        setPadding(new Insets(24));

        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("Service Plans & Tiers");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("High-speed broadband, commercial leased lines, and FTTH packages");
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toggleViewBtn = new Button("Switch to Table View");
        toggleViewBtn.getStyleClass().add("btn-secondary");
        FontIcon toggleIcon = new FontIcon("fas-table");
        toggleIcon.setIconSize(13);
        toggleViewBtn.setGraphic(toggleIcon);

        header.getChildren().addAll(titleBox, spacer, toggleViewBtn);

        viewContainer = new StackPane();
        VBox.setVgrow(viewContainer, Priority.ALWAYS);

        getChildren().addAll(header, viewContainer);

        toggleViewBtn.setOnAction(e -> {
            isGridView = !isGridView;
            if (isGridView) {
                toggleViewBtn.setText("Switch to Table View");
                toggleIcon.setIconLiteral("fas-table");
                loadCardsView();
            } else {
                toggleViewBtn.setText("Switch to Cards Grid");
                toggleIcon.setIconLiteral("fas-th-large");
                loadTableView();
            }
        });

        loadCardsView();
    }

    private void loadCardsView() {
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(36, 36);
        viewContainer.getChildren().setAll(spinner);

        Task<List<PlanModel>> task = new Task<>() {
            @Override
            protected List<PlanModel> call() throws Exception {
                List<PlanModel> list = new ArrayList<>();
                String sql = "SELECT plan_id, plan_name, plan_type, speed_mbps, upload_mbps, " +
                             "NVL(TO_CHAR(data_cap_gb), 'UNLIMITED') AS cap, price, billing_cycle FROM plan WHERE is_active = 'Y' ORDER BY price ASC";
                try (Connection conn = Db.getConnection();
                     Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(sql)) {
                    while (rs.next()) {
                        list.add(new PlanModel(
                            rs.getInt(1), rs.getString(2), rs.getString(3),
                            rs.getInt(4), rs.getInt(5), rs.getString(6),
                            rs.getDouble(7), rs.getString(8)
                        ));
                    }
                }
                return list;
            }
        };

        task.setOnSucceeded(e -> {
            List<PlanModel> plans = task.getValue();
            FlowPane grid = new FlowPane();
            grid.setHgap(20);
            grid.setVgap(20);
            grid.setPadding(new Insets(10));

            for (PlanModel p : plans) {
                boolean isPopular = p.name.toLowerCase().contains("gold") || p.name.toLowerCase().contains("200");
                PlanCard card = new PlanCard(
                    p.id, p.name, p.type, p.speed, p.upload, p.cap, p.price, p.cycle, isPopular,
                    () -> ToastManager.showInfo("Plan selected: " + p.name + " (" + p.speed + " Mbps)")
                );
                grid.getChildren().add(card);
            }

            ScrollPane scroll = new ScrollPane(grid);
            scroll.setFitToWidth(true);
            scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            viewContainer.getChildren().setAll(scroll);
        });

        task.setOnFailed(e -> {
            viewContainer.getChildren().setAll(new EmptyState("Failed to load plans", task.getException().getMessage(), "fas-exclamation-circle"));
        });

        new Thread(task).start();
    }

    private void loadTableView() {
        String sql = "SELECT plan_id, plan_name, plan_type, speed_mbps || ' Mbps' AS download_speed, " +
                     "upload_mbps || ' Mbps' AS upload_speed, NVL(TO_CHAR(data_cap_gb), 'UNLIMITED') AS data_cap, " +
                     "price AS monthly_price, billing_cycle, is_active, description FROM plan ORDER BY price ASC";
        TableViewBuilder.TableContainer table = TableViewBuilder.buildAsyncTable(sql);
        VBox tableCard = new VBox(table);
        tableCard.getStyleClass().add("card");
        tableCard.setFillWidth(true);
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        viewContainer.getChildren().setAll(tableCard);
    }

    private static class PlanModel {
        final int id;
        final String name;
        final String type;
        final int speed;
        final int upload;
        final String cap;
        final double price;
        final String cycle;

        PlanModel(int id, String name, String type, int speed, int upload, String cap, double price, String cycle) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.speed = speed;
            this.upload = upload;
            this.cap = cap;
            this.price = price;
            this.cycle = cycle;
        }
    }
}
