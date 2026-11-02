package com.isp.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

public class KpiTile extends VBox {
    private final Label titleLbl;
    private final Label valueLbl;
    private final Label subLbl;
    private final Label deltaLbl;
    private final FontIcon icon;
    private final HBox iconWrap;

    public KpiTile(String title, String value, String subtitle, String iconLiteral, String iconColorHex) {
        this(title, value, subtitle, null, iconLiteral, iconColorHex);
    }

    public KpiTile(String title, String value, String subtitle, String deltaText, String iconLiteral, String iconColorHex) {
        getStyleClass().add("kpi-tile");
        setSpacing(6);
        setFillWidth(true);
        setMaxWidth(Double.MAX_VALUE);

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        titleLbl = new Label(title);
        titleLbl.getStyleClass().add("kpi-title");
        titleLbl.setWrapText(true);
        titleLbl.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(titleLbl, Priority.ALWAYS);

        Region spacer = new Region();
        spacer.setMinWidth(4);

        icon = new FontIcon(iconLiteral);
        icon.setIconSize(15);
        icon.setStyle("-fx-icon-color: " + iconColorHex + ";");

        iconWrap = new HBox(icon);
        iconWrap.getStyleClass().add("kpi-icon-wrap");
        iconWrap.setStyle("-fx-background-color: " + iconColorHex + "22; -fx-padding: 6;");

        topRow.getChildren().addAll(titleLbl, spacer, iconWrap);

        valueLbl = new Label(value);
        valueLbl.getStyleClass().add("kpi-value");

        HBox subRow = new HBox(6);
        subRow.setAlignment(Pos.CENTER_LEFT);

        subLbl = new Label(subtitle);
        subLbl.getStyleClass().add("kpi-subtitle");

        deltaLbl = new Label();
        deltaLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 700;");

        setDelta(deltaText);

        subRow.getChildren().addAll(subLbl, deltaLbl);

        getChildren().addAll(topRow, valueLbl, subRow);
    }

    public void setValue(String value) {
        valueLbl.setText(value);
    }

    public void setSubtitle(String subtitle) {
        subLbl.setText(subtitle);
    }

    public void setDelta(String deltaText) {
        if (deltaText == null || deltaText.isBlank()) {
            deltaLbl.setVisible(false);
            deltaLbl.setManaged(false);
        } else {
            deltaLbl.setVisible(true);
            deltaLbl.setManaged(true);
            deltaLbl.setText(deltaText);
            if (deltaText.startsWith("+")) {
                deltaLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #16A34A;");
            } else if (deltaText.startsWith("-") && !deltaText.equals("-")) {
                deltaLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #DC2626;");
            } else {
                deltaLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #64748B;");
            }
        }
    }
}
