// File: src/main/java/com/expense/app/MainApp.java

package com.expense.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.net.URL;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        BorderPane root = loader.load();
        Scene scene = new Scene(root, 1200, 768); // A slightly larger default size

        // --- THIS IS THE CRITICAL PART ---
        // We ensure the CSS file is loaded.
        URL cssUrl = getClass().getResource("/style.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
            System.out.println("SUCCESS: Stylesheet loaded successfully.");
        } else {
            System.err.println("!!! FATAL ERROR: Cannot find stylesheet 'style.css'. Make sure it's in src/main/resources/");
        }
        // --- END OF CRITICAL PART ---

        primaryStage.setTitle("Smart Expense Analyzer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}