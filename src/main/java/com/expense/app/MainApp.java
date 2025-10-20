// File: src/main/java/com/expense/app/MainApp.java

package com.expense.app;

import com.expense.db.DatabaseUpdater; // Import the new class
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // --- THIS IS THE FIX ---
        // Run the database schema update BEFORE loading any UI.
        // This guarantees the database is correct and prevents "no such table" errors.
        DatabaseUpdater.updateSchema();
        // --- END OF FIX ---

        // The rest of the startup process remains the same
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/splash.fxml"));
        VBox root = loader.load();
        Scene scene = new Scene(root);

        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}