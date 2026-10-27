package com.isp.ui.components;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import org.kordamp.ikonli.javafx.FontIcon;

public class ConfirmDialog extends Dialog<Boolean> {

    public ConfirmDialog(String title, String message, String confirmButtonText, boolean isDanger) {
        initModality(Modality.APPLICATION_MODAL);
        setTitle(title);
        setHeaderText(null);

        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Node defaultBar = getDialogPane().lookup(".button-bar");
        if (defaultBar != null) {
            defaultBar.setVisible(false);
            defaultBar.setManaged(false);
        }

        getDialogPane().addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                setResult(false);
                close();
                event.consume();
            }
        });

        VBox content = new VBox(16);
        content.getStyleClass().add("card");
        content.setPrefWidth(380);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        FontIcon icon = new FontIcon(isDanger ? "fas-exclamation-triangle" : "fas-question-circle");
        icon.setIconSize(24);
        icon.setStyle("-fx-icon-color: " + (isDanger ? "#DC2626" : "#2563EB") + ";");

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-text;");

        header.getChildren().addAll(icon, titleLbl);

        Label msgLbl = new Label(message);
        msgLbl.setWrapText(true);
        msgLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-text-secondary;");

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> setResult(false));

        Button actionBtn = new Button(confirmButtonText);
        actionBtn.getStyleClass().add(isDanger ? "btn-danger" : "btn-primary");
        actionBtn.setOnAction(e -> setResult(true));

        btnRow.getChildren().addAll(cancelBtn, actionBtn);
        content.getChildren().addAll(header, msgLbl, btnRow);

        getDialogPane().setContent(content);
        getDialogPane().getStylesheets().addAll(
            getClass().getResource("/css/isp-theme.css").toExternalForm()
        );
    }
}
