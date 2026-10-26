package com.isp.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

public class StepIndicator extends HBox {
    private final int totalSteps;
    private final HBox[] stepNodes;

    public StepIndicator(String... stepTitles) {
        this.totalSteps = stepTitles.length;
        this.stepNodes = new HBox[totalSteps];
        setAlignment(Pos.CENTER);
        setSpacing(14);
        setStyle("-fx-padding: 10 0 20 0;");

        for (int i = 0; i < totalSteps; i++) {
            HBox stepBox = new HBox(8);
            stepBox.setAlignment(Pos.CENTER);

            Label badge = new Label(String.valueOf(i + 1));
            badge.setStyle("-fx-background-color: -color-divider; -fx-text-fill: -color-muted; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 14;");

            Label title = new Label(stepTitles[i]);
            title.setStyle("-fx-font-weight: 600; -fx-text-fill: -color-muted;");

            stepBox.getChildren().addAll(badge, title);
            stepNodes[i] = stepBox;
            getChildren().add(stepBox);

            if (i < totalSteps - 1) {
                FontIcon arrow = new FontIcon("fas-chevron-right");
                arrow.setIconSize(12);
                arrow.setStyle("-fx-icon-color: -color-muted;");
                getChildren().add(arrow);
            }
        }
        setStep(1);
    }

    public void setStep(int step) {
        for (int i = 0; i < totalSteps; i++) {
            HBox box = stepNodes[i];
            Label badge = (Label) box.getChildren().get(0);
            Label title = (Label) box.getChildren().get(1);

            if (i + 1 == step) {
                badge.setStyle("-fx-background-color: -color-primary; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 14;");
                title.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-primary;");
            } else if (i + 1 < step) {
                badge.setStyle("-fx-background-color: -color-success; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 14;");
                title.setStyle("-fx-font-weight: 600; -fx-text-fill: -color-success;");
            } else {
                badge.setStyle("-fx-background-color: -color-divider; -fx-text-fill: -color-muted; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 14;");
                title.setStyle("-fx-font-weight: 600; -fx-text-fill: -color-muted;");
            }
        }
    }
}
