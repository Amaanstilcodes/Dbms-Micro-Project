package com.isp.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

public class KpiTile extends VBox {

    public KpiTile(String title, String value, String subtitle, String iconLiteral, String iconColorHex) {
        getStyleClass().add("kpi-tile");
        setSpacing(6);

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("kpi-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconSize(18);
        icon.setStyle("-fx-icon-color: " + iconColorHex + ";");

        HBox iconWrap = new HBox(icon);
        iconWrap.getStyleClass().add("kpi-icon-wrap");
        iconWrap.setStyle("-fx-background-color: " + iconColorHex + "22;");

        topRow.getChildren().addAll(titleLbl, spacer, iconWrap);

        Label valueLbl = new Label(value);
        valueLbl.getStyleClass().add("kpi-value");

        Label subLbl = new Label(subtitle);
        subLbl.getStyleClass().add("kpi-subtitle");

        getChildren().addAll(topRow, valueLbl, subLbl);
    }
}
