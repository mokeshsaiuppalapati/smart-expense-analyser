// File: src/main/java/com/expense/ui/SplashController.java
package com.expense.ui;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class SplashController {
    @FXML
    private VBox root;

    @FXML
    public void initialize() {
        // Wait for 2 seconds, then transition to the login screen
        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(event -> transitionToLogin());
        pause.play();
    }

    private void transitionToLogin() {
        try {
            Stage currentStage = (Stage) root.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            VBox loginRoot = loader.load();
            Scene loginScene = new Scene(loginRoot);

            // Make sure the new scene gets the stylesheet
            loginScene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

            Stage loginStage = new Stage();
            loginStage.setTitle("Login");
            loginStage.setScene(loginScene);

            currentStage.close(); // Close splash screen
            loginStage.show();    // Show login screen

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}