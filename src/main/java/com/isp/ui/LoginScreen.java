package com.isp.ui;

import com.isp.Main;
import com.isp.Session;
import com.isp.dao.UserAuthDao;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class LoginScreen extends VBox {

    public LoginScreen() {
        setAlignment(Pos.CENTER);
        setSpacing(15);
        setPadding(new Insets(30));
        setStyle("-fx-background-color: #f4f6f8;");

        Label title = new Label("ISP Management System");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        title.setStyle("-fx-text-fill: #1e293b;");

        Label subtitle = new Label("Sign in to access your dashboard");
        subtitle.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");

        VBox form = new VBox(10);
        form.setMaxWidth(340);
        form.setStyle("-fx-background-color: white; -fx-padding: 24; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 10, 0, 0, 4);");

        Label userLbl = new Label("Username");
        userLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");
        TextField usernameField = new TextField();
        usernameField.setPromptText("e.g. admin, billing1, support.priya");
        usernameField.setText("admin");

        Label passLbl = new Label("Password");
        passLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter password");
        passwordField.setText("password123");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");
        errorLabel.setWrapText(true);

        Button loginBtn = new Button("Sign In");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 16; -fx-background-radius: 6;");

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setVisible(false);
        spinner.setMaxSize(24, 24);

        Runnable doLogin = () -> {
            String u = usernameField.getText().trim();
            String p = passwordField.getText();
            if (u.isEmpty() || p.isEmpty()) {
                errorLabel.setText("Please enter username and password.");
                return;
            }
            loginBtn.setDisable(true);
            spinner.setVisible(true);
            errorLabel.setText("");

            Task<UserAuthDao.AuthResult> task = new Task<>() {
                @Override
                protected UserAuthDao.AuthResult call() {
                    return UserAuthDao.authenticate(u, p);
                }
            };

            task.setOnSucceeded(ev -> {
                loginBtn.setDisable(false);
                spinner.setVisible(false);
                UserAuthDao.AuthResult res = task.getValue();
                if (res.success()) {
                    Session.login(res.userId(), res.username(), res.role());
                    Main.showAppScreen();
                } else {
                    errorLabel.setText(res.error());
                }
            });

            task.setOnFailed(ev -> {
                loginBtn.setDisable(false);
                spinner.setVisible(false);
                errorLabel.setText("Login request failed: " + task.getException().getMessage());
            });

            new Thread(task).start();
        };

        loginBtn.setOnAction(e -> doLogin.run());
        passwordField.setOnAction(e -> doLogin.run());

        form.getChildren().addAll(userLbl, usernameField, passLbl, passwordField, errorLabel, loginBtn, spinner);
        getChildren().addAll(title, subtitle, form);
    }
}
