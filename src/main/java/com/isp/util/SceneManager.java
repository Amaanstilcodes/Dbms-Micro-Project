package com.isp.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Utility class for navigating between screens.
 * Each screen swap calls SceneManager.switchTo(stage, "ViewName.fxml").
 */
public class SceneManager {

    private static Stage primaryStage;

    public static void init(Stage stage) {
        primaryStage = stage;
    }

    /**
     * Replaces the current scene with the FXML at /com/isp/views/{fxmlName}.
     *
     * @param fxmlName e.g. "Dashboard.fxml"
     */
    public static void switchTo(String fxmlName) throws IOException {
        switchTo(primaryStage, fxmlName);
    }

    public static void switchTo(Stage stage, String fxmlName) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                SceneManager.class.getResource("/com/isp/views/" + fxmlName));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(
                SceneManager.class.getResource("/com/isp/css/style.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }
}
