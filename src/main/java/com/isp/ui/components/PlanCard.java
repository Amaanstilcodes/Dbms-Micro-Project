package com.isp.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

public class PlanCard extends VBox {

    public PlanCard(int planId, String planName, String planType, int speedMbps, int uploadMbps,
                    String dataCap, double price, String cycle, boolean isPopular, Runnable onSelect) {
        getStyleClass().addAll("card", "card-hover");
        setPrefWidth(240);
        setMaxWidth(260);
        setSpacing(14);

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label nameLbl = new Label(planName);
        nameLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-text;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topRow.getChildren().addAll(nameLbl, spacer);

        if (isPopular) {
            Label popularBadge = new Label("POPULAR");
            popularBadge.getStyleClass().addAll("badge", "badge-success");
            topRow.getChildren().add(popularBadge);
        }

        Label typeLbl = new Label(planType);
        typeLbl.getStyleClass().addAll("badge", "badge-info");

        HBox speedBox = new HBox(6);
        speedBox.setAlignment(Pos.BASELINE_LEFT);

        Label speedVal = new Label(String.valueOf(speedMbps));
        speedVal.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: -color-primary;");

        Label speedUnit = new Label("Mbps download");
        speedUnit.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-muted;");

        speedBox.getChildren().addAll(speedVal, speedUnit);

        VBox specs = new VBox(8);
        specs.getChildren().addAll(
            createSpecRow("fas-arrow-up", uploadMbps + " Mbps upload"),
            createSpecRow("fas-database", dataCap + (dataCap.matches("\\d+") ? " GB data cap" : "")),
            createSpecRow("fas-sync-alt", cycle + " billing")
        );

        Separator sep = new Separator();

        HBox priceBox = new HBox(4);
        priceBox.setAlignment(Pos.BASELINE_LEFT);

        Label rupee = new Label("₹");
        rupee.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-text;");

        Label priceVal = new Label(String.format("%.0f", price));
        priceVal.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: -color-text;");

        Label cycleLbl = new Label(" / " + cycle.toLowerCase());
        cycleLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-muted;");

        priceBox.getChildren().addAll(rupee, priceVal, cycleLbl);

        Button selectBtn = new Button("Choose Plan");
        selectBtn.setMaxWidth(Double.MAX_VALUE);
        selectBtn.getStyleClass().add(isPopular ? "btn-primary" : "btn-secondary");
        if (onSelect != null) {
            selectBtn.setOnAction(e -> onSelect.run());
        }

        getChildren().addAll(topRow, typeLbl, speedBox, specs, sep, priceBox, selectBtn);
    }

    private HBox createSpecRow(String iconLiteral, String text) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconSize(13);
        icon.setStyle("-fx-icon-color: -color-muted;");

        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-secondary;");

        row.getChildren().addAll(icon, lbl);
        return row;
    }
}
