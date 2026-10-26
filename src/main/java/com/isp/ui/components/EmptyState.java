package com.isp.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

public class EmptyState extends VBox {

    public EmptyState(String title, String subtitle, String iconLiteral) {
        getStyleClass().add("empty-state");
        setAlignment(Pos.CENTER);

        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconSize(48);
        icon.setStyle("-fx-icon-color: -color-muted;");

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("empty-state-title");

        Label subLbl = new Label(subtitle);
        subLbl.getStyleClass().add("empty-state-subtitle");

        getChildren().addAll(icon, titleLbl, subLbl);
    }
}
