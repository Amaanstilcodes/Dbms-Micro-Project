package com.isp;

import com.isp.util.SceneManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Main entry point for the ISP Management System JavaFX application.
 * Loads the Login screen on startup.
 * <p>
 * IMPORTANT: SceneManager.init(primaryStage) must be called here so that
 * every controller can switch screens via SceneManager.switchTo().
 * Without it, primaryStage inside SceneManager is null and any scene switch
 * silently throws a NullPointerException.
 * </p>
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Register the stage FIRST so SceneManager can use it from any controller.
        SceneManager.init(primaryStage);

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/isp/views/Login.fxml"));
        Scene scene = new Scene(loader.load(), 900, 600);
        scene.getStylesheets().add(
                getClass().getResource("/com/isp/css/style.css").toExternalForm());

        primaryStage.setTitle("ISP Management System");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
