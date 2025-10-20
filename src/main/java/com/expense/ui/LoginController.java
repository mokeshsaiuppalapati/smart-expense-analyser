// File: src/main/java/com/expense/ui/LoginController.java

package com.expense.ui;

import com.expense.service.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    private final AuthService authService = new AuthService();

    @FXML
    private void onLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Username and password cannot be empty.");
            return;
        }

        try {
            if (authService.login(username, password)) {
                openMainApplication();
            } else {
                showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid username or password.");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "An error occurred during login: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onSignup() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Username and password cannot be empty.");
            return;
        }
        if (password.length() < 4) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Password must be at least 4 characters long.");
            return;
        }

        try {
            if (authService.signup(username, password)) {
                showAlert(Alert.AlertType.INFORMATION, "Signup Successful", "Account created successfully! Please log in.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Signup Failed", "Username already exists. Please choose another one.");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "An error occurred during signup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void openMainApplication() {
        try {
            Stage currentStage = (Stage) usernameField.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            BorderPane mainRoot = loader.load();

            // Get the controller and initialize it AFTER login
            MainController mainController = loader.getController();
            mainController.postLoginInit();

            Scene mainScene = new Scene(mainRoot, 1200, 768);
            mainScene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

            Stage mainStage = new Stage();
            mainStage.setTitle("Smart Expense Analyser");
            mainStage.setScene(mainScene);

            currentStage.close();
            mainStage.show();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Application Error", "Failed to load the main application window.");
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type, content, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}