package com.isp.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class PlansScreen extends VBox {

    public PlansScreen() {
        setSpacing(16);
        setPadding(new Insets(20));

        Label title = new Label("Service Plans");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));

        String sql = "SELECT plan_id, plan_name, plan_type, speed_mbps || ' Mbps' AS download_speed, " +
                     "upload_mbps || ' Mbps' AS upload_speed, NVL(TO_CHAR(data_cap_gb), 'UNLIMITED') AS data_cap, " +
                     "'₹' || price AS monthly_price, billing_cycle, is_active, description FROM plan ORDER BY price ASC";

        StackPane table = TableViewBuilder.buildAsyncTable(sql);
        VBox.setVgrow(table, Priority.ALWAYS);

        getChildren().addAll(title, table);
    }
}
