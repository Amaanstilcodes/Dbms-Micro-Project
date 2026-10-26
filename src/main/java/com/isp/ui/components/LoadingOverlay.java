package com.isp.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;

public class LoadingOverlay extends StackPane {

    public LoadingOverlay() {
        setStyle("-fx-background-color: rgba(255, 255, 255, 0.65);");
        setAlignment(Pos.CENTER);
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(40, 40);
        getChildren().add(spinner);
        setVisible(false);
    }

    public void show() {
        setVisible(true);
        toFront();
    }

    public void hide() {
        setVisible(false);
    }
}
