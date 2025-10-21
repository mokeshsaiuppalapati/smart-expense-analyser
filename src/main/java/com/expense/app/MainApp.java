// File: src/main/java/com/expense/app/MainApp.java

package com.expense.app;

import com.expense.db.DatabaseUpdater;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Run the database schema update before loading any UI.
        DatabaseUpdater.updateSchema();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/splash.fxml"));
        VBox root = loader.load();

        // --- THIS IS THE FIX ---
        // Create the scene with a fixed, larger size (e.g., 800x500).
        Scene scene = new Scene(root, 800, 500);
        // --- END OF FIX ---

        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setScene(scene);

        // This ensures the splash screen appears in the middle of the monitor.
        primaryStage.centerOnScreen();

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}