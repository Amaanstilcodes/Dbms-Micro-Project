package com.isp.ui.components;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

public class ToastManager {
    private static VBox toastContainer;

    public static void bindContainer(Pane parent) {
        if (toastContainer == null) {
            toastContainer = new VBox(10);
            toastContainer.setAlignment(Pos.TOP_RIGHT);
            toastContainer.setMouseTransparent(false);
            toastContainer.setPickOnBounds(false);
            toastContainer.setStyle("-fx-padding: 20;");
        }
        if (!parent.getChildren().contains(toastContainer)) {
            parent.getChildren().add(toastContainer);
        }
    }

    public static void showSuccess(String message) {
        show(message, "toast-success", "fas-check-circle", "#16A34A");
    }

    public static void showError(String message) {
        show(message, "toast-error", "fas-exclamation-circle", "#DC2626");
    }

    public static void showInfo(String message) {
        show(message, "toast-info", "fas-info-circle", "#0284C7");
    }

    private static void show(String message, String styleClass, String iconCode, String iconColor) {
        Platform.runLater(() -> {
            if (toastContainer == null) return;

            HBox toast = new HBox(12);
            toast.getStyleClass().addAll("toast", styleClass);
            toast.setAlignment(Pos.CENTER_LEFT);

            FontIcon icon = new FontIcon(iconCode);
            icon.setIconSize(18);
            icon.setStyle("-fx-icon-color: " + iconColor + ";");

            Label msgLabel = new Label(message);
            msgLabel.setWrapText(true);
            msgLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: -color-text;");

            toast.getChildren().addAll(icon, msgLabel);

            toast.setOpacity(0.0);
            toastContainer.getChildren().add(0, toast);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(250), toast);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);

            PauseTransition stay = new PauseTransition(Duration.seconds(3.5));

            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toast);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> toastContainer.getChildren().remove(toast));

            SequentialTransition seq = new SequentialTransition(fadeIn, stay, fadeOut);
            seq.play();
        });
    }
}
