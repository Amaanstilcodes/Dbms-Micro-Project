package com.isp.controller;

import com.isp.util.SceneManager;
import com.isp.util.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;

/**
 * Controller for Login.fxml.
 * Validates credentials against the `user` table, populates SessionManager,
 * then navigates to the Dashboard.
 *
 * TODO: inject a UserDAO and call userDAO.authenticate(username, password).
 */
public class LoginController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;
    @FXML private Button        loginButton;

    @FXML
    private void initialize() {
        errorLabel.setVisible(false);
        // Allow Enter key on password field to trigger login
        passwordField.setOnAction(this::handleLogin);
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Username and password are required.");
            return;
        }

        // --- TODO: replace with real DAO call ---
        // UserDAO userDAO = DAOFactory.getUserDAO();
        // Optional<User> user = userDAO.authenticate(username, password);
        // For skeleton, accept admin / admin as demo credentials
        if ("admin".equals(username) && "admin".equals(password)) {
            SessionManager.login(1, username, "ADMIN", null, null);
            navigateToDashboard();
        } else {
            showError("Invalid username or password.");
        }
    }

    private void navigateToDashboard() {
        try {
            SceneManager.switchTo("Dashboard.fxml");
        } catch (IOException e) {
            showError("Failed to load dashboard: " + e.getMessage());
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
