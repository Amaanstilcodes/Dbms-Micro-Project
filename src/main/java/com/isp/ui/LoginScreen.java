package com.isp.ui;

import com.isp.Main;
import com.isp.Session;
import com.isp.dao.UserAuthDao;
import com.isp.ui.components.ToastManager;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

public class LoginScreen extends StackPane {

    public LoginScreen() {
        HBox container = new HBox();
        container.setAlignment(Pos.CENTER);

        // Left Branded Panel (Gradient, Telecom aesthetic)
        VBox leftBrand = new VBox(20);
        leftBrand.setAlignment(Pos.CENTER_LEFT);
        leftBrand.setPadding(new Insets(50));
        leftBrand.setPrefWidth(420);
        leftBrand.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #1E3A8A, #2563EB, #06B6D4);"
        );
        HBox.setHgrow(leftBrand, Priority.ALWAYS);

        FontIcon brandIcon = new FontIcon("fas-network-wired");
        brandIcon.setIconSize(48);
        brandIcon.setStyle("-fx-icon-color: #FFFFFF;");

        Label brandTitle = new Label("ISP Management");
        brandTitle.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");

        Label brandTagline = new Label("Enterprise Telecom Operations & Oracle 23ai Control Center");
        brandTagline.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(255, 255, 255, 0.85);");
        brandTagline.setWrapText(true);

        VBox featureList = new VBox(12);
        featureList.setPadding(new Insets(30, 0, 0, 0));
        featureList.getChildren().addAll(
            createBullet("fas-check-circle", "Subscribers & high-speed FTTH plan contracts"),
            createBullet("fas-check-circle", "Automated PL/SQL billing & 18% GST invoicing"),
            createBullet("fas-check-circle", "Field installation & support ticket resolution")
        );

        leftBrand.getChildren().addAll(brandIcon, brandTitle, brandTagline, featureList);

        // Right Panel (Clean Auth Card)
        VBox rightAuth = new VBox(20);
        rightAuth.setAlignment(Pos.CENTER);
        rightAuth.setPadding(new Insets(40, 60, 40, 60));
        rightAuth.setPrefWidth(480);
        rightAuth.setStyle("-fx-background-color: -color-surface;");
        HBox.setHgrow(rightAuth, Priority.ALWAYS);

        VBox loginCard = new VBox(16);
        loginCard.setMaxWidth(360);

        Label welcomeLbl = new Label("Welcome back");
        welcomeLbl.getStyleClass().add("page-title");

        Label subLbl = new Label("Enter your credentials to access your console");
        subLbl.getStyleClass().add("page-subtitle");

        Label userLabel = new Label("Username");
        userLabel.getStyleClass().add("form-label");

        TextField usernameField = new TextField("admin");
        usernameField.setPromptText("Enter username");

        Label passLabel = new Label("Password");
        passLabel.getStyleClass().add("form-label");

        // Password with eye show/hide toggle
        PasswordField passwordField = new PasswordField();
        passwordField.setText("password123");
        passwordField.setPromptText("Enter password");

        TextField passwordVisibleField = new TextField();
        passwordVisibleField.setPromptText("Enter password");
        passwordVisibleField.setManaged(false);
        passwordVisibleField.setVisible(false);

        Button toggleEyeBtn = new Button();
        toggleEyeBtn.getStyleClass().add("btn-icon");
        FontIcon eyeIcon = new FontIcon("fas-eye");
        eyeIcon.setIconSize(14);
        eyeIcon.setStyle("-fx-icon-color: -color-muted;");
        toggleEyeBtn.setGraphic(eyeIcon);

        toggleEyeBtn.setOnAction(e -> {
            if (passwordField.isVisible()) {
                passwordVisibleField.setText(passwordField.getText());
                passwordField.setVisible(false);
                passwordField.setManaged(false);
                passwordVisibleField.setVisible(true);
                passwordVisibleField.setManaged(true);
                eyeIcon.setIconLiteral("fas-eye-slash");
            } else {
                passwordField.setText(passwordVisibleField.getText());
                passwordVisibleField.setVisible(false);
                passwordVisibleField.setManaged(false);
                passwordField.setVisible(true);
                passwordField.setManaged(true);
                eyeIcon.setIconLiteral("fas-eye");
            }
        });

        HBox passBox = new HBox(passwordField, passwordVisibleField, toggleEyeBtn);
        HBox.setHgrow(passwordField, Priority.ALWAYS);
        HBox.setHgrow(passwordVisibleField, Priority.ALWAYS);
        passBox.setAlignment(Pos.CENTER_RIGHT);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("form-error");
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        Button loginBtn = new Button("Sign In");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.getStyleClass().add("btn-primary");
        loginBtn.setStyle("-fx-padding: 10 16; -fx-font-size: 14px;");

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(22, 22);
        spinner.setVisible(false);
        spinner.setManaged(false);

        HBox submitBox = new HBox(10, loginBtn, spinner);
        submitBox.setAlignment(Pos.CENTER);

        loginCard.getChildren().addAll(
            welcomeLbl, subLbl,
            userLabel, usernameField,
            passLabel, passBox,
            errorLabel,
            submitBox
        );

        // Shake animation for login card on error
        Runnable shakeCard = () -> {
            Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(0), new KeyValue(loginCard.translateXProperty(), 0)),
                new KeyFrame(Duration.millis(50), new KeyValue(loginCard.translateXProperty(), -10)),
                new KeyFrame(Duration.millis(100), new KeyValue(loginCard.translateXProperty(), 10)),
                new KeyFrame(Duration.millis(150), new KeyValue(loginCard.translateXProperty(), -10)),
                new KeyFrame(Duration.millis(200), new KeyValue(loginCard.translateXProperty(), 10)),
                new KeyFrame(Duration.millis(250), new KeyValue(loginCard.translateXProperty(), 0))
            );
            timeline.play();
        };

        Runnable doLogin = () -> {
            String u = usernameField.getText().trim();
            String p = passwordField.isVisible() ? passwordField.getText() : passwordVisibleField.getText();

            if (u.isEmpty() || p.isEmpty()) {
                errorLabel.setText("Please enter username and password.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                shakeCard.run();
                return;
            }

            loginBtn.setDisable(true);
            spinner.setVisible(true);
            spinner.setManaged(true);
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);

            Task<UserAuthDao.AuthResult> task = new Task<>() {
                @Override
                protected UserAuthDao.AuthResult call() {
                    return UserAuthDao.authenticate(u, p);
                }
            };

            task.setOnSucceeded(ev -> {
                loginBtn.setDisable(false);
                spinner.setVisible(false);
                spinner.setManaged(false);
                UserAuthDao.AuthResult res = task.getValue();
                if (res.success()) {
                    Session.login(res.userId(), res.username(), res.role());
                    Main.showAppScreen();
                } else {
                    errorLabel.setText(res.error());
                    errorLabel.setVisible(true);
                    errorLabel.setManaged(true);
                    shakeCard.run();
                }
            });

            task.setOnFailed(ev -> {
                loginBtn.setDisable(false);
                spinner.setVisible(false);
                spinner.setManaged(false);
                errorLabel.setText("Login request failed: " + task.getException().getMessage());
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                shakeCard.run();
            });

            new Thread(task).start();
        };

        loginBtn.setOnAction(e -> doLogin.run());
        passwordField.setOnAction(e -> doLogin.run());
        passwordVisibleField.setOnAction(e -> doLogin.run());
        usernameField.setOnAction(e -> doLogin.run());

        rightAuth.getChildren().add(loginCard);
        container.getChildren().addAll(leftBrand, rightAuth);

        getChildren().add(container);
        ToastManager.bindContainer(this);
    }

    private HBox createBullet(String iconLiteral, String text) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconSize(16);
        icon.setStyle("-fx-icon-color: #67E8F9;");
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #E2E8F0; -fx-font-size: 13px;");
        box.getChildren().addAll(icon, lbl);
        return box;
    }
}
