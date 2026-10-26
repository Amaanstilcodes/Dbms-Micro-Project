package com.isp.controller;

import com.isp.util.SceneManager;
import com.isp.util.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

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
        // Always clear any stale error from a previous attempt first.
        errorLabel.setVisible(false);

        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Username and password are required.");
            return;
        }

        // --- Demo / development credentials (no database required) ---
        // Replace this block with a real UserDAO call once the DB is configured:
        //   UserDAO userDAO = DAOFactory.getUserDAO();
        //   Optional<User> user = userDAO.authenticate(username, password);
        if ("admin".equals(username) && "admin".equals(password)) {
            SessionManager.login(1, username, "ADMIN", null, null);
            navigateToDashboard();
        } else {
            showError("Invalid username or password.");
        }
    }

    /**
     * Switches to the Dashboard scene.
     * Catches all Throwable (not just IOException) so that a NullPointerException
     * or other RuntimeException from SceneManager never freezes the screen silently.
     */
    private void navigateToDashboard() {
        try {
            SceneManager.switchTo("Dashboard.fxml");
        } catch (Throwable t) {
            showError("Failed to open dashboard: " + t.getMessage()
                    + "  (check console for details)");
            t.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
