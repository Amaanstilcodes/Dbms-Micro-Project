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
        primaryStage.setTitle("ISP Management System — Oracle 23ai");
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(720);

        showLoginScreen();
        primaryStage.show();
    }

    public static void showLoginScreen() {
        LoginScreen loginScreen = new LoginScreen();
        Scene scene = new Scene(loginScreen, 1200, 720);
        applyStyles(scene);
        primaryStage.setScene(scene);
    }

    public static void showAppScreen() {
        MainLayout layout = new MainLayout();
        Scene scene = new Scene(layout, 1280, 800);
        applyStyles(scene);
        primaryStage.setScene(scene);
    }

    private static void applyStyles(Scene scene) {
        try {
            String themeCss = Main.class.getResource("/css/isp-theme.css").toExternalForm();
            scene.getStylesheets().add(themeCss);
            com.isp.ui.util.ThemeManager.applyInitialTheme(scene);
        } catch (Exception ignored) {}
    }

    public static void main(String[] args) {
        launch(args);
    }
}
