package com.isp;

import com.isp.ui.LoginScreen;
import com.isp.ui.MainLayout;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle("ISP Management System");
        primaryStage.setMinWidth(960);
        primaryStage.setMinHeight(640);

        showLoginScreen();
        primaryStage.show();
    }

    public static void showLoginScreen() {
        LoginScreen loginScreen = new LoginScreen();
        Scene scene = new Scene(loginScreen, 960, 640);
        applyStyles(scene);
        primaryStage.setScene(scene);
    }

    public static void showAppScreen() {
        MainLayout layout = new MainLayout();
        Scene scene = new Scene(layout, 1100, 700);
        applyStyles(scene);
        primaryStage.setScene(scene);
    }

    private static void applyStyles(Scene scene) {
        try {
            String css = Main.class.getResource("/css/app.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception ignored) {}
    }

    public static void main(String[] args) {
        launch(args);
    }
}
